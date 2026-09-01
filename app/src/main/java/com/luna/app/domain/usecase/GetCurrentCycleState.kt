package com.luna.app.domain.usecase

import com.luna.app.data.repo.CycleRepository
import com.luna.app.domain.model.CycleState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/**
 * Observes the user's current cycle position, re-emitting whenever period history changes.
 *
 * Reads the full history as a [Flow] rather than calling PeriodDao.getRecentPeriods(), which is
 * one-shot and would leave the computed state stale after a new period is logged. The six-cycle
 * window is applied in memory by [computeCycleState] instead — history is a handful of rows, not
 * a scan worth pushing into SQL.
 */
class GetCurrentCycleState @Inject constructor(
    private val repository: CycleRepository
) {
    operator fun invoke(today: LocalDate): Flow<CycleState?> =
        repository.getAllPeriods().map { periods -> computeCycleState(periods, today) }
}
