package com.doge.simulator.data.repository

import com.doge.simulator.data.local.dao.ExpeditionReportDao
import com.doge.simulator.data.local.dao.StoryProgressDao
import com.doge.simulator.data.local.entity.ExpeditionReportEntity
import com.doge.simulator.data.local.entity.StoryEventEntity
import com.doge.simulator.data.local.entity.StoryProgressEntity
import com.doge.simulator.domain.model.ExpeditionReport
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.model.StoryEvent
import com.doge.simulator.domain.model.StoryProgress
import com.doge.simulator.domain.repository.StoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StoryRepositoryImpl @Inject constructor(
    private val progressDao: StoryProgressDao,
    private val reportDao: ExpeditionReportDao
) : StoryRepository {

    override fun getProgress(): Flow<StoryProgress> =
        progressDao.get().map { it?.toDomain() ?: StoryProgress() }

    override suspend fun getProgressOnce(): StoryProgress =
        progressDao.getOnce()?.toDomain() ?: StoryProgress()

    override suspend fun saveProgress(progress: StoryProgress) =
        progressDao.upsert(progress.toEntity())

    override fun getAllReports(): Flow<List<ExpeditionReport>> =
        reportDao.getAllReports().map { reports ->
            reports.map { report ->
                val events = reportDao.getEventsForReport(report.expeditionId)
                report.toDomain(events)
            }
        }

    override fun getUnreadReports(): Flow<List<ExpeditionReport>> =
        reportDao.getUnreadReports().map { reports ->
            reports.map { report ->
                val events = reportDao.getEventsForReport(report.expeditionId)
                report.toDomain(events)
            }
        }

    override fun getUnreadCount(): Flow<Int> = reportDao.getUnreadCount()

    override suspend fun saveReport(report: ExpeditionReport) {
        reportDao.insertReport(report.toEntity())
        reportDao.insertEvents(report.events.map { it.toEntity() })
    }

    override suspend fun markReportAsRead(expeditionId: String) =
        reportDao.markAsRead(expeditionId)

    override suspend fun claimEventChoice(eventId: String, expeditionId: String, choiceIndex: Int): Boolean {
        val claimed = reportDao.claimChoice(eventId, choiceIndex) > 0
        if (claimed) {
            // story_event_table만 갱신하면 보고서 Flow가 갱신되지 않으므로 강제로 invalidation을 트리거한다
            reportDao.touchReport(expeditionId)
        }
        return claimed
    }

    override suspend fun setEventOutcomeNote(eventId: String, expeditionId: String, outcomeNote: String) {
        reportDao.setOutcomeNote(eventId, outcomeNote)
        reportDao.touchReport(expeditionId)
    }

    override suspend fun addEvent(event: StoryEvent) {
        reportDao.insertEvents(listOf(event.toEntity()))
        reportDao.touchReport(event.expeditionId)
    }

    // ── 매핑 ─────────────────────────────────────────────────────────
    private fun StoryProgressEntity.toDomain() = StoryProgress(
        totalRecordsCompleted = totalRecordsCompleted,
        currentChapter = currentChapter,
        firstRuinsCompleted = firstRuinsCompleted,
        firstAlienCivCompleted = firstAlienCivCompleted,
        firstT3Completed = firstT3Completed,
        firstT5Completed = firstT5Completed
    )

    private fun StoryProgress.toEntity() = StoryProgressEntity(
        totalRecordsCompleted = totalRecordsCompleted,
        currentChapter = currentChapter,
        firstRuinsCompleted = firstRuinsCompleted,
        firstAlienCivCompleted = firstAlienCivCompleted,
        firstT3Completed = firstT3Completed,
        firstT5Completed = firstT5Completed
    )

    private fun ExpeditionReportEntity.toDomain(events: List<StoryEventEntity>) = ExpeditionReport(
        expeditionId = expeditionId,
        recordNumber = recordNumber,
        chapter = chapter,
        chapterTitle = chapterTitle,
        recordTitle = recordTitle,
        isChapterEnding = isChapterEnding,
        events = events.map { it.toDomain() },
        isRead = isRead,
        completedAt = completedAt
    )

    private fun ExpeditionReport.toEntity() = ExpeditionReportEntity(
        expeditionId = expeditionId,
        recordNumber = recordNumber,
        chapter = chapter,
        chapterTitle = chapterTitle,
        recordTitle = recordTitle,
        isChapterEnding = isChapterEnding,
        isRead = isRead,
        completedAt = completedAt
    )

    private fun StoryEventEntity.toDomain() = StoryEvent(
        id = id,
        expeditionId = expeditionId,
        title = title,
        description = description,
        choice1Label = choice1Label,
        choice1ResourceType = ResourceType.valueOf(choice1ResourceType),
        choice1Amount = choice1Amount,
        choice2Label = choice2Label,
        choice2ResourceType = ResourceType.valueOf(choice2ResourceType),
        choice2Amount = choice2Amount,
        choice3Label = choice3Label,
        choice3ResourceType = choice3ResourceType?.let { ResourceType.valueOf(it) },
        choice3Amount = choice3Amount,
        selectedChoiceIndex = selectedChoiceIndex,
        isDeparture = isDeparture,
        outcomeNote = outcomeNote
    )

    private fun StoryEvent.toEntity() = StoryEventEntity(
        id = id,
        expeditionId = expeditionId,
        title = title,
        description = description,
        choice1Label = choice1Label,
        choice1ResourceType = choice1ResourceType.name,
        choice1Amount = choice1Amount,
        choice2Label = choice2Label,
        choice2ResourceType = choice2ResourceType.name,
        choice2Amount = choice2Amount,
        choice3Label = choice3Label,
        choice3ResourceType = choice3ResourceType?.name,
        choice3Amount = choice3Amount,
        selectedChoiceIndex = selectedChoiceIndex,
        isDeparture = isDeparture,
        outcomeNote = outcomeNote
    )
}
