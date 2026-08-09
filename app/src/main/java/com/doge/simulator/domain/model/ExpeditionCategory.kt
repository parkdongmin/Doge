package com.doge.simulator.domain.model

enum class ExpeditionCategory(
    val displayName: String,
    val researchLevelRequired: Int
) {
    MINERAL("광물 탐사", 0),
    PLANET("행성 탐사", 0),
    RUINS("유적 탐사", 3),
    ALIEN_CIVILIZATION("외계 문명 탐사", 6)
}
