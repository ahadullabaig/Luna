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

    /** Mid-cycle after [regularHistory]. The next period is due 2026-04-27, so nothing is late. */
    private val today = date("2026-04-10")

    /** Eight days after that period was due, with nothing logged. */
    private val late = date("2026-05-05")

    private fun projection(periods: List<PeriodEntity>, on: LocalDate = today) = projectionFrom(periods, on)

    // --- empty and unplaceable dates -----------------------------------------

    @Test
    fun `empty history has nothing to project`() {
        val projection = projection(emptyList())
        assertFalse(projection.hasHistory)
        assertEquals(DayInfo.Unknown, projection.infoFor(date("2026-03-15")))
    }

    @Test
    fun `dates before all logged history cannot be placed`() {
        val info = projection(regularHistory).infoFor(date("2025-12-31"))
        assertNull(info.phase)
        assertNull(info.cycleDay)
        assertFalse(info.isLoggedPeriod)
    }

    // --- record versus projection --------------------------------------------

    @Test
    fun `a date inside a logged period is menstrual and marked as logged`() {
        val info = projection(regularHistory).infoFor(date("2026-03-04"))
        assertEquals(CyclePhase.MENSTRUAL, info.phase)
        assertTrue(info.isLoggedPeriod)
    }

    @Test
    fun `a future period is menstrual but not marked as logged`() {
        // 28 days past the last logged start: the app expects a period, the user never logged one.
        val info = projection(regularHistory).infoFor(date("2026-04-27"))
        assertEquals(CyclePhase.MENSTRUAL, info.phase)
        assertFalse(
            "an expected period must never claim to be a record",
            info.isLoggedPeriod
        )
    }

    @Test
    fun `a menstrual day inferred across a gap in logging is not marked as logged`() {
        // One period either side of a six-month silence. Today is after the second, so the months
        // between are an old cycle — which wraps, and draws the periods that went unlogged.
        val sparse = listOf(period("2026-01-05", "2026-01-09"), period("2026-07-06", "2026-07-10"))
        val info = projection(sparse, on = date("2026-07-15")).infoFor(date("2026-02-02"))
        assertEquals(CyclePhase.MENSTRUAL, info.phase)
        assertFalse(info.isLoggedPeriod)
    }

    @Test
    fun `an ongoing period covers the projected period length`() {
        val ongoing = regularHistory + period("2026-04-27", null)
        val projection = projection(ongoing, on = date("2026-05-03"))
        assertTrue(projection.infoFor(date("2026-05-01")).isLoggedPeriod)   // day 5, last covered
        assertFalse(projection.infoFor(date("2026-05-02")).isLoggedPeriod)  // day 6, past the end
    }

    @Test
    fun `days after a period logged as ending early are not drawn as expected`() {
        // The median is five days but the last period was logged as three. Days 4 and 5 are over:
        // a record outranks the projection, in this direction as much as the other.
        val projection = projection(regularHistory.dropLast(1) + period("2026-03-30", "2026-04-01"))
        assertEquals(5, projection.periodLength)
        val dayFour = projection.infoFor(date("2026-04-02"))
        assertEquals(CyclePhase.FOLLICULAR, dayFour.phase)
        assertFalse(dayFour.isLoggedPeriod)
    }

    @Test
    fun `the next projected cycle goes back to the median period length`() {
        // 2026-04-27 opens the next cycle, which nobody has logged, so only the median applies.
        val projection = projection(regularHistory.dropLast(1) + period("2026-03-30", "2026-04-01"))
        val dayFive = projection.infoFor(date("2026-05-01"))
        assertEquals(CyclePhase.MENSTRUAL, dayFive.phase)
        assertFalse(dayFive.isLoggedPeriod)
    }

    // --- anchoring and cycle day ---------------------------------------------

    @Test
    fun `cycle day counts from the nearest period start at or before the date`() {
        val projection = projection(regularHistory)
        assertEquals(1, projection.infoFor(date("2026-03-30")).cycleDay)
        assertEquals(9, projection.infoFor(date("2026-04-07")).cycleDay)
    }

    @Test
    fun `dates from the next expected start repeat the cycle forward`() {
        val projection = projection(regularHistory)
        // 2026-04-27 is 28 days after the 2026-03-30 anchor, so it opens a fresh cycle.
        assertEquals(1, projection.infoFor(date("2026-04-27")).cycleDay)
        assertEquals(2, projection.infoFor(date("2026-04-28")).cycleDay)
    }

    @Test
    fun `an unsorted history still anchors to the most recent start`() {
        // projectionFrom must impose the ordering infoFor relies on, whatever the caller passes.
        val shuffled = regularHistory.reversed()
        assertEquals(
            projection(regularHistory).infoFor(date("2026-04-07")),
            projection(shuffled).infoFor(date("2026-04-07"))
        )
    }

    // --- a late period -------------------------------------------------------

    @Test
    fun `a late cycle keeps counting instead of wrapping into one nobody started`() {
        val info = projection(regularHistory, on = late).infoFor(date("2026-04-28"))
        assertEquals(30, info.cycleDay)
        assertEquals(CyclePhase.LUTEAL, info.phase)
    }

    @Test
    fun `days already behind are never drawn as an expected period`() {
        // The period was due 27 April to 1 May. Those days have passed with nothing logged, so they
        // stay in the running cycle rather than showing a prediction about the past.
        val projection = projection(regularHistory, on = late)
        (0..4).forEach { offset ->
            val day = date("2026-04-27").plusDays(offset)
            assertEquals("$day", CyclePhase.LUTEAL, projection.infoFor(day).phase)
        }
    }

    @Test
    fun `while late, the expected period starts tomorrow and later cycles follow from it`() {
        val projection = projection(regularHistory, on = late)
        assertEquals(CyclePhase.LUTEAL, projection.infoFor(late).phase)
        val tomorrow = projection.infoFor(date("2026-05-06"))
        assertEquals(CyclePhase.MENSTRUAL, tomorrow.phase)
        assertFalse(tomorrow.isLoggedPeriod)
        assertEquals(1, projection.infoFor(date("2026-06-03")).cycleDay)  // one cycle on
    }

    @Test
    fun `the expected start moves forward a day for every day it is late`() {
        assertEquals(date("2026-05-06"), projection(regularHistory, on = late).nextExpectedStart)
        assertEquals(date("2026-05-07"), projection(regularHistory, on = date("2026-05-06")).nextExpectedStart)
        // On time, it is simply a cycle length after the last start.
        assertEquals(date("2026-04-27"), projection(regularHistory).nextExpectedStart)
    }

    // --- medians and phases --------------------------------------------------

    @Test
    fun `regular history yields the personalised 28 by 5 cycle`() {
        val projection = projection(regularHistory)
        assertEquals(28, projection.cycleLength)
        assertEquals(5, projection.periodLength)
        assertTrue(projection.isPersonalised)
    }

    @Test
    fun `a single period falls back to the defaults and is not personalised`() {
        val projection = projection(listOf(period("2026-03-30", "2026-04-03")))
        assertEquals(DEFAULT_CYCLE_LENGTH, projection.cycleLength)
        assertEquals(DEFAULT_PERIOD_LENGTH, projection.periodLength)
        assertFalse(projection.isPersonalised)
    }

    @Test
    fun `every day of the running cycle matches the phase rule for its day number`() {
        val projection = projection(regularHistory)
        val anchor = date("2026-03-30")
        for (day in 1..28) {
            val info = projection.infoFor(anchor.plusDays(day - 1))
            assertEquals("cycle day $day", day, info.cycleDay)
            // Days 1-5 are the logged period itself; the rest are pure projection.
            val expected = phaseForDay(day, projection.cycleLength, projection.periodLength)
            assertEquals("cycle day $day", expected, info.phase)
        }
    }
}
