package com.doge.simulator.domain.model

import java.util.UUID

data class Expedition(
    val id: String = UUID.randomUUID().toString(),
    val category: ExpeditionCategory,
    val tier: Int,
    val astronautIds: List<String>,
    val spaceshipId: String,
    val startTime: Long,
    val endTime: Long,
    val status: ExpeditionStatus = ExpeditionStatus.IN_PROGRESS,
    // "IRON_ORE:5,CRYSTAL:2" 형식, 완료 시 채워짐
    val resourcesResult: String? = null,
    // 행성 발견 시 PlanetType 이름, 없으면 null
    val discoveredPlanetType: String? = null
)
