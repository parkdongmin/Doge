package com.doge.simulator.data.local.snapshot

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
import kotlinx.serialization.Serializable

// 로컬 Room 게임 상태 전체를 클라우드로 옮기기 위한 직렬화 가능한 스냅샷.
// planet_event_log_table(24h 휘발성 뉴스 피드)은 의도적으로 제외.
@Serializable
data class GameSnapshot(
    // 이 스냅샷 포맷의 버전. GameSnapshot이 참조하는 엔티티의 모양을 바꾸면 올린다.
    // 복원 시 이 값 기준으로 SnapshotMigrations를 태워 현재 버전 모양으로 맞춘다.
    val snapshotSchemaVersion: Int = SCHEMA_VERSION,
    // 스냅샷을 찍을 때의 Room DB 버전. 더 이상 복원 게이트가 아니고 참고·디버깅용.
    val roomDbVersion: Int,

    val user: UserEntity? = null,
    val researchLab: ResearchLabEntity? = null,
    val storyProgress: StoryProgressEntity? = null,
    val recruitmentMeta: RecruitmentMetaEntity? = null,

    val planets: List<PlanetEntity> = emptyList(),
    val resources: List<ResourceEntity> = emptyList(),
    val astronauts: List<AstronautEntity> = emptyList(),
    val spaceships: List<SpaceshipEntity> = emptyList(),
    val recruitmentCandidates: List<RecruitmentCandidateEntity> = emptyList(),

    // 이력 테이블 — 최근 HISTORY_CAP 개만 (Firestore 1MiB 문서 한도 방어).
    // cap을 벗어난 오래된 탐사 기록 로그는 기기 이전 시 유실되지만 진행상황은 아님.
    val expeditions: List<ExpeditionEntity> = emptyList(),
    val expeditionReports: List<ExpeditionReportEntity> = emptyList(),
    val storyEvents: List<StoryEventEntity> = emptyList(),
) {
    companion object {
        // 엔티티 모양을 바꿀 때마다 +1. 이름변경/타입변경/구조변경이면 SnapshotMigrations에 step도 등록.
        // (단순 필드 추가/삭제는 step 불필요 — ignoreUnknownKeys + 기본값이 처리)
        const val SCHEMA_VERSION = 1
        const val HISTORY_CAP = 200
    }
}
