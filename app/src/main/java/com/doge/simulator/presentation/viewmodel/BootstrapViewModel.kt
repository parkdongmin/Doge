package com.doge.simulator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.data.repository.CloudSaveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 스플래시에서 게임 진입 직전에 클라우드 세이브를 1회 내려받아 로컬에 반영한다.
// 실패해도(네트워크 등) 로컬 데이터로 그냥 진입 — 사용자를 막지 않는다.
@HiltViewModel
class BootstrapViewModel @Inject constructor(
    private val cloudSaveManager: CloudSaveManager
) : ViewModel() {

    enum class Phase { IDLE, RESTORING, DONE }

    private val _phase = MutableStateFlow(Phase.IDLE)
    val phase: StateFlow<Phase> = _phase.asStateFlow()

    fun restoreOnce() {
        if (_phase.value != Phase.IDLE) return
        _phase.value = Phase.RESTORING
        viewModelScope.launch {
            runCatching { cloudSaveManager.restore() }
            _phase.value = Phase.DONE
        }
    }
}
