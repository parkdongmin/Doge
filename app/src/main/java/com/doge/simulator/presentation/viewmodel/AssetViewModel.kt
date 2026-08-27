package com.doge.simulator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.domain.model.Resource
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.model.effectiveProduction
import com.doge.simulator.domain.model.marketValue
import com.doge.simulator.domain.repository.UserRepository
import com.doge.simulator.domain.usecase.GetOwnedPlanetsUseCase
import com.doge.simulator.domain.usecase.GetResourcesUseCase
import com.doge.simulator.domain.usecase.SellResourceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssetUiState(
    val coins: Long = 0L,
    val planetCount: Int = 0,
    val totalMarketValue: Long = 0L,
    val totalProfit: Long = 0L,
    val netProductionPerMin: Long = 0L,
    val resources: List<Resource> = emptyList()
)

@HiltViewModel
class AssetViewModel @Inject constructor(
    getOwnedPlanetsUseCase: GetOwnedPlanetsUseCase,
    getResourcesUseCase: GetResourcesUseCase,
    userRepository: UserRepository,
    private val sellResourceUseCase: SellResourceUseCase
) : ViewModel() {

    val uiState: StateFlow<AssetUiState> = combine(
        userRepository.getCoins(),
        getOwnedPlanetsUseCase(),
        getResourcesUseCase()
    ) { coins, planets, resources ->
        AssetUiState(
            coins = coins,
            planetCount = planets.size,
            totalMarketValue = planets.sumOf { it.marketValue },
            totalProfit = planets.sumOf { it.totalProfit },
            netProductionPerMin = planets.sumOf { it.effectiveProduction },
            resources = resources.filter { it.amount > 0 }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AssetUiState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // 보유한 자원 전량을 코인으로 판매 — 행성이 없어 방치 수익이 없을 때의 최소한의 환금 수단
    fun sellResource(type: ResourceType, amount: Long) {
        viewModelScope.launch {
            when (val result = sellResourceUseCase(type, amount)) {
                is SellResourceUseCase.Result.Success ->
                    showMessage("${type.displayName} 판매 완료: +${"%,d".format(result.coinsEarned)} 코인")
                SellResourceUseCase.Result.InsufficientAmount ->
                    showMessage("판매할 수량이 부족합니다")
            }
        }
    }

    private suspend fun showMessage(msg: String) {
        _message.value = msg
        delay(3000)
        _message.value = null
    }
}
