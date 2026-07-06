package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Resource
import com.doge.simulator.domain.repository.ResourceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetResourcesUseCase @Inject constructor(
    private val repository: ResourceRepository
) {
    operator fun invoke(): Flow<List<Resource>> = repository.getAll()
}
