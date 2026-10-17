package com.doge.simulator.domain.model

data class ExpeditionReport(
    val expeditionId: String,
    val recordNumber: Int,
    val chapter: Int,
    val chapterTitle: String,
    val recordTitle: String,
    val isChapterEnding: Boolean = false,
    // 1부·2부 완주 기록에서 딱 한 번씩 true. 장 번호로 구분한다 — 5장이면 "1부 완주"(챕터5를 T10 달성으로
    // 빠져나간 순간), 10장이면 "2부 완주"(10장을 T15 달성으로 빠져나간 순간)
    val isStoryEnding: Boolean = false,
    val events: List<StoryEvent> = emptyList(),
    val isRead: Boolean = false,
    val completedAt: Long
) {
    val hasPendingChoices: Boolean get() = events.any { it.isPending }
    val recordLabel: String get() = "탐사기록 #%02d".format(recordNumber)
}
