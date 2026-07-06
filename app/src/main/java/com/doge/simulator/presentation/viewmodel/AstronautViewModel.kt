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
import com.doge.simulator.domain.model.AstronautSpecialty
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.repository.UserRepository
import com.doge.simulator.domain.usecase.CompleteTrainingUseCase
import com.doge.simulator.domain.usecase.GetAstronautsUseCase
import com.doge.simulator.domain.usecase.GetResearchLabUseCase
import com.doge.simulator.domain.usecase.HireAstronautUseCase
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
    private val hireAstronautUseCase: HireAstronautUseCase,
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

    val coins: StateFlow<Long> = userRepository.getCoins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        // 1분마다 훈련 완료 체크
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                checkTrainingCompletions()
            }
        }
    }

    private suspend fun checkTrainingCompletions() {
        val now = System.currentTimeMillis()
        astronauts.value
            .filter { it.status == AstronautStatus.TRAINING && (it.trainingEndTime ?: Long.MAX_VALUE) <= now }
            .forEach { completeTrainingUseCase(it) }
    }

    fun hire(specialty: AstronautSpecialty) {
        viewModelScope.launch {
            when (hireAstronautUseCase(specialty)) {
                HireAstronautUseCase.Result.Success -> showMessage("${specialty.displayName} 고용 완료!")
                HireAstronautUseCase.Result.InsufficientCoins -> showMessage("코인이 부족합니다")
                HireAstronautUseCase.Result.MaxLimitReached -> showMessage("고용 한도에 도달했습니다")
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
