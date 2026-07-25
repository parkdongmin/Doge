package com.doge.simulator.presentation.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.doge.simulator.ads.RewardPlacement
import com.doge.simulator.ads.RewardedAdManager
import com.doge.simulator.ads.RewardedAdResult
import com.doge.simulator.data.worker.ExplorationCompleteWorker
import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionReport
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.StoryEvent
import com.doge.simulator.domain.repository.ExpeditionRepository
import com.doge.simulator.domain.usecase.ChooseStoryEventUseCase
import com.doge.simulator.domain.usecase.GetActiveExpeditionsUseCase
import com.doge.simulator.domain.usecase.GetAstronautsUseCase
import com.doge.simulator.domain.usecase.GetExpeditionReportsUseCase
import com.doge.simulator.domain.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpeditionHistoryViewModel @Inject constructor(
    private val getReportsUseCase: GetExpeditionReportsUseCase,
    private val chooseEventUseCase: ChooseStoryEventUseCase,
    private val storyRepository: StoryRepository,
    private val getActiveExpeditionsUseCase: GetActiveExpeditionsUseCase,
    private val getAstronautsUseCase: GetAstronautsUseCase,
    private val expeditionRepository: ExpeditionRepository,
    private val rewardedAdManager: RewardedAdManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val activeExpeditions: StateFlow<List<Expedition>> = getActiveExpeditionsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val astronauts: StateFlow<List<Astronaut>> = getAstronautsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<ExpeditionReport>> = getReportsUseCase.all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadReports: StateFlow<List<ExpeditionReport>> = getReportsUseCase.unread()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        rewardedAdManager.preload(RewardPlacement.SKIP_WAIT)
    }

    fun choose(event: StoryEvent, choiceIndex: Int) {
        viewModelScope.launch {
            chooseEventUseCase(event, choiceIndex)
        }
    }

    fun markAsRead(expeditionId: String) {
        viewModelScope.launch {
            storyRepository.markReportAsRead(expeditionId)
        }
    }

    fun skipExpeditionWait(expedition: Expedition, activity: Activity) {
        rewardedAdManager.show(activity, RewardPlacement.SKIP_WAIT) { result ->
            viewModelScope.launch {
                if (result is RewardedAdResult.Earned) {
                    val now = System.currentTimeMillis()
                    val newEndTime = maxOf(now, expedition.endTime - GameConstants.AD_SKIP_MAX_MS)
                    expeditionRepository.updateEndTime(expedition.id, newEndTime)
                    scheduleExpeditionWorker(expedition.id, newEndTime)
                    showActionMessage("탐사 대기시간이 단축되었습니다!")
                } else {
                    showActionMessage("광고를 끝까지 시청해야 단축할 수 있어요")
                }
            }
        }
    }

    private fun scheduleExpeditionWorker(expeditionId: String, endTime: Long) {
        val delay = (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<ExplorationCompleteWorker>()
            .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ExplorationCompleteWorker.KEY_EXPEDITION_ID to expeditionId))
            .addTag("expedition_$expeditionId")
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("expedition_$expeditionId", ExistingWorkPolicy.REPLACE, request)
    }

    private suspend fun showActionMessage(msg: String) {
        _actionMessage.value = msg
        delay(3000)
        _actionMessage.value = null
    }
}
