package com.doge.simulator.domain.model

data class ExpeditionReport(
    val expeditionId: String,
    val recordNumber: Int,
    val chapter: Int,
    val chapterTitle: String,
    val recordTitle: String,
    val isChapterEnding: Boolean = false,
    // 챕터5("미지의 공간")를 T10 달성으로 빠져나가는 순간 딱 한 번 true — 챕터가 6으로
    // 넘어가진 않고(콘텐츠 없음) 계속 5에 머물지만, 이 기록 하나만은 "완주"로 특별 표시된다
    val isStoryEnding: Boolean = false,
    val events: List<StoryEvent> = emptyList(),
    val isRead: Boolean = false,
    val completedAt: Long
) {
    val hasPendingChoices: Boolean get() = events.any { it.isPending }
    val recordLabel: String get() = "탐사기록 #%02d".format(recordNumber)
}
