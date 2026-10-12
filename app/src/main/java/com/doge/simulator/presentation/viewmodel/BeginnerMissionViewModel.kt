package com.doge.simulator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.domain.model.BeginnerMission
import com.doge.simulator.domain.model.BeginnerMissionStatus
import com.doge.simulator.domain.usecase.ClaimBeginnerMissionUseCase
import com.doge.simulator.domain.usecase.GetBeginnerMissionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BeginnerMissionViewModel @Inject constructor(
    getBeginnerMission: GetBeginnerMissionUseCase,
    private val claimBeginnerMission: ClaimBeginnerMissionUseCase
) : ViewModel() {

    val status: StateFlow<BeginnerMissionStatus?> = getBeginnerMission()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 방금 보상을 받은 임무 — 띠에 잠깐 "보상을 받았어요"를 보여준 뒤 다음 임무로 넘어간다
    private val _justClaimed = MutableStateFlow<BeginnerMission?>(null)
    val justClaimed: StateFlow<BeginnerMission?> = _justClaimed.asStateFlow()

    // 마지막 임무 보상까지 받은 순간 — 관제탑 "모두 마쳤어요" 창을 한 번 띄운다
    private val _allDone = MutableStateFlow(false)
    val allDone: StateFlow<Boolean> = _allDone.asStateFlow()
    fun dismissAllDone() { _allDone.value = false }

    private var claimJob: Job? = null

    fun claim() {
        if (claimJob?.isActive == true) return
        claimJob = viewModelScope.launch {
            val claimed = claimBeginnerMission() ?: return@launch
            if (claimed.index == claimed.total - 1) {
                _allDone.value = true
                return@launch
            }
            _justClaimed.value = claimed.mission
            delay(2_500)
            _justClaimed.value = null
        }
    }
}
