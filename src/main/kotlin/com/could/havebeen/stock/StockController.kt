package com.could.havebeen.stock

import com.could.havebeen.stock.model.StockSearchResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 종목 검색 REST API 엔드포인트.
 *
 * GET /api/stocks/search?q=AAPL
 * 2자 미만의 쿼리는 빈 배열을 즉시 반환한다.
 */
@RestController
@RequestMapping("/api/stocks")
class StockController(private val yahooFinanceClient: YahooFinanceClient) {

    @GetMapping("/search")
    fun search(@RequestParam q: String): List<StockSearchResult> {
        // 너무 짧은 쿼리는 의미 있는 결과를 반환하기 어려우므로 조기 반환
        if (q.isBlank() || q.length < 2) return emptyList()
        return yahooFinanceClient.searchSymbols(q)
    }
}
