package com.doge.simulator.data.local.mapper

import com.doge.simulator.data.local.entity.ExpeditionEntity
import com.doge.simulator.data.local.entity.PlanetEntity
import com.doge.simulator.data.local.snapshot.snapshotJson
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionCategory
import com.doge.simulator.domain.model.ExpeditionStatus
import com.doge.simulator.domain.model.Planet

fun ExpeditionEntity.toDomain() = Expedition(
    id = id,
    category = ExpeditionCategory.valueOf(category),
    tier = tier,
    astronautIds = astronautIds.split(",").filter { it.isNotBlank() },
    spaceshipId = spaceshipId,
    startTime = startTime,
    endTime = endTime,
    status = ExpeditionStatus.valueOf(status),
    resourcesResult = resourcesResult,
    discoveredPlanetType = discoveredPlanetType,
    coinsEarned = coinsEarned,
    discoveredPlanet = decodeDiscoveredPlanet(discoveredPlanetJson)
)

fun Expedition.toEntity() = ExpeditionEntity(
    id = id,
    category = category.name,
    tier = tier,
    astronautIds = astronautIds.joinToString(","),
    spaceshipId = spaceshipId,
    startTime = startTime,
    endTime = endTime,
    status = status.name,
    resourcesResult = resourcesResult,
    discoveredPlanetType = discoveredPlanetType,
    resultHandled = true,
    coinsEarned = coinsEarned,
    discoveredPlanetJson = encodeDiscoveredPlanet(discoveredPlanet)
)

// 탐사 완료 시 한 번 굴려 확정한 발견 행성 — PlanetEntity JSON으로 저장한다(클라우드 스냅샷과 같은 Json)
fun encodeDiscoveredPlanet(planet: Planet?): String? =
    planet?.let { snapshotJson.encodeToString(PlanetEntity.serializer(), it.toEntity()) }

// 깨진 값이면 null — 결과 창이 옛 방식(종류만 보고 새로 굴림)으로 넘어간다
fun decodeDiscoveredPlanet(json: String?): Planet? =
    json?.let { runCatching { snapshotJson.decodeFromString(PlanetEntity.serializer(), it).toDomain() }.getOrNull() }
