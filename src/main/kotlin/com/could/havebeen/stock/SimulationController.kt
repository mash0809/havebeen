package com.could.havebeen.stock

import com.could.havebeen.stock.model.SimulationResult
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

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

    private fun validate(symbol: String, dailyAmount: Long, startDate: LocalDate, endDate: LocalDate) {
        require(symbol.isNotBlank()) { "symbol은 비어있을 수 없습니다." }
        require(dailyAmount > 0) { "dailyAmount는 0보다 커야 합니다." }
        require(startDate.isBefore(endDate)) { "startDate는 endDate보다 이전이어야 합니다." }
        require(!endDate.isAfter(LocalDate.now())) { "endDate는 오늘 이전이어야 합니다." }
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleValidation(e: IllegalArgumentException): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "잘못된 요청입니다.")))

    @ExceptionHandler(StockNotFoundException::class)
    fun handleNotFound(e: StockNotFoundException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(404).body(mapOf("error" to (e.message ?: "종목을 찾을 수 없습니다.")))
}
