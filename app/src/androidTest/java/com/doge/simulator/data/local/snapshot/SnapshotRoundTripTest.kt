package com.doge.simulator.data.local.snapshot

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doge.simulator.data.local.PlanetDatabase
import com.doge.simulator.data.local.entity.AstronautEntity
import com.doge.simulator.data.local.entity.ExpeditionEntity
import com.doge.simulator.data.local.entity.ExpeditionReportEntity
import com.doge.simulator.data.local.entity.PlanetEntity
import com.doge.simulator.data.local.entity.RecruitmentCandidateEntity
import com.doge.simulator.data.local.entity.RecruitmentMetaEntity
import com.doge.simulator.data.local.entity.ResearchLabEntity
import com.doge.simulator.data.local.entity.ResourceEntity
import com.doge.simulator.data.local.entity.SpaceshipEntity
import com.doge.simulator.data.local.entity.StoryEventEntity
import com.doge.simulator.data.local.entity.StoryProgressEntity
import com.doge.simulator.data.local.entity.UserEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotRoundTripTest {

    private lateinit var db: PlanetDatabase
    private lateinit var source: LocalSnapshotDataSource

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PlanetDatabase::class.java
        ).build()
        source = LocalSnapshotDataSource(db, db.snapshotDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun export_import_export_isStable() = runBlocking {
        val dao = db.snapshotDao()
        dao.insertUser(UserEntity(id = 1, coins = 987_654L, discoveredVariantIds = "a,b,c"))
        dao.insertResearchLab(ResearchLabEntity(1, 4, 3, 2, 5))
        dao.insertStoryProgress(
            StoryProgressEntity(1, 12, 3, firstRuinsCompleted = true, firstT5Completed = true)
        )
        dao.insertRecruitmentMeta(RecruitmentMetaEntity(1, 1_700_000_000_000L))
        dao.insertPlanets(
            listOf(
                PlanetEntity(
                    id = "p1", type = "GAS_GIANT", production = 40, risk = 12, investment = 3,
                    eventRate = 45, buyPrice = 1000, acquireTime = 111L,
                    level = 7, totalProfit = 5000L, variantId = "v1", upgradeInvestment = 800L,
                    lastProfitTime = 222L, productionMultiplier = 0.7, marketAdjustment = -300L,
                    lastEventTime = 333L
                )
            )
        )
        dao.insertResources(listOf(ResourceEntity("IRON_ORE", 15L), ResourceEntity("CRYSTAL", 3L)))
        dao.insertAstronauts(
            listOf(
                AstronautEntity("a1", "닐", "PILOT", "B", 55, "TRAINING", 999L, "BASIC", 100L)
            )
        )
        dao.insertSpaceships(listOf(SpaceshipEntity("s1", "1호", 3, 5, 60, 60, 0.78f, 50L)))
        dao.insertRecruitmentCandidates(
            listOf(RecruitmentCandidateEntity(0, "c1", "후보", "SCIENTIST", "A", 30))
        )
        dao.insertExpeditions(
            listOf(
                ExpeditionEntity(
                    "e1", "PLANET", 4, "a1,a2", "s1", 10L, 20L, "IN_PROGRESS",
                    null, null, resultHandled = true, coinsEarned = 0L
                )
            )
        )
        dao.insertReports(
            listOf(ExpeditionReportEntity("e0", 1, 1, "1장", "첫 기록", false, isRead = false, completedAt = 5L))
        )
        dao.insertStoryEvents(
            listOf(
                StoryEventEntity(
                    "ev1", "e0", "제목", "설명", "선택1", "IRON_ORE", 3L, "선택2", "CRYSTAL", 2L,
                    null, null, null, selectedChoiceIndex = -1, isDeparture = true, outcomeNote = null
                )
            )
        )

        val first = source.export()
        source.import(first)
        val second = source.export()

        assertEquals(first.user, second.user)
        assertEquals(first.researchLab, second.researchLab)
        assertEquals(first.storyProgress, second.storyProgress)
        assertEquals(first.recruitmentMeta, second.recruitmentMeta)
        assertEquals(first.planets.toSet(), second.planets.toSet())
        assertEquals(first.resources.toSet(), second.resources.toSet())
        assertEquals(first.astronauts.toSet(), second.astronauts.toSet())
        assertEquals(first.spaceships.toSet(), second.spaceships.toSet())
        assertEquals(first.recruitmentCandidates.toSet(), second.recruitmentCandidates.toSet())
        assertEquals(first.expeditions.toSet(), second.expeditions.toSet())
        assertEquals(first.expeditionReports.toSet(), second.expeditionReports.toSet())
        assertEquals(first.storyEvents.toSet(), second.storyEvents.toSet())
    }
}
