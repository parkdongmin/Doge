package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.ResearchLab
import com.doge.simulator.domain.repository.ResearchLabRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetResearchLabUseCase @Inject constructor(
    private val repository: ResearchLabRepository
) {
    operator fun invoke(): Flow<ResearchLab> = repository.get()
}
