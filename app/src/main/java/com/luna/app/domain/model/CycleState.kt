package com.luna.app.domain.model

import kotlinx.datetime.LocalDate

/**
 * A resolved snapshot of where the user is in their cycle today.
 *
 * [cycleLength] and [periodLength] are the personalised medians computed from
 * logged history, or the 28/5 defaults when there is not enough history yet.
 *
 * [currentPeriodLength] is this cycle's own period: as logged once it has an end date, and the
 * [periodLength] median until then. The ring is drawn from it, so a short period shows as short.
 */
data class CycleState(
    val currentPhase: CyclePhase,
    val cycleDay: Int,                  // 1-based day within the current cycle
    val cycleLength: Int,
    val periodLength: Int,
    val currentPeriodLength: Int,
    val nextPeriodStart: LocalDate,
    val daysUntilNextPeriod: Int,       // negative once nextPeriodStart has passed
    val isOverdue: Boolean,             // past nextPeriodStart + OVERDUE_GRACE_DAYS
    val isCycleLengthMeasured: Boolean, // false while cycleLength is the 28-day default
    val isPeriodLengthMeasured: Boolean // false while periodLength is the 5-day default
) {
    val overdueDays: Int get() = if (isOverdue) -daysUntilNextPeriod else 0

    /**
     * True once either median is the user's own. The two are measured independently — start dates
     * alone give a cycle length, and only end dates give a period length — so anything that claims
     * a figure as "yours" should check that figure's own flag, not this.
     */
    val isPersonalised: Boolean get() = isCycleLengthMeasured || isPeriodLengthMeasured
}
