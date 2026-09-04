package com.luna.app.feature.home.components

import com.luna.app.domain.model.CyclePhase
import com.luna.app.domain.model.CycleState
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one number in the middle of the ring switches meaning depending on where in the cycle you
 * are, which is exactly the kind of rule that breaks quietly. Pure function, no Compose.
 */
class HeroReadingTest {

    private fun state(
        phase: CyclePhase = CyclePhase.FOLLICULAR,
        cycleDay: Int = 8,
        daysUntil: Int = 20,
        overdue: Boolean = false
    ) = CycleState(
        currentPhase = phase,
        cycleDay = cycleDay,
        cycleLength = 28,
        periodLength = 5,
        nextPeriodStart = LocalDate.parse("2026-09-24"),
        daysUntilNextPeriod = daysUntil,
        isOverdue = overdue,
        isPersonalised = true
    )

    @Test
    fun `outside a period the number counts down to the next one`() {
        val reading = heroReading(state(daysUntil = 21))
        assertEquals("21", reading.value)
        assertEquals("days to your period", reading.caption)
        assertFalse(reading.alert)
    }

    @Test
    fun `during a period the number is the day of that period, not a countdown`() {
        // "20 days to your period" is a non-answer while you are having one.
        val reading = heroReading(state(phase = CyclePhase.MENSTRUAL, cycleDay = 3))
        assertEquals("3", reading.value)
        assertEquals("day of your period", reading.caption)
        assertFalse(reading.alert)
    }

    @Test
    fun `being overdue outranks being in a period`() {
        val reading = heroReading(
            state(phase = CyclePhase.MENSTRUAL, cycleDay = 3, daysUntil = -6, overdue = true)
        )
        assertEquals("6", reading.value)
        assertEquals("days late", reading.caption)
        assertTrue(reading.alert)
    }

    @Test
    fun `a single day is not pluralised`() {
        assertEquals("day to your period", heroReading(state(daysUntil = 1)).caption)
        assertEquals(
            "day late",
            heroReading(state(daysUntil = -1, overdue = true)).caption
        )
    }

    @Test
    fun `the day it is due, and the grace days after, read as due rather than zero`() {
        val dueToday = heroReading(state(daysUntil = 0))
        assertEquals("Due", dueToday.value)
        assertEquals("today", dueToday.caption)

        // Past the expected date but inside the grace window, so not yet overdue.
        val grace = heroReading(state(daysUntil = -2))
        assertEquals("Due", grace.value)
        assertEquals("any day now", grace.caption)
    }

    @Test
    fun `the value never renders a bare zero or a negative`() {
        (-10..40).forEach { days ->
            listOf(CyclePhase.MENSTRUAL, CyclePhase.LUTEAL).forEach { phase ->
                val reading = heroReading(state(phase = phase, daysUntil = days))
                assertFalse("days=$days phase=$phase", reading.value.startsWith("-"))
                assertFalse("days=$days phase=$phase", reading.value == "0")
            }
        }
    }
}
