package com.luna.app.domain.usecase

import com.luna.app.data.repo.CycleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Exposes phase projection for arbitrary dates — the lookup the calendar grid needs.
 *
 * Emits a whole [CycleProjection] rather than a single date's phase so a month of cells can be
 * coloured from one collection of history instead of one query per day.
 */
class GetPhaseForDate @Inject constructor(
    private val repository: CycleRepository
) {
    operator fun invoke(): Flow<CycleProjection> =
        repository.getAllPeriods().map(::projectionFrom)
}
