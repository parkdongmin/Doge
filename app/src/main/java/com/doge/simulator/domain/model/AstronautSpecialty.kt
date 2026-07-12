package com.doge.simulator.domain.model

import androidx.annotation.DrawableRes
import com.doge.simulator.R

enum class AstronautSpecialty(
    val displayName: String,
    val icon: String,
    val relatedCategory: ExpeditionCategory
) {
    MINERAL("광물 전문가", "⛏️", ExpeditionCategory.MINERAL),
    PLANET("행성 탐사가", "🪐", ExpeditionCategory.PLANET),
    RUINS("유적 발굴가", "🏛️", ExpeditionCategory.RUINS),
    ALIEN_CIVILIZATION("외계 연구가", "👽", ExpeditionCategory.ALIEN_CIVILIZATION);

    // 전문 분야 × 등급(인턴~레전드) 조합별 캐릭터 이미지
    @DrawableRes
    fun characterImageRes(grade: AstronautGrade): Int = CHARACTER_IMAGES.getValue(this)[grade.ordinal]

    companion object {
        private val CHARACTER_IMAGES: Map<AstronautSpecialty, List<Int>> = mapOf(
            MINERAL to listOf(R.drawable.ch_mineral_1, R.drawable.ch_mineral_2, R.drawable.ch_mineral_3, R.drawable.ch_mineral_4, R.drawable.ch_mineral_5),
            PLANET to listOf(R.drawable.ch_planet_1, R.drawable.ch_planet_2, R.drawable.ch_planet_3, R.drawable.ch_planet_4, R.drawable.ch_planet_5),
            RUINS to listOf(R.drawable.ch_ruins_1, R.drawable.ch_ruins_2, R.drawable.ch_ruins_3, R.drawable.ch_ruins_4, R.drawable.ch_ruins_5),
            ALIEN_CIVILIZATION to listOf(R.drawable.ch_alien_1, R.drawable.ch_alien_2, R.drawable.ch_alien_3, R.drawable.ch_alien_4, R.drawable.ch_alien_5)
        )
    }
}
