package com.doge.simulator.domain.model

// 행성 시세/생산 이벤트가 실제로 발생했을 때(오프라인 중 정산 포함) 남기는 기록.
// "소식" 탭에서 최근 24시간 내역만 모아서 보여준다
data class PlanetEventLog(
    val id: Long = 0L,
    val planetId: String,
    val planetDisplayName: String,
    val planetVariantCode: String,
    val isPositive: Boolean,
    val flavorText: String,
    // 이번 이벤트 하나만으로 바뀐 시간당 생산량(코인/시) — 퍼센트가 아니라 코인으로 바로 보여줘서
    // 암산 없이 읽히게 함. 100%를 넘나드는 값도 자연스럽게 표현 가능
    val productionDeltaPerHour: Long,
    val marketDelta: Long,
    val occurredAt: Long
)
