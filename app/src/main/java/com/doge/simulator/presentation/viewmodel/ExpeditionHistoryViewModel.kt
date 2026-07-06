package com.doge.simulator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionReport
import com.doge.simulator.domain.model.StoryEvent
import com.doge.simulator.domain.usecase.ChooseStoryEventUseCase
import com.doge.simulator.domain.usecase.GetActiveExpeditionsUseCase
import com.doge.simulator.domain.usecase.GetAstronautsUseCase
import com.doge.simulator.domain.usecase.GetExpeditionReportsUseCase
import com.doge.simulator.domain.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpeditionHistoryViewModel @Inject constructor(
    private val getReportsUseCase: GetExpeditionReportsUseCase,
    private val chooseEventUseCase: ChooseStoryEventUseCase,
    private val storyRepository: StoryRepository,
    private val getActiveExpeditionsUseCase: GetActiveExpeditionsUseCase,
    private val getAstronautsUseCase: GetAstronautsUseCase
) : ViewModel() {

    val activeExpeditions: StateFlow<List<Expedition>> = getActiveExpeditionsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val astronauts: StateFlow<List<Astronaut>> = getAstronautsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<ExpeditionReport>> = getReportsUseCase.all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadReports: StateFlow<List<ExpeditionReport>> = getReportsUseCase.unread()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
}
