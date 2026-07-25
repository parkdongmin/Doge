package com.doge.simulator.domain.model

import androidx.annotation.DrawableRes
import com.doge.simulator.R

enum class ResourceType(
    val displayName: String,
    val category: ExpeditionCategory,
    @DrawableRes val iconRes: Int
) {
    // 광물 탐사
    CRYSTAL("크리스탈", ExpeditionCategory.MINERAL, R.drawable.ic_resource_crystal),
    IRON_ORE("철광석", ExpeditionCategory.MINERAL, R.drawable.ic_resource_iron_ore),
    MAGMA_STONE("마그마석", ExpeditionCategory.MINERAL, R.drawable.ic_resource_magma_stone),
    RARE_EARTH("희토류", ExpeditionCategory.MINERAL, R.drawable.ic_resource_rare_earth),

    // 행성 탐사
    BIOMASS("바이오매스", ExpeditionCategory.PLANET, R.drawable.ic_resource_biomass),
    ENERGY_CORE("에너지 코어", ExpeditionCategory.PLANET, R.drawable.ic_resource_energy_core),
    COOLANT("냉각재", ExpeditionCategory.PLANET, R.drawable.ic_resource_coolant),
    LIFE_CRYSTAL("생명 결정", ExpeditionCategory.PLANET, R.drawable.ic_resource_life_crystal),

    // 유적 탐사
    NANOBOT("나노봇", ExpeditionCategory.RUINS, R.drawable.ic_resource_nanobot),
    ANCIENT_ARTIFACT("고대 유물", ExpeditionCategory.RUINS, R.drawable.ic_resource_ancient_artifact),
    DATA_CORE("데이터 코어", ExpeditionCategory.RUINS, R.drawable.ic_resource_data_core),

    // 외계 문명 탐사
    UNKNOWN_MATTER("미지의 물질", ExpeditionCategory.ALIEN_CIVILIZATION, R.drawable.ic_resource_unknown_matter),
    ALIEN_TECH("외계 기술", ExpeditionCategory.ALIEN_CIVILIZATION, R.drawable.ic_resource_alien_tech),
    QUANTUM_CORE("양자 코어", ExpeditionCategory.ALIEN_CIVILIZATION, R.drawable.ic_resource_quantum_core)
}

// 카테고리를 대표하는 자원 아이콘 (첫 번째 자원) — 탐사 카테고리 UI 전반에서 재사용
val ExpeditionCategory.representativeIconRes: Int
    @DrawableRes get() = ResourceType.entries.first { it.category == this }.iconRes
