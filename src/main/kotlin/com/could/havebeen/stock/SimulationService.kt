package com.could.havebeen.stock

import com.could.havebeen.stock.model.ChartPoint
import com.could.havebeen.stock.model.SimulationResult
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class SimulationService(
    private val yahooFinanceClient: YahooFinanceClient,
) {

    fun simulate(
        symbol: String,
        dailyAmount: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): SimulationResult {
        val prices = yahooFinanceClient.fetchPrices(symbol, startDate, endDate)
        if (prices.isEmpty()) {
            throw StockNotFoundException("'$symbol'의 기간 내 주가 데이터가 없습니다.")
        }

        val daily = BigDecimal.valueOf(dailyAmount)
        var totalShares = BigDecimal.ZERO
        val chartData = mutableListOf<ChartPoint>()

        prices.forEach { stockPrice ->
            if (stockPrice.closePrice > BigDecimal.ZERO) {
                totalShares = totalShares.add(daily.divide(stockPrice.closePrice, 10, RoundingMode.HALF_UP))
            }
            val currentValue = totalShares.multiply(stockPrice.closePrice)
                .setScale(0, RoundingMode.HALF_UP).toLong()
            chartData.add(ChartPoint(date = stockPrice.date, value = currentValue))
        }

        val lastPrice = prices.last().closePrice
        val currentValue = totalShares.multiply(lastPrice).setScale(0, RoundingMode.HALF_UP).toLong()
        val totalInvested = daily.multiply(BigDecimal.valueOf(prices.size.toLong()))
            .setScale(0, RoundingMode.HALF_UP).toLong()

        val returnRate = if (totalInvested > 0) {
            (currentValue - totalInvested).toDouble() / totalInvested.toDouble() * 100.0
        } else 0.0

        return SimulationResult(
            symbol = symbol,
            startDate = startDate,
            endDate = endDate,
            dailyAmount = dailyAmount,
            totalInvested = totalInvested,
            currentValue = currentValue,
            returnRate = returnRate.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble(),
            grade = resolveGrade(returnRate),
            analogyText = resolveAnalogy(currentValue),
            chartData = chartData,
        )
    }

    private fun resolveGrade(returnRate: Double): String = when {
        returnRate >= 300.0 -> "황금 껄무새"
        returnRate >= 100.0 -> "대왕 껄무새"
        returnRate >= 50.0  -> "껄무새"
        returnRate >= 10.0  -> "아기 껄무새"
        returnRate >= 0.0   -> "알 껄무새"
        else                -> "현명한 소비자"
    }

    private fun resolveAnalogy(currentValue: Long): String {
        val items = listOf(
            Pair("스타벅스 아메리카노", 5_500L),
            Pair("아이폰 15 Pro", 1_550_000L),
            Pair("MacBook Air M2", 1_590_000L),
        )
        val best = items.maxByOrNull { (_, price) ->
            val count = currentValue.toDouble() / price.toDouble()
            if (count >= 1.0) count else 0.0
        }
        return if (best != null && currentValue >= best.second) {
            val count = currentValue.toDouble() / best.second.toDouble()
            val formatted = "%.1f".format(count)
            "${best.first} ${formatted}개"
        } else {
            "${items.first().first} ${"%.1f".format(currentValue.toDouble() / items.first().second)}잔"
        }
    }
}
