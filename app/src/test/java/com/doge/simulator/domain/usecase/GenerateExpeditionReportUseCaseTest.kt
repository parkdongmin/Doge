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
    fun `T10을 달성하면 챕터5는 그대로 유지되고 완주 기록이 딱 한 번 뜬다`() = runBlocking {
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

        // 이야기를 다 보면 다음 기록이 완주. 챕터는 6으로 안 넘어가고 5 그대로
        finishChapterStory()
        val endingReport = record(ExpeditionCategory.MINERAL, tier = 1)
        assertEquals(5, endingReport.chapter)
        assertFalse("챕터가 6으로 넘어가면 안 된다(콘텐츠 없음)", endingReport.isChapterEnding)
        assertTrue(endingReport.isStoryEnding)
        assertTrue(repo.getProgressOnce().storyCompleted)

        // 그 뒤로 T10을 또 완료해도 완주 기록은 다시 뜨지 않는다(한 번만)
        val afterEnding = record(ExpeditionCategory.MINERAL, tier = 10)
        assertEquals(5, afterEnding.chapter)
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
}
