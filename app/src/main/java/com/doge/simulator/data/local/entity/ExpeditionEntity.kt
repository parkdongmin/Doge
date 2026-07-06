package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expedition_table")
data class ExpeditionEntity(
    @PrimaryKey val id: String,
    val category: String,
    val tier: Int,
    // "id1,id2,id3" 형식
    val astronautIds: String,
    val spaceshipId: String,
    val startTime: Long,
    val endTime: Long,
    val status: String,
    // "IRON_ORE:5,CRYSTAL:2" 형식, 완료 시 채워짐
    val resourcesResult: String?,
    val discoveredPlanetType: String?
)
