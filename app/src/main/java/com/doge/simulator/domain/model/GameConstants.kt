package com.doge.simulator.domain.model

import java.util.concurrent.TimeUnit

object GameConstants {

    // ── 기본 규칙 ──────────────────────────────────────────────────────
    const val SELL_FEE_RATE = 0.05f
    const val MAX_OFFLINE_MINUTES = 1440L
    const val MAX_EVENT_LOG_COUNT = 200
    const val MAX_PLANET_SLOTS_BASE = 10

    // ── 행성 강화 ──────────────────────────────────────────────────────
    const val PLANET_MAX_LEVEL = 20
    const val DANGER_ZONE_START = 11

    val UPGRADE_SUCCESS_RATES = mapOf(
        1 to 0.95f, 2 to 0.90f, 3 to 0.85f, 4 to 0.80f, 5 to 0.75f,
        6 to 0.70f, 7 to 0.65f, 8 to 0.60f, 9 to 0.55f, 10 to 0.50f,
        11 to 0.45f, 12 to 0.40f, 13 to 0.35f, 14 to 0.30f, 15 to 0.25f,
        16 to 0.20f, 17 to 0.15f, 18 to 0.10f, 19 to 0.08f
    )

    // 행성 강화 비용 (coins, Map<ResourceType, amount>)
    // 강화로 갈수록 다른 종류의 자원을 요구해 드롭되는 자원들이 골고루 소모되게 함
    fun planetUpgradeCost(currentLevel: Int): Pair<Long, Map<ResourceType, Int>> = when {
        currentLevel <= 4  -> 300L to emptyMap()
        currentLevel <= 8  -> 800L to mapOf(ResourceType.IRON_ORE to 3)
        currentLevel <= 11 -> 2_000L to mapOf(ResourceType.COOLANT to 3, ResourceType.CRYSTAL to 2)
        currentLevel <= 15 -> 4_000L to mapOf(ResourceType.LIFE_CRYSTAL to 2, ResourceType.RARE_EARTH to 2)
        else               -> 9_000L to mapOf(ResourceType.ENERGY_CORE to 3, ResourceType.QUANTUM_CORE to 1)
    }

    // ── 행성 자원 드롭 ────────────────────────────────────────────────
    // 등급이 높을수록 드롭량이 많아지되, 너무 가파르면 흔한 자원(에너지 코어 등)까지
    // 같이 폭증해버려서 배율은 완만하게 (최상위 등급도 COMMON의 3.2배 수준)
    val RARITY_RESOURCE_MULTIPLIER = mapOf(
        RarityTier.COMMON to 1.0,
        RarityTier.UNCOMMON to 1.3,
        RarityTier.RARE to 1.8,
        RarityTier.EPIC to 2.4,
        RarityTier.LEGENDARY to 3.2
    )

    // 강화 레벨 1~20: 레벨이 오를수록 코인 생산량·자원 드롭량이 함께 늘어나야 강화 의미가 생김 (레벨당 +10%)
    fun planetLevelMultiplier(level: Int): Double = 1.0 + (level - 1) * 0.10

    // ── 우주인 ────────────────────────────────────────────────────────
    const val ASTRONAUT_BASE_HIRE_COST = 500L
    const val ASTRONAUT_HIRE_COST_PER_EXISTING = 200L

    val BASIC_TRAINING_DURATION_MS = TimeUnit.HOURS.toMillis(4)
    val ADVANCED_TRAINING_DURATION_MS = TimeUnit.HOURS.toMillis(12)
    const val BASIC_TRAINING_COST_COINS = 300L
    const val ADVANCED_TRAINING_COST_COINS = 800L
    val ADVANCED_TRAINING_RESOURCE_COST = mapOf(ResourceType.BIOMASS to 3)

    // ── 우주선 ────────────────────────────────────────────────────────
    const val SCOUT_SHIP_BASE_COST = 1_000L

    // 정찰선 초기 스탯
    const val SCOUT_CREW_BASE = 3
    const val SCOUT_SPEED_BASE = 40
    const val SCOUT_CARGO_BASE = 40
    const val SCOUT_SUCCESS_RATE_BASE = 0.70f

    // 우주선 강화 비용. 6등급 이후엔 유적·외계 문명 자원까지 소모처로 사용
    fun spaceshipUpgradeCost(currentGrade: Int): Pair<Long, Map<ResourceType, Int>> = when (currentGrade) {
        1 -> 2_000L to mapOf(ResourceType.IRON_ORE to 5)
        2 -> 5_000L to mapOf(ResourceType.IRON_ORE to 5, ResourceType.MAGMA_STONE to 3)
        3 -> 12_000L to mapOf(ResourceType.MAGMA_STONE to 5, ResourceType.ENERGY_CORE to 3)
        4 -> 25_000L to mapOf(ResourceType.ENERGY_CORE to 5, ResourceType.CRYSTAL to 2)
        5 -> 50_000L to mapOf(ResourceType.CRYSTAL to 5, ResourceType.RARE_EARTH to 3)
        6 -> 90_000L to mapOf(ResourceType.RARE_EARTH to 3, ResourceType.DATA_CORE to 2)
        else -> 150_000L to mapOf(ResourceType.ALIEN_TECH to 2, ResourceType.UNKNOWN_MATTER to 1)
    }

    // 강화당 스탯 증가량
    const val UPGRADE_CREW_PER_GRADE = 1
    const val UPGRADE_SPEED_PER_GRADE = 10
    const val UPGRADE_CARGO_PER_GRADE = 10
    const val UPGRADE_SUCCESS_RATE_PER_GRADE = 0.04f

    // ── 탐사 ──────────────────────────────────────────────────────────
    // 티어별 기본 탐사 시간 (분)
    val EXPEDITION_BASE_MINUTES = mapOf(
        1 to 10L, 2 to 20L, 3 to 40L,  4 to 60L,  5 to 90L,
        6 to 120L, 7 to 180L, 8 to 240L, 9 to 360L, 10 to 480L
    )

    // 티어별 지역 이름
    val TIER_LABELS = mapOf(
        1 to "태양계",      2 to "알파 성계",    3 to "오리온 성운",
        4 to "마젤란 성운", 5 to "처녀자리 성단", 6 to "페르세우스 팔",
        7 to "은하 핵",     8 to "은하 외곽",    9 to "보이드 공간",
        10 to "미지의 공간"
    )

    data class TierUnlockCondition(
        val requiredRarity: RarityTier,
        val requiredCount: Int,
        val label: String
    )

    // null = 잠금 없음
    val TIER_UNLOCK_CONDITIONS: Map<Int, TierUnlockCondition?> = mapOf(
        1  to null,
        2  to null,
        3  to TierUnlockCondition(RarityTier.COMMON,   1, "COMMON 행성 1개+"),
        4  to TierUnlockCondition(RarityTier.COMMON,   3, "COMMON 행성 3개+"),
        5  to TierUnlockCondition(RarityTier.UNCOMMON, 1, "UNCOMMON 행성 1개+"),
        6  to TierUnlockCondition(RarityTier.UNCOMMON, 2, "UNCOMMON 행성 2개+"),
        7  to TierUnlockCondition(RarityTier.UNCOMMON, 3, "UNCOMMON 행성 3개+"),
        8  to TierUnlockCondition(RarityTier.RARE,     1, "RARE 행성 1개+"),
        9  to TierUnlockCondition(RarityTier.EPIC,     1, "EPIC 행성 1개+"),
        10 to TierUnlockCondition(RarityTier.LEGENDARY,1, "LEGENDARY 행성 1개+")
    )

    // 탐사 성공 시 행성 발견 확률 — 초반 체감을 위해 기본값을 올림 (후반은 레벨 보너스로 이미 충분히 오름)
    const val PLANET_DISCOVERY_BASE_CHANCE = 1f
    const val PLANET_DISCOVERY_PLANET_CATEGORY_BONUS = 0.10f
    const val PLANET_DISCOVERY_CELESTIAL_BONUS_PER_LEVEL = 0.03f

    // 전문 분야 일치 시 보너스
    const val SPECIALTY_MATCH_SUCCESS_BONUS = 0.05f
    const val SPECIALTY_MATCH_RESOURCE_BONUS = 0.5f

    // 탐사 성공 시 행성 발견 여부와 무관하게 지급되는 기본 코인 보상.
    // 초반에 코인을 다 쓰고 행성도 못 찾았을 때 완전히 무수입 상태가 되는 것을 막기 위한 안전망
    fun expeditionSuccessCoinReward(tier: Int): Long = 100L + tier * 50L

    // 자원 판매 단가 (코인/개) — 행성이 없어 방치 수익이 없을 때 자원을 코인으로 바꿀 수 있는 최소한의 환금 수단.
    // 탐사 카테고리 해금 순서(광물→행성→유적→외계문명)를 대략적인 희귀도 기준으로 반영
    val RESOURCE_SELL_PRICE: Map<ResourceType, Long> = mapOf(
        ResourceType.IRON_ORE to 8L, ResourceType.MAGMA_STONE to 12L,
        ResourceType.CRYSTAL to 20L, ResourceType.RARE_EARTH to 25L,
        ResourceType.BIOMASS to 10L, ResourceType.COOLANT to 15L,
        ResourceType.ENERGY_CORE to 22L, ResourceType.LIFE_CRYSTAL to 35L,
        ResourceType.NANOBOT to 28L, ResourceType.DATA_CORE to 30L,
        ResourceType.ANCIENT_ARTIFACT to 50L,
        ResourceType.QUANTUM_CORE to 60L, ResourceType.UNKNOWN_MATTER to 65L,
        ResourceType.ALIEN_TECH to 70L
    )

    // 탐사 마무리 선택에서 "자원을 더 싣는다"를 골랐을 때 성공할 확률.
    // 실패하면 자원 대신 코인을 잃고, 후속 이벤트도 발생하지 않는다
    const val DEPARTURE_LOOT_SUCCESS_RATE = 0.65f

    // ── 연구소 ────────────────────────────────────────────────────────
    // 레벨이 무한히 오르는 만큼 자원 종류를 다양하게 분배해 한쪽만 과다 소모되지 않게 함
    fun researchUpgradeCost(currentLevel: Int): Pair<Long, Map<ResourceType, Int>> = when (currentLevel) {
        1 -> 1_000L to emptyMap()
        2 -> 3_000L to mapOf(ResourceType.BIOMASS to 5)
        3 -> 6_000L to mapOf(ResourceType.COOLANT to 4)
        4 -> 10_000L to mapOf(ResourceType.ENERGY_CORE to 3)
        5 -> 15_000L to mapOf(ResourceType.CRYSTAL to 3)
        6 -> 22_000L to mapOf(ResourceType.LIFE_CRYSTAL to 2)
        7 -> 30_000L to mapOf(ResourceType.DATA_CORE to 2)
        8 -> 40_000L to mapOf(ResourceType.NANOBOT to 2)
        else -> (50_000L + (currentLevel - 9) * 15_000L) to
                mapOf(ResourceType.ANCIENT_ARTIFACT to 2, ResourceType.QUANTUM_CORE to 1)
    }
}
