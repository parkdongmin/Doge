package com.doge.simulator.presentation.screen.explore

import com.doge.simulator.domain.model.Planet

data class ExploreUiState(
    val planets: List<Planet> = emptyList(), // 표시할 행성 리스트
    val isLoading: Boolean = false,          // 로딩중 여부
    val error: String? = null                // 에러 메세지
)