package com.doge.simulator.domain.model

import kotlin.random.Random

enum class AstronautGrade(
    val displayName: String,
    val spawnWeight: Int,
    val startProficiencyRange: IntRange,
    val proficiencyCap: Int,
    val hireCostMultiplier: Double
) {
    INTERN("인턴 대원", 65, 10..25, 50, 1.0),
    REGULAR("정규 대원", 25, 25..40, 65, 1.5),
    SENIOR("수석 대원", 6, 40..55, 80, 2.5),
    VETERAN("베테랑 대원", 3, 55..70, 90, 4.0),
    LEGEND("레전드 대원", 1, 70..90, 100, 8.0);

    companion object {
        private val totalWeight = entries.sumOf { it.spawnWeight }

        fun random(): AstronautGrade {
            var roll = Random.nextInt(totalWeight)
            for (grade in entries) {
                if (roll < grade.spawnWeight) return grade
                roll -= grade.spawnWeight
            }
            return entries.last()
        }
    }
}