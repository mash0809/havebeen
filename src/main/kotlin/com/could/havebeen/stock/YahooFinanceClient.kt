package com.could.havebeen.stock

import com.could.havebeen.stock.model.StockPrice
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

@Component
class YahooFinanceClient {

    private val restClient = RestClient.builder()
        .baseUrl("https://query1.finance.yahoo.com")
        .defaultHeader("User-Agent", "Mozilla/5.0")
        .build()

    @Cacheable("stockPrices", key = "#symbol + '_' + #startDate + '_' + #endDate")
    fun fetchPrices(symbol: String, startDate: LocalDate, endDate: LocalDate): List<StockPrice> {
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

        return timestamps.mapIndexed { i, tsNode ->
            val closeNode = closes[i]
            if (closeNode.isNull || closeNode.isMissingNode) return@mapIndexed null
            val date = LocalDate.ofEpochDay(tsNode.asLong() / 86400)
            StockPrice(date = date, closePrice = BigDecimal.valueOf(closeNode.asDouble()))
        }.filterNotNull()
    }
}

class StockNotFoundException(message: String) : RuntimeException(message)
