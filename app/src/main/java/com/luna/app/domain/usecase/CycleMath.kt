package com.luna.app.domain.usecase

import com.luna.app.data.entity.PeriodEntity
import com.luna.app.domain.model.CyclePhase
import com.luna.app.domain.model.CycleState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.math.roundToInt

/**
 * Pure cycle arithmetic. No Room, no Android, no coroutines — everything here is
 * a total function over its arguments so it can be unit-tested directly.
 */

const val DEFAULT_CYCLE_LENGTH = 28
const val DEFAULT_PERIOD_LENGTH = 5

/** Days after a predicted start before we call a period overdue rather than late-but-normal. */
const val OVERDUE_GRACE_DAYS = 3

/** How many recent periods feed the medians. */
const val HISTORY_WINDOW = 6

/** Minimum samples before we trust history over the defaults. */
private const val MIN_SAMPLES = 2

/** The luteal phase is ~14 days regardless of cycle length; ovulation is anchored back from it. */
fun ovulationDayFor(cycleLength: Int): Int = cycleLength - 14

fun List<Int>.medianOrNull(): Int? {
    if (isEmpty()) return null
    val sorted = sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) {
        sorted[mid]
    } else {
        ((sorted[mid - 1] + sorted[mid]) / 2.0).roundToInt()
    }
}

/**
 * Median gap between consecutive period starts, over the most recent [HISTORY_WINDOW] periods.
 * Returns null when there are fewer than [MIN_SAMPLES] gaps to average.
 */
fun cycleLengthFrom(periods: List<PeriodEntity>): Int? {
    val starts = periods.map { it.startDate }
        .sortedDescending()
        .take(HISTORY_WINDOW)
        .sorted()
    if (starts.size < MIN_SAMPLES + 1) return null
    val gaps = starts.zipWithNext { a, b -> a.daysUntil(b) }.filter { it > 0 }
    return if (gaps.size < MIN_SAMPLES) null else gaps.medianOrNull()
}

/**
 * Median duration of completed periods, inclusive of both endpoints — a period running
 * 1st→5th is five days, not four. Rows with a null endDate are still in progress and are
 * excluded, since their true length is unknown.
 */
fun periodLengthFrom(periods: List<PeriodEntity>): Int? {
    val lengths = periods
        .filter { it.endDate != null }
        .sortedByDescending { it.startDate }
        .take(HISTORY_WINDOW)
        .map { it.startDate.daysUntil(it.endDate!!) + 1 }
        .filter { it > 0 }
    return if (lengths.size < MIN_SAMPLES) null else lengths.medianOrNull()
}

/**
 * Phase for a 1-based day of the cycle.
 *
 * Order is deliberate: menstrual claims its days first and the ovulation window second, so
 * short cycles where the windows would overlap degrade predictably instead of producing a
 * negative-width follicular phase.
 */
fun phaseForDay(day: Int, cycleLength: Int, periodLength: Int): CyclePhase {
    val ovulationDay = ovulationDayFor(cycleLength)
    return when {
        day <= periodLength -> CyclePhase.MENSTRUAL
        day in (ovulationDay - 1)..(ovulationDay + 1) -> CyclePhase.OVULATION
        day < ovulationDay - 1 -> CyclePhase.FOLLICULAR
        else -> CyclePhase.LUTEAL
    }
}

/**
 * The cycle as consecutive (phase, dayCount) runs, in day order. Built by walking every day
 * rather than by arithmetic on boundaries, so the counts always sum to [cycleLength] exactly
 * — which is what keeps the donut's arcs adding up to 360°.
 */
fun phaseSegments(cycleLength: Int, periodLength: Int): List<Pair<CyclePhase, Int>> {
    if (cycleLength < 1) return emptyList()
    val segments = mutableListOf<Pair<CyclePhase, Int>>()
    var runPhase = phaseForDay(1, cycleLength, periodLength)
    var runLength = 0
    for (day in 1..cycleLength) {
        val phase = phaseForDay(day, cycleLength, periodLength)
        if (phase == runPhase) {
            runLength++
        } else {
            segments += runPhase to runLength
            runPhase = phase
            runLength = 1
        }
    }
    segments += runPhase to runLength
    return segments
}

/** True when [date] falls inside a logged period. Ongoing rows project [periodLength] days. */
fun periodCovering(
    date: LocalDate,
    periods: List<PeriodEntity>,
    periodLength: Int
): PeriodEntity? = periods.firstOrNull { period ->
    val last = period.endDate ?: period.startDate.plusDays(periodLength - 1)
    date >= period.startDate && date <= last
}

fun LocalDate.plusDays(days: Int): LocalDate =
    LocalDate.fromEpochDays(toEpochDays() + days)

/**
 * Resolve the user's position in their cycle on [today], or null when history cannot place them.
 *
 * Pure: takes the period list rather than a repository, so it is directly unit-testable and the
 * use case above it is reduced to wiring a Flow to this function.
 */
fun computeCycleState(periods: List<PeriodEntity>, today: LocalDate): CycleState? {
    // Periods logged with a future start date cannot describe today.
    val past = periods.filter { it.startDate <= today }
    if (past.isEmpty()) return null

    val measuredCycle = cycleLengthFrom(past)
    val measuredPeriod = periodLengthFrom(past)
    val cycleLength = measuredCycle ?: DEFAULT_CYCLE_LENGTH
    val periodLength = measuredPeriod ?: DEFAULT_PERIOD_LENGTH

    val anchor = past.maxBy { it.startDate }
    val cycleDay = anchor.startDate.daysUntil(today) + 1
    val nextPeriodStart = anchor.startDate.plusDays(cycleLength)
    val daysUntilNextPeriod = today.daysUntil(nextPeriodStart)

    // A logged period always wins over the projection: if today falls inside one, the phase is
    // menstrual whatever the arithmetic would otherwise say.
    val covering = periodCovering(today, past, periodLength)
    val phase = if (covering != null) {
        CyclePhase.MENSTRUAL
    } else {
        phaseForDay(cycleDay.coerceIn(1, cycleLength), cycleLength, periodLength)
    }

    return CycleState(
        currentPhase = phase,
        cycleDay = cycleDay,
        cycleLength = cycleLength,
        periodLength = periodLength,
        nextPeriodStart = nextPeriodStart,
        daysUntilNextPeriod = daysUntilNextPeriod,
        isOverdue = covering == null && daysUntilNextPeriod < -OVERDUE_GRACE_DAYS,
        isPersonalised = measuredCycle != null || measuredPeriod != null
    )
}

/**
 * Phase for an arbitrary date, past or future. Returns null for dates that precede all logged
 * history, which cannot be placed.
 *
 * A one-shot convenience over [CycleProjection]: it rebuilds the projection on every call, so
 * prefer holding a [projectionFrom] result when asking about more than a handful of dates.
 */
fun computePhaseForDate(date: LocalDate, periods: List<PeriodEntity>): CyclePhase? =
    projectionFrom(periods).infoFor(date).phase
