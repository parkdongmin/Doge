package com.doge.simulator.data.local.snapshot

import androidx.room.withTransaction
import com.doge.simulator.data.local.PlanetDatabase
import com.doge.simulator.data.local.dao.SnapshotDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// 로컬 Room 전체 게임 상태를 GameSnapshot으로 뽑거나(=export), 스냅샷으로 통째 되돌린다(=import).
@Singleton
class LocalSnapshotDataSource @Inject constructor(
    private val db: PlanetDatabase,
    private val dao: SnapshotDao
) {

    suspend fun export(): GameSnapshot = withContext(Dispatchers.IO) {
        val expeditions = dao.recentExpeditions(GameSnapshot.HISTORY_CAP)
        val reports = dao.recentReports(GameSnapshot.HISTORY_CAP)
        val eventKeys = (reports.map { it.expeditionId } + expeditions.map { it.id }).distinct()
        GameSnapshot(
            roomDbVersion = PlanetDatabase.VERSION,
            user = dao.user(),
            researchLab = dao.researchLab(),
            storyProgress = dao.storyProgress(),
            recruitmentMeta = dao.recruitmentMeta(),
            planets = dao.planets(),
            resources = dao.resources(),
            astronauts = dao.astronauts(),
            spaceships = dao.spaceships(),
            recruitmentCandidates = dao.recruitmentCandidates(),
            expeditions = expeditions,
            expeditionReports = reports,
            storyEvents = if (eventKeys.isEmpty()) emptyList() else dao.storyEventsFor(eventKeys),
        )
    }

    // 주의: clearAllTables()로 planet_event_log_table 포함 전 테이블을 비운 뒤 스냅샷을 다시 채운다.
    // clear는 트랜잭션 밖에서 호출해야 하므로 clear + insert 전체가 원자적이지는 않다 — import 도중
    // 프로세스가 죽으면 빈 DB가 남고, 다음 진입 시 UserRepository.initialize() 등이 신규 게임으로
    // 재시드한다(드묾, 스플래시 타임에만 발생). 스키마 호환성은 loadSnapshot()이 먼저 걸러야 한다.
    suspend fun import(snapshot: GameSnapshot) = withContext(Dispatchers.IO) {
        db.clearAllTables()
        db.withTransaction {
            snapshot.user?.let { dao.insertUser(it) }
            snapshot.researchLab?.let { dao.insertResearchLab(it) }
            snapshot.storyProgress?.let { dao.insertStoryProgress(it) }
            snapshot.recruitmentMeta?.let { dao.insertRecruitmentMeta(it) }
            dao.insertResources(snapshot.resources)
            dao.insertAstronauts(snapshot.astronauts)
            dao.insertSpaceships(snapshot.spaceships)
            dao.insertPlanets(snapshot.planets)
            dao.insertExpeditions(snapshot.expeditions)
            dao.insertReports(snapshot.expeditionReports)
            dao.insertStoryEvents(snapshot.storyEvents)
            dao.insertRecruitmentCandidates(snapshot.recruitmentCandidates)
        }
    }
}
