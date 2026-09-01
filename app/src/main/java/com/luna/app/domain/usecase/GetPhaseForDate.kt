package com.luna.app.domain.usecase

import com.luna.app.data.repo.CycleRepository
import com.luna.app.domain.model.CyclePhase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/**
 * Exposes phase projection for arbitrary dates — the lookup the M4 calendar grid needs.
 *
 * Emits a resolver rather than a single phase so a month of cells can be coloured from one
 * collection of history instead of one query per day.
 */
class GetPhaseForDate @Inject constructor(
    private val repository: CycleRepository
) {
    operator fun invoke(): Flow<(LocalDate) -> CyclePhase?> =
        repository.getAllPeriods().map { periods ->
            { date: LocalDate -> computePhaseForDate(date, periods) }
        }
}
