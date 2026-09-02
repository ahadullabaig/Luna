package com.luna.app.domain.usecase

import com.luna.app.data.entity.PeriodEntity
import com.luna.app.domain.model.CyclePhase
import com.luna.app.domain.model.DayInfo
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * Cycle history with its medians already resolved, able to place any date cheaply.
 *
 * The calendar asks about ~42 cells per visible month. Calling [computePhaseForDate] for each
 * would re-sort the history and re-derive both medians forty-two times over; building the
 * projection once per history change reduces that to a single pass.
 *
 * It is a data class rather than a lambda so Compose can compare it — the grid then recomposes
 * when the periods actually change, not on every emission.
 *
 * @param periodsNewestFirst sorted by descending start date. [projectionFrom] guarantees this;
 *   [infoFor] relies on it to find the nearest anchor with a first-match scan.
 */
data class CycleProjection(
    val periodsNewestFirst: List<PeriodEntity>,
    val cycleLength: Int,
    val periodLength: Int,
    val isPersonalised: Boolean
) {
    val hasHistory: Boolean get() = periodsNewestFirst.isNotEmpty()

    /**
     * Place [date] in the cycle: walk back to the nearest period start at or before it, take the
     * offset modulo [cycleLength], and read off the phase. Dates beyond the last logged period
     * simply repeat the cycle forward, which is what makes future months predictable.
     */
    fun infoFor(date: LocalDate): DayInfo {
        val anchor = periodsNewestFirst.firstOrNull { it.startDate <= date } ?: return DayInfo.Unknown
        val logged = periodCovering(date, periodsNewestFirst, periodLength)
        val cycleDay = anchor.startDate.daysUntil(date).mod(cycleLength) + 1
        return DayInfo(
            // A logged period always wins over the projection, exactly as on the home screen.
            phase = if (logged != null) CyclePhase.MENSTRUAL else phaseForDay(cycleDay, cycleLength, periodLength),
            cycleDay = cycleDay,
            isLoggedPeriod = logged != null
        )
    }

    companion object {
        val Empty = CycleProjection(
            periodsNewestFirst = emptyList(),
            cycleLength = DEFAULT_CYCLE_LENGTH,
            periodLength = DEFAULT_PERIOD_LENGTH,
            isPersonalised = false
        )
    }
}

fun projectionFrom(periods: List<PeriodEntity>): CycleProjection {
    if (periods.isEmpty()) return CycleProjection.Empty
    val measuredCycle = cycleLengthFrom(periods)
    val measuredPeriod = periodLengthFrom(periods)
    return CycleProjection(
        periodsNewestFirst = periods.sortedByDescending { it.startDate },
        cycleLength = measuredCycle ?: DEFAULT_CYCLE_LENGTH,
        periodLength = measuredPeriod ?: DEFAULT_PERIOD_LENGTH,
        isPersonalised = measuredCycle != null || measuredPeriod != null
    )
}
