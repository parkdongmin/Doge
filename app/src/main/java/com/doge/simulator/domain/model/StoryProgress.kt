package com.doge.simulator.domain.model

data class StoryProgress(
    val totalRecordsCompleted: Int = 0,
    val currentChapter: Int = 1,
    val firstRuinsCompleted: Boolean = false,
    val firstAlienCivCompleted: Boolean = false,
    val firstT3Completed: Boolean = false,
    val firstT5Completed: Boolean = false,
    // 챕터5("미지의 공간")를 "빠져나가는" 조건 = 최초 T10 달성. 챕터6은 없고,
    // 이 조건이 충족되는 기록 하나를 "완주" 기록으로 특별 표시한 뒤 챕터는 계속 5에 머문다
    val firstT10Completed: Boolean = false,
    val storyCompleted: Boolean = false
) {
    // 지금 챕터를 빠져나갈 조건을 이미 채웠는지 — 채웠으면 다음 탐사 기록에서 바로 이야기가 이어진다
    val isCurrentChapterExitMet: Boolean get() = when (currentChapter) {
        1 -> firstRuinsCompleted
        2 -> firstAlienCivCompleted
        3 -> firstT3Completed
        4 -> firstT5Completed
        else -> firstT10Completed
    }

    val chapterTitle: String get() = when (currentChapter) {
        1 -> "태양계의 끝에서"
        2 -> "첫 번째 신호"
        3 -> "조우"
        4 -> "동맹인가, 적인가"
        else -> "미지의 공간"
    }
}
