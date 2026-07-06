package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.repository.AstronautRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAstronautsUseCase @Inject constructor(
    private val repository: AstronautRepository
) {
    operator fun invoke(): Flow<List<Astronaut>> = repository.getAstronauts()
}
