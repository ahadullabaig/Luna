package com.luna.app.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.kizitonwose.calendar.core.nextMonth
import com.kizitonwose.calendar.core.previousMonth
import com.kizitonwose.calendar.core.yearMonth
import com.luna.app.domain.model.CyclePhase
import com.luna.app.feature.calendar.components.AdjacentMonthCell
import com.luna.app.feature.calendar.components.DayDetailSheet
import com.luna.app.feature.calendar.components.PhaseDayCell
import com.luna.app.feature.calendar.components.WeekdayHeader
import com.luna.app.ui.theme.LunaCream
import com.luna.app.ui.theme.color
import com.luna.app.ui.theme.label
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
        // Every month gets a full 6x7 grid, so the legend below it does not jump as you page.
        outDateStyle = OutDateStyle.EndOfGrid
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
            TopBar(
                onBack = onBack,
                onToday = { scope.launch { calendarState.animateScrollToMonth(todayMonth) } }
            )

            val visibleMonth = calendarState.firstVisibleMonth.yearMonth
            MonthNav(
                month = visibleMonth,
                canGoBack = visibleMonth > startMonth,
                canGoForward = visibleMonth < endMonth,
                onPrevious = { scope.launch { calendarState.animateScrollToMonth(visibleMonth.previousMonth) } },
                onNext = { scope.launch { calendarState.animateScrollToMonth(visibleMonth.nextMonth) } }
            )

            Spacer(Modifier.height(8.dp))

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
                    if (day.position != DayPosition.MonthDate) {
                        AdjacentMonthCell(date = day.date.toKotlinLocalDate())
                    } else {
                        val date = day.date.toKotlinLocalDate()
                        PhaseDayCell(
                            date = date,
                            info = state.projection.infoFor(date),
                            isToday = date == state.today,
                            hasLog = state.logsByDate.containsKey(date),
                            onClick = { selectedEpochDay = date.toEpochDays() }
                        )
                    }
                }
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = LunaCream.copy(alpha = 0.08f))
            Spacer(Modifier.height(20.dp))

            Legend(modifier = Modifier.padding(horizontal = 24.dp))

            if (!state.isLoading && !state.projection.hasHistory) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Log a period on the home screen to see your phases here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LunaCream.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    selectedEpochDay?.let { epochDay ->
        val date = LocalDate.fromEpochDays(epochDay)
        DayDetailSheet(
            date = date,
            info = state.projection.infoFor(date),
            log = state.logsByDate[date],
            isToday = date == state.today,
            onDismiss = { selectedEpochDay = null }
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onToday: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = LunaCream
            )
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onToday) {
            Text(
                text = "Today",
                style = MaterialTheme.typography.labelLarge,
                color = LunaCream.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun MonthNav(
    month: YearMonth,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, enabled = canGoBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous month",
                tint = LunaCream.copy(alpha = if (canGoBack) 0.8f else 0.2f)
            )
        }
        Text(
            text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
            style = MaterialTheme.typography.titleLarge,
            color = LunaCream,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = LunaCream.copy(alpha = if (canGoForward) 0.8f else 0.2f)
            )
        }
    }
}

/**
 * Four colours and one outline are not self-evident, so the key is part of the screen rather
 * than something to be inferred.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CyclePhase.entries.forEach { phase ->
                LegendItem(swatch = phase.color, filled = true, label = phase.label)
            }
        }
        LegendItem(
            swatch = CyclePhase.MENSTRUAL.color,
            filled = false,
            label = "Expected — not logged yet"
        )
    }
}

@Composable
private fun LegendItem(swatch: Color, filled: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .then(
                    if (filled) Modifier.background(swatch)
                    else Modifier.border(1.5.dp, swatch, CircleShape)
                )
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LunaCream.copy(alpha = 0.6f)
        )
    }
}

