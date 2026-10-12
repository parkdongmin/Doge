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
        // 챕터5("미지의 공간")를 빠져나가는 조건 = 최초 T10 달성. 챕터6은 없어서 이 마일스톤은
        // 챕터를 더 못 올리고, 대신 "완주" 기록 한 번을 트리거하는 데만 쓰인다(아래 참고)
        val isFirstT10 = !progress.firstT10Completed && expedition.tier >= 10 && isSuccess

        val ruinsMilestoneMet = progress.firstRuinsCompleted || isFirstRuins
        val alienCivMilestoneMet = progress.firstAlienCivCompleted || isFirstAlienCiv
        val t3MilestoneMet = progress.firstT3Completed || isFirstT3
        val t5MilestoneMet = progress.firstT5Completed || isFirstT5
        val t10MilestoneMet = progress.firstT10Completed || isFirstT10

        // 챕터 N을 빠져나가는 데 필요한 조건 (챕터1→2엔 유적, 2→3엔 외계문명, ...)
        fun exitMilestoneMet(chapter: Int): Boolean = when (chapter) {
            1 -> ruinsMilestoneMet
            2 -> alienCivMilestoneMet
            3 -> t3MilestoneMet
            4 -> t5MilestoneMet
            5 -> t10MilestoneMet
            else -> false
        }

        // ── 챕터 이야기 진행도 ───────────────────────────────────────
        // 챕터를 넘기려면 마일스톤뿐 아니라 그 챕터 기록 풀을 끝까지 봤어야 한다(2026-10-12).
        // 조건을 일찍 채우면 이야기 중간에서 바로 넘어가, 챕터1의 "화성 궤도 센서 이상"처럼
        // 엔딩 복선이나 챕터4의 지도 만드는 과정을 건너뛰는 문제가 있었다
        val pool = StoryContent.recordTitlePool[progress.currentChapter] ?: emptyList()
        val indexWithinChapter = storyRepository.countRegularRecordsInChapter(progress.currentChapter)
        val chapterStoryDone = indexWithinChapter >= pool.size

        // ── 챕터 계산 ────────────────────────────────────────────────
        // 밀린 마일스톤이 여러 개 쌓여 있어도 한 기록에서 한 챕터만 전진한다.
        // (이미 밴킹된 뒷 마일스톤들은 앞 챕터가 열릴 때마다 다음 기록들에서 순서대로 소진됨)
        // 챕터5는 그 뒤가 없어 여기서는 전진시키지 않고, 대신 바로 아래 "완주" 트리거로 처리한다
        val isChapterEnding = progress.currentChapter < 5 && chapterStoryDone &&
                exitMilestoneMet(progress.currentChapter)
        val newChapter = if (isChapterEnding) progress.currentChapter + 1 else progress.currentChapter

        // ── 스토리 완주 ──────────────────────────────────────────────
        // 챕터5에 이미 도달해 있고 이야기를 끝까지 봤으며 T10 마일스톤이 이번(혹은 예전에)
        // 충족됐는데 아직 완주 처리를 안 한 경우 — 딱 한 번만 "완주" 기록으로 표시한다
        val triggersEnding = !progress.storyCompleted && progress.currentChapter == 5 &&
                chapterStoryDone && exitMilestoneMet(5)
        val newStoryCompleted = progress.storyCompleted || triggersEnding

        val newTotalRecords = progress.totalRecordsCompleted + 1

        // ── 기록 제목 선정 ───────────────────────────────────────────
        val recordTitle = when {
            triggersEnding -> StoryContent.storyEndingTitle
            isChapterEnding -> StoryContent.chapterEndingTitles[progress.currentChapter]
                ?: "미지의 공간으로 한 발짝 더 나아갔다"
            else -> {
                // 챕터 안에서 몇 번째 기록인지로 풀을 처음부터 순서대로 쓴다. 풀을 다 썼는데도
                // 챕터 조건을 못 채웠으면 같은 이야기를 다시 돌리지 않고 "정체" 문장을 내보낸다 —
                // 반복되는 이야기는 버그처럼 보이고, 정체 문장은 다음에 갈 곳을 이야기로 알려준다.
                // 완주 뒤엔 다음 목표가 없으니 예전처럼 챕터 풀을 계속 돈다
                // (여기선 챕터가 안 바뀌었으므로 newChapter == progress.currentChapter, 위 pool 그대로)
                val stalledPool = StoryContent.stalledRecordTitlePool[newChapter] ?: emptyList()
                when {
                    indexWithinChapter < pool.size -> pool[indexWithinChapter]
                    !newStoryCompleted && stalledPool.isNotEmpty() ->
                        stalledPool[(indexWithinChapter - pool.size) % stalledPool.size]
                    pool.isNotEmpty() -> pool[indexWithinChapter % pool.size]
                    else -> "탐사가 완료됐다"
                }
            }
        }

        // ── 이벤트 생성 ──────────────────────────────────────────────
        val durationMs = expedition.endTime - expedition.startTime
        val events = mutableListOf<StoryEvent>()

        if (isSuccess) {
            val chance = eventChance(durationMs)
            val maxEvents = if (durationMs >= 8 * 60 * 60 * 1000L) 2 else 1

            // 챕터5(미지의 공간, 외계 문명 조우 이후)에 들어서면 지금 뭘 탐사하든
            // 카테고리별 초반 톤 이벤트 대신 챕터5 전용 풀로 완전히 교체한다
            repeat(maxEvents) {
                if (Random.nextFloat() < chance) {
                    val event = if (newChapter >= 5) {
                        StoryContent.randomChapter5Event(expedition.id)
                    } else {
                        StoryContent.randomEvent(expedition.category, expedition.id)
                    }
                    event?.let { events.add(it) }
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
                firstT5Completed = t5MilestoneMet,
                firstT10Completed = t10MilestoneMet,
                storyCompleted = newStoryCompleted
            )
        )

        return ExpeditionReport(
            expeditionId = expedition.id,
            recordNumber = newTotalRecords,
            chapter = newChapter,
            chapterTitle = StoryProgress(currentChapter = newChapter).chapterTitle,
            recordTitle = recordTitle,
            isChapterEnding = isChapterEnding,
            isStoryEnding = triggersEnding,
            events = events,
            isRead = false,
            completedAt = System.currentTimeMillis()
        )
    }
}
