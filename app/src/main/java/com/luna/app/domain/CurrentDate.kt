package com.luna.app.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * The user's local calendar date.
 *
 * Read on demand and never cached — every screen re-reads it on resume so a phone left open
 * overnight rolls over instead of showing yesterday. Shared so both screens agree on which
 * timezone defines "today".
 */
fun currentDate(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
