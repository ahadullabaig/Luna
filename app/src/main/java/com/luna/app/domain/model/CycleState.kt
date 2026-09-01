package com.luna.app.domain.model

import kotlinx.datetime.LocalDate

/**
 * A resolved snapshot of where the user is in their cycle today.
 *
 * [cycleLength] and [periodLength] are the personalised medians computed from
 * logged history, or the 28/5 defaults when there is not enough history yet.
 */
data class CycleState(
    val currentPhase: CyclePhase,
    val cycleDay: Int,                  // 1-based day within the current cycle
    val cycleLength: Int,
    val periodLength: Int,
    val nextPeriodStart: LocalDate,
    val daysUntilNextPeriod: Int,       // negative once nextPeriodStart has passed
    val isOverdue: Boolean,             // past nextPeriodStart + OVERDUE_GRACE_DAYS
    val isPersonalised: Boolean         // false while still using the 28/5 defaults
) {
    val overdueDays: Int get() = if (isOverdue) -daysUntilNextPeriod else 0
}
