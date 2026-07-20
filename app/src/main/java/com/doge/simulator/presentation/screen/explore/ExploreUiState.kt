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
    val completionResult: ExpeditionCompletionResult? = null
)

data class ExpeditionCompletionResult(
    val expeditionId: String,
    val success: Boolean,
    val resources: Map<ResourceType, Long>,
    val coinsEarned: Long = 0L,
    val discoveredPlanet: Planet?,
    val canBuyPlanet: Boolean = false,
    // 발견한 스킨(variantId)이 이미 도감에 있어 구매 대신 자동 지급된 코인 (0이면 해당 없음)
    val duplicatePlanetCoins: Long = 0L
)
