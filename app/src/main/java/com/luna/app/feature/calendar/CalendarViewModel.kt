package com.luna.app.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.app.data.entity.PeriodEntity
import com.luna.app.data.repo.CycleRepository
import com.luna.app.domain.currentDate
import com.luna.app.domain.usecase.GetPhaseForDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import javax.inject.Inject

/** How far the grid scrolls either side of today. Two years back covers any useful history. */
private const val MONTHS_BACK = 24
private const val MONTHS_FORWARD = 12

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CycleRepository,
    getPhaseForDate: GetPhaseForDate
) : ViewModel() {

    private val today = MutableStateFlow(currentDate())

    val uiState: StateFlow<CalendarUiState> = today
        .flatMapLatest { date ->
            val start = date.minus(DatePeriod(months = MONTHS_BACK)).startOfMonth()
            val end = date.plus(DatePeriod(months = MONTHS_FORWARD)).endOfMonth()
            // One log query for the whole scrollable range: a few years of a single user's rows
            // is a trivial table, and querying per visible month would re-subscribe on every swipe.
            combine(
                getPhaseForDate(date),
                repository.getDailyLogsBetween(start, end)
            ) { projection, logs ->
                CalendarUiState(
                    today = date,
                    rangeStart = start,
                    rangeEnd = end,
                    isLoading = false,
                    projection = projection,
                    // Cleared-out rows are dropped here so the grid's dot and the day sheet
                    // agree on what counts as a logged day.
                    logsByDate = logs.filterNot { it.isEmpty }.associateBy { it.date }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = today.value.let { date ->
                CalendarUiState(
                    today = date,
                    rangeStart = date.minus(DatePeriod(months = MONTHS_BACK)).startOfMonth(),
                    rangeEnd = date.plus(DatePeriod(months = MONTHS_FORWARD)).endOfMonth()
                )
            }
        )

    fun refreshToday() {
        val current = currentDate()
        if (current != today.value) today.value = current
    }

    // The calendar is the only place a past period can be seen, so it is where one is corrected.
    // Both writes land on the same getAllPeriods() flow that Home reads, so neither screen needs
    // telling.

    fun updatePeriod(period: PeriodEntity) {
        viewModelScope.launch { repository.updatePeriod(period) }
    }

    fun deletePeriod(period: PeriodEntity) {
        viewModelScope.launch { repository.deletePeriod(period) }
    }
}

private fun LocalDate.startOfMonth(): LocalDate = LocalDate(year, month, 1)

private fun LocalDate.endOfMonth(): LocalDate =
    startOfMonth().plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
