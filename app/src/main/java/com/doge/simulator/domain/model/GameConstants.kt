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

    // 행성 방치 수익 전역 배율. 시뮬레이션 결과 0.6에서도 커먼 행성 페이백이 ~50분으로 여전히
    // 너무 빨라서, 상시 접속 기준 패시브/액티브 수입 비율이 21일 만에 20배까지 벌어짐(탐사 루프가
    // 조기에 무의미해짐) — 커먼 행성 페이백을 ~3시간대로 늦춰 액티브 탐사 비중을 끌어올림 (타입별 상대 밸런스는 유지)
    const val PLANET_PRODUCTION_SCALE = 0.15

    // ── 행성 시세/생산 이벤트 ─────────────────────────────────────────
    // 이벤트 평균 발생 간격(시간). risk(2~100)가 높을수록 짧아짐 — risk=2 → 6h, risk=100 → 3h.
    // 원래 10~42h(하루 1~2회 수준)로 뜸하게 잡았었는데, "너무 잦으면 스팸 같다"는 그 판단이
    // 알림이 존재한다는 전제였음을 깨달음 — 지금 이벤트는 알림이 아예 없고(포그라운드 정산
    // 시점에만 조용히 롤됨) "소식" 탭에 들어가야만 보이므로 14h~3h로 1차 단축했었다가, 그마저도
    // 실기기로 체감해보니 안전한 타입은 여전히 뜸하게 느껴져 최고점만 한 번 더 낮춤(최저점
    // 3h는 유지) — 최종 확정값
    fun planetEventIntervalHours(risk: Int): Double = 6.0 - (risk - 2) * 0.0306

    // 좋음/나쁨 판정 확률(나쁠 확률, %). risk(변동성·빈도)와 분리된 축으로, 희귀도가 높을수록
    // 유리해지되 최상위 등급도 완전 무결점은 아니도록 천장을 둠(LEGENDARY도 최소 25%는 나쁠 수 있음).
    // PlanetMetaDataTable의 eventRateMin/Max에 그대로 반영됨(타입이 아니라 희귀도 기준)
    val PLANET_EVENT_BAD_CHANCE_RANGE: Map<RarityTier, IntRange> = mapOf(
        RarityTier.COMMON to 45..55,
        RarityTier.UNCOMMON to 40..48,
        RarityTier.RARE to 35..42,
        RarityTier.EPIC to 30..37,
        RarityTier.LEGENDARY to 25..32
    )

    // 이벤트 1회당 생산 배율 등락폭(3~30%p)도 매번 랜덤 — 같은 "나쁜 이벤트"라도 매번
    // 크기가 달라야 도박성이 생김. 나쁜 이벤트는 -값, 좋은 이벤트는 +값으로 아래 누적치에 더해짐
    const val PLANET_EVENT_DELTA_MIN = 0.03
    const val PLANET_EVENT_DELTA_MAX = 0.30

    // 백그라운드 워커(PlanetEventWorker)가 "큰 폭" 이벤트로 판단해 알림을 보내는 등락폭 기준.
    // 주식 앱의 급등락 알림처럼 매번 알리면 스팸이 되므로, 전체 델타 범위(3~30%p)의 상위 구간만
    // 알림 대상으로 삼음 — 균등분포 기준 이벤트 중 약 18.5%만 이 기준을 넘음
    const val PLANET_EVENT_NOTIFY_DELTA_THRESHOLD = 0.25
    // 알림 파이프라인만 확인할 땐 잠깐 0.05로 낮추면 이벤트가 뜰 때마다 무조건 알림이 온다

    // 생산 배율은 매 이벤트마다 덮어쓰지 않고 누적(+=)된다 — 나쁜 이벤트가 연달아 겹치면
    // 정말로 생산이 0 밑으로(마이너스, 즉 실제 손해) 내려갈 수 있음. 다만 완전히 무한정
    // 나빠지거나(회복 불가능해 보임) 좋아지지(비현실적) 않도록 바닥·천장을 둠 — 바닥까지
    // 가려면 최악의 델타로만 4~5연속 불운해야 해서 "가끔 진짜 사고"라는 느낌은 유지됨
    const val PLANET_EVENT_MULTIPLIER_FLOOR = -0.30
    const val PLANET_EVENT_MULTIPLIER_CEILING = 3.0

    // 시세 변동폭 = (생산 배율 누적치 − 1.0) × buyPrice + (생산 배율 누적치 − 1.0) × upgradeInvestment × 이 비율.
    // 강화투자액이 클수록 상대적 타격이 작아지지만 이 비율만큼은 항상 남아 완전 무위험이 안 됨.
    // 매 이벤트마다 시세를 따로 누적하지 않고 항상 이 공식으로 "현재 생산 배율 기준"을 다시
    // 계산 — 생산 배율과 시세가 서로 다른 값으로 어긋날 일이 없음
    const val PLANET_EVENT_MARKET_UPGRADE_INVESTMENT_RATIO = 0.25

    // ── 우주인 ────────────────────────────────────────────────────────
    const val ASTRONAUT_BASE_HIRE_COST = 500L
    // 같은 등급 안에서 숙련도 롤이 최고치일 때 기본가 대비 추가로 붙는 비율
    const val HIRE_COST_PROFICIENCY_SPREAD = 0.4f

    val BASIC_TRAINING_DURATION_MS = TimeUnit.HOURS.toMillis(4)
    val ADVANCED_TRAINING_DURATION_MS = TimeUnit.HOURS.toMillis(12)
    const val BASIC_TRAINING_COST_COINS = 300L
    const val ADVANCED_TRAINING_COST_COINS = 800L
    val ADVANCED_TRAINING_RESOURCE_COST = mapOf(ResourceType.BIOMASS to 3)

    // 훈련 시 숙련도 증가량 (등급 캡을 넘지 않도록 클램프됨)
    const val BASIC_TRAINING_PROFICIENCY_GAIN = 5
    const val ADVANCED_TRAINING_PROFICIENCY_GAIN = 20

    // ── 모집 센터 ─────────────────────────────────────────────────────
    const val RECRUITMENT_POOL_SIZE = 5
    val RECRUITMENT_REFRESH_INTERVAL_MS = TimeUnit.HOURS.toMillis(3)

    // ── 우주선 ────────────────────────────────────────────────────────
    const val SCOUT_SHIP_BASE_COST = 1_000L

    // 정찰선 초기 스탯
    const val SCOUT_CREW_BASE = 3
    const val SCOUT_SPEED_BASE = 40
    const val SCOUT_CARGO_BASE = 40
    const val SCOUT_SUCCESS_RATE_BASE = 0.70f

    // 최대 등급. spaceship_2/4/6/8 4단계 이미지에 맞춰 탑승 인원 8명(그림상 최종 형태)에서 강화 종료
    const val MAX_SPACESHIP_GRADE = 6
    const val MAX_CREW_CAPACITY = 8

    // 우주선 강화 비용 (1등급 → MAX_SPACESHIP_GRADE 등급까지, 총 5회)
    fun spaceshipUpgradeCost(currentGrade: Int): Pair<Long, Map<ResourceType, Int>> = when (currentGrade) {
        1 -> 2_000L to mapOf(ResourceType.IRON_ORE to 5)
        2 -> 5_000L to mapOf(ResourceType.IRON_ORE to 5, ResourceType.MAGMA_STONE to 3)
        3 -> 12_000L to mapOf(ResourceType.MAGMA_STONE to 5, ResourceType.ENERGY_CORE to 3)
        4 -> 25_000L to mapOf(ResourceType.ENERGY_CORE to 5, ResourceType.CRYSTAL to 2)
        else -> 50_000L to mapOf(ResourceType.CRYSTAL to 5, ResourceType.RARE_EARTH to 3)
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

    // 티어는 사다리처럼 순서대로만 열린다 — 각 티어 조건은 서로 독립적인 희귀도 기준이라,
    // 운 좋게 상위 희귀도 행성을 하나 일찍 주우면 뒤 티어 조건은 채우고 앞 티어 조건(개수)은
    // 못 채운 "구멍"이 생길 수 있다(예: 언커먼 1개만 있으면 T5 자체 조건은 만족하지만
    // T4의 "커먼 이상 3개+"는 아직 부족). 그래서 1..maxTier를 순서대로 훑어 "가장 먼저
    // 막힌 티어"를 찾고, 그 이전 티어까지만 실제로 열린 것으로 친다.
    // null = maxTier까지 전부 열림.
    //
    // 기준은 "현재 보유 중인 행성"이 아니라 "한 번이라도 발견한 적 있는 도감 기록"
    // (discoveredVariantIds)이다 — 매도는 이 게임의 정상적인 플레이(손절/차익실현)인데,
    // 보유 개수 기준이면 행성 하나 파는 순간 사다리 위 티어가 전부 같이 잠겨버리는 문제가
    // 있었음(2026-09-04). 도감은 챕터 진행·도감 완성과 같은 성격의 영구 기록이라 매도로
    // 흔들리지 않는다.
    fun firstLockedTier(discoveredVariantIds: Set<String>, maxTier: Int = 10): Int? {
        for (t in 1..maxTier) {
            val condition = TIER_UNLOCK_CONDITIONS[t] ?: continue
            val count = discoveredVariantIds.count { variantId ->
                val rarity = PlanetMetaDataTable.variantRarity[variantId]
                rarity != null && rarity.ordinal >= condition.requiredRarity.ordinal
            }
            if (count < condition.requiredCount) return t
        }
        return null
    }

    // 탐사 성공 시 행성 발견 확률. 천체 분석 레벨(+3%/레벨)과 행성 카테고리 보너스로 후반에 상승,
    // 최종 상한은 discoveryChance 계산부(CompleteExpeditionUseCase)에서 80%로 코어스.
    // 기존 0.35+0.10(연구소 투자 없이도 PLANET 카테고리 48%)는 시뮬레이션상 행성 10슬롯이
    // 하루 안에 다 차버릴 정도로 높아서, 기본값과 카테고리 보너스를 함께 낮춰 슬롯이 차는 속도를 늦춤
    const val PLANET_DISCOVERY_BASE_CHANCE = 0.18f
    const val PLANET_DISCOVERY_PLANET_CATEGORY_BONUS = 0.05f
    const val PLANET_DISCOVERY_CELESTIAL_BONUS_PER_LEVEL = 0.03f

    // 발견된 행성이 어느 등급(rarity)으로 나올지의 티어별 가중치(%, 등급 총합 100).
    // 티어가 오를수록 희귀 등급 비중은 커지지만 무제한으로 오르지 않도록 상한을 둠 —
    // LEGENDARY 최대 5%, EPIC 최대 10% (T10에서도 "가끔 터지는 잭팟" 이상은 아니게),
    // COMMON은 T10에서도 최소 28% 유지 (고티어만 돌려도 저등급이 아예 안 나오는 역피라미드 방지).
    // 등급 내 개별 행성 확률은 해당 등급 소속 종류 수로 균등 분배 (rollPlanetType 참고)
    val PLANET_RARITY_WEIGHTS: Map<Int, Map<RarityTier, Float>> = mapOf(
        1  to mapOf(RarityTier.COMMON to 90f, RarityTier.UNCOMMON to 10f),
        2  to mapOf(RarityTier.COMMON to 70f, RarityTier.UNCOMMON to 30f),
        3  to mapOf(RarityTier.COMMON to 55f, RarityTier.UNCOMMON to 42f, RarityTier.RARE to 3f),
        4  to mapOf(RarityTier.COMMON to 45f, RarityTier.UNCOMMON to 45f, RarityTier.RARE to 8f, RarityTier.EPIC to 2f),
        5  to mapOf(RarityTier.COMMON to 40f, RarityTier.UNCOMMON to 43f, RarityTier.RARE to 12f, RarityTier.EPIC to 4f, RarityTier.LEGENDARY to 1f),
        6  to mapOf(RarityTier.COMMON to 36f, RarityTier.UNCOMMON to 40f, RarityTier.RARE to 16f, RarityTier.EPIC to 6f, RarityTier.LEGENDARY to 2f),
        7  to mapOf(RarityTier.COMMON to 32f, RarityTier.UNCOMMON to 38f, RarityTier.RARE to 19f, RarityTier.EPIC to 8f, RarityTier.LEGENDARY to 3f),
        8  to mapOf(RarityTier.COMMON to 30f, RarityTier.UNCOMMON to 36f, RarityTier.RARE to 21f, RarityTier.EPIC to 9f, RarityTier.LEGENDARY to 4f),
        9  to mapOf(RarityTier.COMMON to 28f, RarityTier.UNCOMMON to 34f, RarityTier.RARE to 23f, RarityTier.EPIC to 10f, RarityTier.LEGENDARY to 5f),
        10 to mapOf(RarityTier.COMMON to 28f, RarityTier.UNCOMMON to 32f, RarityTier.RARE to 25f, RarityTier.EPIC to 10f, RarityTier.LEGENDARY to 5f)
    )

    // 발견한 행성이 이미 도감에 있는 베리언트(스킨)와 겹치는데, 행성 슬롯도 가득 차 구매할 수 없을 때
    // "코인으로 받기"를 고르면 지급되는 금액. (production/risk 등 스탯은 매번 새로 롤되므로 중복이라도
    // 슬롯 여유가 있으면 정상 구매 가능 — 이 상수는 슬롯이 없을 때의 최후 대안에만 쓰임)
    // 완전 신규(50%)보다는 낮게 잡음 — 이미 도감엔 등록돼 있어 다시 등록해도 얻는 게 적기 때문
    const val DUPLICATE_PLANET_VARIANT_COIN_RATE = 0.3f

    // 도감에 없던 신규 베리언트인데 행성 슬롯이 가득 차 구매할 수 없을 때, "코인으로 받기"를 고르면
    // 지급되는 금액. 도감 등록(recordVariantDiscovery)은 슬롯과 무관하게 항상 보장됨.
    // "완전히 처음 보는 스킨"이라 중복(30%)보다는 후하게 잡음
    const val SLOT_FULL_DISCOVERY_COIN_RATE = 0.5f

    // 전문 분야 일치 시 보너스 (숙련도 기반)
    // 성공률: 매칭되는 전문가 중 최고 숙련도(MAX) 1명 기준 — 숙련도 100이면 최대치 보너스
    const val SPECIALTY_PROFICIENCY_SUCCESS_COEFFICIENT = 0.25f
    // 자원량: 매칭되는 전문가들의 숙련도 합산 기준 — 인원이 많고 숙련도가 높을수록 커짐
    const val SPECIALTY_PROFICIENCY_RESOURCE_COEFFICIENT = 0.5f
    // 자원량: 파견 인원수 자체도 기여 (매칭 여부 무관, 1명 초과 인원당 가산)
    const val CREW_SIZE_RESOURCE_BONUS_PER_HEAD = 0.15

    // 티어가 오를수록 보상 단가가 완만히 상승하도록 하는 공통 배율. 코인 보상과 탐사 성공 시
    // 자원 드랍량이 이 배율을 공유해, 고티어 탐사(오래 걸림)가 저티어보다 시간당 효율이 떨어지는
    // 역전이 생기지 않게 한다. 티어10은 소요 시간(480분) 자체가 커서 선형 배율(1.9배)까지 얹으면
    // 보상이 과도하게 튀어, 9티어 대비 완만하게만 더 받도록 배율을 낮춰서 고정
    fun expeditionTierMultiplier(tier: Int): Double =
        if (tier >= 10) 1.5 else 1.0 + (tier - 1) * 0.1

    // 탐사 성공 시 행성 발견 여부와 무관하게 지급되는 기본 코인 보상.
    // 초반에 코인을 다 쓰고 행성도 못 찾았을 때 완전히 무수입 상태가 되는 것을 막기 위한 안전망.
    // 소요 시간(분)에 비례해 계산
    const val EXPEDITION_COIN_PER_MINUTE = 12L
    fun expeditionSuccessCoinReward(tier: Int): Long {
        val minutes = EXPEDITION_BASE_MINUTES[tier] ?: EXPEDITION_BASE_MINUTES.getValue(EXPEDITION_BASE_MINUTES.keys.max())
        return (minutes * EXPEDITION_COIN_PER_MINUTE * expeditionTierMultiplier(tier)).toLong()
    }

    // 자원 판매 단가 (코인/개) — 행성이 없어 방치 수익이 없을 때 자원을 코인으로 바꿀 수 있는 최소한의 환금 수단.
    // 행성 드랍 출처 개수·확률과 역전되지 않도록 보정 후 전체 +15% 반영
    // (크리스탈: 8개 행성에서 나오는 흔한 자원인데 마그마석보다 비쌌던 것 보정 / 에너지 코어: 초반 가스자이언트로 쉽게 확보되는데 희토류급으로 비쌌던 것 보정)
    val RESOURCE_SELL_PRICE: Map<ResourceType, Long> = mapOf(
        ResourceType.IRON_ORE to 9L, ResourceType.MAGMA_STONE to 14L,
        ResourceType.CRYSTAL to 12L, ResourceType.RARE_EARTH to 29L,
        ResourceType.BIOMASS to 12L, ResourceType.COOLANT to 17L,
        ResourceType.ENERGY_CORE to 18L, ResourceType.LIFE_CRYSTAL to 40L,
        ResourceType.NANOBOT to 32L, ResourceType.DATA_CORE to 35L,
        ResourceType.ANCIENT_ARTIFACT to 58L,
        ResourceType.QUANTUM_CORE to 69L, ResourceType.UNKNOWN_MATTER to 75L,
        ResourceType.ALIEN_TECH to 81L
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

    // ── 광고 ──────────────────────────────────────────────────────────
    // 이 이상 경과해야 "복귀 시 확인 다이얼로그"를 띄움. 미만이면 기존처럼 조용히 1배로 자동 적립
    const val OFFLINE_PROFIT_DIALOG_THRESHOLD_MINUTES = 10L
    const val OFFLINE_PROFIT_AD_MULTIPLIER = 2.0

    // 대기시간 스킵 광고 1회당 당길 수 있는 최대 시간 (전체 스킵이 아니라 상한을 둬서 밸런스 보호)
    val AD_SKIP_MAX_MS = TimeUnit.HOURS.toMillis(4)

    // 전면광고(탐사 결과 dismiss) 빈도 제한: 첫 N회는 노출 안 함, 이후엔 쿨다운 경과해야 노출
    const val INTERSTITIAL_GRACE_COMPLETIONS = 3
    val INTERSTITIAL_COOLDOWN_MS = TimeUnit.MINUTES.toMillis(3)
}
