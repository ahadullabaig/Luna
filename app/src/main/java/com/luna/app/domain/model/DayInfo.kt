package com.luna.app.domain.model

/**
 * What can be said about a single date on the calendar.
 *
 * [isLoggedPeriod] is the honesty flag: it separates a day the user actually recorded from one
 * the app merely projected onto. The calendar draws the two differently so a prediction is never
 * mistaken for a record.
 */
data class DayInfo(
    val phase: CyclePhase?,
    val cycleDay: Int?,
    val isLoggedPeriod: Boolean
) {
    companion object {
        /** A date preceding all logged history, which nothing can place. */
        val Unknown = DayInfo(phase = null, cycleDay = null, isLoggedPeriod = false)
    }
}
