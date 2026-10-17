package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.ExpeditionReport
import com.doge.simulator.domain.model.StoryEvent
import com.doge.simulator.domain.model.StoryProgress
import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    fun getProgress(): Flow<StoryProgress>
    suspend fun getProgressOnce(): StoryProgress
    suspend fun saveProgress(progress: StoryProgress)

    fun getAllReports(): Flow<List<ExpeditionReport>>
    fun getUnreadReports(): Flow<List<ExpeditionReport>>
    fun getUnreadCount(): Flow<Int>
    fun getReadCount(): Flow<Int>
    // 챕터 전환·완주 기록을 뺀, 이 챕터의 일반 기록 수
    suspend fun countRegularRecordsInChapter(chapter: Int): Int
    suspend fun saveReport(report: ExpeditionReport)
    suspend fun markReportAsRead(expeditionId: String)
    // 반환값이 false면 이미 선택이 끝난 이벤트(중복 처리 방지)라는 뜻
    suspend fun claimEventChoice(eventId: String, expeditionId: String, choiceIndex: Int): Boolean
    suspend fun setEventOutcomeNote(eventId: String, expeditionId: String, outcomeNote: String)
    suspend fun addEvent(event: StoryEvent)

    /** 관제탑 2부 안내를 봤다고 기록(클라우드 세이브에도 남아 기기를 바꿔도 다시 안 뜬다) */
    suspend fun markPart2IntroShown()
}
