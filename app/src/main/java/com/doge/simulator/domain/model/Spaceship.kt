package com.doge.simulator.domain.model

import java.util.UUID

data class Spaceship(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val grade: Int = 1,
    // 탑승 가능한 우주인 수
    val crewCapacity: Int,
    // 탐사 속도 (높을수록 소요 시간 단축, 0~100)
    val speed: Int,
    // 적재량 (높을수록 자원 획득량 증가, 0~100)
    val cargo: Int,
    // 기본 탐사 성공률 보정 (0.0~1.0)
    val successRate: Float,
    val purchasedAt: Long = System.currentTimeMillis()
)
