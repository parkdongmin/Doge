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
        fun reached(already: Boolean, condition: Boolean) = already || (isSuccess && condition)
        val ruinsMilestoneMet = reached(progress.firstRuinsCompleted, expedition.category == ExpeditionCategory.RUINS)
        val alienCivMilestoneMet = reached(progress.firstAlienCivCompleted, expedition.category == ExpeditionCategory.ALIEN_CIVILIZATION)
        val t3MilestoneMet = reached(progress.firstT3Completed, expedition.tier >= 3)
        val t5MilestoneMet = reached(progress.firstT5Completed, expedition.tier >= 5)
        // 챕터5("미지의 공간")를 빠져나가는 조건 = 최초 T10 달성 → "1부 완주" 기록
        val t10MilestoneMet = reached(progress.firstT10Completed, expedition.tier >= 10)
        // 2부 — 7·8·9장은 T11·T12·T13 이상, 10장은 T15 성공으로 빠져나간다
        val t11MilestoneMet = reached(progress.firstT11Completed, expedition.tier >= 11)
        val t12MilestoneMet = reached(progress.firstT12Completed, expedition.tier >= 12)
        val t13MilestoneMet = reached(progress.firstT13Completed, expedition.tier >= 13)
        val t15MilestoneMet = reached(progress.firstT15Completed, expedition.tier >= 15)

        // 챕터 N을 빠져나가는 데 필요한 조건 (챕터1→2엔 유적, 2→3엔 외계문명, ...). 6장은 이야기만 보면 된다
        fun exitMilestoneMet(chapter: Int): Boolean = when (chapter) {
            1 -> ruinsMilestoneMet
            2 -> alienCivMilestoneMet
            3 -> t3MilestoneMet
            4 -> t5MilestoneMet
            5 -> t10MilestoneMet
            6 -> true
            7 -> t11MilestoneMet
            8 -> t12MilestoneMet
            9 -> t13MilestoneMet
            10 -> t15MilestoneMet
            else -> false
        }

        // ── 1부 → 2부 ────────────────────────────────────────────────
        // 1부 완주 기록(5장)이 장 마무리 역할을 하므로, 그다음 기록은 곧바로 6장 1번 기록이 된다.
        // 업데이트 전에 이미 1부를 완주한 플레이어도 같은 경로로 업데이트 후 첫 기록에서 6장이 시작된다.
        // 6장 기록 수는 장별로 세므로 완주 뒤 쌓인 5장 반복 기록은 6장 진행에 들어가지 않는다
        val chapter = if (progress.currentChapter == StoryProgress.PART1_LAST_CHAPTER && progress.part1Completed)
            StoryProgress.PART2_FIRST_CHAPTER else progress.currentChapter

        // ── 챕터 이야기 진행도 ───────────────────────────────────────
        // 챕터를 넘기려면 마일스톤뿐 아니라 그 챕터 기록 풀을 끝까지 봤어야 한다(2026-10-12).
        // 조건을 일찍 채우면 이야기 중간에서 바로 넘어가, 챕터1의 "화성 궤도 센서 이상"처럼
        // 엔딩 복선이나 챕터4의 지도 만드는 과정을 건너뛰는 문제가 있었다
        val pool = StoryContent.recordTitlePool[chapter] ?: emptyList()
        val indexWithinChapter = storyRepository.countRegularRecordsInChapter(chapter)
        val chapterStoryDone = indexWithinChapter >= pool.size

        // ── 챕터 계산 ────────────────────────────────────────────────
        // 밀린 마일스톤이 여러 개 쌓여 있어도 한 기록에서 한 챕터만 전진한다.
        // (이미 밴킹된 뒷 마일스톤들은 앞 챕터가 열릴 때마다 다음 기록들에서 순서대로 소진됨)
        // 5장과 10장은 그 뒤가 없어 장을 넘기지 않고, 대신 아래 "완주" 기록으로 처리한다
        val isChapterEnding = chapter != StoryProgress.PART1_LAST_CHAPTER && chapter < StoryProgress.LAST_CHAPTER &&
                chapterStoryDone && exitMilestoneMet(chapter)
        val newChapter = if (isChapterEnding) chapter + 1 else chapter

        // ── 완주 ─────────────────────────────────────────────────────
        // 5장(1부)·10장(2부)의 이야기를 다 봤고 탈출 조건을 채웠는데 아직 완주 처리를 안 했으면 딱 한 번 완주 기록
        val triggersPart1Ending = !progress.part1Completed && chapter == StoryProgress.PART1_LAST_CHAPTER &&
                chapterStoryDone && exitMilestoneMet(chapter)
        val triggersPart2Ending = !progress.part2Completed && chapter == StoryProgress.LAST_CHAPTER &&
                chapterStoryDone && exitMilestoneMet(chapter)
        val newPart1Completed = progress.part1Completed || triggersPart1Ending
        val newPart2Completed = progress.part2Completed || triggersPart2Ending

        val newTotalRecords = progress.totalRecordsCompleted + 1
        var settlementLogCursor = progress.settlementLogCursor
        fun nextSettlementLog(): String = StoryContent.settlementLog(settlementLogCursor++)

        // ── 기록 제목 선정 ───────────────────────────────────────────
        val recordTitle = when {
            triggersPart1Ending -> StoryContent.storyEndingTitle
            triggersPart2Ending -> StoryContent.part2EndingTitle
            isChapterEnding -> StoryContent.chapterEndingTitles[chapter] ?: "미지의 공간으로 한 발짝 더 나아갔다"
            // 챕터 안에서 몇 번째 기록인지로 풀을 처음부터 순서대로 쓴다
            indexWithinChapter < pool.size -> pool[indexWithinChapter]
            // 2부에서 풀을 다 썼는데 조건을 못 채웠으면 3개 중 1개는 그 장의 막힘 문장, 2개는 정착지 일상 기록 —
            // 막힘 문장만 돌리면 금방 반복되고, 일상 기록이 섞이면 기다리는 동안에도 이야기가 흐른다.
            // 2부 완주 뒤엔 다음 목표가 없으니 일상 기록만
            chapter >= StoryProgress.PART2_FIRST_CHAPTER -> {
                val stalledPool = StoryContent.stalledRecordTitlePool[chapter].orEmpty()
                val k = indexWithinChapter - pool.size
                if (!newPart2Completed && stalledPool.isNotEmpty() && k % 3 == 0) stalledPool[(k / 3) % stalledPool.size]
                else nextSettlementLog()
            }
            // 1부에서 풀을 다 썼는데 조건을 못 채웠으면 같은 이야기를 다시 돌리지 않고 "정체" 문장을 내보낸다 —
            // 반복되는 이야기는 버그처럼 보이고, 정체 문장은 다음에 갈 곳을 이야기로 알려준다
            else -> {
                val stalledPool = StoryContent.stalledRecordTitlePool[chapter].orEmpty()
                when {
                    stalledPool.isNotEmpty() -> stalledPool[(indexWithinChapter - pool.size) % stalledPool.size]
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

            // 챕터5(미지의 공간)는 지금 뭘 탐사하든 챕터5 전용 풀로 완전히 교체한다.
            // 2부(6장 이상)는 이벤트마다 절반은 2부 공용 풀, 절반은 카테고리 풀 — 5장 전용 이벤트는 다시 안 나온다
            repeat(maxEvents) {
                if (Random.nextFloat() < chance) {
                    val event = when {
                        newChapter >= StoryProgress.PART2_FIRST_CHAPTER ->
                            if (Random.nextBoolean()) StoryContent.randomPart2Event(expedition.id)
                            else StoryContent.randomEvent(expedition.category, expedition.id)
                        newChapter == StoryProgress.PART1_LAST_CHAPTER -> StoryContent.randomChapter5Event(expedition.id)
                        else -> StoryContent.randomEvent(expedition.category, expedition.id)
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
                storyCompleted = newPart1Completed,
                firstT11Completed = t11MilestoneMet,
                firstT12Completed = t12MilestoneMet,
                firstT13Completed = t13MilestoneMet,
                firstT15Completed = t15MilestoneMet,
                part2Completed = newPart2Completed,
                settlementLogCursor = settlementLogCursor
            )
        )

        return ExpeditionReport(
            expeditionId = expedition.id,
            recordNumber = newTotalRecords,
            chapter = newChapter,
            chapterTitle = StoryProgress.chapterTitle(newChapter),
            recordTitle = recordTitle,
            isChapterEnding = isChapterEnding,
            isStoryEnding = triggersPart1Ending || triggersPart2Ending,
            events = events,
            isRead = false,
            completedAt = System.currentTimeMillis()
        )
    }
}
