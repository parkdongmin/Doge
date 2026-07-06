package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Spaceship
import com.doge.simulator.domain.repository.SpaceshipRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSpaceshipsUseCase @Inject constructor(
    private val repository: SpaceshipRepository
) {
    operator fun invoke(): Flow<List<Spaceship>> = repository.getSpaceships()
}
