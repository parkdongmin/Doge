package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.repository.ExpeditionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveExpeditionsUseCase @Inject constructor(
    private val repository: ExpeditionRepository
) {
    operator fun invoke(): Flow<List<Expedition>> = repository.getActive()
}
