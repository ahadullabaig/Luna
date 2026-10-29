package com.luna.app.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.yearMonth
import com.luna.app.data.entity.PeriodEntity
import com.luna.app.domain.model.CyclePhase
import com.luna.app.domain.usecase.overlappingPeriod
import com.luna.app.feature.calendar.components.AdjacentMonthCell
import com.luna.app.feature.calendar.components.DayDetailSheet
import com.luna.app.feature.calendar.components.PhaseDayCell
import com.luna.app.feature.calendar.components.WeekdayHeader
import com.luna.app.ui.common.PeriodDialog
import com.luna.app.ui.common.describe
import com.luna.app.ui.theme.LunaBlush
import com.luna.app.ui.theme.LunaNavyRaised
import com.luna.app.ui.theme.LunaOutline
import com.luna.app.ui.theme.LunaTextPrimary
import com.luna.app.ui.theme.LunaTextSecondary
import com.luna.app.ui.theme.color
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }

    val todayMonth = state.today.toJavaLocalDate().yearMonth
    val startMonth = state.rangeStart.toJavaLocalDate().yearMonth
    val endMonth = state.rangeEnd.toJavaLocalDate().yearMonth
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = todayMonth,
        firstDayOfWeek = firstDayOfWeek,
        // Stop at the last week that holds a real day. The sixth row used to be padding whatever
        // the month; now that out-dates carry their phase there is no reason to manufacture one.
        outDateStyle = OutDateStyle.EndOfRow
    )
    val scope = rememberCoroutineScope()

    // Epoch day rather than the date itself: LocalDate is not one of the types rememberSaveable
    // can persist on its own, and an Int survives process death for free.
    var selectedEpochDay by rememberSaveable { mutableStateOf<Int?>(null) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            IconButton(onClick = onBack, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LunaTextPrimary
                )
            }

            MonthHeader(
                month = calendarState.firstVisibleMonth.yearMonth,
                onToday = { scope.launch { calendarState.animateScrollToMonth(todayMonth) } },
                modifier = Modifier.padding(start = 20.dp, end = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            val weekdayLabels = remember(firstDayOfWeek) {
                daysOfWeek(firstDayOfWeek).map {
                    it.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3)
                }
            }
            WeekdayHeader(
                labels = weekdayLabels,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            HorizontalCalendar(
                state = calendarState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                dayContent = { day ->
                    val date = day.date.toKotlinLocalDate()
                    val info = state.projection.infoFor(date)
                    if (day.position != DayPosition.MonthDate) {
                        AdjacentMonthCell(date = date, info = info)
                    } else {
                        PhaseDayCell(
                            date = date,
                            info = info,
                            isToday = date == state.today,
                            hasLog = state.logsByDate.containsKey(date),
                            onClick = { selectedEpochDay = date.toEpochDays() }
                        )
                    }
                }
            )

            Spacer(Modifier.height(24.dp))
            ProvenanceKey(modifier = Modifier.padding(horizontal = 24.dp))

            if (!state.isLoading && !state.projection.hasHistory) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Log a period on the home screen to see your phases here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LunaTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // By id, like the selected day by epoch day, so an edit in progress survives process death. The
    // period itself is looked up fresh from the projection each time, so it is never stale.
    var editingPeriodId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingPeriodId by rememberSaveable { mutableStateOf<Long?>(null) }

    selectedEpochDay?.let { epochDay ->
        val date = LocalDate.fromEpochDays(epochDay)
        DayDetailSheet(
            date = date,
            info = state.projection.infoFor(date),
            period = state.projection.loggedPeriodOn(date),
            log = state.logsByDate[date],
            isToday = date == state.today,
            onEditPeriod = { editingPeriodId = it.id },
            onDeletePeriod = { deletingPeriodId = it.id },
            onDismiss = { selectedEpochDay = null }
        )
    }

    state.periodWithId(editingPeriodId)?.let { period ->
        val others = state.projection.periodsNewestFirst.filter { it.id != period.id }
        PeriodDialog(
            title = "Edit period",
            guidance = "Tap the first day, then the last. Save with just one day if it is still going.",
            today = state.today,
            initialStart = period.startDate,
            initialEnd = period.endDate,
            conflictWith = { start, end ->
                overlappingPeriod(start, end, others, state.projection.periodLength)
            },
            onDismiss = { editingPeriodId = null },
            onConfirm = { start, end ->
                viewModel.updatePeriod(period.copy(startDate = start, endDate = end))
                editingPeriodId = null
            }
        )
    }

    state.periodWithId(deletingPeriodId)?.let { period ->
        AlertDialog(
            onDismissRequest = { deletingPeriodId = null },
            containerColor = LunaNavyRaised,
            title = { Text("Delete this period?") },
            text = {
                Text(
                    // There is no undo and no backup, so say both plainly before the tap.
                    text = "Luna will forget the period logged ${period.describe()}, and work out " +
                        "your averages without it. This cannot be undone.",
                    color = LunaTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePeriod(period)
                    deletingPeriodId = null
                    // The sheet was about this period; with it gone there is nothing left to show.
                    selectedEpochDay = null
                }) { Text("Delete", color = LunaBlush) }
            },
            dismissButton = {
                TextButton(onClick = { deletingPeriodId = null }) {
                    Text("Cancel", color = LunaTextSecondary)
                }
            }
        )
    }
}

private fun CalendarUiState.periodWithId(id: Long?): PeriodEntity? =
    id?.let { projection.periodsNewestFirst.firstOrNull { period -> period.id == it } }

@Composable
private fun MonthHeader(
    month: YearMonth,
    onToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
            Text(
                text = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                style = MaterialTheme.typography.headlineMedium,
                color = LunaTextPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = month.year.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = LunaTextSecondary
            )
        }
        OutlinedButton(
            onClick = onToday,
            shape = RoundedCornerShape(percent = 50),
            border = BorderStroke(1.dp, LunaOutline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LunaTextSecondary),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = "Today", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * The grid's four colours are named on the home screen, where the ring labels the phase you are
 * in. The one rule that is not self-evident from looking is what an outline means, so that is the
 * only thing the key still says — it replaced a five-item legend that was mostly restating colours
 * the reader could already see.
 */
@Composable
private fun ProvenanceKey(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .border(1.5.dp, CyclePhase.MENSTRUAL.color, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "A hollow ring is a prediction, not something you logged.",
            style = MaterialTheme.typography.bodyMedium,
            color = LunaTextSecondary
        )
    }
}
