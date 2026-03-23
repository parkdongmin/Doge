package com.doge.simulator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.repository.PlanetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlanetViewModel(
    private val repository: PlanetRepository
) : ViewModel() {

    // 초기 탐험용 행성 목록 (ex: 새로 발견된 행성들)
    private val _initialPlanets = MutableStateFlow<List<Planet>>(emptyList())
    val initialPlanets: StateFlow<List<Planet>> = _initialPlanets

    // 내가 구매해서 소유중인 행성 목록
    private val _ownedPlanets = MutableStateFlow<List<Planet>>(emptyList())
    val ownedPlanets: StateFlow<List<Planet>> = _ownedPlanets

    init {
        loadInitialPlanets()
        loadOwnedPlanets()
    }

    // 탐험용 행성 목록 불러오기
    fun loadInitialPlanets() {
        viewModelScope.launch {
            val result = repository.getInitialPlanets()
            _initialPlanets.value = result
        }
    }

    // 보유 행성 목록 불러오기
    fun loadOwnedPlanets() {
        viewModelScope.launch {
            val result = repository.getOwnedPlanets()
            _ownedPlanets.value = result
        }
    }

    // 행성 구매
    fun buyPlanet(planet: Planet) {
        viewModelScope.launch {
            repository.buyPlanet(planet)
            loadOwnedPlanets() // 다시 불러서 갱신
        }
    }

    // 행성 판매
    fun sellPlanet(id: String) {
        viewModelScope.launch {
            repository.sellPlanet(id)
            loadOwnedPlanets()
        }
    }

    // 시세 변동 업데이트
    fun updatePlanetValue(id: String, newValue: Int) {
        viewModelScope.launch {
            repository.updatePlanetValue(id, newValue)
            loadOwnedPlanets()
        }
    }
}