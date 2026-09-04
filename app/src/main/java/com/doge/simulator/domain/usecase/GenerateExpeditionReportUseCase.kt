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
        // 마일스톤은 순서 없이 어느 때나 달성될 수 있다(예: 유적/외계문명 탐사 전에
        // 티어5부터 찍을 수도 있음). 달성 여부는 여기서 바로 반영해 progress에 저장하되,
        // 실제 챕터 전진은 아래에서 "현재 챕터를 빠져나가는 조건"이 충족될 때만 한
        // 챕터씩 순서대로만 진행한다 — 나중 마일스톤을 먼저 밟았다고 스토리 비트
        // (조우·동맹인가 적인가)를 건너뛰지 않기 위함
        val isFirstRuins = !progress.firstRuinsCompleted &&
                expedition.category == ExpeditionCategory.RUINS && isSuccess
        val isFirstAlienCiv = !progress.firstAlienCivCompleted &&
                expedition.category == ExpeditionCategory.ALIEN_CIVILIZATION && isSuccess
        val isFirstT3 = !progress.firstT3Completed && expedition.tier >= 3 && isSuccess
        val isFirstT5 = !progress.firstT5Completed && expedition.tier >= 5 && isSuccess

        val ruinsMilestoneMet = progress.firstRuinsCompleted || isFirstRuins
        val alienCivMilestoneMet = progress.firstAlienCivCompleted || isFirstAlienCiv
        val t3MilestoneMet = progress.firstT3Completed || isFirstT3
        val t5MilestoneMet = progress.firstT5Completed || isFirstT5

        // 챕터 N을 빠져나가는 데 필요한 조건 (챕터1→2엔 유적, 2→3엔 외계문명, ...)
        fun exitMilestoneMet(chapter: Int): Boolean = when (chapter) {
            1 -> ruinsMilestoneMet
            2 -> alienCivMilestoneMet
            3 -> t3MilestoneMet
            4 -> t5MilestoneMet
            else -> false
        }

        // ── 챕터 계산 ────────────────────────────────────────────────
        // 밀린 마일스톤이 여러 개 쌓여 있어도 한 기록에서 한 챕터만 전진한다.
        // (이미 밴킹된 뒷 마일스톤들은 앞 챕터가 열릴 때마다 다음 기록들에서 순서대로 소진됨)
        val isChapterEnding = progress.currentChapter < 5 && exitMilestoneMet(progress.currentChapter)
        val newChapter = if (isChapterEnding) progress.currentChapter + 1 else progress.currentChapter

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
                firstRuinsCompleted = ruinsMilestoneMet,
                firstAlienCivCompleted = alienCivMilestoneMet,
                firstT3Completed = t3MilestoneMet,
                firstT5Completed = t5MilestoneMet
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
