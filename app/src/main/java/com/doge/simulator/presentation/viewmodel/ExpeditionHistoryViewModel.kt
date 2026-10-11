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
import com.doge.simulator.ads.SkipWaitAdGate
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.ResearchLabRepository
import com.doge.simulator.domain.repository.SpaceshipRepository
import com.doge.simulator.domain.repository.UserRepository
import com.doge.simulator.domain.usecase.SkipExpeditionWithCoinsUseCase
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val skipWaitAdGate: SkipWaitAdGate,
    private val skipExpeditionWithCoinsUseCase: SkipExpeditionWithCoinsUseCase,
    spaceshipRepository: SpaceshipRepository,
    researchLabRepository: ResearchLabRepository,
    planetRepository: PlanetRepository,
    userRepository: UserRepository,
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

    // 오늘 남은 광고 단축 횟수(우주인 센터 훈련 단축과 공유)
    val skipAdsRemaining: StateFlow<Int> = skipWaitAdGate.remaining

    val coins: StateFlow<Long> = userRepository.getCoins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // 진행 중 탐사별 코인 단축 분당 단가. 남은 시간은 화면이 매초 세니 비용은 화면에서 coinSkipCost로 계산
    val coinSkipUnits: StateFlow<Map<String, Double>> = combine(
        getActiveExpeditionsUseCase(),
        spaceshipRepository.getSpaceships(),
        getAstronautsUseCase(),
        researchLabRepository.get(),
        planetRepository.getOwnedPlanets()
    ) { expeditions, ships, crew, lab, planets ->
        expeditions.associate { it.id to skipExpeditionWithCoinsUseCase.unitPerMinute(it, ships, crew, lab, planets) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        rewardedAdManager.preload(RewardPlacement.SKIP_WAIT)
        skipWaitAdGate.refresh()
    }

    fun skipExpeditionWithCoins(expedition: Expedition, quotedCost: Long) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            when (val result = skipExpeditionWithCoinsUseCase(expedition.id, quotedCost, now)) {
                is SkipExpeditionWithCoinsUseCase.Result.Success -> {
                    scheduleExpeditionWorker(expedition.id, now)
                    showActionMessage("${"%,d".format(result.cost)}코인으로 탐사를 마쳤어요")
                }
                SkipExpeditionWithCoinsUseCase.Result.InsufficientCoins -> showActionMessage("코인이 부족해요")
                SkipExpeditionWithCoinsUseCase.Result.AlreadyFinished -> showActionMessage("이미 끝난 탐사예요")
            }
        }
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

    fun refreshSkipAdsRemaining() = skipWaitAdGate.refresh()

    fun skipExpeditionWait(expedition: Expedition, activity: Activity) {
        skipWaitAdGate.refresh()
        if (skipWaitAdGate.remaining.value <= 0) {
            viewModelScope.launch { showActionMessage("오늘 광고 단축을 모두 썼어요") }
            return
        }
        rewardedAdManager.show(activity, RewardPlacement.SKIP_WAIT) { result ->
            viewModelScope.launch {
                if (result is RewardedAdResult.Earned) {
                    val now = System.currentTimeMillis()
                    // 광고를 보는 사이 탐사가 이미 끝났으면 실제로 줄인 게 없으니 하루 횟수도 차감하지 않는다
                    // (훈련 단축과 같은 기준). 당길 시각은 DB의 최신 종료 시각 기준으로 계산
                    val shortened = expeditionRepository.shortenWaitBy(expedition.id, GameConstants.AD_SKIP_MAX_MS, now)
                    if (shortened) {
                        skipWaitAdGate.recordUse()
                        val newEndTime = expeditionRepository.getActive().first()
                            .firstOrNull { it.id == expedition.id }?.endTime ?: now
                        scheduleExpeditionWorker(expedition.id, newEndTime)
                        showActionMessage("탐사 대기시간이 단축되었습니다!")
                    } else {
                        showActionMessage("이미 끝난 탐사예요")
                    }
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
