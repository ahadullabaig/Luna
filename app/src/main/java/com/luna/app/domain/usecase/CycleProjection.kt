package com.luna.app.domain.usecase

import com.luna.app.data.entity.PeriodEntity
import com.luna.app.domain.model.CyclePhase
import com.luna.app.domain.model.DayInfo
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * Cycle history with its medians already resolved, able to place any date cheaply. It is the one
 * answer to "where is this date in the cycle": the calendar asks it about every cell, and Home asks
 * it about today through [computeCycleState].
 *
 * The calendar asks about ~42 cells per visible month; building the projection once per history
 * change means the medians are derived once rather than forty-two times over.
 *
 * It knows [today] because the latest cycle is not over until the next period is logged. Wrapping
 * it modulo [cycleLength] the moment the prediction ran out used to start a cycle nobody had
 * started: a late period read as "12 days late" on Home while the calendar drew that same day as
 * follicular, with an expected period painted over days already behind.
 *
 * It is a data class rather than a lambda so Compose can compare it — the grid then recomposes
 * when the periods or the date actually change, not on every emission.
 *
 * @param periodsNewestFirst periods that started on or before [today], newest first.
 *   [projectionFrom] guarantees both; [infoFor] relies on the order to find the nearest anchor
 *   with a first-match scan.
 */
data class CycleProjection(
    val periodsNewestFirst: List<PeriodEntity>,
    val today: LocalDate,
    val cycleLength: Int,
    val periodLength: Int,
    val isCycleLengthMeasured: Boolean,
    val isPeriodLengthMeasured: Boolean
) {
    val hasHistory: Boolean get() = periodsNewestFirst.isNotEmpty()

    /** True once either median comes from the user's own history rather than a default. */
    val isPersonalised: Boolean get() = isCycleLengthMeasured || isPeriodLengthMeasured

    /** The latest logged period: the start of the cycle that is still running. */
    val currentAnchor: PeriodEntity? get() = periodsNewestFirst.firstOrNull()

    /**
     * When the next period is expected: a cycle length after the running one began, or tomorrow
     * once that day has passed with nothing logged. A late period is due any day, so the expected
     * window moves forward a day at a time, and every cycle projected after it moves with it, until
     * the real start is logged and the record takes over.
     */
    val nextExpectedStart: LocalDate?
        get() = currentAnchor?.let { maxOf(it.startDate.plusDays(cycleLength), today.plusDays(1)) }

    /**
     * Place [date] in the cycle, or [DayInfo.Unknown] if it precedes everything logged.
     *
     * - From [nextExpectedStart] on, there is only the forecast: cycles repeat from that date.
     * - In the running cycle the day count never wraps. It is not over until a period is logged,
     *   so a late cycle reads day 30, 31, … and keeps its last phase — exactly what Home says —
     *   and a day already past is never drawn as a period that was merely expected.
     * - Older cycles do wrap. That is what draws an expected period across a gap between two logged
     *   ones, which is a prompt to fill in the entry that was missed rather than a forecast.
     */
    fun infoFor(date: LocalDate): DayInfo {
        val anchor = periodsNewestFirst.firstOrNull { it.startDate <= date } ?: return DayInfo.Unknown
        val running = anchor == currentAnchor
        val next = nextExpectedStart
        if (running && next != null && date >= next) {
            val cycleDay = next.daysUntil(date).mod(cycleLength) + 1
            return DayInfo(phaseForDay(cycleDay, cycleLength, periodLength), cycleDay, isLoggedPeriod = false)
        }

        val logged = loggedPeriodOn(date)
        val offset = anchor.startDate.daysUntil(date)
        val cycleDay = if (running) offset + 1 else offset.mod(cycleLength) + 1
        // Inside the anchor's own cycle a logged end date is a fact, and the days after it are over
        // rather than "expected". Cycles projected beyond it have only the median to go on.
        val bleedDays = if (offset < cycleLength) anchor.loggedLength()?.takeIf { it > 0 } ?: periodLength else periodLength
        return DayInfo(
            // A logged period always wins over the projection.
            phase = if (logged != null) {
                CyclePhase.MENSTRUAL
            } else {
                phaseForDay(cycleDay.coerceAtMost(cycleLength), cycleLength, bleedDays)
            },
            cycleDay = cycleDay,
            isLoggedPeriod = logged != null
        )
    }

    /** The logged period [date] falls inside, if any — what the calendar's edit and delete act on. */
    fun loggedPeriodOn(date: LocalDate): PeriodEntity? = periodCovering(date, periodsNewestFirst, periodLength)
}

/**
 * Resolve [periods] as they stand on [today]. A period logged with a future start date cannot
 * describe today, so it is left out of the medians and the anchors alike — on both screens, now
 * that both read from here.
 */
fun projectionFrom(periods: List<PeriodEntity>, today: LocalDate): CycleProjection {
    val past = periods.filter { it.startDate <= today }
    val measuredCycle = cycleLengthFrom(past)
    val measuredPeriod = periodLengthFrom(past)
    return CycleProjection(
        periodsNewestFirst = past.sortedByDescending { it.startDate },
        today = today,
        cycleLength = measuredCycle ?: DEFAULT_CYCLE_LENGTH,
        periodLength = measuredPeriod ?: DEFAULT_PERIOD_LENGTH,
        isCycleLengthMeasured = measuredCycle != null,
        isPeriodLengthMeasured = measuredPeriod != null
    )
}
