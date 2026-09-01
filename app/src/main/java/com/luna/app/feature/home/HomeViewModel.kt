package com.luna.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.app.data.entity.DailyLogEntity
import com.luna.app.data.entity.PeriodEntity
import com.luna.app.data.repo.CycleRepository
import com.luna.app.domain.model.Energy
import com.luna.app.domain.model.FlowLevel
import com.luna.app.domain.usecase.GetCurrentCycleState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CycleRepository,
    getCurrentCycleState: GetCurrentCycleState
) : ViewModel() {

    // Re-read on resume rather than captured once, so a phone left open overnight does not
    // keep showing yesterday's cycle day.
    private val today = MutableStateFlow(currentDate())

    val uiState: StateFlow<HomeUiState> = today
        .flatMapLatest { date ->
            combine(
                getCurrentCycleState(date),
                repository.getDailyLog(date)
            ) { cycle, log ->
                HomeUiState(today = date, isLoading = false, cycle = cycle, todayLog = log)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(today = today.value)
        )

    // upsertDailyLog replaces the whole row rather than merging columns, so every chip tap has
    // to write a complete entity. Serialising through a mutex and carrying the last write
    // forward keeps two quick taps from racing and dropping one of the two changes.
    private val logMutex = Mutex()
    private var lastWrittenLog: DailyLogEntity? = null

    fun refreshToday() {
        val current = currentDate()
        if (current != today.value) {
            lastWrittenLog = null   // yesterday's row must not seed today's edits
            today.value = current
        }
    }

    fun setFlowLevel(level: FlowLevel?) = updateLog { it.copy(flowLevel = level) }

    fun setEnergy(energy: Energy?) = updateLog { it.copy(energy = energy) }

    fun togglePainFlag(flag: Int) = updateLog { it.copy(painFlags = it.painFlags xor flag) }

    fun toggleBodyFlag(flag: Int) = updateLog { it.copy(bodyFlags = it.bodyFlags xor flag) }

    fun logPeriod(start: LocalDate, end: LocalDate?) {
        viewModelScope.launch {
            repository.insertPeriod(PeriodEntity(startDate = start, endDate = end))
        }
    }

    private fun updateLog(transform: (DailyLogEntity) -> DailyLogEntity) {
        viewModelScope.launch {
            logMutex.withLock {
                val date = today.value
                val base = lastWrittenLog?.takeIf { it.date == date }
                    ?: uiState.value.todayLog
                    ?: DailyLogEntity(date = date)
                val updated = transform(base)
                lastWrittenLog = updated
                repository.upsertDailyLog(updated)
            }
        }
    }

    private fun currentDate(): LocalDate =
        Clock.System.todayIn(TimeZone.currentSystemDefault())
}
