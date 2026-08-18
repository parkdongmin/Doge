package com.doge.simulator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.domain.model.ResearchField
import com.doge.simulator.domain.repository.UserRepository
import com.doge.simulator.domain.usecase.GetResearchLabUseCase
import com.doge.simulator.domain.usecase.GetResourcesUseCase
import com.doge.simulator.domain.usecase.UpgradeResearchFieldUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun upgrade(field: ResearchField) {
        // 결과는 카드 자체(레벨 뱃지·비용 색상·버튼 활성화)가 즉시 반영해 보여주므로 별도 메시지가 불필요.
        // Conflict(연타 충돌, 비용은 자동 환불됨)만 조용히 무시 — 재시도하면 정상 처리된다
        viewModelScope.launch { upgradeResearchFieldUseCase(field) }
    }
}
