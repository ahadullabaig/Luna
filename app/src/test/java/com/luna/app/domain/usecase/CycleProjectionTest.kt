package com.luna.app.domain.usecase

import com.luna.app.data.entity.PeriodEntity
import com.luna.app.domain.model.CyclePhase
import com.luna.app.domain.model.DayInfo
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The calendar's projection. The claims that matter most here are the ones the day cell draws
 * with: which phase a date lands in, and whether that phase was recorded or merely expected.
 */
class CycleProjectionTest {

    private fun date(iso: String) = LocalDate.parse(iso)

    private fun period(start: String, end: String?) =
        PeriodEntity(startDate = date(start), endDate = end?.let(::date))

    /** Four 28-day cycles ending 2026-04-05, each period five days long. */
    private val regularHistory = listOf(
        period("2026-01-05", "2026-01-09"),
        period("2026-02-02", "2026-02-06"),
        period("2026-03-02", "2026-03-06"),
        period("2026-03-30", "2026-04-03")
    )

    // --- empty and unplaceable dates -----------------------------------------

    @Test
    fun `empty history has nothing to project`() {
        val projection = projectionFrom(emptyList())
        assertFalse(projection.hasHistory)
        assertEquals(DayInfo.Unknown, projection.infoFor(date("2026-03-15")))
    }

    @Test
    fun `dates before all logged history cannot be placed`() {
        val info = projectionFrom(regularHistory).infoFor(date("2025-12-31"))
        assertNull(info.phase)
        assertNull(info.cycleDay)
        assertFalse(info.isLoggedPeriod)
    }

    // --- record versus projection --------------------------------------------

    @Test
    fun `a date inside a logged period is menstrual and marked as logged`() {
        val info = projectionFrom(regularHistory).infoFor(date("2026-03-04"))
        assertEquals(CyclePhase.MENSTRUAL, info.phase)
        assertTrue(info.isLoggedPeriod)
    }

    @Test
    fun `a future period is menstrual but not marked as logged`() {
        // 28 days past the last logged start: the app expects a period, the user never logged one.
        val info = projectionFrom(regularHistory).infoFor(date("2026-04-27"))
        assertEquals(CyclePhase.MENSTRUAL, info.phase)
        assertFalse(
            "an expected period must never claim to be a record",
            info.isLoggedPeriod
        )
    }

    @Test
    fun `a menstrual day inferred across a gap in logging is not marked as logged`() {
        // Only one period either side of a six-month silence; the months between are projected.
        val sparse = listOf(period("2026-01-05", "2026-01-09"), period("2026-07-06", "2026-07-10"))
        val info = projectionFrom(sparse).infoFor(date("2026-02-02"))
        assertEquals(CyclePhase.MENSTRUAL, info.phase)
        assertFalse(info.isLoggedPeriod)
    }

    @Test
    fun `an ongoing period covers the projected period length`() {
        val ongoing = regularHistory + period("2026-04-27", null)
        val projection = projectionFrom(ongoing)
        assertTrue(projection.infoFor(date("2026-05-01")).isLoggedPeriod)   // day 5, last covered
        assertFalse(projection.infoFor(date("2026-05-02")).isLoggedPeriod)  // day 6, past the end
    }

    // --- anchoring and cycle day ---------------------------------------------

    @Test
    fun `cycle day counts from the nearest period start at or before the date`() {
        val projection = projectionFrom(regularHistory)
        assertEquals(1, projection.infoFor(date("2026-03-30")).cycleDay)
        assertEquals(9, projection.infoFor(date("2026-04-07")).cycleDay)
    }

    @Test
    fun `dates past the last logged period wrap into the next projected cycle`() {
        val projection = projectionFrom(regularHistory)
        // 2026-04-27 is 28 days after the 2026-03-30 anchor, so it opens a fresh cycle.
        assertEquals(1, projection.infoFor(date("2026-04-27")).cycleDay)
        assertEquals(2, projection.infoFor(date("2026-04-28")).cycleDay)
    }

    @Test
    fun `an unsorted history still anchors to the most recent start`() {
        // projectionFrom must impose the ordering infoFor relies on, whatever the caller passes.
        val shuffled = regularHistory.reversed()
        assertEquals(
            projectionFrom(regularHistory).infoFor(date("2026-04-07")),
            projectionFrom(shuffled).infoFor(date("2026-04-07"))
        )
    }

    // --- medians and phases --------------------------------------------------

    @Test
    fun `regular history yields the personalised 28 by 5 cycle`() {
        val projection = projectionFrom(regularHistory)
        assertEquals(28, projection.cycleLength)
        assertEquals(5, projection.periodLength)
        assertTrue(projection.isPersonalised)
    }

    @Test
    fun `a single period falls back to the defaults and is not personalised`() {
        val projection = projectionFrom(listOf(period("2026-03-30", "2026-04-03")))
        assertEquals(DEFAULT_CYCLE_LENGTH, projection.cycleLength)
        assertEquals(DEFAULT_PERIOD_LENGTH, projection.periodLength)
        assertFalse(projection.isPersonalised)
    }

    @Test
    fun `every day of a projected cycle matches the phase rule for its day number`() {
        val projection = projectionFrom(regularHistory)
        val anchor = date("2026-03-30")
        for (day in 1..28) {
            val info = projection.infoFor(anchor.plusDays(day - 1))
            assertEquals("cycle day $day", day, info.cycleDay)
            // Days 1-5 are the logged period itself; the rest are pure projection.
            val expected = phaseForDay(day, projection.cycleLength, projection.periodLength)
            assertEquals("cycle day $day", expected, info.phase)
        }
    }

    // --- agreement with the one-shot helper ----------------------------------

    @Test
    fun `computePhaseForDate agrees with the projection it delegates to`() {
        val projection = projectionFrom(regularHistory)
        val start = date("2026-01-05")
        for (offset in 0..200) {
            val day = start.plusDays(offset)
            assertEquals(
                "offset $offset",
                projection.infoFor(day).phase,
                computePhaseForDate(day, regularHistory)
            )
        }
    }
}
