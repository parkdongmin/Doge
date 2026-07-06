package com.doge.simulator.domain.model

data class StoryProgress(
    val totalRecordsCompleted: Int = 0,
    val currentChapter: Int = 1,
    val firstRuinsCompleted: Boolean = false,
    val firstAlienCivCompleted: Boolean = false,
    val firstT3Completed: Boolean = false,
    val firstT5Completed: Boolean = false
) {
    val chapterTitle: String get() = when (currentChapter) {
        1 -> "태양계의 끝에서"
        2 -> "첫 번째 신호"
        3 -> "조우"
        4 -> "동맹인가, 적인가"
        else -> "미지의 공간"
    }
}
