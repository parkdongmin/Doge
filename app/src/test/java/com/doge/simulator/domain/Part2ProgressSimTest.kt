package com.doge.simulator.domain

import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionCategory
import com.doge.simulator.domain.model.ExpeditionReport
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.model.PlanetTrait
import com.doge.simulator.domain.model.PlanetType
import com.doge.simulator.domain.model.StoryContent
import com.doge.simulator.domain.model.StoryEvent
import com.doge.simulator.domain.model.StoryProgress
import com.doge.simulator.domain.model.TierProgress
import com.doge.simulator.domain.model.TraitType
import com.doge.simulator.domain.model.effectiveBadChance
import com.doge.simulator.domain.model.hasTraits
import com.doge.simulator.domain.model.highFitGrade
import com.doge.simulator.domain.model.isBroken
import com.doge.simulator.domain.repository.StoryRepository
import com.doge.simulator.domain.usecase.GenerateExpeditionReportUseCase
import com.doge.simulator.domain.usecase.GeneratePlanetsUseCase
import com.doge.simulator.domain.usecase.RollPlanetEventUseCase.Companion.applyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

// 2부 진행 속도·반복·고장 빈도 시뮬레이션 — 모델은 specs/004-part2-planet-traits/sim/progress_sim.js(플레이어 유형·
// 우주선 수·속도·발견률·공백 시간)와 verify.js 2절(고장 빈도)을 옮겼고, 특성 굴리기·티어 판정·스토리 기록·이벤트는
// 실제 앱 코드를 그대로 부른다. 수치표와 코드가 어긋나면 여기서 드러난다(balance.md 9절)
class Part2ProgressSimTest {

    private class Profile(val ships: Int, val speed: Double, val success: Double, val discovery: Double, val lagHours: Double)

    // balance.md 9절: 보통 유저 — 우주선 4척, 속도 35%, 성공률 90%, 발견률 40%, 공백 2시간
    private val normal = Profile(ships = 4, speed = 0.35, success = 0.90, discovery = 0.40, lagHours = 2.0)

    private class FakeStoryRepository : StoryRepository {
        private val progress = MutableStateFlow(StoryProgress())
        val reports = mutableListOf<ExpeditionReport>()
        override fun getProgress() = progress
        override suspend fun getProgressOnce() = progress.value
        override suspend fun saveProgress(progress: StoryProgress) { this.progress.value = progress }
        override fun getAllReports() = throw NotImplementedError()
        override fun getUnreadReports() = throw NotImplementedError()
        override fun getUnreadCount() = throw NotImplementedError()
        override fun getReadCount() = throw NotImplementedError()
        override suspend fun countRegularRecordsInChapter(chapter: Int) =
            reports.count { it.chapter == chapter && !it.isChapterEnding && !it.isStoryEnding }
        override suspend fun saveReport(report: ExpeditionReport) { reports.add(report) }
        override suspend fun markReportAsRead(expeditionId: String) {}
        override suspend fun claimEventChoice(eventId: String, expeditionId: String, choiceIndex: Int) = true
        override suspend fun setEventOutcomeNote(eventId: String, expeditionId: String, outcomeNote: String) {}
        override suspend fun addEvent(event: StoryEvent) {}
        override suspend fun markPart2IntroShown() {}
    }

    private class Run(val completionDay: Double?, val stalledChapters: Set<Int>, val maxRepeatsInAChapter: Int)

    // 1부를 막 완주한 플레이어(T1~T11 열림) 한 명이 2부를 끝낼 때까지 — 우주선마다 열린 가장 높은 티어로 계속 보낸다
    private fun play(profile: Profile, random: Random, maxDays: Double = 150.0): Run = runBlocking {
        val generator = GeneratePlanetsUseCase()
        val repo = FakeStoryRepository()
        repo.saveProgress(
            StoryProgress(
                currentChapter = 5, storyCompleted = true, firstRuinsCompleted = true, firstAlienCivCompleted = true,
                firstT3Completed = true, firstT5Completed = true, firstT10Completed = true
            )
        )
        val reportUseCase = GenerateExpeditionReportUseCase(repo)
        val allPart1 = PlanetMetaDataTable.variantRarity.keys
        var tierProgress = TierProgress(discoveredVariantIds = allPart1, part1Completed = true)
        fun openTier() = (11..GameConstants.MAX_TIER).last { tierProgress.isUnlocked(it) }
        fun hours(tier: Int) = GameConstants.EXPEDITION_BASE_MINUTES.getValue(tier) / 60.0 * (1 - profile.speed)

        data class Ship(var readyAt: Double, var tier: Int)
        val ships = List(profile.ships) { Ship(random.nextDouble() * 2 + hours(11), 11) }

        while (true) {
            val ship = ships.minBy { it.readyAt }
            val now = ship.readyAt
            if (now / 24 > maxDays) return@runBlocking Run(null, emptySet(), 0)
            val success = random.nextDouble() < profile.success
            if (success && random.nextDouble() < profile.discovery) {
                // 실제 발견 경로: 티어 희귀도로 종류 → 특성·가격까지 확정 → 획득(발견 = 획득으로 가정, balance.md 2절)
                val planet = generator.discoveredPlanet(generator.rollPlanetType(ship.tier, random), ship.tier, random)
                if (planet.hasTraits) {
                    val grade = planet.highFitGrade
                    tierProgress = tierProgress.copy(
                        traitPlanetsAcquired = tierProgress.traitPlanetsAcquired + 1,
                        highFitTier2Acquired = tierProgress.highFitTier2Acquired + if (grade >= 2) 1 else 0,
                        highFitTier3Acquired = tierProgress.highFitTier3Acquired + if (grade >= 3) 1 else 0
                    )
                }
            }
            val expedition = Expedition(
                category = ExpeditionCategory.MINERAL, tier = ship.tier, astronautIds = emptyList(),
                spaceshipId = "s", startTime = 0L, endTime = 0L
            )
            val report = reportUseCase(expedition, success)
            repo.saveReport(report)
            if (report.isStoryEnding && report.chapter == StoryProgress.LAST_CHAPTER) break
            ship.tier = openTier()
            ship.readyAt = now + hours(ship.tier) + random.nextDouble() * profile.lagHours * 2
        }

        // 장마다 이야기 풀 밖(막힘·일상) 문장이 몇 번씩 나왔는지
        val stalled = mutableSetOf<Int>()
        var maxRepeats = 0
        for (chapter in StoryProgress.PART2_FIRST_CHAPTER..StoryProgress.LAST_CHAPTER) {
            val pool = StoryContent.recordTitlePool.getValue(chapter).toSet()
            val extra = repo.reports.filter { it.chapter == chapter && !it.isChapterEnding && !it.isStoryEnding && it.recordTitle !in pool }
            if (extra.isNotEmpty()) stalled += chapter
            maxRepeats = maxOf(maxRepeats, extra.groupingBy { it.recordTitle }.eachCount().values.maxOrNull() ?: 0)
        }
        Run(ships.maxOf { it.readyAt } / 24, stalled, maxRepeats)
    }

    private fun median(values: List<Double>) = values.sorted()[values.size / 2]

    @Test
    fun `normal player finishes part 2 in about two weeks and waits in chapters 9 and 10`() {
        val random = Random(2026)
        val runs = List(300) { play(normal, random) }
        val days = runs.mapNotNull { it.completionDay }
        assertTrue("150일 안에 모두 완주", days.size == runs.size)
        val median = median(days)
        println("보통 유저 2부 완주 중앙값 %.1f일".format(median))
        // SC-005: 중앙값 11~17일
        assertTrue("중앙값 $median", median in 11.0..17.0)
        val ch9 = runs.count { 9 in it.stalledChapters } / runs.size.toDouble()
        val ch10 = runs.count { 10 in it.stalledChapters } / runs.size.toDouble()
        println("조건 대기 발생 9장 %.0f%% · 10장 %.0f%%".format(ch9 * 100, ch10 * 100))
        assertTrue("9장 대기 $ch9", ch9 in 0.4..0.8)
        assertTrue("10장 대기 $ch10", ch10 >= 0.85)
    }

    // SC-006 — 보통 유저 기준 장 하나에서 같은 막힘·일상 문장이 2회 넘게 반복되지 않는다
    @Test
    fun `waiting lines rarely repeat within a chapter for a normal player`() {
        val random = Random(77)
        val repeats = List(300) { play(normal, random).maxRepeatsInAChapter }
        val typical = median(repeats.map { it.toDouble() })
        println("장 하나에서 같은 문장 최대 반복 — 중앙값 $typical, 분포 ${repeats.groupingBy { it }.eachCount().toSortedMap()}")
        assertTrue("보통 유저 반복 중앙값 $typical", typical <= 2.0)
    }

    // SC-002 — 튼튼한 구조 Ⅲ·안정된 환경 Ⅲ이 붙어도 고장 빈도는 특성 없는 행성의 56% 이상(정비 자원 소비 유지).
    // verify.js 2절과 같은 조건: Lv10, 이벤트 간격은 risk 기준, 고장 나면 0.5~8시간 뒤 정비, 90일
    @Test
    fun `grade III sturdy or stable traits keep at least about half of the breakdowns`() {
        for (type in PlanetType.entries) {
            val meta = PlanetMetaDataTable.data.getValue(type)
            val plain = Planet(
                type = type, production = meta.productionMin, risk = (meta.riskMin + meta.riskMax) / 2,
                investment = 0, eventRate = (meta.eventRateMin + meta.eventRateMax) / 2, buyPrice = meta.basePrice, level = 10
            )
            val base = breakdownsPerDay(plain, Random(9))
            for (trait in listOf(TraitType.STURDY_STRUCTURE, TraitType.STABLE_ENVIRONMENT)) {
                val ratio = breakdownsPerDay(plain.copy(traits = listOf(PlanetTrait(trait, 3))), Random(9)) / base
                // 최저(항성 튼튼한 구조)가 시뮬레이션에서 56%로 경계값이라 난수 차이를 감안해 54%
                assertTrue("$type $trait ${"%.0f".format(ratio * 100)}%", ratio >= 0.54)
            }
        }
    }

    private fun breakdownsPerDay(planet: Planet, random: Random, days: Int = 90, runs: Int = 300): Double {
        val interval = GameConstants.planetEventIntervalHours(planet.risk)
        val end = days * 24.0
        var breaks = 0
        repeat(runs) {
            var p = planet
            var t = 0.0
            var repairAt: Double? = null
            while (true) {
                val next = t + interval
                val repair = repairAt
                if (repair != null && repair < next) {
                    p = p.copy(lossMultiplier = 0.0); t = repair; repairAt = null; continue
                }
                t = next
                if (t >= end) break
                val isBad = random.nextInt(100) < p.effectiveBadChance
                val delta = random.nextDouble(GameConstants.PLANET_EVENT_DELTA_MIN, GameConstants.PLANET_EVENT_DELTA_MAX)
                val wasBroken = p.isBroken
                p = applyEvent(p, isBad, delta)
                if (!wasBroken && p.isBroken) {
                    breaks++
                    repairAt = t + random.nextDouble(0.5, 8.0)
                }
            }
        }
        return breaks.toDouble() / runs / days
    }
}
