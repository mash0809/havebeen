package com.could.havebeen.stock

import com.could.havebeen.stock.model.StockPrice
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import java.time.LocalDate

class SimulationServiceTest {

    private val client = mockk<YahooFinanceClient>()
    private val service = SimulationService(client)

    private val start = LocalDate.of(2022, 1, 3)
    private val end = LocalDate.of(2022, 1, 7)

    /** 고정 주가: [100, 200, 400] 3영업일 */
    private fun fixedPrices() = listOf(
        StockPrice(LocalDate.of(2022, 1, 3), BigDecimal("100")),
        StockPrice(LocalDate.of(2022, 1, 4), BigDecimal("200")),
        StockPrice(LocalDate.of(2022, 1, 5), BigDecimal("400")),
    )

    @Test
    fun `DCA 수익률 계산이 정확해야 한다`() {
        // 매일 10,000원 투자
        // day1: 10000/100 = 100주
        // day2: 10000/200 = 50주
        // day3: 10000/400 = 25주
        // 총 175주 × 400원 = 70,000원
        // 투자금 = 30,000원 → 수익률 = (70000-30000)/30000*100 = 133.33%
        every { client.fetchPrices(any(), any(), any()) } returns fixedPrices()

        val result = service.simulate("TEST", 10_000L, start, end)

        assertEquals(30_000L, result.totalInvested)
        assertEquals(70_000L, result.currentValue)
        assertEquals(133.33, result.returnRate)
    }

    @Test
    fun `수익률이 0 미만이면 현명한 소비자 등급이어야 한다`() {
        // 고가 매수 후 하락 시나리오
        val prices = listOf(
            StockPrice(LocalDate.of(2022, 1, 3), BigDecimal("400")),
            StockPrice(LocalDate.of(2022, 1, 4), BigDecimal("100")),
        )
        every { client.fetchPrices(any(), any(), any()) } returns prices

        val result = service.simulate("TEST", 10_000L, start, end)

        assertEquals("현명한 소비자", result.grade)
        assertTrue(result.returnRate < 0)
    }

    @ParameterizedTest(name = "종가 100→{0} → 등급 {1}")
    @CsvSource(
        // 2일 DCA 수익률 공식: returnRate = (P2/P1 - 1) * 50
        // P2=110 → ~5%  → 알 껄무새
        // P2=130 → ~15% → 아기 껄무새
        // P2=250 → 75%  → 껄무새
        // P2=400 → 150% → 대왕 껄무새
        // P2=800 → 350% → 황금 껄무새
        "110, 알 껄무새",
        "130, 아기 껄무새",
        "250, 껄무새",
        "400, 대왕 껄무새",
        "800, 황금 껄무새",
    )
    fun `등급 경계값이 정확해야 한다`(finalPrice: Long, expectedGrade: String) {
        // [100 → finalPrice] 2일 시나리오
        val prices = listOf(
            StockPrice(LocalDate.of(2022, 1, 3), BigDecimal("100")),
            StockPrice(LocalDate.of(2022, 1, 4), BigDecimal(finalPrice.toString())),
        )
        every { client.fetchPrices(any(), any(), any()) } returns prices

        val result = service.simulate("TEST", 10_000L, start, end)

        assertEquals(expectedGrade.trim(), result.grade)
    }

    @Test
    fun `주가가 0인 날은 매수 없이 skip되어야 한다`() {
        val prices = listOf(
            StockPrice(LocalDate.of(2022, 1, 3), BigDecimal("0")),
            StockPrice(LocalDate.of(2022, 1, 4), BigDecimal("200")),
        )
        every { client.fetchPrices(any(), any(), any()) } returns prices

        val result = service.simulate("TEST", 10_000L, start, end)

        // 0원짜리 날은 skip → 2일치 투자금만
        assertEquals(20_000L, result.totalInvested)
    }

    @Test
    fun `현물 비유 텍스트가 생성되어야 한다`() {
        every { client.fetchPrices(any(), any(), any()) } returns fixedPrices()

        val result = service.simulate("TEST", 10_000L, start, end)

        assertTrue(result.analogyText.isNotBlank())
    }

    @Test
    fun `chartData 포인트 수는 주가 데이터 수와 같아야 한다`() {
        every { client.fetchPrices(any(), any(), any()) } returns fixedPrices()

        val result = service.simulate("TEST", 10_000L, start, end)

        assertEquals(3, result.chartData.size)
    }
}
