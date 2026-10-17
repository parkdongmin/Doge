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
    val planetUpgradeTried: Boolean = false,
    // ── 2부 특성 행성 획득 기록 (2026-10-14) ──────────────────────────────
    // T12~T15 해금 조건용 누적 숫자. 1부 도감 등록과 같은 시점(구매, 슬롯이 가득 찼을 때 "코인으로 받기")에
    // +1 되고 매도해도 줄지 않는다. 옛 세이브는 0에서 시작
    val traitPlanetsAcquired: Int = 0,
    // 적합도 높음 + 등급 Ⅱ 이상 좋은 특성이 붙은 행성 수 / 적합도 높음 + 등급 Ⅲ 좋은 특성이 붙은 행성 수
    val highFitTier2Acquired: Int = 0,
    val highFitTier3Acquired: Int = 0
)
