package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "user_table")
data class UserEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Long,
    val discoveredVariantIds: String = "",
    // ── 초보 임무 (2026-10-12) ──────────────────────────────────────────
    // 기기 설정값이 아니라 게임 데이터에 둬야 계정별로 갈리고, 재설치·기기 변경 때 클라우드에서 같이
    // 돌아오며, 코인과 같은 스냅샷에 묶여 보상을 두 번 받거나 잃는 일이 없다.
    // missionStep: 지금 임무의 번호(0부터, BeginnerMission.entries 순서). -1 = 대상 아님 —
    // 업데이트 전부터 하던 게임과 예전 클라우드 세이브는 기본값(-1)으로 채워져 임무가 안 뜬다.
    // 새 게임만 UserRepository.initialize()에서 0으로 시작한다
    val missionStep: Int = -1,
    // 게임 상태로는 알 수 없어 따로 남기는 기록 — 임무 순서와 무관하게 게임 시작부터 기록해서,
    // 임무보다 먼저 해둔 사람도 그 임무 차례에 바로 완료로 뜨게 한다
    val orbitPlayed: Boolean = false,
    val planetUpgradeTried: Boolean = false
)
