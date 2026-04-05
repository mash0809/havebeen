package com.could.havebeen.stock

import com.could.havebeen.stock.model.ChartPoint
import com.could.havebeen.stock.model.SimulationResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class SimulationService(
    private val yahooFinanceClient: YahooFinanceClient,
) {

    private val log = LoggerFactory.getLogger(SimulationService::class.java)

    /**
     * 적립식(DCA) 투자 시뮬레이션을 수행한다.
     *
     * 매 거래일마다 [dailyAmount]원을 종가로 매수한다고 가정하고,
     * 기간 말 평가금액과 수익률을 계산한다.
     * 종가가 0인 날(거래 정지 등)은 매수를 건너뛰며, 해당 날은 투자금에도 포함되지 않는다.
     */
    fun simulate(
        symbol: String,
        dailyAmount: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): SimulationResult {
        log.info("시뮬레이션 요청: symbol={}, dailyAmount={}, period={} ~ {}", symbol, dailyAmount, startDate, endDate)
        val prices = yahooFinanceClient.fetchPrices(symbol, startDate, endDate)
        if (prices.isEmpty()) {
            log.warn("주가 데이터 없음: symbol={}", symbol)
            throw StockNotFoundException("'$symbol'의 기간 내 주가 데이터가 없습니다.")
        }

        val daily = BigDecimal.valueOf(dailyAmount)
        var totalShares = BigDecimal.ZERO
        var investedDays = 0L
        val chartData = mutableListOf<ChartPoint>()

        // 날짜 순으로 매수 시뮬레이션: 종가 0원인 날은 매수 불가로 skip
        prices.forEach { stockPrice ->
            if (stockPrice.closePrice > BigDecimal.ZERO) {
                // 소수점 10자리까지 보존해 주식 수 계산 오차를 최소화
                totalShares = totalShares.add(daily.divide(stockPrice.closePrice, 10, RoundingMode.HALF_UP))
                investedDays++
            }
            // 해당 시점의 평가금액(보유 주식 수 × 당일 종가)을 차트 데이터로 누적
            val currentValue = totalShares.multiply(stockPrice.closePrice)
                .setScale(0, RoundingMode.HALF_UP).toLong()
            chartData.add(ChartPoint(date = stockPrice.date, value = currentValue))
        }

        val lastPrice = prices.last().closePrice
        val currentValue = totalShares.multiply(lastPrice).setScale(0, RoundingMode.HALF_UP).toLong()
        // 총 투자금 = 일일 투자금 × 실제 매수일 수 (종가 0원으로 skip된 날 제외)
        val totalInvested = daily.multiply(BigDecimal.valueOf(investedDays))
            .setScale(0, RoundingMode.HALF_UP).toLong()

        // 수익률(%) = (평가금액 - 투자금) / 투자금 × 100
        val returnRate = if (totalInvested > 0) {
            (currentValue - totalInvested).toDouble() / totalInvested.toDouble() * 100.0
        } else 0.0

        val result = SimulationResult(
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
        log.info("시뮬레이션 완료: symbol={}, returnRate={}%, grade={}", symbol, result.returnRate, result.grade)
        return result
    }

    /** 수익률에 따라 껄무새 등급을 반환한다. 300% 이상이 최고 등급, 음수이면 최하 등급. */
    private fun resolveGrade(returnRate: Double): String = when {
        returnRate >= 300.0 -> "황금 껄무새"
        returnRate >= 100.0 -> "대왕 껄무새"
        returnRate >= 50.0  -> "껄무새"
        returnRate >= 10.0  -> "아기 껄무새"
        returnRate >= 0.0   -> "알 껄무새"
        else                -> "현명한 소비자"
    }

    /**
     * 평가금액을 일상 소비재에 빗대어 표현한다.
     *
     * 1개 이상 살 수 있는 아이템 중 가장 많이 살 수 있는 것을 선택한다.
     * 가장 저렴한 아이템(스타벅스 아메리카노)도 살 수 없을 경우 소수점 "잔" 단위로 표현한다.
     */
    private fun resolveAnalogy(currentValue: Long): String {
        // 1개 이상 구매 가능한 아이템 중 구매 수량이 가장 많은 것을 선택
        val best = ANALOGY_ITEMS.maxByOrNull { (_, price) ->
            val count = currentValue.toDouble() / price.toDouble()
            if (count >= 1.0) count else 0.0
        }
        return if (best != null && currentValue >= best.second) {
            val count = currentValue.toDouble() / best.second.toDouble()
            val formatted = "%.1f".format(count)
            "${best.first} ${formatted}개"
        } else {
            // 아메리카노 1잔도 안 되는 경우: 소수점으로 몇 잔인지 표현
            val cheapest = ANALOGY_ITEMS.first()
            "${cheapest.first} ${"%.1f".format(currentValue.toDouble() / cheapest.second)}잔"
        }
    }

    companion object {
        /** 평가금액 비유에 사용하는 소비재 목록 (이름, 가격 원) */
        private val ANALOGY_ITEMS = listOf(
            Pair("스타벅스 아메리카노", 5_500L),
            Pair("아이폰 15 Pro", 1_550_000L),
            Pair("MacBook Air M2", 1_590_000L),
        )
    }
}
