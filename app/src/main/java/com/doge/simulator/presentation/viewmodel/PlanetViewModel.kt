package com.doge.simulator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.repository.UserRepository
import com.doge.simulator.domain.usecase.GetOwnedPlanetsUseCase
import com.doge.simulator.domain.usecase.GetResourcesUseCase
import com.doge.simulator.domain.usecase.SellPlanetUseCase
import com.doge.simulator.domain.usecase.UpgradePlanetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlanetViewModel @Inject constructor(
    getOwnedPlanetsUseCase: GetOwnedPlanetsUseCase,
    private val sellPlanetUseCase: SellPlanetUseCase,
    private val upgradePlanetUseCase: UpgradePlanetUseCase,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    val planets = getOwnedPlanetsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoveredVariantIds = userRepository.getDiscoveredVariantIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val coins: StateFlow<Long> = userRepository.getCoins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val resources = getResourcesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _upgradeMessage = MutableStateFlow<String?>(null)
    val upgradeMessage: StateFlow<String?> = _upgradeMessage.asStateFlow()

    fun sellPlanet(planet: Planet) {
        viewModelScope.launch { sellPlanetUseCase(planet) }
    }

    fun upgradePlanet(planet: Planet) {
        viewModelScope.launch {
            val msg = when (val result = upgradePlanetUseCase(planet)) {
                is UpgradePlanetUseCase.Result.Success ->
                    "🎉 강화 성공! Lv.${result.newLevel}"
                is UpgradePlanetUseCase.Result.Failed ->
                    if (result.levelDropped) "💥 강화 실패 — Lv.${result.currentLevel}으로 하락"
                    else "😞 강화 실패 — 레벨 유지"
                UpgradePlanetUseCase.Result.MaxLevel -> "이미 최대 레벨입니다"
                UpgradePlanetUseCase.Result.InsufficientCoins -> "코인이 부족합니다"
                UpgradePlanetUseCase.Result.InsufficientResources -> "자원이 부족합니다"
            }
            _upgradeMessage.value = msg
            delay(3000)
            _upgradeMessage.value = null
        }
    }
}
