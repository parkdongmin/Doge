package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionCategory
import com.doge.simulator.domain.model.ExpeditionReport
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
 */
class GenerateExpeditionReportUseCaseTest {

    private class FakeStoryRepository : StoryRepository {
        private val progressFlow = MutableStateFlow(StoryProgress())

        override fun getProgress() = progressFlow
        override suspend fun getProgressOnce(): StoryProgress = progressFlow.value
        override suspend fun saveProgress(progress: StoryProgress) { progressFlow.value = progress }

        override fun getAllReports() = throw NotImplementedError()
        override fun getUnreadReports() = throw NotImplementedError()
        override fun getUnreadCount() = throw NotImplementedError()
        override suspend fun saveReport(report: ExpeditionReport) {}
        override suspend fun markReportAsRead(expeditionId: String) {}
        override suspend fun claimEventChoice(eventId: String, expeditionId: String, choiceIndex: Int) = true
        override suspend fun setEventOutcomeNote(eventId: String, expeditionId: String, outcomeNote: String) {}
        override suspend fun addEvent(event: StoryEvent) {}
    }

    private fun expedition(category: ExpeditionCategory, tier: Int) = Expedition(
        category = category,
        tier = tier,
        astronautIds = emptyList(),
        spaceshipId = "ship",
        startTime = 0L,
        endTime = 0L
    )

    @Test
    fun `T5를 먼저 달성해도 조우 챕터를 건너뛰지 않는다`() = runBlocking {
        val repo = FakeStoryRepository()
        val useCase = GenerateExpeditionReportUseCase(repo)

        // 유적·외계문명은 한 번도 안 하고 T5부터 찍는다
        val t5Report = useCase(expedition(ExpeditionCategory.MINERAL, tier = 5), isSuccess = true)
        assertEquals(1, t5Report.chapter)
        assertFalse("T5만으로 챕터가 전진하면 안 된다", t5Report.isChapterEnding)

        // 유적을 마침내 완료 → 챕터1→2 (건너뛰지 않고 정확히 한 칸)
        val ruinsReport = useCase(expedition(ExpeditionCategory.RUINS, tier = 1), isSuccess = true)
        assertEquals(2, ruinsReport.chapter)
        assertTrue(ruinsReport.isChapterEnding)

        // 외계문명을 완료 → 챕터2→3 ("조우"), 5로 바로 안 뜀
        val alienReport = useCase(expedition(ExpeditionCategory.ALIEN_CIVILIZATION, tier = 1), isSuccess = true)
        assertEquals(3, alienReport.chapter)
        assertTrue(alienReport.isChapterEnding)

        // T3는 이전에 이미 달성해둔 상태(T5>=3이라 t3 마일스톤도 같이 충족됨) → 다음 기록에서 챕터3→4로 캐치업
        val catchUp1 = useCase(expedition(ExpeditionCategory.PLANET, tier = 1), isSuccess = true)
        assertEquals(4, catchUp1.chapter)
        assertTrue(catchUp1.isChapterEnding)

        // T5도 이미 밴킹돼 있었으므로 다음 기록에서 챕터4→5로 캐치업
        val catchUp2 = useCase(expedition(ExpeditionCategory.PLANET, tier = 1), isSuccess = true)
        assertEquals(5, catchUp2.chapter)
        assertTrue(catchUp2.isChapterEnding)
    }

    @Test
    fun `정상 순서로 밟으면 매번 한 챕터씩 전진한다`() = runBlocking {
        val repo = FakeStoryRepository()
        val useCase = GenerateExpeditionReportUseCase(repo)

        assertEquals(1, useCase(expedition(ExpeditionCategory.MINERAL, 1), true).chapter)
        assertEquals(2, useCase(expedition(ExpeditionCategory.RUINS, 1), true).chapter)
        assertEquals(3, useCase(expedition(ExpeditionCategory.ALIEN_CIVILIZATION, 1), true).chapter)
        assertEquals(4, useCase(expedition(ExpeditionCategory.MINERAL, 3), true).chapter)
        assertEquals(5, useCase(expedition(ExpeditionCategory.MINERAL, 5), true).chapter)
        // 챕터5 도달 후엔 더 전진하지 않는다
        val afterEnd = useCase(expedition(ExpeditionCategory.MINERAL, 5), true)
        assertEquals(5, afterEnd.chapter)
        assertFalse(afterEnd.isChapterEnding)
    }

    @Test
    fun `실패한 탐사도 이미 밴킹된 마일스톤으로 캐치업 챕터전진이 가능하다`() = runBlocking {
        val repo = FakeStoryRepository()
        val useCase = GenerateExpeditionReportUseCase(repo)

        useCase(expedition(ExpeditionCategory.RUINS, 1), isSuccess = true)
        useCase(expedition(ExpeditionCategory.ALIEN_CIVILIZATION, 1), isSuccess = true)
        // T3를 실패한 탐사로 시도 — 마일스톤은 안 쌓이지만, 이미 챕터3에 진입해 있어야 한다
        assertEquals(3, repo.getProgressOnce().currentChapter)

        // 실패한 탐사라도 기록 자체는 생성되며 챕터는 유지된다(전진 없음)
        val failedReport = useCase(expedition(ExpeditionCategory.MINERAL, 1), isSuccess = false)
        assertEquals(3, failedReport.chapter)
        assertFalse(failedReport.isChapterEnding)
    }

    @Test
    fun `T10을 달성하면 챕터5는 그대로 유지되고 완주 기록이 딱 한 번 뜬다`() = runBlocking {
        val repo = FakeStoryRepository()
        val useCase = GenerateExpeditionReportUseCase(repo)

        // 챕터5까지 정상 순서로 도달
        useCase(expedition(ExpeditionCategory.RUINS, 1), true)
        useCase(expedition(ExpeditionCategory.ALIEN_CIVILIZATION, 1), true)
        useCase(expedition(ExpeditionCategory.MINERAL, 3), true)
        useCase(expedition(ExpeditionCategory.MINERAL, 5), true)
        assertEquals(5, repo.getProgressOnce().currentChapter)
        assertFalse(repo.getProgressOnce().storyCompleted)

        // T10 달성 → 챕터는 6으로 안 넘어가고 5 그대로, 대신 완주 기록이 뜬다
        val endingReport = useCase(expedition(ExpeditionCategory.MINERAL, tier = 10), true)
        assertEquals(5, endingReport.chapter)
        assertFalse("챕터가 6으로 넘어가면 안 된다(콘텐츠 없음)", endingReport.isChapterEnding)
        assertTrue(endingReport.isStoryEnding)
        assertTrue(repo.getProgressOnce().storyCompleted)

        // 그 뒤로 T10을 또 완료해도 완주 기록은 다시 뜨지 않는다(한 번만)
        val afterEnding = useCase(expedition(ExpeditionCategory.MINERAL, tier = 10), true)
        assertEquals(5, afterEnding.chapter)
        assertFalse(afterEnding.isStoryEnding)
        assertFalse(afterEnding.isChapterEnding)
    }
}
