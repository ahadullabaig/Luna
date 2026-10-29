package com.luna.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luna.app.data.entity.PeriodEntity
import com.luna.app.ui.theme.LunaBlush
import com.luna.app.ui.theme.LunaDeepNavy
import com.luna.app.ui.theme.LunaTextFaint
import com.luna.app.ui.theme.LunaTextPrimary
import com.luna.app.ui.theme.LunaTextSecondary
import kotlinx.datetime.LocalDate

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Full-screen range picker for a period: logging a new one, ending one still going, or correcting
 * one already on record. One dialog for all three, because they are the same act — say which days
 * — and [PeriodEntity] is a range everywhere else.
 *
 * Leaving the second date unset is how an ongoing period is expressed. Future dates are
 * unselectable: a period cannot start tomorrow.
 *
 * [conflictWith] names a logged period the chosen days would overlap, and Save stays disabled while
 * there is one. Save is also disabled until the selection differs from what was passed in, so
 * opening an existing period and saving it untouched is not a write.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodDialog(
    title: String,
    guidance: String,
    today: LocalDate,
    conflictWith: (start: LocalDate, end: LocalDate?) -> PeriodEntity?,
    onDismiss: () -> Unit,
    onConfirm: (start: LocalDate, end: LocalDate?) -> Unit,
    initialStart: LocalDate? = null,
    initialEnd: LocalDate? = null
) {
    val todayMillis = remember(today) { today.toMillis() }
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStart?.toMillis(),
        initialSelectedEndDateMillis = initialEnd?.toMillis(),
        selectableDates = remember(todayMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
                override fun isSelectableYear(year: Int) = year <= today.year
            }
        }
    )

    val start = state.selectedStartDateMillis?.toLocalDate()
    val end = state.selectedEndDateMillis?.toLocalDate()
    val conflict = start?.let { conflictWith(it, end) }
    val canSave = start != null && conflict == null && (start != initialStart || end != initialEnd)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(color = LunaDeepNavy, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = LunaTextSecondary)
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { if (start != null) onConfirm(start, end) },
                            enabled = canSave
                        ) {
                            Text(
                                text = "Save",
                                color = if (canSave) LunaBlush else LunaTextFaint
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = LunaDeepNavy,
                        titleContentColor = LunaTextPrimary
                    )
                )

                // The overlap replaces the guidance rather than sitting under it: while it stands,
                // it is the only thing worth reading, and it says what to do instead.
                Text(
                    text = if (conflict == null) guidance else "Those days overlap a period " +
                        "already logged (${conflict.describe()}). Change that one from the calendar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (conflict == null) LunaTextSecondary else LunaBlush,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )

                DateRangePicker(
                    state = state,
                    modifier = Modifier.weight(1f),
                    title = null,
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
                        containerColor = LunaDeepNavy,
                        titleContentColor = LunaTextPrimary,
                        headlineContentColor = LunaTextPrimary,
                        weekdayContentColor = LunaTextSecondary,
                        subheadContentColor = LunaTextSecondary,
                        dayContentColor = LunaTextPrimary,
                        selectedDayContentColor = LunaDeepNavy,
                        selectedDayContainerColor = LunaBlush,
                        todayContentColor = LunaBlush,
                        todayDateBorderColor = LunaBlush,
                        // The two endpoints are solid blush and take navy. The days between them
                        // are a wash of the same colour, which navy cannot be read on at all
                        // (2.21:1) — cream clears it at 8.07:1, and the endpoints stay distinct
                        // because they are the only solid ones.
                        dayInSelectionRangeContentColor = LunaTextPrimary,
                        dayInSelectionRangeContainerColor = LunaBlush.copy(alpha = 0.35f)
                    )
                )
            }
        }
    }
}

/**
 * "1 to 5 March", "28 February to 3 March", or "started 1 March" while it has no end. English, like
 * the rest of the app's dates; the year is left out because a period you are looking at is recent.
 */
fun PeriodEntity.describe(): String {
    val end = endDate ?: return "started ${startDate.dayMonth()}"
    return when {
        end == startDate -> startDate.dayMonth()
        end.month == startDate.month && end.year == startDate.year ->
            "${startDate.dayOfMonth} to ${end.dayMonth()}"
        else -> "${startDate.dayMonth()} to ${end.dayMonth()}"
    }
}

private fun LocalDate.dayMonth(): String =
    "$dayOfMonth ${month.name.lowercase().replaceFirstChar { it.uppercase() }}"

/** Picker millis are UTC midnight, so these are exact whole-day conversions. */
private fun LocalDate.toMillis(): Long = toEpochDays().toLong() * MILLIS_PER_DAY

private fun Long.toLocalDate(): LocalDate =
    LocalDate.fromEpochDays(Math.floorDiv(this, MILLIS_PER_DAY).toInt())
