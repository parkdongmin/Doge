package com.doge.simulator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.doge.simulator.data.local.entity.AstronautEntity
import com.doge.simulator.data.local.entity.ExpeditionEntity
import com.doge.simulator.data.local.entity.ExpeditionReportEntity
import com.doge.simulator.data.local.entity.PlanetEntity
import com.doge.simulator.data.local.entity.RecruitmentCandidateEntity
import com.doge.simulator.data.local.entity.RecruitmentMetaEntity
import com.doge.simulator.data.local.entity.ResearchLabEntity
import com.doge.simulator.data.local.entity.ResourceEntity
import com.doge.simulator.data.local.entity.SpaceshipEntity
import com.doge.simulator.data.local.entity.StoryEventEntity
import com.doge.simulator.data.local.entity.StoryProgressEntity
import com.doge.simulator.data.local.entity.UserEntity

// 클라우드 세이브용 일괄 읽기/쓰기 전용 DAO. 게임 로직은 각 기능별 DAO를 쓰고, 이건
// 전체 스냅샷 export/import 에만 쓴다. planet_event_log_table(24h 휘발성 뉴스)은 제외.
@Dao
interface SnapshotDao {

    // ── export (1회성 전체 조회) ──────────────────────────────────────
    @Query("SELECT * FROM user_table WHERE id = 1")
    suspend fun user(): UserEntity?

    @Query("SELECT * FROM research_lab_table WHERE id = 1")
    suspend fun researchLab(): ResearchLabEntity?

    @Query("SELECT * FROM story_progress_table WHERE id = 1")
    suspend fun storyProgress(): StoryProgressEntity?

    @Query("SELECT * FROM recruitment_meta_table WHERE id = 1")
    suspend fun recruitmentMeta(): RecruitmentMetaEntity?

    @Query("SELECT * FROM planet_table")
    suspend fun planets(): List<PlanetEntity>

    @Query("SELECT * FROM resource_table")
    suspend fun resources(): List<ResourceEntity>

    @Query("SELECT * FROM astronaut_table")
    suspend fun astronauts(): List<AstronautEntity>

    @Query("SELECT * FROM spaceship_table")
    suspend fun spaceships(): List<SpaceshipEntity>

    @Query("SELECT * FROM recruitment_candidate_table")
    suspend fun recruitmentCandidates(): List<RecruitmentCandidateEntity>

    // 이력 테이블은 최근 N개만 (Firestore 1MiB 문서 한도 방어)
    @Query("SELECT * FROM expedition_table ORDER BY startTime DESC LIMIT :limit")
    suspend fun recentExpeditions(limit: Int): List<ExpeditionEntity>

    @Query("SELECT * FROM expedition_report_table ORDER BY completedAt DESC LIMIT :limit")
    suspend fun recentReports(limit: Int): List<ExpeditionReportEntity>

    @Query("SELECT * FROM story_event_table WHERE expeditionId IN (:expeditionIds)")
    suspend fun storyEventsFor(expeditionIds: List<String>): List<StoryEventEntity>

    // ── import (일괄 삽입, import() 트랜잭션 안에서만 호출) ────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertUser(row: UserEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertResearchLab(row: ResearchLabEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertStoryProgress(row: StoryProgressEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRecruitmentMeta(row: RecruitmentMetaEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPlanets(rows: List<PlanetEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertResources(rows: List<ResourceEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAstronauts(rows: List<AstronautEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSpaceships(rows: List<SpaceshipEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExpeditions(rows: List<ExpeditionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReports(rows: List<ExpeditionReportEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertStoryEvents(rows: List<StoryEventEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRecruitmentCandidates(rows: List<RecruitmentCandidateEntity>)
}
