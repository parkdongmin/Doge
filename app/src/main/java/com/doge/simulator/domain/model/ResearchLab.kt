package com.doge.simulator.domain.model

data class ResearchLab(
    val explorationTechLevel: Int = 1,
    val celestialAnalysisLevel: Int = 1,
    val hrLevel: Int = 1,
    val engineeringLevel: Int = 1
) {
    fun getLevel(field: ResearchField): Int = when (field) {
        ResearchField.EXPLORATION_TECH -> explorationTechLevel
        ResearchField.CELESTIAL_ANALYSIS -> celestialAnalysisLevel
        ResearchField.HR_MANAGEMENT -> hrLevel
        ResearchField.SPACE_ENGINEERING -> engineeringLevel
    }

    // 연구소 총 레벨 (랭킹 점수 산정에 사용)
    val totalLevel: Int get() = explorationTechLevel + celestialAnalysisLevel + hrLevel + engineeringLevel

    // 행성 최대 보유 슬롯 (기본 10 + 천체 분석 레벨당 +1)
    val maxPlanetSlots: Int get() = GameConstants.MAX_PLANET_SLOTS_BASE + celestialAnalysisLevel - 1

    // 우주인 최대 고용 한도 (기본 5 + 인사 관리 레벨당 +2)
    val maxAstronauts: Int get() = 5 + (hrLevel - 1) * 2

    // 동시 훈련 가능 인원 (기본 1 + 인사 관리 3레벨마다 +1)
    val maxTrainingSlots: Int get() = 1 + (hrLevel - 1) / 3

    // 최대 보유 우주선 수 (기본 2 + 우주 공학 레벨당 +1)
    val maxSpaceships: Int get() = 2 + engineeringLevel - 1

    // 언락된 탐사 카테고리
    fun unlockedCategories(): List<ExpeditionCategory> =
        ExpeditionCategory.entries.filter { it.researchLevelRequired <= explorationTechLevel }

    companion object {
        // 탐사 기술은 모든 카테고리가 해금되는 레벨 이후로는 더 줄 효과가 없어 그 지점에서 캡.
        // 나머지 3개 분야(천체 분석·인사 관리·우주 공학)는 레벨업할수록 계속 실질 효과가 커져 무제한
        fun maxLevel(field: ResearchField): Int? = when (field) {
            ResearchField.EXPLORATION_TECH -> ExpeditionCategory.entries.maxOf { it.researchLevelRequired }
            else -> null
        }
    }
}
