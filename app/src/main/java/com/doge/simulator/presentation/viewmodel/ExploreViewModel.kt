package com.doge.simulator.presentation.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.usecase.BuyPlanetUseCase
import com.doge.simulator.domain.usecase.GeneratePlanetsUseCase
import com.doge.simulator.domain.usecase.GetOwnedPlanetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val generatePlanetsUseCase: GeneratePlanetsUseCase,
    private val buyPlanetUseCase: BuyPlanetUseCase,
    private val getOwnedPlanetsUseCase: GetOwnedPlanetsUseCase
) : ViewModel() {

    val ownedPlanets: StateFlow<List<Planet>> =
        getOwnedPlanetsUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // 행성 구매 로직
    fun buyPlanet(planet: Planet) {
        viewModelScope.launch {
            buyPlanetUseCase(planet)
        }
    }

    // 행성 탐험 로직
    suspend fun generatePlanet(): Planet {
        return generatePlanetsUseCase()
    }

}