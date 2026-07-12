package com.doge.simulator.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.doge.simulator.data.worker.TrainingCompleteWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.model.RecruitmentPool
import com.doge.simulator.domain.repository.UserRepository
import com.doge.simulator.domain.usecase.CompleteTrainingUseCase
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
    private val userRepository: UserRepository,
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

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch { ensureRecruitmentPoolFreshUseCase() }
        // 1분마다 훈련 완료 및 모집 풀 자동 새로고침 체크
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                checkTrainingCompletions()
                ensureRecruitmentPoolFreshUseCase()
            }
        }
    }

    private suspend fun checkTrainingCompletions() {
        val now = System.currentTimeMillis()
        astronauts.value
            .filter { it.status == AstronautStatus.TRAINING && (it.trainingEndTime ?: Long.MAX_VALUE) <= now }
            .forEach { completeTrainingUseCase(it) }
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

    fun refreshPoolWithAd() {
        viewModelScope.launch {
            refreshRecruitmentPoolUseCase()
            showMessage("모집 풀이 새로고침되었습니다!")
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

    private suspend fun showMessage(msg: String) {
        _message.value = msg
        delay(3000)
        _message.value = null
    }
}