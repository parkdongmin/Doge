package com.doge.simulator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.domain.model.ResearchField
import com.doge.simulator.domain.repository.UserRepository
import com.doge.simulator.domain.usecase.GetResearchLabUseCase
import com.doge.simulator.domain.usecase.GetResourcesUseCase
import com.doge.simulator.domain.usecase.UpgradeResearchFieldUseCase
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
class ResearchLabViewModel @Inject constructor(
    private val getResearchLabUseCase: GetResearchLabUseCase,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val upgradeResearchFieldUseCase: UpgradeResearchFieldUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    val researchLab = getResearchLabUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            com.doge.simulator.domain.model.ResearchLab())

    val resources = getResourcesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coins: StateFlow<Long> = userRepository.getCoins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun upgrade(field: ResearchField) {
        viewModelScope.launch {
            when (val result = upgradeResearchFieldUseCase(field)) {
                is UpgradeResearchFieldUseCase.Result.Success ->
                    showMessage("${field.displayName} Lv.${result.newLevel} 달성!")
                UpgradeResearchFieldUseCase.Result.InsufficientCoins ->
                    showMessage("코인이 부족합니다")
                UpgradeResearchFieldUseCase.Result.InsufficientResources ->
                    showMessage("자원이 부족합니다")
            }
        }
    }

    private suspend fun showMessage(msg: String) {
        _message.value = msg
        delay(3000)
        _message.value = null
    }
}
