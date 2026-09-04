package com.luna.app.feature.home.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luna.app.ui.theme.LunaBlush
import com.luna.app.ui.theme.LunaTextFaint
import com.luna.app.ui.theme.LunaTextPrimary
import com.luna.app.ui.theme.LunaTextSecondary
import com.luna.app.ui.theme.LunaDeepNavy
import kotlinx.datetime.LocalDate

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Full-screen range picker for logging a period.
 *
 * A range rather than a single date because [com.luna.app.data.entity.PeriodEntity] is a range,
 * and because leaving the second date unset is how an ongoing period is expressed — the same
 * gesture covers both cases. Future dates are unselectable: a period cannot start tomorrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogPeriodDialog(
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (start: LocalDate, end: LocalDate?) -> Unit
) {
    val todayMillis = remember(today) { today.toEpochDays().toLong() * MILLIS_PER_DAY }
    val state = rememberDateRangePickerState(
        selectableDates = remember(todayMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
                override fun isSelectableYear(year: Int) = year <= today.year
            }
        }
    )

    val startMillis = state.selectedStartDateMillis

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(color = LunaDeepNavy, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Log a period") },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = LunaTextSecondary)
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val start = startMillis?.toLocalDate() ?: return@TextButton
                                onConfirm(start, state.selectedEndDateMillis?.toLocalDate())
                            },
                            enabled = startMillis != null
                        ) {
                            Text(
                                text = "Save",
                                color = if (startMillis != null) LunaBlush else LunaTextFaint
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = LunaDeepNavy,
                        titleContentColor = LunaTextPrimary
                    )
                )

                Text(
                    text = "Pick the first day. Tap a second day to set the end, " +
                        "or save with just one if your period is still going.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LunaTextSecondary,
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

/** Picker millis are UTC midnight, so this is an exact whole-day conversion. */
private fun Long.toLocalDate(): LocalDate =
    LocalDate.fromEpochDays(Math.floorDiv(this, MILLIS_PER_DAY).toInt())
