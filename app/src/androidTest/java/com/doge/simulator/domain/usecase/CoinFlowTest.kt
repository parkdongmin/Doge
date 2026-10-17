package com.doge.simulator.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doge.simulator.data.local.PlanetDatabase
import com.doge.simulator.data.repository.AstronautRepositoryImpl
import com.doge.simulator.data.repository.ExpeditionRepositoryImpl
import com.doge.simulator.data.repository.PlanetEventLogRepositoryImpl
import com.doge.simulator.data.repository.PlanetRepositoryImpl
import com.doge.simulator.data.repository.RecruitmentRepositoryImpl
import com.doge.simulator.data.repository.ResearchLabRepositoryImpl
import com.doge.simulator.data.repository.ResourceRepositoryImpl
import com.doge.simulator.data.repository.SpaceshipRepositoryImpl
import com.doge.simulator.data.repository.UserRepositoryImpl
import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.AstronautGrade
import com.doge.simulator.domain.model.AstronautSpecialty
import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionCategory
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetType
import com.doge.simulator.domain.model.RecruitmentCandidate
import com.doge.simulator.domain.model.ResearchField
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.model.Spaceship
import com.doge.simulator.domain.model.maintenancePlan
import com.doge.simulator.domain.model.maintenanceResourceCost
import com.doge.simulator.domain.model.marketValue
import com.doge.simulator.domain.model.upgradeCost
import com.doge.simulator.domain.model.orbit.OrbitRiskTier
import com.doge.simulator.domain.usecase.orbit.StartOrbitMatchUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// 코인을 쓰는·받는 곳마다 "화면에 보이는 가격(같은 계산식)만큼 정확히 한 번" 빠지고 들어오는지,
// 실패하면 코인·자원이 그대로 돌아오는지 실제 Room(in-memory)으로 확인한다
@RunWith(AndroidJUnit4::class)
class CoinFlowTest {

    private lateinit var db: PlanetDatabase
    private lateinit var user: UserRepositoryImpl
    private lateinit var lab: ResearchLabRepositoryImpl
    private lateinit var resources: ResourceRepositoryImpl
    private lateinit var planets: PlanetRepositoryImpl
    private lateinit var ships: SpaceshipRepositoryImpl
    private lateinit var astronauts: AstronautRepositoryImpl
    private lateinit var recruitment: RecruitmentRepositoryImpl
    private lateinit var expeditions: ExpeditionRepositoryImpl

    @Before
    fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PlanetDatabase::class.java).build()
        user = UserRepositoryImpl(db.userDao())
        lab = ResearchLabRepositoryImpl(db.researchLabDao())
        resources = ResourceRepositoryImpl(db.resourceDao())
        planets = PlanetRepositoryImpl(db.planetDao())
        ships = SpaceshipRepositoryImpl(db.spaceshipDao())
        astronauts = AstronautRepositoryImpl(db.astronautDao())
        recruitment = RecruitmentRepositoryImpl(db.recruitmentDao())
        expeditions = ExpeditionRepositoryImpl(db.expeditionDao())
        user.initialize()
        lab.initialize()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun coins() = user.getCoins().first()
    private suspend fun setCoins(target: Long) {
        val now = coins()
        if (target > now) user.addCoins(target - now) else user.deductCoins(now - target)
    }
    private suspend fun amount(type: ResourceType) = resources.getAmount(type)
    private suspend fun giveResources(cost: Map<ResourceType, Int>) = cost.forEach { (t, a) -> resources.add(t, a.toLong()) }

    private fun planet(level: Int = 1, lossMultiplier: Double = 0.0) = Planet(
        type = PlanetType.TERRAN_WET, production = 100, risk = 10, investment = 1, eventRate = 30,
        buyPrice = 5_000, level = level, variantId = "terran_wet-1", lossMultiplier = lossMultiplier
    )

    // ── 쓰는 곳 ─────────────────────────────────────────────────────

    @Test
    fun buySpaceship_chargesExactPriceOnce() = runBlocking {
        setCoins(GameConstants.SCOUT_SHIP_BASE_COST + 10)
        val before = ships.getSpaceships().first().size
        assertEquals(BuySpaceshipUseCase.Result.Success, BuySpaceshipUseCase(ships, user, lab)())
        assertEquals(10L, coins())
        assertEquals(before + 1, ships.getSpaceships().first().size)
        // 코인이 모자라면 아무것도 안 빠지고 안 생긴다
        assertEquals(BuySpaceshipUseCase.Result.InsufficientCoins, BuySpaceshipUseCase(ships, user, lab)())
        assertEquals(10L, coins())
        assertEquals(before + 1, ships.getSpaceships().first().size)
    }

    @Test
    fun hire_chargesCandidatePrice() = runBlocking {
        val candidate = RecruitmentCandidate(name = "테스트", specialty = AstronautSpecialty.MINERAL,
            grade = AstronautGrade.REGULAR, proficiency = 30)
        recruitment.setCandidate(0, candidate)
        setCoins(candidate.hireCost() + 7)
        val useCase = HireFromPoolUseCase(recruitment, astronauts, user, lab)
        assertEquals(HireFromPoolUseCase.Result.Success, useCase(0))
        assertEquals(7L, coins())
        assertEquals(1, astronauts.getAstronauts().first().size)
        // 같은 칸을 다시 눌러도 이미 영입돼 빈 칸 — 코인 그대로
        assertEquals(HireFromPoolUseCase.Result.SlotEmpty, useCase(0))
        assertEquals(7L, coins())
    }

    @Test
    fun upgradeSpaceship_chargesCoinsAndResources_refundsOnShortage() = runBlocking {
        val ship = Spaceship(id = "s1", name = "정찰선", crewCapacity = 2, speed = 0, cargo = 0, successRate = 0.5f)
        ships.buy(ship)
        val (coinCost, resCost) = GameConstants.spaceshipUpgradeCost(ship.grade)
        val useCase = UpgradeSpaceshipUseCase(ships, user, resources)

        // 자원 부족 → 코인·자원 전부 원래대로
        setCoins(coinCost + 5)
        assertEquals(UpgradeSpaceshipUseCase.Result.InsufficientResources, useCase(ship))
        assertEquals(coinCost + 5, coins())

        giveResources(resCost)
        assertEquals(UpgradeSpaceshipUseCase.Result.Success, useCase(ship))
        assertEquals(5L, coins())
        resCost.forEach { (t, _) -> assertEquals(0L, amount(t)) }
        // 같은 등급 정보로 한 번 더 → 이미 강화됨으로 보고 전액 환불
        setCoins(coinCost + 5); giveResources(resCost)
        assertEquals(UpgradeSpaceshipUseCase.Result.Conflict, useCase(ship))
        assertEquals(coinCost + 5, coins())
    }

    @Test
    fun upgradeResearch_chargesCoinsAndResources_refundsOnShortage() = runBlocking {
        val field = ResearchField.CELESTIAL_ANALYSIS
        val level = lab.get().first().getLevel(field)
        val (coinCost, resCost) = GameConstants.researchUpgradeCost(level, field)
        val useCase = UpgradeResearchFieldUseCase(lab, user, resources)

        setCoins(coinCost + 3)
        if (resCost.isNotEmpty()) {
            assertEquals(UpgradeResearchFieldUseCase.Result.InsufficientResources, useCase(field))
            assertEquals(coinCost + 3, coins())
        }
        giveResources(resCost)
        assertTrue(useCase(field) is UpgradeResearchFieldUseCase.Result.Success)
        assertEquals(3L, coins())
        assertEquals(level + 1, lab.get().first().getLevel(field))
        resCost.forEach { (t, _) -> assertEquals(0L, amount(t)) }
    }

    @Test
    fun train_chargesCoinsAndResources_refundsOnShortage() = runBlocking {
        val a = Astronaut(id = "a1", name = "a1", specialty = AstronautSpecialty.MINERAL,
            grade = AstronautGrade.INTERN, proficiency = 10)
        astronauts.hire(a)
        val useCase = TrainAstronautUseCase(astronauts, user, resources, lab)

        setCoins(GameConstants.BASIC_TRAINING_COST_COINS + 1)
        assertEquals(TrainAstronautUseCase.Result.InsufficientResources, useCase(a, false))
        assertEquals(GameConstants.BASIC_TRAINING_COST_COINS + 1, coins())

        giveResources(GameConstants.BASIC_TRAINING_RESOURCE_COST)
        assertEquals(TrainAstronautUseCase.Result.Success, useCase(a, false))
        assertEquals(1L, coins())
        assertEquals(AstronautStatus.TRAINING, astronauts.getAstronauts().first().first().status)
    }

    @Test
    fun upgradePlanet_chargesShownCost_refundsOnShortage() = runBlocking {
        val p = planet(level = 3)
        planets.buyPlanet(p)
        val (coinCost, resCost) = p.upgradeCost
        val useCase = UpgradePlanetUseCase(planets, user, resources)

        setCoins(coinCost + 9)
        if (resCost.isNotEmpty()) {
            assertEquals(UpgradePlanetUseCase.Result.InsufficientResources, useCase(p))
            assertEquals(coinCost + 9, coins())
        }
        giveResources(resCost)
        val result = useCase(p)
        assertTrue(result is UpgradePlanetUseCase.Result.Success || result is UpgradePlanetUseCase.Result.Failed)
        // 성공이든 실패든 비용은 한 번 빠지고 투자액에 쌓인다
        assertEquals(9L, coins())
        resCost.forEach { (t, _) -> assertEquals(0L, amount(t)) }
        assertEquals(coinCost, planets.getOwnedPlanets().first().first().upgradeInvestment)
    }

    @Test
    fun maintain_chargesPlanShown() = runBlocking {
        val p = planet(level = 5, lossMultiplier = 1.0)
        planets.buyPlanet(p)
        // 자원 일부만 있어서 모자란 몫은 코인으로 — 화면과 같은 maintenancePlan으로 계산
        for ((t, a) in p.maintenanceResourceCost) resources.add(t, a / 2)
        val plan = p.maintenancePlan { t -> (p.maintenanceResourceCost[t] ?: 0L) / 2 }
        setCoins(plan.totalCoins + 4)

        val collect = CollectProfitUseCase(planets, user, resources,
            RollPlanetEventUseCase(planets, PlanetEventLogRepositoryImpl(db.planetEventLogDao())))
        val result = MaintainPlanetUseCase(planets, user, resources, collect)(p.id)
        assertTrue(result is MaintainPlanetUseCase.Result.Success)
        assertEquals(4L, coins())
        plan.resourcesUsed.forEach { (t, used) -> assertEquals((p.maintenanceResourceCost[t] ?: 0L) / 2 - used, amount(t)) }
        assertEquals(0.0, planets.getOwnedPlanets().first().first().lossMultiplier, 0.0)
    }

    @Test
    fun skipWithCoins_neverChargesMoreThanQuoted() = runBlocking {
        ships.buy(Spaceship(id = "s1", name = "정찰선", crewCapacity = 2, speed = 0, cargo = 0, successRate = 0.5f))
        val now = System.currentTimeMillis()
        expeditions.start(Expedition(id = "e1", category = ExpeditionCategory.MINERAL, tier = 3, astronautIds = emptyList(),
            spaceshipId = "s1", startTime = now, endTime = now + 3_600_000))
        setCoins(1_000_000)
        val useCase = SkipExpeditionWithCoinsUseCase(expeditions, ships, astronauts, lab, planets, user)

        // 화면에 보인 값이 더 싸면 그 값만 낸다
        val result = useCase("e1", quotedCost = 123L, now = now)
        assertTrue(result is SkipExpeditionWithCoinsUseCase.Result.Success)
        assertEquals(1_000_000L - (result as SkipExpeditionWithCoinsUseCase.Result.Success).cost, coins())
        assertTrue(result.cost <= 123L)
        // 이미 끝난 탐사는 안 빠진다
        assertEquals(SkipExpeditionWithCoinsUseCase.Result.AlreadyFinished, useCase("e1", 123L, now))
        assertEquals(1_000_000L - result.cost, coins())
    }

    @Test
    fun orbitBet_chargesBetOnce() = runBlocking {
        setCoins(1_000)
        val useCase = StartOrbitMatchUseCase(user)
        assertTrue(useCase(400, OrbitRiskTier.entries.first()) is StartOrbitMatchUseCase.Result.Success)
        assertEquals(600L, coins())
        assertEquals(StartOrbitMatchUseCase.Result.InsufficientCoins, useCase(601, OrbitRiskTier.entries.first()))
        assertEquals(600L, coins())
    }

    // ── 받는 곳 ─────────────────────────────────────────────────────

    @Test
    fun sellPlanet_paysMarketValueMinusFeeOnce() = runBlocking {
        val p = planet()
        planets.buyPlanet(p)
        setCoins(0)
        val useCase = SellPlanetUseCase(planets, user)
        useCase(p)
        val expected = p.marketValue - (p.marketValue * GameConstants.SELL_FEE_RATE).toLong()
        assertEquals(expected, coins())
        // 연타로 같은 행성을 또 팔아도 한 번만 받는다
        useCase(p)
        assertEquals(expected, coins())
    }

    @Test
    fun sellResource_paysUnitPriceTimesAmount() = runBlocking {
        val type = ResourceType.entries.first()
        resources.add(type, 10)
        setCoins(0)
        val useCase = SellResourceUseCase(resources, user)
        assertTrue(useCase(type, 4) is SellResourceUseCase.Result.Success)
        assertEquals(GameConstants.RESOURCE_SELL_PRICE.getValue(type) * 4, coins())
        assertEquals(6L, amount(type))
        // 가진 것보다 많이 팔면 아무 일도 없다
        assertEquals(SellResourceUseCase.Result.InsufficientAmount, useCase(type, 7))
        assertEquals(GameConstants.RESOURCE_SELL_PRICE.getValue(type) * 4, coins())
    }
}
