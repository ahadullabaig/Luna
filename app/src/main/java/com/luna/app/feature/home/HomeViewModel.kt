package com.luna.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.app.data.entity.PeriodEntity
import com.luna.app.data.repo.CycleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cycleRepository: CycleRepository
) : ViewModel() {

    val periodCount: StateFlow<Int> = cycleRepository.getAllPeriods()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun insertFakePeriod() {
        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val fakePeriod = PeriodEntity(
                startDate = today,
                endDate = null
            )
            cycleRepository.insertPeriod(fakePeriod)
        }
    }
}
