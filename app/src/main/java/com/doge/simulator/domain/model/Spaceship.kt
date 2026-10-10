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
) {
    // 탐사 시간 단축 비율(0.2 = 20% 단축). 탐사 계산과 화면 표기("시간 −20%")가 같은 식을 쓰도록 한곳에 둔다
    val durationReduction: Double get() = speed / 200.0

    // 자원 획득량 보정(0.0 = 기본, 0.1 = +10%). 적재 50이 기준
    val cargoBonus: Double get() = cargoBonusOf(cargo)

    // 기본 정찰선 대비 자원 증가율 — 화면 표기용. 정찰선 적재(40)가 계산 기준(50)보다 낮아
    // cargoBonus를 그대로 보여주면 처음 받는 우주선이 "자원 −10%"로 보여서, 정찰선을 +0%로 놓고
    // 실제 배율 비율로 환산한다(등급 2 = +11%). 실제 자원량 계산은 그대로
    val cargoGainOverScout: Double
        get() = (1.0 + cargoBonus) / (1.0 + cargoBonusOf(GameConstants.SCOUT_CARGO_BASE)) - 1.0

    private companion object {
        fun cargoBonusOf(cargo: Int): Double = (cargo - 50) / 100.0
    }
}

// 탐사 성공률: 우주선 기본값 + 전문 분야 일치 보너스(매칭 전문가 중 최고 숙련도 1명 기준).
// 실제 판정(CompleteExpeditionUseCase)과 파견 화면의 예상 성공률이 같은 식을 쓴다
fun expeditionSuccessChance(
    spaceship: Spaceship?,
    astronauts: List<Astronaut>,
    category: ExpeditionCategory
): Float {
    val shipBaseRate = spaceship?.successRate ?: GameConstants.SCOUT_SUCCESS_RATE_BASE
    val maxProficiency = astronauts
        .filter { it.specialty.relatedCategory == category }
        .maxOfOrNull { it.proficiency } ?: 0
    val specialtyBonus = (maxProficiency / 100f) * GameConstants.SPECIALTY_PROFICIENCY_SUCCESS_COEFFICIENT
    return (shipBaseRate + specialtyBonus).coerceIn(0.1f, 0.95f)
}
