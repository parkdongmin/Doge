package com.doge.simulator.domain.model

data class ExpeditionReport(
    val expeditionId: String,
    val recordNumber: Int,
    val chapter: Int,
    val chapterTitle: String,
    val recordTitle: String,
    val isChapterEnding: Boolean = false,
    val events: List<StoryEvent> = emptyList(),
    val isRead: Boolean = false,
    val completedAt: Long
) {
    val hasPendingChoices: Boolean get() = events.any { it.isPending }
    val recordLabel: String get() = "탐사기록 #%02d".format(recordNumber)
}
