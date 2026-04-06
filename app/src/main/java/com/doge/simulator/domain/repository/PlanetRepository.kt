package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.Planet

interface PlanetRepository {

    // 행성 구매
    suspend fun buyPlanet(planet: Planet)

    // 보유 중인 행성 목록
    suspend fun getOwnedPlanets(): List<Planet>

    // 행성 판매
    suspend fun sellPlanet(planetId: String)

    // 행성 가치(시세) 업데이트
    suspend fun updatePlanetValue(planetId: String, newValue: Int)

}