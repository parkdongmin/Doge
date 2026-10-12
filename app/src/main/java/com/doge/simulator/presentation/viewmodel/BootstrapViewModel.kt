package com.doge.simulator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.data.repository.CloudRestoreResult
import com.doge.simulator.data.repository.CloudSaveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 스플래시에서 게임 진입 직전에 클라우드 세이브를 1회 내려받아 로컬에 반영한다.
// 실패해도 이 기기에 게임이 있으면 로컬 데이터로 그냥 진입 — 사용자를 막지 않는다.
// 단, 로컬이 비어 있는데(재설치·새 기기·로그아웃 후) 복원에 실패하면 진입을 막는다 — 그대로
// 들어가면 신규 게임이 시드되고, 그 신규 게임이 다음 push 때 이 계정의 기존 세이브를 덮어쓴다(2026-10-12)
@HiltViewModel
class BootstrapViewModel @Inject constructor(
    private val cloudSaveManager: CloudSaveManager
) : ViewModel() {

    enum class Phase {
        IDLE, RESTORING, DONE,
        FAILED_NETWORK,   // 불러오기 실패 + 로컬 비어 있음 → 다시 시도
        NEEDS_UPDATE      // 클라우드 세이브가 이 앱보다 새 버전 + 로컬 비어 있음 → 앱 업데이트 필요
    }

    private val _phase = MutableStateFlow(Phase.IDLE)
    val phase: StateFlow<Phase> = _phase.asStateFlow()

    fun restoreOnce() {
        if (_phase.value != Phase.IDLE) return
        restore()
    }

    fun retry() {
        if (_phase.value != Phase.FAILED_NETWORK && _phase.value != Phase.NEEDS_UPDATE) return
        restore()
    }

    private fun restore() {
        _phase.value = Phase.RESTORING
        viewModelScope.launch {
            val result = runCatching { cloudSaveManager.restore() }.getOrDefault(CloudRestoreResult.FAILED)
            val hasLocalGame = runCatching { cloudSaveManager.hasLocalGame() }.getOrDefault(false)
            _phase.value = when {
                hasLocalGame -> Phase.DONE
                result == CloudRestoreResult.FAILED -> Phase.FAILED_NETWORK
                result == CloudRestoreResult.SKIPPED_VERSION -> Phase.NEEDS_UPDATE
                else -> Phase.DONE // IMPORTED(로컬 채워짐이 정상) · NO_CLOUD(진짜 신규 계정)
            }
        }
    }
}
