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
import com.doge.simulator.data.worker.TrainingCompleteWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.model.RecruitmentPool
import com.doge.simulator.domain.repository.AstronautRepository
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.repository.ResourceRepository
import com.doge.simulator.domain.repository.UserRepository
import com.doge.simulator.domain.usecase.CompleteTrainingUseCase
import com.doge.simulator.domain.usecase.DismissAstronautUseCase
import com.doge.simulator.domain.usecase.EnsureRecruitmentPoolFreshUseCase
import com.doge.simulator.domain.usecase.GetAstronautsUseCase
import com.doge.simulator.domain.usecase.GetRecruitmentPoolUseCase
import com.doge.simulator.domain.usecase.GetResearchLabUseCase
import com.doge.simulator.domain.usecase.HireFromPoolUseCase
import com.doge.simulator.domain.usecase.RefreshRecruitmentPoolUseCase
import com.doge.simulator.domain.usecase.TrainAstronautUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AstronautViewModel @Inject constructor(
    private val getAstronautsUseCase: GetAstronautsUseCase,
    private val getResearchLabUseCase: GetResearchLabUseCase,
    private val getRecruitmentPoolUseCase: GetRecruitmentPoolUseCase,
    private val hireFromPoolUseCase: HireFromPoolUseCase,
    private val ensureRecruitmentPoolFreshUseCase: EnsureRecruitmentPoolFreshUseCase,
    private val refreshRecruitmentPoolUseCase: RefreshRecruitmentPoolUseCase,
    private val trainAstronautUseCase: TrainAstronautUseCase,
    private val completeTrainingUseCase: CompleteTrainingUseCase,
    private val dismissAstronautUseCase: DismissAstronautUseCase,
    private val astronautRepository: AstronautRepository,
    private val userRepository: UserRepository,
    private val resourceRepository: ResourceRepository,
    private val rewardedAdManager: RewardedAdManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val astronauts = getAstronautsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val researchLab = getResearchLabUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            com.doge.simulator.domain.model.ResearchLab())

    val recruitmentPool: StateFlow<RecruitmentPool> = getRecruitmentPoolUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecruitmentPool(emptyList(), 0L))

    val coins: StateFlow<Long> = userRepository.getCoins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // 훈련 버튼 활성 판단·자원 비용 표시용
    val resources: StateFlow<Map<ResourceType, Long>> = resourceRepository.getAll()
        .map { list -> list.associate { it.type to it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch { ensureRecruitmentPoolFreshUseCase() }
        // 훈련 완료 체크: 화면에 들어오자마자 한 번, 이후엔 가장 먼저 끝나는 훈련 시각에 맞춰(최대 1분 간격).
        // 예전엔 "1분 기다린 뒤" 첫 체크를 해서, 백그라운드 워커가 늦게 돌면(기기 절전·앱 재설치 등)
        // 이미 끝난 훈련이 "훈련 완료까지: 0분"인 채로 최대 1분간 멈춰 보였다.
        viewModelScope.launch {
            while (true) {
                val nextEndTime = checkTrainingCompletions()
                val waitMs = nextEndTime
                    ?.let { (it - System.currentTimeMillis()).coerceIn(1_000L, 60_000L) }
                    ?: 60_000L
                delay(waitMs)
            }
        }
        // 1분마다 모집 풀 자동 새로고침 체크
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                ensureRecruitmentPoolFreshUseCase()
            }
        }
        rewardedAdManager.preload(RewardPlacement.POOL_REFRESH)
        rewardedAdManager.preload(RewardPlacement.SKIP_WAIT)
    }

    // 끝난 훈련을 완료 처리하고, 아직 진행 중인 훈련 중 가장 빠른 종료 시각을 돌려준다(없으면 null).
    // astronauts StateFlow는 화면 진입 직후엔 아직 비어 있을 수 있어 저장소에서 직접 읽는다.
    private suspend fun checkTrainingCompletions(): Long? {
        val now = System.currentTimeMillis()
        val training = astronautRepository.getAstronauts().first()
            .filter { it.status == AstronautStatus.TRAINING }
        training
            .filter { (it.trainingEndTime ?: Long.MAX_VALUE) <= now }
            .forEach { completeTrainingUseCase(it) }
        return training.mapNotNull { it.trainingEndTime }.filter { it > now }.minOrNull()
    }

    fun hireFromPool(slotIndex: Int) {
        viewModelScope.launch {
            when (hireFromPoolUseCase(slotIndex)) {
                HireFromPoolUseCase.Result.Success -> showMessage("영입 완료!")
                HireFromPoolUseCase.Result.InsufficientCoins -> showMessage("코인이 부족합니다")
                HireFromPoolUseCase.Result.MaxLimitReached -> showMessage("고용 한도에 도달했습니다")
                HireFromPoolUseCase.Result.SlotEmpty -> showMessage("이미 영입된 후보입니다")
            }
        }
    }

    fun dismiss(astronaut: Astronaut) {
        viewModelScope.launch {
            when (dismissAstronautUseCase(astronaut)) {
                DismissAstronautUseCase.Result.Success -> showMessage("${astronaut.name} 대원을 방출했어요")
                DismissAstronautUseCase.Result.NotIdle -> showMessage("대기 중인 우주인만 방출할 수 있어요")
                DismissAstronautUseCase.Result.LastAstronaut -> showMessage("마지막 우주인은 방출할 수 없어요")
                DismissAstronautUseCase.Result.NotFound -> Unit
            }
        }
    }

    fun refreshPoolWithAd(activity: Activity) {
        rewardedAdManager.show(activity, RewardPlacement.POOL_REFRESH) { result ->
            viewModelScope.launch {
                if (result is RewardedAdResult.Earned) {
                    refreshRecruitmentPoolUseCase()
                    showMessage("모집 풀이 새로고침되었습니다!")
                } else {
                    showMessage("광고를 끝까지 시청해야 새로고침할 수 있어요")
                }
            }
        }
    }

    fun skipTrainingWait(astronaut: Astronaut, activity: Activity) {
        val endTime = astronaut.trainingEndTime ?: return
        rewardedAdManager.show(activity, RewardPlacement.SKIP_WAIT) { result ->
            viewModelScope.launch {
                if (result is RewardedAdResult.Earned) {
                    val now = System.currentTimeMillis()
                    val newEndTime = maxOf(now, endTime - GameConstants.AD_SKIP_MAX_MS)
                    // 광고 시청 도중 훈련이 이미 완료됐다면(status != TRAINING) 반영하지 않음 —
                    // 그렇지 않으면 이미 끝나서 다른 상태로 넘어갔을 수도 있는 우주인을 훈련 중으로
                    // 되살리고 숙련도가 중복 지급될 수 있다
                    val extended = astronautRepository.extendTraining(
                        astronaut.id, newEndTime, astronaut.trainingType
                    )
                    if (extended) {
                        scheduleTrainingWorkerAt(astronaut.id, newEndTime)
                        if (newEndTime <= now) checkTrainingCompletions()
                        showMessage("훈련 대기시간이 단축되었습니다!")
                    } else {
                        showMessage("이미 훈련이 완료되었습니다")
                    }
                } else {
                    showMessage("광고를 끝까지 시청해야 단축할 수 있어요")
                }
            }
        }
    }

    fun train(astronaut: Astronaut, isAdvanced: Boolean) {
        viewModelScope.launch {
            when (trainAstronautUseCase(astronaut, isAdvanced)) {
                TrainAstronautUseCase.Result.Success -> {
                    val duration = if (isAdvanced) GameConstants.ADVANCED_TRAINING_DURATION_MS
                                   else GameConstants.BASIC_TRAINING_DURATION_MS
                    scheduleTrainingWorker(astronaut.id, astronaut.name, duration)
                    showMessage("${if (isAdvanced) "심화" else "기초"} 훈련 시작!")
                }
                TrainAstronautUseCase.Result.InsufficientCoins -> showMessage("코인이 부족합니다")
                TrainAstronautUseCase.Result.InsufficientResources -> showMessage("자원이 부족합니다")
                TrainAstronautUseCase.Result.TrainingSlotFull -> showMessage("훈련 슬롯이 가득 찼습니다")
                TrainAstronautUseCase.Result.AstronautNotIdle -> showMessage("해당 우주인은 현재 사용 중입니다")
                TrainAstronautUseCase.Result.AlreadyAtCap -> showMessage("이미 등급 숙련도 한계에 도달했습니다")
            }
        }
    }

    private fun scheduleTrainingWorker(astronautId: String, name: String, durationMs: Long) {
        val request = OneTimeWorkRequestBuilder<TrainingCompleteWorker>()
            .setInitialDelay(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(TrainingCompleteWorker.KEY_ASTRONAUT_ID to astronautId))
            .addTag("training_$astronautId")
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("training_$astronautId", ExistingWorkPolicy.REPLACE, request)
    }

    private fun scheduleTrainingWorkerAt(astronautId: String, endTime: Long) {
        val delayMs = (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
        scheduleTrainingWorker(astronautId, "", delayMs)
    }

    private suspend fun showMessage(msg: String) {
        _message.value = msg
        delay(3000)
        _message.value = null
    }
}