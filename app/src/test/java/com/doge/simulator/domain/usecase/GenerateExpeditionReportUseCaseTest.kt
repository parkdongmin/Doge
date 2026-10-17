package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionCategory
import com.doge.simulator.domain.model.ExpeditionReport
import com.doge.simulator.domain.model.StoryContent
import com.doge.simulator.domain.model.StoryEvent
import com.doge.simulator.domain.model.StoryProgress
import com.doge.simulator.domain.repository.StoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 챕터가 마일스톤 달성 "순서"와 무관하게 항상 1→2→3→4→5로만 전진하는지 검증한다.
 * (2026-09-04 수정: T3/T5를 유적/외계문명보다 먼저 달성해도 챕터3·4를 건너뛰지 않게 고침)
 * (2026-10-12 수정: 챕터는 마일스톤 + 그 챕터 기록 풀을 끝까지 봐야 넘어간다)
 */
class GenerateExpeditionReportUseCaseTest {

    private class FakeStoryRepository : StoryRepository {
        private val progressFlow = MutableStateFlow(StoryProgress())
        private val reports = mutableListOf<ExpeditionReport>()

        override fun getProgress() = progressFlow
        override suspend fun getProgressOnce(): StoryProgress = progressFlow.value
        override suspend fun saveProgress(progress: StoryProgress) { progressFlow.value = progress }

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
        override suspend fun markPart2IntroShown() { progressFlow.value = progressFlow.value.copy(part2IntroShown = true) }
    }

    private val repo = FakeStoryRepository()
    private val useCase = GenerateExpeditionReportUseCase(repo)

    private fun expedition(category: ExpeditionCategory, tier: Int) = Expedition(
        category = category,
        tier = tier,
        astronautIds = emptyList(),
        spaceshipId = "ship",
        startTime = 0L,
        endTime = 0L
    )

    // 실제 워커처럼 생성한 기록을 저장해야 챕터 안 기록 수가 쌓인다
    private suspend fun record(category: ExpeditionCategory, tier: Int, isSuccess: Boolean = true) =
        useCase(expedition(category, tier), isSuccess).also { repo.saveReport(it) }

    // 현재 챕터의 기록 풀을 끝까지 소진 (마일스톤과 무관한 T1 광물 탐사로)
    private suspend fun finishChapterStory() {
        val chapter = repo.getProgressOnce().currentChapter
        val left = StoryContent.recordTitlePool.getValue(chapter).size - repo.countRegularRecordsInChapter(chapter)
        repeat(left) { record(ExpeditionCategory.MINERAL, 1) }
    }

    @Test
    fun `T5를 먼저 달성해도 조우 챕터를 건너뛰지 않는다`() = runBlocking {
        // 유적·외계문명은 한 번도 안 하고 T5부터 찍는다
        val t5Report = record(ExpeditionCategory.MINERAL, tier = 5)
        assertEquals(1, t5Report.chapter)
        assertFalse("T5만으로 챕터가 전진하면 안 된다", t5Report.isChapterEnding)

        // 유적을 마침내 완료 → 챕터1→2 (건너뛰지 않고 정확히 한 칸)
        finishChapterStory()
        val ruinsReport = record(ExpeditionCategory.RUINS, tier = 1)
        assertEquals(2, ruinsReport.chapter)
        assertTrue(ruinsReport.isChapterEnding)

        // 외계문명을 완료 → 챕터2→3 ("조우"), 5로 바로 안 뜀
        finishChapterStory()
        val alienReport = record(ExpeditionCategory.ALIEN_CIVILIZATION, tier = 1)
        assertEquals(3, alienReport.chapter)
        assertTrue(alienReport.isChapterEnding)

        // T3는 이전에 이미 달성해둔 상태(T5>=3이라 t3 마일스톤도 같이 충족됨) → 이야기를 다 보면 챕터3→4로 캐치업
        finishChapterStory()
        val catchUp1 = record(ExpeditionCategory.PLANET, tier = 1)
        assertEquals(4, catchUp1.chapter)
        assertTrue(catchUp1.isChapterEnding)

        // T5도 이미 밴킹돼 있었으므로 이야기를 다 보면 챕터4→5로 캐치업
        finishChapterStory()
        val catchUp2 = record(ExpeditionCategory.PLANET, tier = 1)
        assertEquals(5, catchUp2.chapter)
        assertTrue(catchUp2.isChapterEnding)
    }

    @Test
    fun `조건을 일찍 채워도 챕터 이야기를 끝까지 봐야 넘어간다`() = runBlocking {
        val pool = StoryContent.recordTitlePool.getValue(1)

        // 첫 기록부터 유적 성공 — 마일스톤은 쌓이지만 챕터는 그대로, 이야기 첫 문장이 나온다
        val early = record(ExpeditionCategory.RUINS, 1)
        assertEquals(1, early.chapter)
        assertFalse(early.isChapterEnding)
        assertEquals(pool.first(), early.recordTitle)
        assertTrue(repo.getProgressOnce().isCurrentChapterExitMet)

        // 남은 이야기도 순서대로 전부 나온다(엔딩 복선 "화성 궤도"도 건너뛰지 않음)
        val rest = (2..pool.size).map { record(ExpeditionCategory.MINERAL, 1) }
        assertTrue(rest.none { it.isChapterEnding })
        assertEquals(pool.drop(1), rest.map { it.recordTitle })

        // 이야기를 다 본 다음 기록에서 바로 넘어간다(정체 문장 없이)
        val next = record(ExpeditionCategory.MINERAL, 1)
        assertTrue(next.isChapterEnding)
        assertEquals(2, next.chapter)
    }

    @Test
    fun `정상 순서로 밟으면 매번 한 챕터씩 전진한다`() = runBlocking {
        finishChapterStory()
        assertEquals(2, record(ExpeditionCategory.RUINS, 1).chapter)
        finishChapterStory()
        assertEquals(3, record(ExpeditionCategory.ALIEN_CIVILIZATION, 1).chapter)
        finishChapterStory()
        assertEquals(4, record(ExpeditionCategory.MINERAL, 3).chapter)
        finishChapterStory()
        assertEquals(5, record(ExpeditionCategory.MINERAL, 5).chapter)
        // 챕터5 도달 후엔 더 전진하지 않는다
        val afterEnd = record(ExpeditionCategory.MINERAL, 5)
        assertEquals(5, afterEnd.chapter)
        assertFalse(afterEnd.isChapterEnding)
    }

    @Test
    fun `실패한 탐사도 이미 밴킹된 마일스톤으로 캐치업 챕터전진이 가능하다`() = runBlocking {
        finishChapterStory()
        record(ExpeditionCategory.RUINS, 1)
        finishChapterStory()
        record(ExpeditionCategory.ALIEN_CIVILIZATION, 1)
        assertEquals(3, repo.getProgressOnce().currentChapter)

        // 실패한 탐사라도 기록 자체는 생성되며 챕터는 유지된다(전진 없음)
        val failedReport = record(ExpeditionCategory.MINERAL, 1, isSuccess = false)
        assertEquals(3, failedReport.chapter)
        assertFalse(failedReport.isChapterEnding)
    }

    @Test
    fun `T10을 달성하면 1부 완주 기록이 딱 한 번 뜨고 다음 기록부터 6장`() = runBlocking {
        // 챕터5까지 정상 순서로 도달
        finishChapterStory(); record(ExpeditionCategory.RUINS, 1)
        finishChapterStory(); record(ExpeditionCategory.ALIEN_CIVILIZATION, 1)
        finishChapterStory(); record(ExpeditionCategory.MINERAL, 3)
        finishChapterStory(); record(ExpeditionCategory.MINERAL, 5)
        assertEquals(5, repo.getProgressOnce().currentChapter)
        assertFalse(repo.getProgressOnce().storyCompleted)

        // 챕터5 이야기를 다 보기 전에 T10을 성공해도 엔딩은 아직 — 반전 복선을 먼저 봐야 한다
        val tooEarly = record(ExpeditionCategory.MINERAL, tier = 10)
        assertFalse(tooEarly.isStoryEnding)

        // 이야기를 다 보면 다음 기록이 1부 완주
        finishChapterStory()
        val endingReport = record(ExpeditionCategory.MINERAL, tier = 1)
        assertEquals("1부 완주 기록은 5장으로 남는다", 5, endingReport.chapter)
        assertFalse(endingReport.isChapterEnding)
        assertTrue(endingReport.isStoryEnding)
        assertTrue(repo.getProgressOnce().storyCompleted)

        // 그 뒤 기록은 2부 6장 1번 — T10을 또 완료해도 1부 완주 기록은 다시 뜨지 않는다(한 번만)
        val afterEnding = record(ExpeditionCategory.MINERAL, tier = 10)
        assertEquals(6, afterEnding.chapter)
        assertEquals(StoryContent.recordTitlePool.getValue(6).first(), afterEnding.recordTitle)
        assertFalse(afterEnding.isStoryEnding)
        assertFalse(afterEnding.isChapterEnding)
    }

    @Test
    fun `챕터 풀을 다 쓰고도 조건을 못 채우면 같은 이야기 대신 정체 문장이 나온다`() = runBlocking {
        val pool = StoryContent.recordTitlePool.getValue(1)
        val titles = (1..pool.size).map { record(ExpeditionCategory.MINERAL, 1).recordTitle }
        assertEquals("챕터 풀은 처음부터 순서대로 한 번씩", pool, titles)

        val stalled = record(ExpeditionCategory.MINERAL, 1).recordTitle
        assertEquals(StoryContent.stalledRecordTitlePool.getValue(1).first(), stalled)

        // 조건을 채우면 바로 다음 챕터로 넘어가고, 새 챕터 풀은 처음부터 시작한다
        assertTrue(record(ExpeditionCategory.RUINS, 1).isChapterEnding)
        assertEquals(StoryContent.recordTitlePool.getValue(2).first(), record(ExpeditionCategory.MINERAL, 1).recordTitle)
    }

    // ── 2부 (6~10장) ──────────────────────────────────────────────────

    // 1부를 완주하고 5장에 머문 상태(업데이트 직후의 기존 완주 플레이어와 같음)
    private suspend fun startPart2() {
        repo.saveProgress(
            StoryProgress(
                currentChapter = 5, storyCompleted = true,
                firstRuinsCompleted = true, firstAlienCivCompleted = true,
                firstT3Completed = true, firstT5Completed = true, firstT10Completed = true
            )
        )
    }

    @Test
    fun `1부 완주 상태의 다음 기록은 6장 1번이고 6장은 이야기만 보면 넘어간다`() = runBlocking {
        startPart2()
        val first = record(ExpeditionCategory.MINERAL, 1)
        assertEquals(6, first.chapter)
        assertEquals(StoryContent.recordTitlePool.getValue(6).first(), first.recordTitle)

        finishChapterStory()
        val ending = record(ExpeditionCategory.MINERAL, 1)
        assertTrue(ending.isChapterEnding)
        assertEquals(StoryContent.chapterEndingTitles.getValue(6), ending.recordTitle)
        assertEquals(7, repo.getProgressOnce().currentChapter)
    }

    @Test
    fun `조건을 못 채운 동안은 막힘 1 일상 2로 섞이고 채우면 장 마무리가 나온다`() = runBlocking {
        startPart2()
        repo.saveProgress(repo.getProgressOnce().copy(currentChapter = 7, storyCompleted = true))
        finishChapterStory()

        val stalledPool = StoryContent.stalledRecordTitlePool.getValue(7)
        val waiting = (0 until 9).map { record(ExpeditionCategory.MINERAL, 10).recordTitle }
        waiting.forEachIndexed { k, title ->
            if (k % 3 == 0) assertEquals("k=$k", stalledPool[(k / 3) % stalledPool.size], title)
            else assertTrue("k=$k 일상 기록이어야 함: $title", title !in stalledPool)
        }
        assertEquals(6, repo.getProgressOnce().settlementLogCursor)

        val ending = record(ExpeditionCategory.MINERAL, 11)
        assertTrue(ending.isChapterEnding)
        assertEquals(8, repo.getProgressOnce().currentChapter)
    }

    @Test
    fun `조건을 몰아서 채워도 한 기록에 한 장씩만 넘어간다`() = runBlocking {
        startPart2()
        repo.saveProgress(repo.getProgressOnce().copy(currentChapter = 7, storyCompleted = true))
        record(ExpeditionCategory.MINERAL, 15) // T11·T12·T13·T15 조건을 한 번에
        finishChapterStory()
        assertTrue(record(ExpeditionCategory.MINERAL, 1).isChapterEnding)
        assertEquals(8, repo.getProgressOnce().currentChapter)
        assertEquals("8장 이야기를 건너뛰지 않는다", StoryContent.recordTitlePool.getValue(8).first(),
            record(ExpeditionCategory.MINERAL, 1).recordTitle)
    }

    @Test
    fun `10장 이야기를 다 보고 T15에 성공하면 2부 완주가 한 번 나오고 이후엔 일상 기록만`() = runBlocking {
        startPart2()
        repo.saveProgress(repo.getProgressOnce().copy(currentChapter = 10, storyCompleted = true))
        finishChapterStory()
        val stalled = record(ExpeditionCategory.MINERAL, 14)
        assertEquals(StoryContent.stalledRecordTitlePool.getValue(10).first(), stalled.recordTitle)

        val ending = record(ExpeditionCategory.MINERAL, 15)
        assertTrue(ending.isStoryEnding)
        assertEquals(10, ending.chapter)
        assertEquals(StoryContent.part2EndingTitle, ending.recordTitle)
        assertTrue(repo.getProgressOnce().part2Completed)

        val stalledPool = StoryContent.stalledRecordTitlePool.getValue(10)
        repeat(6) {
            val after = record(ExpeditionCategory.MINERAL, 15)
            assertFalse(after.isStoryEnding)
            assertTrue("2부 완주 뒤엔 막힘 문장이 안 나온다", after.recordTitle !in stalledPool)
        }
    }

    @Test
    fun `정착지 일상 기록은 한 바퀴 안에서 겹치지 않고 바퀴 경계에서도 연달아 같지 않다`() {
        val pool = com.doge.simulator.domain.model.Part2StoryContent.settlementLogPool.size
        val logs = (0 until pool * 10).map { StoryContent.settlementLog(it) }
        logs.chunked(pool).forEach { cycle -> assertEquals(pool, cycle.toSet().size) }
        logs.zipWithNext().forEachIndexed { i, (a, b) -> assertTrue("$i: $a", a != b) }
    }

    @Test
    fun `2부에서는 1부 5장 전용 이벤트가 나오지 않는다`() = runBlocking {
        startPart2()
        val chapter5Titles = StoryContent.chapter5EventPool.map { it.title }.toSet()
        val part2Titles = com.doge.simulator.domain.model.Part2StoryContent.eventPool.map { it.title }.toSet()
        var sawPart2Event = false
        repeat(200) {
            // 9시간짜리 탐사 — 이벤트 확률 60%, 최대 2개
            val report = useCase(expedition(ExpeditionCategory.MINERAL, 11).copy(endTime = 9 * 3_600_000L), true)
            report.events.filterNot { it.isDeparture }.forEach { event ->
                assertTrue(event.title, event.title !in chapter5Titles)
                if (event.title in part2Titles) sawPart2Event = true
            }
        }
        assertTrue(sawPart2Event)
    }
}
