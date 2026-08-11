package com.doge.simulator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.doge.simulator.data.local.entity.ExpeditionReportEntity
import com.doge.simulator.data.local.entity.StoryEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpeditionReportDao {

    // ── 보고서 ────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ExpeditionReportEntity)

    @Query("SELECT * FROM expedition_report_table ORDER BY completedAt DESC")
    fun getAllReports(): Flow<List<ExpeditionReportEntity>>

    @Query("SELECT * FROM expedition_report_table WHERE isRead = 0 ORDER BY completedAt DESC")
    fun getUnreadReports(): Flow<List<ExpeditionReportEntity>>

    @Query("SELECT COUNT(*) FROM expedition_report_table WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("UPDATE expedition_report_table SET isRead = 1 WHERE expeditionId = :expeditionId")
    suspend fun markAsRead(expeditionId: String)

    // story_event_table에 이벤트만 추가하면 보고서 Flow가 갱신되지 않으므로,
    // 같은 행을 다시 써서 invalidation을 강제로 트리거한다
    @Query("UPDATE expedition_report_table SET completedAt = completedAt WHERE expeditionId = :expeditionId")
    suspend fun touchReport(expeditionId: String)

    // ── 스토리 이벤트 ─────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<StoryEventEntity>)

    @Query("SELECT * FROM story_event_table WHERE expeditionId = :expeditionId")
    suspend fun getEventsForReport(expeditionId: String): List<StoryEventEntity>

    @Query("SELECT * FROM story_event_table WHERE expeditionId = :expeditionId")
    fun observeEventsForReport(expeditionId: String): Flow<List<StoryEventEntity>>

    // selectedChoiceIndex = -1(미선택) 가드 — 같은 이벤트에 대한 중복 선택(연타 등)이 들어와도
    // 한 번만 반영되도록 원자적으로 처리. 반환값이 0이면 이미 선택된 이벤트라는 뜻
    @Query("UPDATE story_event_table SET selectedChoiceIndex = :choiceIndex WHERE id = :eventId AND selectedChoiceIndex = -1")
    suspend fun claimChoice(eventId: String, choiceIndex: Int): Int

    @Query("UPDATE story_event_table SET outcomeNote = :outcomeNote WHERE id = :eventId")
    suspend fun setOutcomeNote(eventId: String, outcomeNote: String?)

    @Query("SELECT * FROM story_event_table WHERE selectedChoiceIndex = -1")
    fun getPendingEvents(): Flow<List<StoryEventEntity>>
}
