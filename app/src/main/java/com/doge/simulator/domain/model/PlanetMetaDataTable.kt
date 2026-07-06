package com.doge.simulator.domain.model

import com.doge.simulator.BuildConfig

object PlanetMetaDataTable {

    val data: Map<PlanetType, PlanetMetaData> = mapOf(

        PlanetType.TERRAN_WET to PlanetMetaData(
            type = PlanetType.TERRAN_WET,
            displayName = "습지 행성",
            description = "습하고 물이 풍부한 지구형 행성. 농업·바이오 자원이 풍부하다.",
            productionMin = 80, productionMax = 140,
            riskMin = 5, riskMax = 15,
            investmentMin = 900, investmentMax = 1200,
            eventRateMin = 10, eventRateMax = 20,
            baseMaintenanceCostMin = 5, baseMaintenanceCostMax = 10,
            rarity = RarityTier.COMMON,
            basePrice = 1000,
            variants = generateVariants("TERRAN_WET", 30),
            // 습지·물·바이오 행성 → 냉각재(물), 바이오매스, 생명 결정
            resourceDrops = mapOf(
                ResourceType.COOLANT to 8,
                ResourceType.BIOMASS to 8,
                ResourceType.LIFE_CRYSTAL to 2
            )
        ),

        PlanetType.TERRAN_DRY to PlanetMetaData(
            type = PlanetType.TERRAN_DRY,
            displayName = "사막 행성",
            description = "건조하고 사막이 많은 지구형 행성. 광물 중심 경제 구조.",
            productionMin = 60, productionMax = 120,
            riskMin = 15, riskMax = 30,
            investmentMin = 1100, investmentMax = 1500,
            eventRateMin = 10, eventRateMax = 25,
            baseMaintenanceCostMin = 5, baseMaintenanceCostMax = 12,
            rarity = RarityTier.COMMON,
            basePrice = 1000,
            variants = generateVariants("TERRAN_DRY", 20),
            // 사막·광물 행성 → 철광석, 사막 수정(크리스탈). 희토류는 소행성 쪽 희귀 자원으로 분리
            resourceDrops = mapOf(
                ResourceType.IRON_ORE to 8,
                ResourceType.CRYSTAL to 2
            )
        ),

        PlanetType.ISLANDS to PlanetMetaData(
            type = PlanetType.ISLANDS,
            displayName = "군도 행성",
            description = "바다와 섬으로 구성된 행성. 관광 및 해양 자원이 중심.",
            productionMin = 70, productionMax = 130,
            riskMin = 10, riskMax = 20,
            investmentMin = 1500, investmentMax = 2000,
            eventRateMin = 5, eventRateMax = 15,
            baseMaintenanceCostMin = 6, baseMaintenanceCostMax = 11,
            rarity = RarityTier.COMMON,
            basePrice = 1000,
            variants = generateVariants("ISLANDS", 15),
            // 해양·관광 행성 → 산호/생물(바이오매스), 해수 에너지, 진주(크리스탈)
            resourceDrops = mapOf(
                ResourceType.BIOMASS to 7,
                ResourceType.ENERGY_CORE to 3,
                ResourceType.CRYSTAL to 1
            )
        ),

        PlanetType.NO_ATMOSPHERE to PlanetMetaData(
            type = PlanetType.NO_ATMOSPHERE,
            displayName = "무대기 행성",
            description = "대기가 없는 황량한 행성. 자동 채굴 드론에 의존한다.",
            productionMin = 40, productionMax = 80,
            riskMin = 2, riskMax = 10,
            investmentMin = 400, investmentMax = 700,
            eventRateMin = 3, eventRateMax = 10,
            baseMaintenanceCostMin = 3, baseMaintenanceCostMax = 8,
            rarity = RarityTier.COMMON,
            basePrice = 1000,
            variants = generateVariants("NO_ATMOSPHERE", 25),
            // 무대기·채굴 드론 행성 → 운석/중금속(철광석), 진공 결정, 드론 자체 자원인 나노봇
            resourceDrops = mapOf(
                ResourceType.IRON_ORE to 7,
                ResourceType.CRYSTAL to 2,
                ResourceType.NANOBOT to 1
            )
        ),

        PlanetType.GAS_GIANT_1 to PlanetMetaData(
            type = PlanetType.GAS_GIANT_1,
            displayName = "가스 자이언트 I",
            description = "고압 에너지 추출이 가능한 거대 가스 행성.",
            productionMin = 150, productionMax = 250,
            riskMin = 20, riskMax = 40,
            investmentMin = 2500, investmentMax = 3500,
            eventRateMin = 15, eventRateMax = 25,
            baseMaintenanceCostMin = 15, baseMaintenanceCostMax = 25,
            rarity = RarityTier.UNCOMMON,
            basePrice = 4000,
            variants = generateVariants("GAS_GIANT_1", 10),
            // 고압 에너지 추출 행성 → 에너지 코어 위주, 헬륨 냉매
            resourceDrops = mapOf(
                ResourceType.ENERGY_CORE to 8,
                ResourceType.COOLANT to 3
            )
        ),

        PlanetType.GAS_GIANT_2 to PlanetMetaData(
            type = PlanetType.GAS_GIANT_2,
            displayName = "가스 자이언트 II",
            description = "고리가 있는 아름다운 가스 행성. 관광 가치 높음.",
            productionMin = 90, productionMax = 160,
            riskMin = 10, riskMax = 20,
            investmentMin = 3000, investmentMax = 3800,
            eventRateMin = 5, eventRateMax = 10,
            baseMaintenanceCostMin = 12, baseMaintenanceCostMax = 20,
            rarity = RarityTier.UNCOMMON,
            basePrice = 4000,
            variants = generateVariants("GAS_GIANT_2", 8),
            // 고리·관광 행성 → 메탄/암모니아(냉각재), 관광 데이터(데이터 코어), 고리 먼지(크리스탈)
            resourceDrops = mapOf(
                ResourceType.COOLANT to 6,
                ResourceType.DATA_CORE to 2,
                ResourceType.CRYSTAL to 1
            )
        ),

        PlanetType.ICE_WORLD to PlanetMetaData(
            type = PlanetType.ICE_WORLD,
            displayName = "빙하 행성",
            description = "혹한의 행성. 극저온 자원이 풍부하다.",
            productionMin = 70, productionMax = 110,
            riskMin = 15, riskMax = 25,
            investmentMin = 1600, investmentMax = 2200,
            eventRateMin = 8, eventRateMax = 15,
            baseMaintenanceCostMin = 14, baseMaintenanceCostMax = 22,
            rarity = RarityTier.UNCOMMON,
            basePrice = 4000,
            variants = generateVariants("ICE_WORLD", 12),
            // 극저온 행성 → 드라이아이스/냉동 메탄(냉각재), 빙하 결정
            resourceDrops = mapOf(
                ResourceType.COOLANT to 8,
                ResourceType.CRYSTAL to 3
            )
        ),

        PlanetType.LAVA_WORLD to PlanetMetaData(
            type = PlanetType.LAVA_WORLD,
            displayName = "용암 행성",
            description = "격렬한 화산 활동이 지속되는 행성. 고위험·고수익.",
            productionMin = 180, productionMax = 300,
            riskMin = 40, riskMax = 60,
            investmentMin = 2000, investmentMax = 3000,
            eventRateMin = 20, eventRateMax = 35,
            baseMaintenanceCostMin = 20, baseMaintenanceCostMax = 35,
            rarity = RarityTier.UNCOMMON,
            basePrice = 4000,
            variants = generateVariants("LAVA_WORLD", 10),
            // 화산 행성 → 용암석/마그마 코어(마그마석), 불꽃 결정
            resourceDrops = mapOf(
                ResourceType.MAGMA_STONE to 8,
                ResourceType.CRYSTAL to 3
            )
        ),

        PlanetType.ASTEROID to PlanetMetaData(
            type = PlanetType.ASTEROID,
            displayName = "소행성",
            description = "작은 자원 덩어리. 빠른 회전 투자.",
            productionMin = 30, productionMax = 60,
            riskMin = 2, riskMax = 10,
            investmentMin = 200, investmentMax = 400,
            eventRateMin = 3, eventRateMax = 10,
            baseMaintenanceCostMin = 2, baseMaintenanceCostMax = 6,
            rarity = RarityTier.UNCOMMON,
            basePrice = 4000,
            variants = generateVariants("ASTEROID", 40),
            // 소행성 채굴 → 철-니켈(철광석), 백금(희토류)
            resourceDrops = mapOf(
                ResourceType.IRON_ORE to 9,
                ResourceType.RARE_EARTH to 2
            )
        ),

        PlanetType.BLACK_HOLE to PlanetMetaData(
            type = PlanetType.BLACK_HOLE,
            displayName = "블랙홀",
            description = "예측 불가한 초고위험 투자 대상.",
            productionMin = 300, productionMax = 600,
            riskMin = 80, riskMax = 100,
            investmentMin = 4000, investmentMax = 6000,
            eventRateMin = 30, eventRateMax = 50,
            baseMaintenanceCostMin = 40, baseMaintenanceCostMax = 70,
            rarity = RarityTier.RARE,
            basePrice = 50000,
            variants = generateVariants("BLACK_HOLE", 5),
            // 예측불가 초고위험 → 호킹 에너지, 다크매터(미지의 물질 - 정확히 매칭), 특이점(퀀텀 코어)
            resourceDrops = mapOf(
                ResourceType.ENERGY_CORE to 6,
                ResourceType.UNKNOWN_MATTER to 2,
                ResourceType.QUANTUM_CORE to 1
            )
        ),

        PlanetType.GALAXY to PlanetMetaData(
            type = PlanetType.GALAXY,
            displayName = "은하",
            description = "은하 전체에 투자하는 최고급 자산. 안정적이며 고급 투자처.",
            productionMin = 200, productionMax = 300,
            riskMin = 5, riskMax = 15,
            investmentMin = 8000, investmentMax = 12000,
            eventRateMin = 2, eventRateMax = 10,
            baseMaintenanceCostMin = 20, baseMaintenanceCostMax = 40,
            rarity = RarityTier.EPIC,
            basePrice = 50000,
            variants = generateVariants("GALAXY", 3),
            // 안정적인 최고급 투자처 → 은하 에너지, 항성 코어(퀀텀 코어), 우주 결정
            resourceDrops = mapOf(
                ResourceType.ENERGY_CORE to 5,
                ResourceType.QUANTUM_CORE to 3,
                ResourceType.CRYSTAL to 2
            )
        ),

        PlanetType.STAR to PlanetMetaData(
            type = PlanetType.STAR,
            displayName = "항성",
            description = "항성의 에너지를 직접 수확하는 고급 자원원.",
            productionMin = 250, productionMax = 350,
            riskMin = 8, riskMax = 20,
            investmentMin = 7000, investmentMax = 10000,
            eventRateMin = 5, eventRateMax = 12,
            baseMaintenanceCostMin = 25, baseMaintenanceCostMax = 50,
            rarity = RarityTier.LEGENDARY,
            basePrice = 200000,
            variants = generateVariants("STAR", 3),
            // 항성 에너지 직접 수확 → 태양풍/플레어(에너지 코어), 핵융합 연료(퀀텀 코어), 코로나 결정
            resourceDrops = mapOf(
                ResourceType.ENERGY_CORE to 7,
                ResourceType.QUANTUM_CORE to 3,
                ResourceType.CRYSTAL to 1
            )
        )
    )

    private fun generateVariants(prefix: String, count: Int): List<PlanetVariant> {
        val base = BuildConfig.FIREBASE_STORAGE_BASE_URL
        return (1..count).map { index ->
            val variantId = "$prefix-${index.toString().padStart(2, '0')}"
            PlanetVariant(
                variantId = variantId,
                imageUrl = "$base/planets%2F$variantId.gif?alt=media"
            )
        }
    }
}