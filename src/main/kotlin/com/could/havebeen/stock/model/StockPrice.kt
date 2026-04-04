package com.could.havebeen.stock.model

import java.math.BigDecimal
import java.time.LocalDate

data class StockPrice(
    val date: LocalDate,
    val closePrice: BigDecimal,
)
