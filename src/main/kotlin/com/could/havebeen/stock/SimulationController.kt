package com.could.havebeen.stock

import com.could.havebeen.stock.model.SimulationResult
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/**
 * 적립식 투자 시뮬레이션 REST API 엔드포인트.
 *
 * GET /api/simulation?symbol=AAPL&dailyAmount=10000&startDate=2023-01-01&endDate=2024-01-01
 * 한국 주식은 거래소 접미사를 포함한 심볼을 사용한다 (예: 삼성전자 → 005930.KS).
 */
@RestController
@RequestMapping("/api/simulation")
class SimulationController(
    private val simulationService: SimulationService,
) {

    @GetMapping
    fun simulate(
        @RequestParam symbol: String,
        @RequestParam dailyAmount: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    ): SimulationResult {
        validate(symbol, dailyAmount, startDate, endDate)
        return simulationService.simulate(symbol, dailyAmount, startDate, endDate)
    }

    /** 요청 파라미터의 기본 유효성을 검사한다. 실패 시 IllegalArgumentException을 던진다. */
    private fun validate(symbol: String, dailyAmount: Long, startDate: LocalDate, endDate: LocalDate) {
        require(symbol.isNotBlank()) { "symbol은 비어있을 수 없습니다." }
        require(dailyAmount > 0) { "dailyAmount는 0보다 커야 합니다." }
        require(startDate.isBefore(endDate)) { "startDate는 endDate보다 이전이어야 합니다." }
        // 미래 데이터는 Yahoo Finance에서 제공하지 않으므로 오늘 이후 날짜는 허용하지 않는다
        require(!endDate.isAfter(LocalDate.now())) { "endDate는 오늘 이전이어야 합니다." }
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleValidation(e: IllegalArgumentException): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "잘못된 요청입니다.")))

    @ExceptionHandler(StockNotFoundException::class)
    fun handleNotFound(e: StockNotFoundException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(404).body(mapOf("error" to (e.message ?: "종목을 찾을 수 없습니다.")))
}
