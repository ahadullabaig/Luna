package com.luna.app.feature.home

import com.luna.app.data.entity.DailyLogEntity
import com.luna.app.data.entity.PeriodEntity
import com.luna.app.domain.model.CycleState
import kotlinx.datetime.LocalDate

/**
 * [cycle] is null when there is no period history to compute from — the home screen shows its
 * empty prompt in place of the donut, but symptom logging stays available either way.
 *
 * [today] travels with the state rather than being read at the call site, so everything on
 * screen agrees on which day it is even after a midnight rollover.
 *
 * [periods] is the whole history, which the period dialog checks a new range against.
 * [openPeriod] is the period running today with no end yet; while there is one, the primary
 * action is to end it rather than to log another.
 */
data class HomeUiState(
    val today: LocalDate,
    val isLoading: Boolean = true,
    val cycle: CycleState? = null,
    val periods: List<PeriodEntity> = emptyList(),
    val openPeriod: PeriodEntity? = null,
    val todayLog: DailyLogEntity? = null
)
