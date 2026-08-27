package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
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
    val discoveredPlanetType: String?,
    // 완료 결과(자원 획득·행성 발견)를 UI에 보여줬는지 여부. 완료 직후 false로 설정되며,
    // 포그라운드 폴링과 백그라운드 워커 중 어느 쪽이 완료 처리를 선점하든 상관없이
    // 이 플래그를 기준으로 결과 팝업을 띄운다
    val resultHandled: Boolean = true,
    val coinsEarned: Long = 0L
)
