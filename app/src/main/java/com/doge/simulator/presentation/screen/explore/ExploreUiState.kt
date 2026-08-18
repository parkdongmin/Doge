package com.doge.simulator.presentation.screen.explore

import com.doge.simulator.domain.model.ExpeditionCategory
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.ResourceType

data class ExploreUiState(
    val isTeamBuilderOpen: Boolean = false,
    val selectedCategory: ExpeditionCategory = ExpeditionCategory.MINERAL,
    val selectedTier: Int = 1,
    val selectedAstronautIds: Set<String> = emptySet(),
    val selectedSpaceshipId: String? = null,
    val isDispatching: Boolean = false,
    val dispatchError: String? = null,
    val completionResult: ExpeditionCompletionResult? = null,
    // 발견한 행성이 있을 때만 false로 시작 — 신호 분석 연출이 끝나면 true로 바뀌며 카드가 공개된다.
    // 발견한 행성이 없으면 애초에 스캔 단계를 건너뛰므로 true로 유지
    val discoveryRevealed: Boolean = true,
    // 슬롯 풀 상태에서 신규 행성을 발견했을 때, 보유 행성을 팔고 대신 가질지 고르는 선택창
    val isSwapPickerOpen: Boolean = false,
    // 발견 결과(구매/코인 전환) 처리 중 중복 진입 방지 — 연타 시 코인이 중복 지급되거나
    // 두 번 빠지는 것을 막음. 같은 completionResult를 소비하는 두 액션이 공유하는 플래그
    val isResolvingDiscovery: Boolean = false
)

data class ExpeditionCompletionResult(
    val expeditionId: String,
    val success: Boolean,
    val resources: Map<ResourceType, Long>,
    val coinsEarned: Long = 0L,
    val discoveredPlanet: Planet?,
    val canBuyPlanet: Boolean = false,
    // 발견한 행성(variantId)이 이미 도감(전체 발견 기록)에 있는지 — 슬롯 풀일 때 코인 전환가 산정용.
    // production/risk 등 스탯은 매번 새로 롤되므로 도감에 있어도 구매 자체는 신규와 동일하게 허용한다
    val isDuplicateVariant: Boolean = false,
    // 지금 슬롯에 같은 variantId의 행성을 실제로 보유 중인지 — "이미 보유 중이니 능력치를
    // 비교해보라"는 안내를 띄울지 결정하는 기준 (도감 등록 여부와는 별개)
    val isCurrentlyOwned: Boolean = false,
    // 행성 슬롯이 가득 찬 경우: true면 사용자가 "코인으로 받기" 또는 "보유 행성 팔고 구매하기" 중
    // 하나를 선택해야 함 (slotFullCoins는 코인으로 받을 때 지급될 금액 — 중복이면 더 낮게 책정됨)
    val isSlotFull: Boolean = false,
    val slotFullCoins: Long = 0L
)
