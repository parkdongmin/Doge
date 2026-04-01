package com.doge.simulator.domain.model

object PlanetMetaDataTable {

    val data: Map<PlanetType, PlanetMetaData> = mapOf(

        // ---------------------------
        // COMMON (낮은 희귀도)
        // ---------------------------
        PlanetType.TERRAN_WET to PlanetMetaData(
            type = PlanetType.TERRAN_WET,
            displayName = "Terran Wet",
            description = "습하고 물이 풍부한 지구형 행성. 농업·바이오 자원이 풍부하다.",
            productionMin = 80,
            productionMax = 140,
            riskMin = 5,
            riskMax = 15,
            investmentMin = 900,
            investmentMax = 1200,
            eventRateMin = 10,
            eventRateMax = 20,
            rarity = RarityTier.COMMON,
            basePrice = 1000,
            variants = generateVariants("TERRAN_WET", 30)
        ),

        PlanetType.TERRAN_DRY to PlanetMetaData(
            type = PlanetType.TERRAN_DRY,
            displayName = "Terran Dry",
            description = "건조하고 사막이 많은 지구형 행성. 광물 중심 경제 구조.",
            productionMin = 60,
            productionMax = 120,
            riskMin = 15,
            riskMax = 30,
            investmentMin = 1100,
            investmentMax = 1500,
            eventRateMin = 10,
            eventRateMax = 25,
            rarity = RarityTier.COMMON,
            basePrice = 1000,
            variants = generateVariants("TERRAN_DRY", 20)
        ),

        PlanetType.ISLANDS to PlanetMetaData(
            type = PlanetType.ISLANDS,
            displayName = "Islands",
            description = "바다와 섬으로 구성된 행성. 관광 및 해양 자원이 중심.",
            productionMin = 70,
            productionMax = 130,
            riskMin = 10,
            riskMax = 20,
            investmentMin = 1500,
            investmentMax = 2000,
            eventRateMin = 5,
            eventRateMax = 15,
            rarity = RarityTier.COMMON,
            basePrice = 1000,
            variants = generateVariants("ISLANDS", 15)
        ),

        PlanetType.NO_ATMOSPHERE to PlanetMetaData(
            type = PlanetType.NO_ATMOSPHERE,
            displayName = "No Atmosphere",
            description = "대기가 없는 황량한 행성. 자동 채굴 드론에 의존한다.",
            productionMin = 40,
            productionMax = 80,
            riskMin = 2,
            riskMax = 10,
            investmentMin = 400,
            investmentMax = 700,
            eventRateMin = 3,
            eventRateMax = 10,
            rarity = RarityTier.COMMON,
            basePrice = 1000, // 가장 흔함
            variants = generateVariants("NO_ATMOSPHERE", 25)
        ),


        // ---------------------------
        // UNCOMMON (중간 희귀도)
        // ---------------------------
        PlanetType.GAS_GIANT_1 to PlanetMetaData(
            type = PlanetType.GAS_GIANT_1,
            displayName = "Gas Giant I",
            description = "고압 에너지 추출이 가능한 거대 가스 행성.",
            productionMin = 150,
            productionMax = 250,
            riskMin = 20,
            riskMax = 40,
            investmentMin = 2500,
            investmentMax = 3500,
            eventRateMin = 15,
            eventRateMax = 25,
            rarity = RarityTier.UNCOMMON,
            basePrice = 4000,
            variants = generateVariants("GAS_GIANT_1", 10)
        ),

        PlanetType.GAS_GIANT_2 to PlanetMetaData(
            type = PlanetType.GAS_GIANT_2,
            displayName = "Gas Giant II",
            description = "고리가 있는 아름다운 가스 행성. 관광 가치 높음.",
            productionMin = 90,
            productionMax = 160,
            riskMin = 10,
            riskMax = 20,
            investmentMin = 3000,
            investmentMax = 3800,
            eventRateMin = 5,
            eventRateMax = 10,
            rarity = RarityTier.UNCOMMON,
            basePrice = 4000,
            variants = generateVariants("GAS_GIANT_2", 8)
        ),

        PlanetType.ICE_WORLD to PlanetMetaData(
            type = PlanetType.ICE_WORLD,
            displayName = "Ice World",
            description = "혹한의 행성. 극저온 자원이 풍부하다.",
            productionMin = 70,
            productionMax = 110,
            riskMin = 15,
            riskMax = 25,
            investmentMin = 1600,
            investmentMax = 2200,
            eventRateMin = 8,
            eventRateMax = 15,
            rarity = RarityTier.UNCOMMON,
            basePrice = 4000,
            variants = generateVariants("ICE_WORLD", 12)
        ),

        PlanetType.LAVA_WORLD to PlanetMetaData(
            type = PlanetType.LAVA_WORLD,
            displayName = "Lava World",
            description = "격렬한 화산 활동이 지속되는 행성. 고위험·고수익.",
            productionMin = 180,
            productionMax = 300,
            riskMin = 40,
            riskMax = 60,
            investmentMin = 2000,
            investmentMax = 3000,
            eventRateMin = 20,
            eventRateMax = 35,
            rarity = RarityTier.UNCOMMON,
            basePrice = 4000,
            variants = generateVariants("LAVA_WORLD", 10)
        ),

        PlanetType.ASTEROID to PlanetMetaData(
            type = PlanetType.ASTEROID,
            displayName = "Asteroid",
            description = "작은 자원 덩어리. 회전이 빠른 투자.",
            productionMin = 30,
            productionMax = 60,
            riskMin = 2,
            riskMax = 10,
            investmentMin = 200,
            investmentMax = 400,
            eventRateMin = 3,
            eventRateMax = 10,
            rarity = RarityTier.UNCOMMON,
            basePrice = 4000,
            variants = generateVariants("ASTEROID", 40)
        ),


        // ---------------------------
        // RARE (중상 희귀도)
        // ---------------------------
        PlanetType.BLACK_HOLE to PlanetMetaData(
            type = PlanetType.BLACK_HOLE,
            displayName = "Black Hole",
            description = "예측 불가한 초고위험 투자 대상.",
            productionMin = 300,
            productionMax = 600,
            riskMin = 80,
            riskMax = 100,
            investmentMin = 4000,
            investmentMax = 6000,
            eventRateMin = 30,
            eventRateMax = 50,
            rarity = RarityTier.RARE,
            basePrice = 50000,
            variants = generateVariants("BLACK_HOLE", 5)
        ),


        // ---------------------------
        // EPIC (높은 희귀도)
        // ---------------------------
        PlanetType.GALAXY to PlanetMetaData(
            type = PlanetType.GALAXY,
            displayName = "Galaxy",
            description = "은하 전체에 투자하는 최고급 자산. 안정적이며 고급 투자처.",
            productionMin = 200,
            productionMax = 300,
            riskMin = 5,
            riskMax = 15,
            investmentMin = 8000,
            investmentMax = 12000,
            eventRateMin = 2,
            eventRateMax = 10,
            rarity = RarityTier.EPIC,
            basePrice = 50000,
            variants = generateVariants("GALAXY", 3)
        ),

        // ---------------------------
        // LEGENDARY (최희귀)
        // ---------------------------
        PlanetType.STAR to PlanetMetaData(
            type = PlanetType.STAR,
            displayName = "Star",
            description = "항성의 에너지를 직접 수확하는 고급 자원원.",
            productionMin = 250,
            productionMax = 350,
            riskMin = 8,
            riskMax = 20,
            investmentMin = 7000,
            investmentMax = 10000,
            eventRateMin = 5,
            eventRateMax = 12,
            rarity = RarityTier.LEGENDARY,
            basePrice = 200000,
            variants = generateVariants("STAR", 3)
        )
    )

    // 행성 변종 리스트 생성
    private fun generateVariants(prefix: String, count: Int): List<PlanetVariant> {
        return (1..count).map { index ->
            PlanetVariant(
                variantId = "$prefix-${index.toString().padStart(2, '0')}",
                imageUrl = "https://dummy.firebase.com/planet/$prefix/$index.png"
            )
        }
    }
}