package com.doge.simulator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.domain.model.LeaderboardEntry
import com.doge.simulator.domain.repository.AuthRepository
import com.doge.simulator.domain.repository.LeaderboardRepository
import com.doge.simulator.domain.usecase.SyncLeaderboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RankViewModel @Inject constructor(
    private val leaderboardRepository: LeaderboardRepository,
    private val syncLeaderboardUseCase: SyncLeaderboardUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val entries: StateFlow<List<LeaderboardEntry>> = _entries.asStateFlow()

    private val _myRank = MutableStateFlow<Int?>(null)
    val myRank: StateFlow<Int?> = _myRank.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val myUid: String? get() = authRepository.getCurrentUser()?.uid

    /** 홈/랭킹 화면 진입 시: 내 점수 동기화 후 목록 새로고침 */
    fun syncAndLoad() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                syncLeaderboardUseCase()       // 내 점수 Firestore 업데이트
                val list = leaderboardRepository.getTopEntries(50)
                _entries.value = list
                myUid?.let { uid ->
                    _myRank.value = list.indexOfFirst { it.uid == uid }
                        .takeIf { it >= 0 }
                        ?.plus(1)
                        ?: (list.size + 1)
                }
            } catch (_: Exception) { /* 네트워크 오류 시 조용히 무시 */ }
            finally { _isLoading.value = false }
        }
    }

    /** 랭킹 화면에서 수동 새로고침 */
    fun refresh() = syncAndLoad()
}