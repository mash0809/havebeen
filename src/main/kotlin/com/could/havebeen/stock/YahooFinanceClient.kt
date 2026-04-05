package com.could.havebeen.stock

import com.could.havebeen.stock.model.StockPrice
import com.could.havebeen.stock.model.StockSearchResult
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Yahoo Finance v8 Chart API를 통해 주가 데이터를 조회한다.
 *
 * 한국 주식은 심볼 뒤에 거래소 접미사를 붙인다 (예: 삼성전자 → 005930.KS).
 * 조회 결과는 Caffeine 캐시에 저장되어 동일 요청의 반복 호출을 방지한다.
 */
@Component
class YahooFinanceClient(
    // Spring Boot의 자동 구성된 RestClient.Builder를 주입받아 공통 설정(타임아웃 등)을 활용
    builder: RestClient.Builder,
) {

    private val log = LoggerFactory.getLogger(YahooFinanceClient::class.java)

    // Yahoo Finance 비공개 API는 브라우저 User-Agent를 요구하므로 명시적으로 설정
    private val restClient = builder
        .baseUrl("https://query1.finance.yahoo.com")
        .defaultHeader("User-Agent", "Mozilla/5.0")
        .build()

    /**
     * 주어진 기간의 일별 종가 목록을 반환한다.
     *
     * 캐시 키는 symbol + startDate + endDate 조합으로, TTL은 application.yaml의 캐시 설정을 따른다.
     * 종가 데이터가 null인 날(거래 정지, 공휴일 등)은 결과 목록에서 제외된다.
     */
    @Cacheable("stockPrices", key = "#symbol + '_' + #startDate + '_' + #endDate")
    fun fetchPrices(symbol: String, startDate: LocalDate, endDate: LocalDate): List<StockPrice> {
        // Yahoo Finance API는 기간을 Unix epoch 초(second) 단위로 받는다
        val period1 = startDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond()
        val period2 = endDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond()
        log.debug("Yahoo Finance API 호출: symbol={}, period1={}, period2={}", symbol, period1, period2)

        val response = try {
            restClient.get()
                .uri("/v8/finance/chart/{symbol}?interval=1d&period1={p1}&period2={p2}",
                    symbol, period1, period2,)
                .retrieve()
                .body(JsonNode::class.java)
        } catch (e: RestClientException) {
            log.error("Yahoo Finance API 오류: symbol={}", symbol, e)
            throw StockNotFoundException("심볼 '$symbol'의 데이터를 Yahoo Finance에서 가져올 수 없습니다.", e)
        } ?: throw StockNotFoundException("심볼 '$symbol'에 대한 응답이 없습니다.")

        // 응답 구조: { chart: { result: [ { timestamp: [...], indicators: { quote: [{ close: [...] }] } } ] } }
        val result = response.path("chart").path("result")
        if (result.isMissingNode || result.isEmpty || result.isNull) {
            throw StockNotFoundException("심볼 '$symbol'을 찾을 수 없습니다.")
        }

        val resultNode = result[0]
        val timestamps = resultNode.path("timestamp")
        val closes = resultNode.path("indicators").path("quote")[0].path("close")

        if (timestamps.isEmpty || closes.isEmpty) {
            throw StockNotFoundException("심볼 '$symbol'의 기간 내 데이터가 없습니다.")
        }

        // timestamps[i]와 closes[i]는 같은 날짜에 대응한다
        val prices = timestamps.mapIndexed { i, tsNode ->
            val closeNode = closes[i]
            // 거래 정지 등으로 종가가 없는 날은 null 반환 후 filterNotNull로 제거
            if (closeNode.isNull || closeNode.isMissingNode) return@mapIndexed null
            val date = Instant.ofEpochSecond(tsNode.asLong())
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            StockPrice(date = date, closePrice = BigDecimal.valueOf(closeNode.asDouble()))
        }.filterNotNull()
        log.debug("Yahoo Finance 응답 수신: symbol={}, 데이터 건수={}", symbol, prices.size)
        return prices
    }

    /**
     * 종목 심볼/이름 검색 결과를 반환한다.
     *
     * Yahoo Finance Search API를 호출하며, 오류 발생 시 빈 목록을 반환한다.
     * 검색 실패가 시뮬레이션 흐름을 막지 않도록 예외를 전파하지 않는다.
     */
    fun searchSymbols(query: String): List<StockSearchResult> {
        log.debug("Yahoo Finance 종목 검색 호출: query={}", query)
        val response = try {
            restClient.get()
                .uri(
                    "/v1/finance/search?q={q}&quotesCount=8&newsCount=0&enableFuzzyQuery=false&lang=ko-KR",
                    query,
                )
                .retrieve()
                .body(JsonNode::class.java)
        } catch (e: RestClientException) {
            log.warn("Yahoo Finance 종목 검색 오류: query={}", query, e)
            return emptyList()
        } ?: return emptyList()

        // 응답 구조: { quotes: [ { symbol, shortname, longname, exchange }, ... ] }
        val quotes = response.path("quotes")
        if (quotes.isMissingNode || quotes.isNull) return emptyList()

        return quotes.mapNotNull { node ->
            val symbol = node.path("symbol").asText(null) ?: return@mapNotNull null
            // shortname이 없으면 longname으로 대체
            val name = node.path("shortname").asText(null)
                ?: node.path("longname").asText(null)
                ?: return@mapNotNull null
            val exchange = node.path("exchange").asText("")
            StockSearchResult(symbol = symbol, name = name, exchange = exchange)
        }
    }
}

class StockNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
