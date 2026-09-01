package com.luna.app.domain.usecase

import com.luna.app.data.entity.PeriodEntity
import com.luna.app.domain.model.CyclePhase
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the three edge cases CLAUDE.md calls out as must-handle, plus the ordinary path.
 * Targets the pure computeCycleState() directly — no repository, no coroutines.
 */
class GetCurrentCycleStateTest {

    private fun date(iso: String) = LocalDate.parse(iso)

    /** Six periods on a clean 28-day rhythm, each lasting five days. */
    private fun regularHistory() = listOf(
        PeriodEntity(1, date("2025-11-20"), date("2025-11-24")),
        PeriodEntity(2, date("2025-12-18"), date("2025-12-22")),
        PeriodEntity(3, date("2026-01-15"), date("2026-01-19")),
        PeriodEntity(4, date("2026-02-12"), date("2026-02-16")),
        PeriodEntity(5, date("2026-03-12"), date("2026-03-16")),
        PeriodEntity(6, date("2026-04-09"), date("2026-04-13"))
    )

    @Test
    fun `no periods logged yields no cycle state`() {
        assertNull(computeCycleState(emptyList(), date("2026-04-20")))
    }

    @Test
    fun `periods logged only in the future yield no cycle state`() {
        val future = listOf(PeriodEntity(1, date("2026-05-01"), null))
        assertNull(computeCycleState(future, date("2026-04-20")))
    }

    @Test
    fun `a day inside a logged period is menstrual regardless of the projection`() {
        // Day 3 of the last period. Arithmetic would agree here, but the override is what
        // guarantees it even when history is irregular.
        val state = computeCycleState(regularHistory(), date("2026-04-11"))!!
        assertEquals(CyclePhase.MENSTRUAL, state.currentPhase)
        assertEquals(3, state.cycleDay)
        assertFalse(state.isOverdue)
    }

    @Test
    fun `an ongoing period still covers today via the projected period length`() {
        val history = regularHistory().dropLast(1) +
            PeriodEntity(6, date("2026-04-09"), null)   // ongoing, no end date
        val state = computeCycleState(history, date("2026-04-12"))!!
        assertEquals(CyclePhase.MENSTRUAL, state.currentPhase)
    }

    @Test
    fun `mid cycle resolves to the follicular phase and counts down`() {
        val state = computeCycleState(regularHistory(), date("2026-04-17"))!!  // day 9
        assertEquals(9, state.cycleDay)
        assertEquals(CyclePhase.FOLLICULAR, state.currentPhase)
        assertEquals(28, state.cycleLength)
        assertEquals(5, state.periodLength)
        assertEquals(date("2026-05-07"), state.nextPeriodStart)
        assertEquals(20, state.daysUntilNextPeriod)
        assertTrue(state.isPersonalised)
    }

    @Test
    fun `ovulation lands on the anchored window`() {
        val state = computeCycleState(regularHistory(), date("2026-04-22"))!!  // day 14
        assertEquals(14, state.cycleDay)
        assertEquals(CyclePhase.OVULATION, state.currentPhase)
    }

    @Test
    fun `a late period inside the grace window is not yet overdue`() {
        // nextPeriodStart is 2026-05-07; three days late is still within the grace period.
        val state = computeCycleState(regularHistory(), date("2026-05-10"))!!
        assertEquals(-3, state.daysUntilNextPeriod)
        assertFalse(state.isOverdue)
    }

    @Test
    fun `past the grace window the period is reported overdue`() {
        val state = computeCycleState(regularHistory(), date("2026-05-11"))!!
        assertEquals(-4, state.daysUntilNextPeriod)
        assertTrue(state.isOverdue)
        assertEquals(4, state.overdueDays)
    }

    @Test
    fun `a single logged period falls back to the defaults and says so`() {
        val history = listOf(PeriodEntity(1, date("2026-04-09"), null))
        val state = computeCycleState(history, date("2026-04-20"))!!
        assertEquals(28, state.cycleLength)
        assertEquals(5, state.periodLength)
        assertFalse(state.isPersonalised)
    }

    @Test
    fun `a longer personal cycle moves ovulation later`() {
        // Consistent 32-day rhythm: ovulation should sit on day 18, not day 14.
        val history = listOf(
            PeriodEntity(1, date("2026-01-01"), date("2026-01-05")),
            PeriodEntity(2, date("2026-02-02"), date("2026-02-06")),
            PeriodEntity(3, date("2026-03-06"), date("2026-03-10")),
            PeriodEntity(4, date("2026-04-07"), date("2026-04-11"))
        )
        val state = computeCycleState(history, date("2026-04-24"))!!  // day 18
        assertEquals(32, state.cycleLength)
        assertEquals(18, state.cycleDay)
        assertEquals(CyclePhase.OVULATION, state.currentPhase)
    }
}
