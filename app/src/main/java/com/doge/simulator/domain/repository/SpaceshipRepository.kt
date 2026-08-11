package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.Spaceship
import kotlinx.coroutines.flow.Flow

interface SpaceshipRepository {
    fun getSpaceships(): Flow<List<Spaceship>>
    suspend fun buy(spaceship: Spaceship)
    // 반환값이 false면 expectedGrade와 현재 등급이 달라(중복 강화 시도 등) 반영되지 않았다는 뜻
    suspend fun upgrade(id: String, expectedGrade: Int, grade: Int, crewCapacity: Int, speed: Int, cargo: Int, successRate: Float): Boolean
    suspend fun sell(id: String)
}
