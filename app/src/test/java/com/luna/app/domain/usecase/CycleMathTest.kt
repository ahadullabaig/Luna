package com.luna.app.domain.usecase

import com.luna.app.data.entity.PeriodEntity
import com.luna.app.domain.model.CyclePhase
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CycleMathTest {

    private fun date(iso: String) = LocalDate.parse(iso)

    // --- medians -------------------------------------------------------------

    @Test
    fun `median of odd sample is the middle value`() {
        assertEquals(28, listOf(26, 28, 31).medianOrNull())
    }

    @Test
    fun `median of even sample averages the two middles`() {
        assertEquals(29, listOf(26, 28, 30, 34).medianOrNull())
    }

    @Test
    fun `median resists a single outlier cycle`() {
        // One 60-day gap must not drag the estimate the way a mean would.
        assertEquals(28, listOf(27, 28, 28, 29, 60).medianOrNull())
    }

    @Test
    fun `median of empty sample is null`() {
        assertNull(emptyList<Int>().medianOrNull())
    }

    // --- phase boundaries, 28-day reference cycle ----------------------------

    @Test
    fun `28 day cycle assigns the documented phase boundaries`() {
        val phase = { day: Int -> phaseForDay(day, cycleLength = 28, periodLength = 5) }
        (1..5).forEach { assertEquals("day $it", CyclePhase.MENSTRUAL, phase(it)) }
        (6..12).forEach { assertEquals("day $it", CyclePhase.FOLLICULAR, phase(it)) }
        (13..15).forEach { assertEquals("day $it", CyclePhase.OVULATION, phase(it)) }
        (16..28).forEach { assertEquals("day $it", CyclePhase.LUTEAL, phase(it)) }
    }

    @Test
    fun `ovulation is anchored 14 days before the next period, not mid-cycle`() {
        // PLAN section 6: a 32-day cycle ovulates on day 18, not day 16.
        assertEquals(18, ovulationDayFor(32))
        assertEquals(CyclePhase.OVULATION, phaseForDay(18, cycleLength = 32, periodLength = 5))
        assertEquals(CyclePhase.FOLLICULAR, phaseForDay(16, cycleLength = 32, periodLength = 5))
    }

    @Test
    fun `ovulation window is exactly three days`() {
        val ovulationDays = (1..28).filter {
            phaseForDay(it, cycleLength = 28, periodLength = 5) == CyclePhase.OVULATION
        }
        assertEquals(listOf(13, 14, 15), ovulationDays)
    }

    // --- donut invariant -----------------------------------------------------

    @Test
    fun `phase segments always sum to the cycle length`() {
        // The donut divides 360 degrees by these counts; if they do not sum exactly,
        // the ring visibly fails to close.
        for (cycleLength in 21..45) {
            for (periodLength in 2..8) {
                val total = phaseSegments(cycleLength, periodLength).sumOf { it.second }
                assertEquals("cycle=$cycleLength period=$periodLength", cycleLength, total)
            }
        }
    }

    @Test
    fun `phase segments stay in cycle order and never repeat a phase run`() {
        val segments = phaseSegments(28, 5)
        assertEquals(
            listOf(
                CyclePhase.MENSTRUAL,
                CyclePhase.FOLLICULAR,
                CyclePhase.OVULATION,
                CyclePhase.LUTEAL
            ),
            segments.map { it.first }
        )
        assertEquals(listOf(5, 7, 3, 13), segments.map { it.second })
    }

    @Test
    fun `short cycle degrades without producing a negative follicular phase`() {
        // cycleLength 21 puts ovulation on day 7, immediately after a 5-day period,
        // which squeezes the follicular phase out entirely.
        val segments = phaseSegments(cycleLength = 21, periodLength = 5)
        assertEquals(21, segments.sumOf { it.second })
        segments.forEach { (phase, days) ->
            assert(days > 0) { "$phase produced a non-positive run of $days days" }
        }
    }

    // --- averaging over history ---------------------------------------------

    @Test
    fun `cycle length is the median gap between consecutive starts`() {
        val periods = listOf(
            PeriodEntity(1, date("2026-01-01"), date("2026-01-05")),
            PeriodEntity(2, date("2026-01-29"), date("2026-02-02")),  // gap 28
            PeriodEntity(3, date("2026-02-28"), date("2026-03-04")),  // gap 30
            PeriodEntity(4, date("2026-03-28"), date("2026-04-01"))   // gap 28
        )
        assertEquals(28, cycleLengthFrom(periods))
    }

    @Test
    fun `cycle length falls back to null below two gaps`() {
        val periods = listOf(
            PeriodEntity(1, date("2026-01-01"), date("2026-01-05")),
            PeriodEntity(2, date("2026-01-29"), date("2026-02-02"))
        )
        assertNull(cycleLengthFrom(periods))
    }

    @Test
    fun `period length counts both endpoints`() {
        // 1st to 5th inclusive is five days of bleeding, not four.
        val periods = listOf(
            PeriodEntity(1, date("2026-01-01"), date("2026-01-05")),
            PeriodEntity(2, date("2026-01-29"), date("2026-02-02"))
        )
        assertEquals(5, periodLengthFrom(periods))
    }

    @Test
    fun `ongoing periods are excluded from the period length median`() {
        val periods = listOf(
            PeriodEntity(1, date("2026-01-01"), date("2026-01-05")),  // 5 days
            PeriodEntity(2, date("2026-01-29"), date("2026-02-02")),  // 5 days
            PeriodEntity(3, date("2026-02-28"), null)                 // ongoing, unknown
        )
        assertEquals(5, periodLengthFrom(periods))
    }

    @Test
    fun `period length is null when only ongoing periods exist`() {
        val periods = listOf(PeriodEntity(1, date("2026-02-28"), null))
        assertNull(periodLengthFrom(periods))
    }

    @Test
    fun `only the six most recent periods feed the medians`() {
        // Eight periods: the two oldest sit on a 40-day rhythm that must fall out of the window.
        val periods = listOf(
            PeriodEntity(1, date("2025-09-01"), null),
            PeriodEntity(2, date("2025-10-11"), null),  // gap 40
            PeriodEntity(3, date("2025-11-20"), null),  // gap 40
            PeriodEntity(4, date("2025-12-18"), null),  // gap 28
            PeriodEntity(5, date("2026-01-15"), null),  // gap 28
            PeriodEntity(6, date("2026-02-12"), null),  // gap 28
            PeriodEntity(7, date("2026-03-12"), null),  // gap 28
            PeriodEntity(8, date("2026-04-09"), null)   // gap 28
        )
        assertEquals(28, cycleLengthFrom(periods))
    }
}
