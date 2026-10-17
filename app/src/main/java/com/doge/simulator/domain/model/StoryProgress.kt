package com.doge.simulator.domain.model

data class StoryProgress(
    val totalRecordsCompleted: Int = 0,
    val currentChapter: Int = 1,
    val firstRuinsCompleted: Boolean = false,
    val firstAlienCivCompleted: Boolean = false,
    val firstT3Completed: Boolean = false,
    val firstT5Completed: Boolean = false,
    // 챕터5("미지의 공간")를 "빠져나가는" 조건 = 최초 T10 달성. 이 조건이 충족되는 기록 하나를
    // "1부 완주" 기록으로 특별 표시하고, 다음 기록부터 2부(6장)가 시작된다
    val firstT10Completed: Boolean = false,
    // 1부 완주 여부. 칼럼 이름을 바꾸면 클라우드 스냅샷 변환이 필요해 이름은 그대로 두고 part1Completed로 읽는다
    val storyCompleted: Boolean = false,

    // ── 2부 (6~10장, 2026-10-14) ─────────────────────────────────────────
    // 7·8·9·10장을 빠져나가는 조건 — 처음으로 그 티어 이상 탐사에 성공했는지(1부 마일스톤과 같이 미리 쌓일 수 있음)
    val firstT11Completed: Boolean = false,
    val firstT12Completed: Boolean = false,
    val firstT13Completed: Boolean = false,
    val firstT15Completed: Boolean = false,
    val part2Completed: Boolean = false,
    // 관제탑 2부 안내를 봤는지 — 기기 설정이 아니라 게임 데이터에 둬야 기기를 바꿔도 다시 안 뜬다
    val part2IntroShown: Boolean = false,
    // 정착지 일상 기록을 지금까지 몇 줄 썼는지(장과 무관하게 누적) — StoryContent.settlementLog(cursor)
    val settlementLogCursor: Int = 0
) {
    val part1Completed: Boolean get() = storyCompleted

    // 지금 챕터를 빠져나갈 조건을 이미 채웠는지 — 채웠으면 다음 탐사 기록에서 바로 이야기가 이어진다.
    // 6장은 조건 없이 이야기만 보면 넘어간다
    val isCurrentChapterExitMet: Boolean get() = when (currentChapter) {
        1 -> firstRuinsCompleted
        2 -> firstAlienCivCompleted
        3 -> firstT3Completed
        4 -> firstT5Completed
        5 -> firstT10Completed
        6 -> true
        7 -> firstT11Completed
        8 -> firstT12Completed
        9 -> firstT13Completed
        else -> firstT15Completed
    }

    val chapterTitle: String get() = chapterTitle(currentChapter)

    companion object {
        const val PART1_LAST_CHAPTER = 5
        const val PART2_FIRST_CHAPTER = 6
        const val LAST_CHAPTER = 10

        fun chapterTitle(chapter: Int): String = when (chapter) {
            1 -> "태양계의 끝에서"
            2 -> "첫 번째 신호"
            3 -> "조우"
            4 -> "동맹인가, 적인가"
            5 -> "미지의 공간"
            6 -> "붉은 돔"
            7 -> "균열"
            8 -> "다시 바깥으로"
            9 -> "재회"
            else -> "귀환 신호"
        }
    }
}
