package com.doge.simulator.domain.model

import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

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

    // 위험 구간 실패 시 레벨이 떨어질 확률(실패했다는 전제의 조건부 확률). 원래 실패 = 무조건 하락이라
    // 상위 레벨(성공률 8~10%)에선 오르는 것보다 떨어지는 게 많아 광고 없이는 19→20에 기대 37만 회가
    // 필요했음. 성공률이 낮은 구간일수록 하락 확률도 낮춰, 광고 없이도 도달은 가능하되(19→20 기대 ~64회)
    // 고레벨에선 하락 되돌리기 광고가 3~5배 이득이 되게 맞춤
    val UPGRADE_DROP_CHANCES = mapOf(
        11 to 0.50f, 12 to 0.45f, 13 to 0.40f, 14 to 0.35f, 15 to 0.30f,
        16 to 0.25f, 17 to 0.20f, 18 to 0.15f, 19 to 0.10f
    )

    // 강화 자원 비용 등급 배율. 레벨당 이득(생산량 +10%)은 행성 생산량에 비례하는데 비용이 같으면
    // 커먼은 고레벨 강화가 손해(12→13 회수 16일)라, 좋은 행성일수록 재료를 더 먹게 해 등급과 무관하게
    // 회수 기간을 맞춘다 (등급 평균 생산량에 대략 비례)
    val PLANET_UPGRADE_RESOURCE_RARITY_SCALE = mapOf(
        RarityTier.COMMON to 1.0,
        RarityTier.UNCOMMON to 1.5,
        RarityTier.RARE to 2.0,
        RarityTier.EPIC to 3.0,
        RarityTier.LEGENDARY to 3.5
    )

    // 행성 강화 비용 (coins, Map<ResourceType, amount>) — 1회 시도당, 실패해도 소모.
    // 자원 기본량은 커먼 기준 "소모 자원을 늘어난 수입으로 회수하는 기간"을 역산해 잡음
    // (1~4: ~0.1일, 5~8: ~0.6일, 9~10: ~2일, 11~13: ~3일, 14~15: 5~6일, 16~19: 1~3주. 되돌리기 광고 사용 기준).
    // Lv10까지는 스타터 행성(무대기: 철광석·크리스탈·나노봇)과 광물 탐사만으로 오를 수 있게 하고,
    // 위험 구간(11~)부터 다른 행성·탐사의 자원을 요구해 "한 단계 위"라는 감각을 준다
    fun planetUpgradeCost(currentLevel: Int, rarity: RarityTier): Pair<Long, Map<ResourceType, Int>> {
        val coins = when {
            currentLevel <= 4  -> 300L
            currentLevel <= 8  -> 800L
            currentLevel <= 11 -> 2_000L
            currentLevel <= 15 -> 4_000L
            else               -> 9_000L
        }
        val base = when {
            currentLevel <= 4  -> mapOf(ResourceType.IRON_ORE to 20)
            currentLevel <= 8  -> mapOf(ResourceType.IRON_ORE to 60, ResourceType.CRYSTAL to 25)
            currentLevel <= 10 -> mapOf(ResourceType.IRON_ORE to 100, ResourceType.CRYSTAL to 60, ResourceType.NANOBOT to 5)
            currentLevel <= 13 -> mapOf(ResourceType.COOLANT to 60, ResourceType.MAGMA_STONE to 60, ResourceType.BIOMASS to 40)
            currentLevel <= 15 -> mapOf(ResourceType.ENERGY_CORE to 60, ResourceType.LIFE_CRYSTAL to 20, ResourceType.RARE_EARTH to 20, ResourceType.NANOBOT to 20)
            else               -> mapOf(ResourceType.ENERGY_CORE to 80, ResourceType.QUANTUM_CORE to 10, ResourceType.DATA_CORE to 10, ResourceType.UNKNOWN_MATTER to 8)
        }
        val scale = PLANET_UPGRADE_RESOURCE_RARITY_SCALE[rarity] ?: 1.0
        return coins to base.mapValues { (_, amount) -> (amount * scale).roundToInt() }
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
    // PlanetMetaDataTable의 eventRateMin/Max에 그대로 반영됨(타입이 아니라 희귀도 기준 — 블랙홀만 예외로 COMMON 범위)
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

    // 정상 상태의 생산 배율 범위. 호재는 +델타, 악재는 −델타(큰 악재면 고장까지)로 누적된다.
    // 악재도 배율을 깎아야 호재만 쌓여 모든 행성이 천장에 붙는 일이 없다
    const val PLANET_EVENT_MULTIPLIER_FLOOR = 0.5
    const val PLANET_EVENT_MULTIPLIER_CEILING = 2.0

    // ── 행성 고장/정비 ───────────────────────────────────────────────
    // 정상 행성에 큰 악재가 뜨면 생산 배율을 깎은 뒤 부호가 뒤집혀 "고장"(마이너스 생산) 상태가 된다.
    // 고장 동안의 손해 = 정상일 때 생산량 × 손해 배율. 손해 배율은 고장 시 1.0에서 시작해
    // 악재면 +델타, 호재면 −델타(플러스로는 안 돌아옴 — 회복은 정비로만)
    // 정상 행성은 악재 폭이 이 값(15%p) 이상일 때만 고장 난다. 원래 악재면 무조건 고장이었는데
    // 행성마다 하루 1~2번씩 정비가 떠서 과했음 — 균등분포(3~30%p) 기준 악재의 약 56%만 고장
    const val PLANET_BREAKDOWN_DELTA_THRESHOLD = 0.15
    const val PLANET_LOSS_MULTIPLIER_START = 1.0
    const val PLANET_LOSS_MULTIPLIER_MIN = 0.5
    const val PLANET_LOSS_MULTIPLIER_MAX = 2.0

    // 고장 중 매도가 할인 = 정상 매도가 × (이 비율 × 손해 배율) — 손해 배율 1.0이면 −30%, 2.0이면 −60%
    const val PLANET_BROKEN_SELL_DISCOUNT_PER_LOSS = 0.30

    // 정비 코인 비용 = 정상일 때 분당 생산량 × 이 시간(분). 자원 비용이 함께 붙으면서 120분 → 90분으로 낮춤.
    // 고장 난 채 하룻밤(8h) 두면 이 비용의 약 5배를 잃는다
    const val PLANET_MAINTENANCE_COST_MINUTES = 90L

    // 정비 자원 비용(등급별 기본량) × 강화 레벨 배율². 행성 생산·탐사로 끝없이 쌓이는 자원을 반복적으로
    // 받아줄 소비처 — 1회성 강화만으론 후반에 자원이 다시 남는다. 레벨이 오를수록 탐사 자원량도 가파르게
    // 늘어서 1제곱으론 못 따라가 제곱으로 키움. 커먼은 가장 기본 행성이라 철광석만
    val PLANET_MAINTENANCE_RESOURCE_BASE: Map<RarityTier, Map<ResourceType, Int>> = mapOf(
        RarityTier.COMMON to mapOf(ResourceType.IRON_ORE to 15),
        RarityTier.UNCOMMON to mapOf(ResourceType.IRON_ORE to 20, ResourceType.COOLANT to 15),
        RarityTier.RARE to mapOf(ResourceType.IRON_ORE to 25, ResourceType.COOLANT to 20, ResourceType.NANOBOT to 6),
        RarityTier.EPIC to mapOf(ResourceType.IRON_ORE to 30, ResourceType.ENERGY_CORE to 20, ResourceType.NANOBOT to 8),
        RarityTier.LEGENDARY to mapOf(ResourceType.IRON_ORE to 35, ResourceType.ENERGY_CORE to 25, ResourceType.NANOBOT to 10)
    )

    // 정비 자원이 모자라면 모자란 몫을 (판매 단가 × 이 배율) 코인으로 대신 낸다. 고장 난 행성은 계속 손해를
    // 보는 상태라 자원 부족으로 정비 자체가 막히면 안 됨. 1배 이하면 "팔아두고 코인으로 정비"가 손해 없는
    // 선택이 돼 자원을 쥐고 있을 이유가 사라지고, 3배는 레전더리 정비가 코인만일 때의 5배까지 튀어 2배로 잡음
    const val PLANET_MAINTENANCE_RESOURCE_COIN_RATE = 2.0

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
    // 훈련은 계속 반복되는 행동이라 바이오매스의 상시 소비처 역할을 겸한다
    val BASIC_TRAINING_RESOURCE_COST = mapOf(ResourceType.BIOMASS to 30)
    val ADVANCED_TRAINING_RESOURCE_COST = mapOf(ResourceType.BIOMASS to 100)

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

    // 우주선 강화 비용 (1등급 → MAX_SPACESHIP_GRADE 등급까지, 총 5회). 자원은 탐사 자원량 규모에 맞춤 —
    // 예전 한 자릿수 비용은 하루 수백 개씩 들어오는 드롭 대비 사실상 공짜였음
    fun spaceshipUpgradeCost(currentGrade: Int): Pair<Long, Map<ResourceType, Int>> = when (currentGrade) {
        1 -> 2_000L to mapOf(ResourceType.IRON_ORE to 120, ResourceType.CRYSTAL to 50)
        2 -> 5_000L to mapOf(ResourceType.IRON_ORE to 300, ResourceType.MAGMA_STONE to 150, ResourceType.COOLANT to 100)
        3 -> 12_000L to mapOf(ResourceType.MAGMA_STONE to 400, ResourceType.ENERGY_CORE to 300, ResourceType.NANOBOT to 80)
        4 -> 25_000L to mapOf(ResourceType.ENERGY_CORE to 600, ResourceType.RARE_EARTH to 300, ResourceType.DATA_CORE to 150)
        else -> 50_000L to mapOf(ResourceType.CRYSTAL to 1_000, ResourceType.RARE_EARTH to 500, ResourceType.ALIEN_TECH to 200)
    }

    // 강화당 스탯 증가량
    const val UPGRADE_CREW_PER_GRADE = 1
    const val UPGRADE_SPEED_PER_GRADE = 10
    const val UPGRADE_CARGO_PER_GRADE = 10
    const val UPGRADE_SUCCESS_RATE_PER_GRADE = 0.04f

    // ── 탐사 ──────────────────────────────────────────────────────────
    // 티어별 기본 탐사 시간 (분). 초반(1~5)은 첫 세션에서 결과를 몇 번 바로 받아보게 짧게 잡고
    // (예전 10/20/40/60/90분은 시작하자마자 10분 대기라 이탈 우려), 기다림은 중반(6~)부터 늘림.
    // 코인·자원 보상은 이 시간에 비례하므로 분당 수입은 티어 간 그대로 유지된다
    val EXPEDITION_BASE_MINUTES = mapOf(
        1 to 4L, 2 to 8L, 3 to 15L,  4 to 30L,  5 to 60L,
        6 to 120L, 7 to 180L, 8 to 240L, 9 to 360L, 10 to 480L
    )

    // 탐사 자원량 배율의 기준 시간(분) — 이 시간짜리 탐사가 배율 1(기본 랜덤 1~5개). 1티어 시간에
    // 묶어두면 1티어 시간을 바꿀 때마다 전 티어 자원량이 같이 출렁여서 고정값으로 분리
    const val EXPEDITION_RESOURCE_REFERENCE_MINUTES = 10.0

    // 우주선 속도로 줄어든 탐사 시간의 하한
    val EXPEDITION_MIN_DURATION_MS = TimeUnit.MINUTES.toMillis(1)

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
    // 티어가 오를수록 COMMON을 덜고 상위 등급 비중을 키움 — LEGENDARY 최대 12%, EPIC 최대 18%,
    // COMMON은 T10에서도 12%는 남김 (고티어만 돌려도 저등급이 아예 안 나오는 역피라미드 방지).
    // EPIC/LEGENDARY는 T9/T10 해금 조건이라, 예전 상한(5%/10%)으로는 우주선 1척 기준 T10 해금까지
    // 약 12일이 걸려 진행이 막혔음(2026-10-09) — 현재 값으로 약 6.5일.
    // 등급 내 개별 행성 확률은 해당 등급 소속 종류 수로 균등 분배 (rollPlanetType 참고)
    val PLANET_RARITY_WEIGHTS: Map<Int, Map<RarityTier, Float>> = mapOf(
        1  to mapOf(RarityTier.COMMON to 85f, RarityTier.UNCOMMON to 15f),
        2  to mapOf(RarityTier.COMMON to 65f, RarityTier.UNCOMMON to 35f),
        3  to mapOf(RarityTier.COMMON to 50f, RarityTier.UNCOMMON to 43f, RarityTier.RARE to 7f),
        4  to mapOf(RarityTier.COMMON to 40f, RarityTier.UNCOMMON to 44f, RarityTier.RARE to 12f, RarityTier.EPIC to 4f),
        5  to mapOf(RarityTier.COMMON to 33f, RarityTier.UNCOMMON to 42f, RarityTier.RARE to 16f, RarityTier.EPIC to 7f, RarityTier.LEGENDARY to 2f),
        6  to mapOf(RarityTier.COMMON to 27f, RarityTier.UNCOMMON to 38f, RarityTier.RARE to 22f, RarityTier.EPIC to 10f, RarityTier.LEGENDARY to 3f),
        7  to mapOf(RarityTier.COMMON to 22f, RarityTier.UNCOMMON to 35f, RarityTier.RARE to 25f, RarityTier.EPIC to 13f, RarityTier.LEGENDARY to 5f),
        8  to mapOf(RarityTier.COMMON to 18f, RarityTier.UNCOMMON to 32f, RarityTier.RARE to 28f, RarityTier.EPIC to 15f, RarityTier.LEGENDARY to 7f),
        9  to mapOf(RarityTier.COMMON to 15f, RarityTier.UNCOMMON to 30f, RarityTier.RARE to 29f, RarityTier.EPIC to 17f, RarityTier.LEGENDARY to 9f),
        10 to mapOf(RarityTier.COMMON to 12f, RarityTier.UNCOMMON to 28f, RarityTier.RARE to 30f, RarityTier.EPIC to 18f, RarityTier.LEGENDARY to 12f)
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

    // "코인으로 받기" 금액의 티어별 배율. 발견 확률은 탐사 1회당 고정이라 짧은 탐사일수록 시간당 발견이
    // 잦아, 슬롯이 꽉 찬 뒤엔 저티어 반복이 코인 농사가 됐다(1티어 3분대: 분당 ~54코인, 기본 보상의 5배).
    // 짧은 티어만 깎아 티어별 분당 총수입(기본 보상 + 코인으로 받기)이 비슷하고 상위 티어가 조금 더 낫게
    // 맞춤 — 1등급 우주선 기준 T1 24 · T2 24 · T3 25 · T4 27 · T5 30 코인/분 (ExpeditionEconomyTest가 검증).
    // 발견 빈도 자체는 그대로라 초반에 행성을 자주 만나는 경험은 유지된다
    private val SLOT_FULL_COIN_TIER_SCALE = mapOf(1 to 0.25, 2 to 0.35, 3 to 0.5, 4 to 0.7)
    fun slotFullCoinTierScale(tier: Int): Double = SLOT_FULL_COIN_TIER_SCALE[tier] ?: 1.0

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
    // 보상이 과도하게 튀어 9티어와 같은 1.8로 묶음 — 예전 1.5는 1회 보상만 보고 잡아 분당 효율이
    // 9티어보다 낮아지는 역전이 있었다(ExpeditionEconomyTest가 검증)
    fun expeditionTierMultiplier(tier: Int): Double =
        if (tier >= 10) 1.8 else 1.0 + (tier - 1) * 0.1

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
    // 외계 자원은 69/75/81 → 60/65/70: 탐사 시간은 카테고리와 무관하게 티어로만 정해지는데 판매가만 높아, 외계 문명이
    // 열리면 분당 판매 수입이 광물의 약 2.4배였다. 연구 투자 보상으로 가장 비싸게는 두되 약 1.9배(유적의 약 1.3배)로 낮춤
    val RESOURCE_SELL_PRICE: Map<ResourceType, Long> = mapOf(
        ResourceType.IRON_ORE to 9L, ResourceType.MAGMA_STONE to 14L,
        ResourceType.CRYSTAL to 12L, ResourceType.RARE_EARTH to 29L,
        ResourceType.BIOMASS to 12L, ResourceType.COOLANT to 17L,
        ResourceType.ENERGY_CORE to 18L, ResourceType.LIFE_CRYSTAL to 40L,
        ResourceType.NANOBOT to 32L, ResourceType.DATA_CORE to 35L,
        ResourceType.ANCIENT_ARTIFACT to 58L,
        ResourceType.QUANTUM_CORE to 60L, ResourceType.UNKNOWN_MATTER to 65L,
        ResourceType.ALIEN_TECH to 70L
    )

    // 탐사 마무리 선택에서 "자원을 더 싣는다"를 골랐을 때 성공할 확률.
    // 실패하면 자원 대신 코인을 잃고, 후속 이벤트도 발생하지 않는다
    const val DEPARTURE_LOOT_SUCCESS_RATE = 0.65f

    // ── 연구소 ────────────────────────────────────────────────────────
    // 레벨이 무한히 오르는 만큼 자원 종류를 다양하게 분배해 한쪽만 과다 소모되지 않게 함.
    // 단계가 오를수록 흔한 자원 → 보통 → 희귀 순. 요구 자원은 그 레벨에 열려 있는 탐사로 구할 수 있게
    // 맞춤(데이터 코어는 유적 탐사가 열리는 탐사 기술 3 이후, 외계 자원은 6 이후부터 요구).
    // 1~2단계 바이오매스는 스타터 행성(무대기)에서 안 나와 행성 탐사로만 모아야 해서 초반 병목이 됐음
    // (50/120 → 30/60). 습지·해양 행성을 얻고 나면 오히려 남는 자원이라 초반 요구량만 낮춤
    fun researchUpgradeCost(currentLevel: Int): Pair<Long, Map<ResourceType, Int>> = when (currentLevel) {
        1 -> 1_000L to mapOf(ResourceType.BIOMASS to 30)
        2 -> 3_000L to mapOf(ResourceType.BIOMASS to 60, ResourceType.COOLANT to 80)
        3 -> 6_000L to mapOf(ResourceType.COOLANT to 200, ResourceType.IRON_ORE to 200)
        4 -> 10_000L to mapOf(ResourceType.ENERGY_CORE to 300, ResourceType.CRYSTAL to 200)
        5 -> 15_000L to mapOf(ResourceType.LIFE_CRYSTAL to 200, ResourceType.MAGMA_STONE to 200)
        6 -> 22_000L to mapOf(ResourceType.LIFE_CRYSTAL to 300, ResourceType.DATA_CORE to 200)
        7 -> 30_000L to mapOf(ResourceType.DATA_CORE to 400, ResourceType.NANOBOT to 300)
        8 -> 40_000L to mapOf(ResourceType.NANOBOT to 500, ResourceType.ANCIENT_ARTIFACT to 300)
        else -> {
            val n = currentLevel - 9
            (50_000L + n * 15_000L) to mapOf(
                ResourceType.ANCIENT_ARTIFACT to 400 + 100 * n,
                ResourceType.QUANTUM_CORE to 300 + 80 * n,
                ResourceType.UNKNOWN_MATTER to 300 + 80 * n,
                ResourceType.ALIEN_TECH to 200 + 60 * n
            )
        }
    }

    // ── 광고 ──────────────────────────────────────────────────────────
    // 이 이상 경과해야 "복귀 시 확인 다이얼로그"를 띄움. 미만이면 기존처럼 조용히 1배로 자동 적립
    const val OFFLINE_PROFIT_DIALOG_THRESHOLD_MINUTES = 10L
    const val OFFLINE_PROFIT_AD_MULTIPLIER = 2.0

    // 대기시간 스킵 광고 1회당 당길 수 있는 최대 시간 (전체 스킵이 아니라 상한을 둬서 밸런스 보호)
    val AD_SKIP_MAX_MS = TimeUnit.HOURS.toMillis(4)

    // 대기시간 단축 광고 하루 최대 횟수(탐사·훈련 공유, 기기 로컬 자정 리셋). 횟수 제한이 없을 땐
    // 빨리 크려면 광고만 계속 봐야 하는 "광고 보는 게임"이 된다는 피드백 — 나머지는 코인 단축으로
    const val SKIP_WAIT_AD_DAILY_MAX = 5

    // 코인으로 탐사 대기를 없앨 때의 배율: 남은 분 × 분당 단가(coinSkipUnitPerMinute) × 이 값.
    // 1.5배도 탐사 기대 가치보다 비싸지만, 기대 가치는 슬롯이 남을 때의 행성 발견 가치(사서 계속 버는 몫)를
    // 낮게 잡은 값이라 2배로 여유를 둠 — 1등급 T1 약 310코인, 중반 T6 방치 2시간 48분치, 후반 T10 9시간치
    const val COIN_SKIP_PREMIUM = 2.0

    // 전면광고(탐사 결과 dismiss) 빈도 제한: 첫 N회는 노출 안 함, 이후엔 결과 INTERVAL번마다 1회
    // (예전 3분 쿨다운은 짧은 탐사를 연달아 돌리면 너무 자주 떠서 횟수 기준으로 바꿈).
    // 초반 탐사가 2~5분대라 첫 세션엔 광고 없이 루프를 익히게 유예를 넉넉히 둠
    const val INTERSTITIAL_GRACE_COMPLETIONS = 10
    const val INTERSTITIAL_INTERVAL_COMPLETIONS = 5

    // ── ORBIT 카드게임 (휴게실) ──────────────────────────────────────
    // 고정 4단계 베팅 금액. 위험도(배율) 티어는 별도 축(OrbitRiskTier)이며 재화 규모와
    // 무관하게 독립적으로 선택한다.
    val ORBIT_BET_AMOUNTS: List<Long> = listOf(500L, 1_000L, 2_500L, 5_000L)

    // 휴게실 일일 리워드 광고: 1회당 지급 재화 및 하루 최대 시청 횟수(기기 로컬 자정 리셋)
    const val ORBIT_DAILY_AD_REWARD_COINS = 500L
    const val ORBIT_DAILY_AD_MAX_COUNT = 5
}
