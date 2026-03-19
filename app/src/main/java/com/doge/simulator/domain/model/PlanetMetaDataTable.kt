package com.doge.simulator.domain.model

object PlanetMetaDataTable {
    val data: Map<PlanetType, PlanetMetaData> = mapOf(
        PlanetType.TERRAN_WET to PlanetMetaData(
            type = PlanetType.TERRAN_WET,
            displayName = "Terran Wet",
            description = "습하고 물이 풍부한 지구형 행성. 농업·바이오 자원이 풍부하다.",
            baseProduction = 120,
            baseRisk = 10,
            baseInvestmentCost = 1000,
            eventRate = 15
        ),

        PlanetType.TERRAN_DRY to PlanetMetaData(
            type = PlanetType.TERRAN_DRY,
            displayName = "Terran Dry",
            description = "건조하고 사막이 많은 지구형 행성. 광물 중심 경제 구조.",
            baseProduction = 80,
            baseRisk = 25,
            baseInvestmentCost = 1500,
            eventRate = 20
        ),

        PlanetType.ISLANDS to PlanetMetaData(
            type = PlanetType.ISLANDS,
            displayName = "Islands",
            description = "바다와 섬으로 구성된 행성. 관광 및 해양 자원이 중심.",
            baseProduction = 100,
            baseRisk = 15,
            baseInvestmentCost = 2000,
            eventRate = 10
        ),

        PlanetType.NO_ATMOSPHERE to PlanetMetaData(
            type = PlanetType.NO_ATMOSPHERE,
            displayName = "No Atmosphere",
            description = "대기가 없는 황량한 행성. 자동 채굴용 드론에 의존한다.",
            baseProduction = 60,
            baseRisk = 5,
            baseInvestmentCost = 500,
            eventRate = 5
        ),

        PlanetType.GAS_GIANT_1 to PlanetMetaData(
            type = PlanetType.GAS_GIANT_1,
            displayName = "Gas Giant I",
            description = "수소와 헬륨 기반의 거대 가스 행성. 고압 에너지 수익 가능.",
            baseProduction = 180,
            baseRisk = 35,
            baseInvestmentCost = 3000,
            eventRate = 25
        ),

        PlanetType.GAS_GIANT_2 to PlanetMetaData(
            type = PlanetType.GAS_GIANT_2,
            displayName = "Gas Giant II",
            description = "토성형 아름다운 고리를 가진 가스 행성. 관광 가치 높음.",
            baseProduction = 100,
            baseRisk = 15,
            baseInvestmentCost = 3500,
            eventRate = 10
        ),

        PlanetType.ICE_WORLD to PlanetMetaData(
            type = PlanetType.ICE_WORLD,
            displayName = "Ice World",
            description = "얼음과 혹한으로 덮인 행성. 극저온 자원 채굴.",
            baseProduction = 90,
            baseRisk = 20,
            baseInvestmentCost = 1800,
            eventRate = 12
        ),

        PlanetType.LAVA_WORLD to PlanetMetaData(
            type = PlanetType.LAVA_WORLD,
            displayName = "Lava World",
            description = "끊임없이 화산활동이 일어나는 위험한 행성. 고위험 고수익.",
            baseProduction = 200,
            baseRisk = 50,
            baseInvestmentCost = 2500,
            eventRate = 30
        ),

        PlanetType.ASTEROID to PlanetMetaData(
            type = PlanetType.ASTEROID,
            displayName = "Asteroid",
            description = "소규모 자원 덩어리. 소액 투자로 빠른 회전 가능.",
            baseProduction = 40,
            baseRisk = 5,
            baseInvestmentCost = 300,
            eventRate = 5
        ),

        PlanetType.BLACK_HOLE to PlanetMetaData(
            type = PlanetType.BLACK_HOLE,
            displayName = "Black Hole",
            description = "예측 불가한 초고위험 투자. 대박 또는 파산.",
            baseProduction = 500,  // 확률용 베이스, 실제 알고리즘에서 변동 예정
            baseRisk = 90,
            baseInvestmentCost = 5000,
            eventRate = 50   // 이벤트 훨씬 자주 발생
        ),

        PlanetType.GALAXY to PlanetMetaData(
            type = PlanetType.GALAXY,
            displayName = "Galaxy",
            description = "은하 전체에 투자하는 최고급 자산. 매우 안정적.",
            baseProduction = 250,
            baseRisk = 8,
            baseInvestmentCost = 10000,
            eventRate = 5
        ),

        PlanetType.STAR to PlanetMetaData(
            type = PlanetType.STAR,
            displayName = "Star",
            description = "항성 에너지 추출을 통한 거대한 전력 수입원.",
            baseProduction = 300,
            baseRisk = 12,
            baseInvestmentCost = 8000,
            eventRate = 7
        )
    )
}