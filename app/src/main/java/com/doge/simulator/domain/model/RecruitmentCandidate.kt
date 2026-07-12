package com.doge.simulator.domain.model

import java.util.UUID
import kotlin.random.Random

data class RecruitmentCandidate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val specialty: AstronautSpecialty,
    val grade: AstronautGrade,
    val proficiency: Int
) {
    // 같은 등급 안에서도 숙련도 롤이 높을수록 조금 더 비싸짐 (등급 경계를 넘어서지는 않음)
    fun hireCost(): Long {
        val range = grade.startProficiencyRange
        val positionInGrade = (proficiency - range.first).toFloat() / (range.last - range.first)
        val proficiencyMultiplier = 1.0f + positionInGrade.coerceIn(0f, 1f) * GameConstants.HIRE_COST_PROFICIENCY_SPREAD
        return (GameConstants.ASTRONAUT_BASE_HIRE_COST * grade.hireCostMultiplier * proficiencyMultiplier).toLong()
    }

    companion object {
        private val NAME_POOL = mapOf(
            AstronautSpecialty.MINERAL to listOf("박 탐석", "김 광부", "이 지질", "최 채굴", "정 광석"),
            AstronautSpecialty.PLANET to listOf("유 우주", "강 탐사", "조 행성", "윤 항법", "장 천문"),
            AstronautSpecialty.RUINS to listOf("임 고고", "한 유물", "오 발굴", "서 역사", "신 탐험"),
            AstronautSpecialty.ALIEN_CIVILIZATION to listOf("권 외계", "황 연구", "안 분석", "송 접촉", "전 번역")
        )

        fun random(): RecruitmentCandidate {
            val specialty = AstronautSpecialty.entries.random()
            val grade = AstronautGrade.random()
            val proficiency = Random.nextInt(
                grade.startProficiencyRange.first,
                grade.startProficiencyRange.last + 1
            )
            return RecruitmentCandidate(
                name = NAME_POOL.getValue(specialty).random(),
                specialty = specialty,
                grade = grade,
                proficiency = proficiency
            )
        }
    }
}