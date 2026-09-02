package com.luna.app.feature.calendar

import com.luna.app.data.entity.DailyLogEntity
import com.luna.app.domain.usecase.CycleProjection
import kotlinx.datetime.LocalDate

/**
 * @param rangeStart first day of the earliest month the grid can scroll to.
 * @param rangeEnd last day of the latest. Both bound the daily-log query as well as the grid, so
 *   the two can never drift apart.
 */
data class CalendarUiState(
    val today: LocalDate,
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val isLoading: Boolean = true,
    val projection: CycleProjection = CycleProjection.Empty,
    val logsByDate: Map<LocalDate, DailyLogEntity> = emptyMap()
)
