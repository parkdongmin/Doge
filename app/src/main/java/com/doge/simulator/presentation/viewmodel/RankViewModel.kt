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

    // 상위 50명 목록에 없을 때 — 서버에 전체 순위를 매기는 별도 집계 쿼리가 없어 정확한 순위를
    // 알 수 없다. 이전엔 이 경우 무조건 "51위"로 표시해 실제 순위와 무관하게 늘 같은 값이
    // 뜨는 오해를 줬다 — 대신 "50위 밖"이라고만 표시한다
    private val _isOutsideTopRank = MutableStateFlow(false)
    val isOutsideTopRank: StateFlow<Boolean> = _isOutsideTopRank.asStateFlow()

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
                val uid = myUid
                val foundIndex = uid?.let { u -> list.indexOfFirst { it.uid == u } } ?: -1
                _myRank.value = foundIndex.takeIf { it >= 0 }?.plus(1)
                _isOutsideTopRank.value = uid != null && foundIndex < 0
            } catch (_: Exception) { /* 네트워크 오류 시 조용히 무시 */ }
            finally { _isLoading.value = false }
        }
    }

    /** 랭킹 화면에서 수동 새로고침 */
    fun refresh() = syncAndLoad()
}