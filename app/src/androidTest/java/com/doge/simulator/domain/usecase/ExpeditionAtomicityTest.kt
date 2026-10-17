package com.doge.simulator.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doge.simulator.data.local.PlanetDatabase
import com.doge.simulator.data.local.RoomTransactionRunner
import com.doge.simulator.data.local.entity.AstronautEntity
import com.doge.simulator.data.local.entity.ExpeditionEntity
import com.doge.simulator.data.repository.AstronautRepositoryImpl
import com.doge.simulator.data.repository.ExpeditionRepositoryImpl
import com.doge.simulator.data.repository.PlanetRepositoryImpl
import com.doge.simulator.data.repository.ResearchLabRepositoryImpl
import com.doge.simulator.data.repository.ResourceRepositoryImpl
import com.doge.simulator.data.repository.SpaceshipRepositoryImpl
import com.doge.simulator.data.repository.StoryRepositoryImpl
import com.doge.simulator.data.repository.UserRepositoryImpl
import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.AstronautGrade
import com.doge.simulator.domain.model.AstronautSpecialty
import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionCategory
import com.doge.simulator.domain.model.ExpeditionReport
import com.doge.simulator.domain.model.ExpeditionStatus
import com.doge.simulator.domain.model.PlanetType
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.model.Spaceship
import com.doge.simulator.domain.model.TrainingType
import com.doge.simulator.domain.repository.AstronautRepository
import com.doge.simulator.domain.repository.ResourceRepository
import com.doge.simulator.domain.repository.StoryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// 탐사 파견·완료가 한 트랜잭션으로 묶였는지 실제 Room(in-memory)으로 확인한다.
// 핵심: 완료 처리 도중 취소돼도 대원이 "탐사 중"에 묶이지 않고, 다시 처리하면 정상 완료된다.
@RunWith(AndroidJUnit4::class)
class ExpeditionAtomicityTest {

    private lateinit var db: PlanetDatabase
    private lateinit var astronautRepo: AstronautRepositoryImpl
    private lateinit var expeditionRepo: ExpeditionRepositoryImpl
    private lateinit var resourceRepo: ResourceRepositoryImpl
    private lateinit var userRepo: UserRepositoryImpl
    private lateinit var storyRepo: StoryRepositoryImpl
    private lateinit var spaceshipRepo: SpaceshipRepositoryImpl
    private lateinit var labRepo: ResearchLabRepositoryImpl

    @Before
    fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PlanetDatabase::class.java).build()
        astronautRepo = AstronautRepositoryImpl(db.astronautDao())
        expeditionRepo = ExpeditionRepositoryImpl(db.expeditionDao())
        resourceRepo = ResourceRepositoryImpl(db.resourceDao())
        userRepo = UserRepositoryImpl(db.userDao())
        storyRepo = StoryRepositoryImpl(db.storyProgressDao(), db.expeditionReportDao())
        spaceshipRepo = SpaceshipRepositoryImpl(db.spaceshipDao())
        labRepo = ResearchLabRepositoryImpl(db.researchLabDao())
        userRepo.initialize()
        labRepo.initialize()
    }

    @After
    fun tearDown() = db.close()

    private fun completeUseCase(
        resources: ResourceRepository = resourceRepo,
        story: StoryRepository = storyRepo
    ) = CompleteExpeditionUseCase(
        expeditionRepository = expeditionRepo,
        astronautRepository = astronautRepo,
        spaceshipRepository = spaceshipRepo,
        resourceRepository = resources,
        researchLabRepository = labRepo,
        userRepository = userRepo,
        generateReportUseCase = GenerateExpeditionReportUseCase(story),
        generatePlanetsUseCase = GeneratePlanetsUseCase(),
        storyRepository = story,
        transactionRunner = RoomTransactionRunner(db)
    )

    private fun startUseCase(astronauts: AstronautRepository = astronautRepo) = StartExpeditionUseCase(
        expeditionRepository = expeditionRepo,
        astronautRepository = astronauts,
        spaceshipRepository = spaceshipRepo,
        researchLabRepository = labRepo,
        getTierProgress = GetTierProgressUseCase(userRepo, storyRepo),
        transactionRunner = RoomTransactionRunner(db)
    )

    private suspend fun hire(id: String) = astronautRepo.hire(
        Astronaut(id = id, name = id, specialty = AstronautSpecialty.MINERAL, grade = AstronautGrade.INTERN, proficiency = 20)
    )

    private suspend fun buyShip(id: String = "ship") = spaceshipRepo.buy(
        Spaceship(id = id, name = "정찰선", crewCapacity = 3, speed = 0, cargo = 0, successRate = 0.5f)
    )

    private suspend fun status(id: String) = astronautRepo.getAstronauts().first().first { it.id == id }.status

    private suspend fun expedition(id: String) = db.snapshotDao().recentExpeditions(100).first { it.id == id }

    // 이미 끝난 탐사를 DEPLOYED 대원과 함께 넣어 둔다 (파견 직후 시간이 다 흐른 상태)
    private suspend fun seedFinishedExpedition(id: String, crew: List<String>): Expedition {
        crew.forEach { astronautRepo.updateStatus(it, AstronautStatus.DEPLOYED) }
        val now = System.currentTimeMillis()
        val e = Expedition(
            id = id, category = ExpeditionCategory.MINERAL, tier = 1, astronautIds = crew,
            spaceshipId = "ship", startTime = now - 60_000, endTime = now - 1
        )
        expeditionRepo.start(e)
        return e
    }

    @Test
    fun complete_normal_releasesCrewAndGrantsOnce() = runBlocking {
        buyShip(); hire("a1"); hire("a2")
        val e = seedFinishedExpedition("e1", listOf("a1", "a2"))
        val coinsBefore = userRepo.getCoins().first()

        completeUseCase()(e)

        assertEquals(AstronautStatus.IDLE, status("a1"))
        assertEquals(AstronautStatus.IDLE, status("a2"))
        assertTrue(expedition("e1").status != "IN_PROGRESS")
        assertTrue(userRepo.getCoins().first() >= coinsBefore)
        assertEquals(1, storyRepo.getAllReports().first().size)
    }

    @Test
    fun complete_cancelledMidway_rollsBackEverything_thenRetrySucceeds() = runBlocking {
        buyShip(); hire("a1")
        val e = seedFinishedExpedition("e1", listOf("a1"))
        val coinsBefore = userRepo.getCoins().first()

        // 선점(완료 표시) 이후 자원 지급에서 멈춰 두고 취소 — 앱이 이 순간 꺼진 것과 같다
        val entered = CompletableDeferred<Unit>()
        val never = CompletableDeferred<Unit>()
        val stalling = object : ResourceRepository by resourceRepo {
            override suspend fun add(type: ResourceType, amount: Long) {
                entered.complete(Unit); never.await()
            }
        }
        val job = launch { completeUseCase(resources = stalling)(e) }
        entered.await()
        job.cancelAndJoin()

        // 반쪽 상태가 남지 않는다: 탐사는 진행 중, 대원은 탐사 중, 보상·보고서 없음
        assertEquals("IN_PROGRESS", expedition("e1").status)
        assertEquals(AstronautStatus.DEPLOYED, status("a1"))
        assertEquals(coinsBefore, userRepo.getCoins().first())
        assertTrue(storyRepo.getAllReports().first().isEmpty())
        // 복구 쿼리도 진행 중인 탐사의 대원은 건드리지 않는다
        assertEquals(0, astronautRepo.releaseOrphanedDeployed())

        // 다음 폴링에서 다시 처리하면 정상 완료
        completeUseCase()(e)
        assertEquals(AstronautStatus.IDLE, status("a1"))
        assertTrue(expedition("e1").status != "IN_PROGRESS")
        assertEquals(1, storyRepo.getAllReports().first().size)
    }

    @Test
    fun complete_concurrentCalls_onlyOneClaims() = runBlocking {
        buyShip(); hire("a1")
        val e = seedFinishedExpedition("e1", listOf("a1"))
        val useCase = completeUseCase()
        val coinsBefore = userRepo.getCoins().first()

        // 포그라운드 폴링 + 백그라운드 워커가 같은 탐사를 동시에 처리하는 상황
        listOf(async { useCase(e) }, async { useCase(e) }).awaitAll()

        assertEquals(1, storyRepo.getAllReports().first().size)
        assertEquals(AstronautStatus.IDLE, status("a1"))
        val coinsEarned = expedition("e1").coinsEarned
        assertEquals(coinsBefore + coinsEarned, userRepo.getCoins().first())
    }

    @Test
    fun complete_reportFails_stillReleasesCrew() = runBlocking {
        buyShip(); hire("a1")
        val e = seedFinishedExpedition("e1", listOf("a1"))
        val broken = object : StoryRepository by storyRepo {
            override suspend fun saveReport(report: ExpeditionReport) = throw IllegalStateException("boom")
        }

        completeUseCase(story = broken)(e)

        assertEquals(AstronautStatus.IDLE, status("a1"))
        assertTrue(expedition("e1").status != "IN_PROGRESS")
    }

    @Test
    fun start_cancelledMidway_leavesNoExpedition() = runBlocking {
        buyShip(); hire("a1"); hire("a2")
        val entered = CompletableDeferred<Unit>()
        val never = CompletableDeferred<Unit>()
        // 탐사 행을 넣은 뒤 두 번째 대원 DEPLOYED 표시에서 멈춰 두고 취소
        var calls = 0
        val stalling = object : AstronautRepository by astronautRepo {
            override suspend fun updateStatus(id: String, status: AstronautStatus, trainingEndTime: Long?, trainingType: TrainingType?) {
                if (++calls == 2) { entered.complete(Unit); never.await() }
                astronautRepo.updateStatus(id, status, trainingEndTime, trainingType)
            }
        }
        val job = launch { startUseCase(stalling)(ExpeditionCategory.MINERAL, 1, listOf("a1", "a2"), "ship") }
        entered.await()
        job.cancelAndJoin()

        assertTrue(expeditionRepo.getActive().first().isEmpty())
        assertEquals(AstronautStatus.IDLE, status("a1"))
        assertEquals(AstronautStatus.IDLE, status("a2"))
    }

    @Test
    fun start_normal_deploysCrew() = runBlocking {
        buyShip(); hire("a1")
        val result = startUseCase()(ExpeditionCategory.MINERAL, 1, listOf("a1"), "ship")
        assertTrue(result is StartExpeditionUseCase.Result.Success)
        assertEquals(AstronautStatus.DEPLOYED, status("a1"))
        assertEquals(1, expeditionRepo.getActive().first().size)
        // 정상 파견 대원은 복구 쿼리가 풀지 않는다
        assertEquals(0, astronautRepo.releaseOrphanedDeployed())
        assertEquals(AstronautStatus.DEPLOYED, status("a1"))
    }

    @Test
    fun releaseOrphanedDeployed_onlyFreesTrulyStuckCrew() = runBlocking {
        val dao = db.astronautDao()
        fun a(id: String, s: AstronautStatus) = AstronautEntity(
            id = id, name = id, specialty = "MINERAL", grade = "INTERN", proficiency = 20, status = s.name,
            trainingEndTime = null, trainingType = null, hiredAt = 0L
        )
        // a1: 묶임(어디에도 없음) / a10: 진행 중 탐사 가운데 칸 / a2: 끝난 탐사에만 있음(묶임)
        // a3: 진행 중 탐사 첫 칸 / t1: 훈련 중(건드리면 안 됨) / i1: 대기
        listOf(
            a("a1", AstronautStatus.DEPLOYED), a("a10", AstronautStatus.DEPLOYED), a("a2", AstronautStatus.DEPLOYED),
            a("a3", AstronautStatus.DEPLOYED), a("t1", AstronautStatus.TRAINING), a("i1", AstronautStatus.IDLE)
        ).forEach { dao.insert(it) }
        val now = System.currentTimeMillis()
        db.expeditionDao().insert(ExpeditionEntity(id = "run", category = "MINERAL", tier = 1, astronautIds = "a3,a10,zz",
            spaceshipId = "s", startTime = now, endTime = now + 60_000, status = "IN_PROGRESS", resourcesResult = null, discoveredPlanetType = null))
        db.expeditionDao().insert(ExpeditionEntity(id = "done", category = "MINERAL", tier = 1, astronautIds = "a2,a1",
            spaceshipId = "s", startTime = now, endTime = now, status = "COMPLETED", resourcesResult = null, discoveredPlanetType = null))

        assertEquals(2, astronautRepo.releaseOrphanedDeployed())

        assertEquals(AstronautStatus.IDLE, status("a1"))
        assertEquals(AstronautStatus.IDLE, status("a2"))
        assertEquals(AstronautStatus.DEPLOYED, status("a10"))
        assertEquals(AstronautStatus.DEPLOYED, status("a3"))
        assertEquals(AstronautStatus.TRAINING, status("t1"))
        assertEquals(AstronautStatus.IDLE, status("i1"))
    }

    // ── 발견 행성 구매·코인으로 받기 ─────────────────────────────────────

    private val planetRepo by lazy { PlanetRepositoryImpl(db.planetDao()) }

    private fun buyUseCase() = BuyPlanetUseCase(planetRepo, userRepo, labRepo, expeditionRepo, RoomTransactionRunner(db))
    private fun claimUseCase() = ClaimSlotFullCoinsUseCase(userRepo, expeditionRepo, RoomTransactionRunner(db))

    // 결과 창이 아직 처리되지 않은(완료됐지만 결과를 안 본) 탐사
    private suspend fun seedUnhandledResult(id: String) {
        val now = System.currentTimeMillis()
        db.expeditionDao().insert(ExpeditionEntity(id = id, category = "MINERAL", tier = 1, astronautIds = "",
            spaceshipId = "s", startTime = now, endTime = now, status = "COMPLETED", resourcesResult = null,
            discoveredPlanetType = "TERRAN_WET", resultHandled = false))
    }

    private fun discovered(price: Int) =
        GeneratePlanetsUseCase().discoveredPlanet(PlanetType.TERRAN_WET, 1, kotlin.random.Random(1)).copy(buyPrice = price)

    @Test
    fun buy_marksResultHandled_andSecondBuyIsRejected() = runBlocking {
        seedUnhandledResult("e1")
        val planet = discovered(1_000)
        val coinsBefore = userRepo.getCoins().first()

        assertEquals(BuyPlanetUseCase.Result.BOUGHT, buyUseCase()(planet, "e1"))
        assertTrue(expeditionRepo.getUnhandledResults().first().isEmpty())
        // 결과 창이 다시 떠서 또 눌러도 코인은 한 번만 빠지고 행성도 하나
        assertEquals(BuyPlanetUseCase.Result.ALREADY_HANDLED, buyUseCase()(planet, "e1"))
        assertEquals(coinsBefore - 1_000, userRepo.getCoins().first())
        assertEquals(1, planetRepo.getOwnedPlanets().first().size)
    }

    @Test
    fun buy_notEnoughCoins_leavesResultOpen() = runBlocking {
        seedUnhandledResult("e1")
        assertEquals(BuyPlanetUseCase.Result.FAILED, buyUseCase()(discovered(Int.MAX_VALUE), "e1"))
        assertEquals(1, expeditionRepo.getUnhandledResults().first().size)
        assertTrue(planetRepo.getOwnedPlanets().first().isEmpty())
    }

    @Test
    fun claimCoins_paysOnce() = runBlocking {
        seedUnhandledResult("e1")
        val coinsBefore = userRepo.getCoins().first()
        assertTrue(claimUseCase()(discovered(1_000), 500L, "e1"))
        assertTrue(!claimUseCase()(discovered(1_000), 500L, "e1"))
        assertEquals(coinsBefore + 500L, userRepo.getCoins().first())
    }
}
