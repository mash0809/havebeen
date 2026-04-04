package com.could.havebeen.stock.model

import java.time.LocalDate

data class SimulationResult(
    val symbol: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val dailyAmount: Long,
    val totalInvested: Long,
    val currentValue: Long,
    val returnRate: Double,
    val grade: String,
    val analogyText: String,
    val chartData: List<ChartPoint>,
)

data class ChartPoint(
    val date: LocalDate,
    val value: Long,
)
