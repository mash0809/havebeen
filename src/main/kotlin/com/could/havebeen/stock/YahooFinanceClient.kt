package com.could.havebeen.stock

import com.could.havebeen.stock.model.StockPrice
import com.fasterxml.jackson.databind.JsonNode
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

        val response = try {
            restClient.get()
                .uri("/v8/finance/chart/{symbol}?interval=1d&period1={p1}&period2={p2}",
                    symbol, period1, period2,)
                .retrieve()
                .body(JsonNode::class.java)
        } catch (e: RestClientException) {
            throw StockNotFoundException("심볼 '$symbol'의 데이터를 Yahoo Finance에서 가져올 수 없습니다.")
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
        return timestamps.mapIndexed { i, tsNode ->
            val closeNode = closes[i]
            // 거래 정지 등으로 종가가 없는 날은 null 반환 후 filterNotNull로 제거
            if (closeNode.isNull || closeNode.isMissingNode) return@mapIndexed null
            val date = Instant.ofEpochSecond(tsNode.asLong())
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            StockPrice(date = date, closePrice = BigDecimal.valueOf(closeNode.asDouble()))
        }.filterNotNull()
    }
}

class StockNotFoundException(message: String) : RuntimeException(message)
