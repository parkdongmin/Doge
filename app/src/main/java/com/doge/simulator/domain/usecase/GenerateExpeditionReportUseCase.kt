package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionCategory
import com.doge.simulator.domain.model.ExpeditionReport
import com.doge.simulator.domain.model.StoryContent
import com.doge.simulator.domain.model.StoryEvent
import com.doge.simulator.domain.model.StoryProgress
import com.doge.simulator.domain.repository.StoryRepository
import javax.inject.Inject
import kotlin.random.Random

class GenerateExpeditionReportUseCase @Inject constructor(
    private val storyRepository: StoryRepository
) {
    // 탐사 시간별 이벤트 발생 확률
    private fun eventChance(durationMs: Long): Float = when {
        durationMs < 2 * 60 * 60 * 1000L -> 0f      // 2시간 미만: 없음
        durationMs < 4 * 60 * 60 * 1000L -> 0.20f   // 2~4시간: 20%
        durationMs < 8 * 60 * 60 * 1000L -> 0.40f   // 4~8시간: 40%
        else -> 0.60f                                 // 8시간+: 60%
    }

    suspend operator fun invoke(expedition: Expedition, isSuccess: Boolean): ExpeditionReport {
        val progress = storyRepository.getProgressOnce()

        // ── 챕터 마일스톤 체크 ───────────────────────────────────────
        val isFirstRuins = !progress.firstRuinsCompleted &&
                expedition.category == ExpeditionCategory.RUINS && isSuccess
        val isFirstAlienCiv = !progress.firstAlienCivCompleted &&
                expedition.category == ExpeditionCategory.ALIEN_CIVILIZATION && isSuccess
        val isFirstT3 = !progress.firstT3Completed && expedition.tier >= 3 && isSuccess
        val isFirstT5 = !progress.firstT5Completed && expedition.tier >= 5 && isSuccess

        val isChapterEnding = isFirstRuins || isFirstAlienCiv || isFirstT3 || isFirstT5

        // ── 챕터 계산 ────────────────────────────────────────────────
        val newChapter = when {
            isFirstT5 || progress.firstT5Completed -> 5
            isFirstT3 || progress.firstT3Completed -> 4
            isFirstAlienCiv || progress.firstAlienCivCompleted -> 3
            isFirstRuins || progress.firstRuinsCompleted -> 2
            else -> 1
        }

        val newTotalRecords = progress.totalRecordsCompleted + 1

        // ── 기록 제목 선정 ───────────────────────────────────────────
        val recordTitle = if (isChapterEnding) {
            StoryContent.chapterEndingTitles[progress.currentChapter]
                ?: "미지의 공간으로 한 발짝 더 나아갔다"
        } else {
            val pool = StoryContent.recordTitlePool[newChapter] ?: emptyList()
            val indexWithinChapter = (newTotalRecords - 1) % pool.size.coerceAtLeast(1)
            pool.getOrNull(indexWithinChapter) ?: "탐사가 완료됐다"
        }

        // ── 이벤트 생성 ──────────────────────────────────────────────
        val durationMs = expedition.endTime - expedition.startTime
        val events = mutableListOf<StoryEvent>()

        if (isSuccess) {
            val chance = eventChance(durationMs)
            val maxEvents = if (durationMs >= 8 * 60 * 60 * 1000L) 2 else 1

            repeat(maxEvents) {
                if (Random.nextFloat() < chance) {
                    StoryContent.randomEvent(expedition.category, expedition.id)?.let { events.add(it) }
                }
            }

            // 탐사를 마치고 떠나기 전 항상 등장하는 마무리 선택
            events.add(StoryContent.randomDeparture(expedition.category, expedition.id))
        }

        // ── 진행 상태 저장 ───────────────────────────────────────────
        storyRepository.saveProgress(
            progress.copy(
                totalRecordsCompleted = newTotalRecords,
                currentChapter = newChapter,
                firstRuinsCompleted = progress.firstRuinsCompleted || isFirstRuins,
                firstAlienCivCompleted = progress.firstAlienCivCompleted || isFirstAlienCiv,
                firstT3Completed = progress.firstT3Completed || isFirstT3,
                firstT5Completed = progress.firstT5Completed || isFirstT5
            )
        )

        return ExpeditionReport(
            expeditionId = expedition.id,
            recordNumber = newTotalRecords,
            chapter = newChapter,
            chapterTitle = StoryProgress(currentChapter = newChapter).chapterTitle,
            recordTitle = recordTitle,
            isChapterEnding = isChapterEnding,
            events = events,
            isRead = false,
            completedAt = System.currentTimeMillis()
        )
    }
}
