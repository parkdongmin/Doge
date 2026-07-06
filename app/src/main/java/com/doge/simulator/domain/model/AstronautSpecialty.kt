package com.doge.simulator.domain.model

enum class AstronautSpecialty(
    val displayName: String,
    val icon: String,
    val relatedCategory: ExpeditionCategory
) {
    MINERAL("광물 전문가", "⛏️", ExpeditionCategory.MINERAL),
    PLANET("행성 탐사가", "🪐", ExpeditionCategory.PLANET),
    RUINS("유적 발굴가", "🏛️", ExpeditionCategory.RUINS),
    ALIEN_CIVILIZATION("외계 연구가", "👽", ExpeditionCategory.ALIEN_CIVILIZATION)
}
