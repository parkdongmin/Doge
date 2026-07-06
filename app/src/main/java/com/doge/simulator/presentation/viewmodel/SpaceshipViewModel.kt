package com.doge.simulator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.domain.model.Spaceship
import com.doge.simulator.domain.repository.UserRepository
import com.doge.simulator.domain.usecase.BuySpaceshipUseCase
import com.doge.simulator.domain.usecase.GetResearchLabUseCase
import com.doge.simulator.domain.usecase.GetResourcesUseCase
import com.doge.simulator.domain.usecase.GetSpaceshipsUseCase
import com.doge.simulator.domain.usecase.UpgradeSpaceshipUseCase
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
class SpaceshipViewModel @Inject constructor(
    private val getSpaceshipsUseCase: GetSpaceshipsUseCase,
    private val getResearchLabUseCase: GetResearchLabUseCase,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val buySpaceshipUseCase: BuySpaceshipUseCase,
    private val upgradeSpaceshipUseCase: UpgradeSpaceshipUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    val spaceships = getSpaceshipsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val researchLab = getResearchLabUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            com.doge.simulator.domain.model.ResearchLab())

    val resources = getResourcesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coins: StateFlow<Long> = userRepository.getCoins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun buyShip() {
        viewModelScope.launch {
            when (buySpaceshipUseCase()) {
                BuySpaceshipUseCase.Result.Success -> showMessage("정찰선 구매 완료!")
                BuySpaceshipUseCase.Result.InsufficientCoins -> showMessage("코인이 부족합니다")
                BuySpaceshipUseCase.Result.MaxLimitReached -> showMessage("보유 가능한 우주선 수를 초과했습니다")
            }
        }
    }

    fun upgrade(spaceship: Spaceship) {
        viewModelScope.launch {
            when (upgradeSpaceshipUseCase(spaceship)) {
                UpgradeSpaceshipUseCase.Result.Success -> showMessage("${spaceship.name} 강화 완료!")
                UpgradeSpaceshipUseCase.Result.InsufficientCoins -> showMessage("코인이 부족합니다")
                UpgradeSpaceshipUseCase.Result.InsufficientResources -> showMessage("자원이 부족합니다")
            }
        }
    }

    private suspend fun showMessage(msg: String) {
        _message.value = msg
        delay(3000)
        _message.value = null
    }
}
