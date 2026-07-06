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
    suspend fun saveReport(report: ExpeditionReport)
    suspend fun markReportAsRead(expeditionId: String)
    suspend fun selectEventChoice(eventId: String, choiceIndex: Int, outcomeNote: String? = null)
    suspend fun addEvent(event: StoryEvent)
}
