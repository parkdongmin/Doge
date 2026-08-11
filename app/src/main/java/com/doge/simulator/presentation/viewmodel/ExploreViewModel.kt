package com.doge.simulator.presentation.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.doge.simulator.ads.AdFrequencyGate
import com.doge.simulator.ads.InterstitialAdManager
import com.doge.simulator.data.worker.ExplorationCompleteWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionCategory
import com.doge.simulator.domain.model.ExpeditionStatus
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.model.PlanetType
import com.doge.simulator.domain.repository.ExpeditionRepository
import com.doge.simulator.domain.repository.UserRepository
import com.doge.simulator.domain.usecase.BuyPlanetUseCase
import com.doge.simulator.domain.usecase.CollectProfitUseCase
import com.doge.simulator.domain.usecase.CompleteExpeditionUseCase
import com.doge.simulator.domain.usecase.GeneratePlanetsUseCase
import com.doge.simulator.domain.usecase.GetActiveExpeditionsUseCase
import com.doge.simulator.domain.usecase.GetAstronautsUseCase
import com.doge.simulator.domain.usecase.GetExpeditionReportsUseCase
import com.doge.simulator.domain.usecase.GetOwnedPlanetsUseCase
import com.doge.simulator.domain.usecase.GetResearchLabUseCase
import com.doge.simulator.domain.usecase.GetSpaceshipsUseCase
import com.doge.simulator.domain.usecase.SellPlanetUseCase
import com.doge.simulator.domain.usecase.StartExpeditionUseCase
import com.doge.simulator.domain.repository.ResourceRepository
import com.doge.simulator.domain.repository.StoryRepository
import com.doge.simulator.presentation.screen.explore.ExpeditionCompletionResult
import com.doge.simulator.presentation.screen.explore.ExploreUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val startExpeditionUseCase: StartExpeditionUseCase,
    private val completeExpeditionUseCase: CompleteExpeditionUseCase,
    private val getActiveExpeditionsUseCase: GetActiveExpeditionsUseCase,
    private val getAstronautsUseCase: GetAstronautsUseCase,
    private val getSpaceshipsUseCase: GetSpaceshipsUseCase,
    private val getResearchLabUseCase: GetResearchLabUseCase,
    private val generatePlanetsUseCase: GeneratePlanetsUseCase,
    private val buyPlanetUseCase: BuyPlanetUseCase,
    private val sellPlanetUseCase: SellPlanetUseCase,
    private val getOwnedPlanetsUseCase: GetOwnedPlanetsUseCase,
    private val collectProfitUseCase: CollectProfitUseCase,
    private val userRepository: UserRepository,
    private val resourceRepository: ResourceRepository,
    private val storyRepository: StoryRepository,
    private val expeditionRepository: ExpeditionRepository,
    private val getExpeditionReportsUseCase: GetExpeditionReportsUseCase,
    private val interstitialAdManager: InterstitialAdManager,
    private val adFrequencyGate: AdFrequencyGate,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val coins: StateFlow<Long> = userRepository.getCoins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val activeExpeditions = getActiveExpeditionsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val busyShipIds: StateFlow<Set<String>> = getActiveExpeditionsUseCase()
        .map { expeditions -> expeditions.map { it.spaceshipId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val resources = resourceRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadReportCount: StateFlow<Int> = getExpeditionReportsUseCase.unreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val latestReport = getExpeditionReportsUseCase.all()
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val storyProgress = storyRepository.getProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            com.doge.simulator.domain.model.StoryProgress())

    val ownedPlanets = getOwnedPlanetsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val astronauts = getAstronautsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val spaceships = getSpaceshipsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val researchLab = getResearchLabUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            com.doge.simulator.domain.model.ResearchLab())

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        // 앱 복귀 시점의 최초 수집(오프라인 수익 2배 다이얼로그 포함)은 AppSessionViewModel이 전담하므로
        // 여기서 즉시 1회 수집하지 않는다 — 안 그러면 같은 경과시간이 중복 적립될 수 있다
        viewModelScope.launch {
            userRepository.initialize()
        }
        interstitialAdManager.preload()
        // 60초마다 수익 누적
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000L)
                val planets = getOwnedPlanetsUseCase().first()
                if (planets.isNotEmpty()) collectProfitUseCase(planets)
            }
        }
        // 5초마다 만료된 탐사 자동 완료 (완료 처리만 하고, 결과 표시는 아래 unhandled 결과 관찰에서 담당)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5_000L)
                checkExpiredExpeditions()
            }
        }
        // 완료됐지만 아직 결과를 보여주지 않은 탐사 관찰 — 포그라운드 폴링과 백그라운드 워커
        // 중 어느 쪽이 완료 처리를 선점했는지와 무관하게 결과 팝업이 항상 뜨도록 보장한다
        viewModelScope.launch {
            expeditionRepository.getUnhandledResults().collect { unhandled ->
                val next = unhandled.firstOrNull() ?: return@collect
                if (_uiState.value.completionResult != null) return@collect
                presentExpeditionResult(next)
            }
        }
    }

    private suspend fun checkExpiredExpeditions() {
        val now = System.currentTimeMillis()
        val expired = activeExpeditions.value.filter {
            it.status == ExpeditionStatus.IN_PROGRESS && now >= it.endTime
        }
        expired.forEach { completeExpeditionUseCase(it) }
    }

    private suspend fun presentExpeditionResult(expedition: Expedition) {
        val resources = parseResourcesResult(expedition.resourcesResult)
        var slotFullCoins = 0L
        var isSlotFull = false
        var isDuplicateVariant = false
        var isCurrentlyOwned = false
        val planet = expedition.discoveredPlanetType?.let { typeName ->
            runCatching {
                val lab = getResearchLabUseCase().first()
                val ownedPlanets = getOwnedPlanetsUseCase().first()
                val ownedCount = ownedPlanets.size
                val type = PlanetType.valueOf(typeName)
                val meta = PlanetMetaDataTable.data[type] ?: return@runCatching null
                val production = (meta.productionMin..meta.productionMax).random()
                val risk = (meta.riskMin..meta.riskMax).random()
                val buyPrice = meta.basePrice + production * 20 + risk * 10
                val variantId = meta.variants.random().variantId
                val discoveredPlanet = Planet(
                    type = type,
                    production = production,
                    risk = risk,
                    investment = (meta.investmentMin..meta.investmentMax).random(),
                    eventRate = (meta.eventRateMin..meta.eventRateMax).random(),
                    variantId = variantId,
                    buyPrice = buyPrice,
                    currentValue = buyPrice
                )

                // 도감 등록 여부(전체 발견 기록)와 "지금 슬롯에 실제로 들고 있는지"는 별개 —
                // 화면에 "이미 보유 중이니 비교해보라"는 안내는 후자(isCurrentlyOwned) 기준으로 띄우고,
                // 도감 등록 여부(isDuplicateVariant)는 슬롯 풀일 때 코인 전환가 산정에만 쓴다.
                // production/risk 등 스탯은 매번 새로 롤되므로 같은 행성이라도 이번 롤이 기존 보유분보다
                // 좋을 수 있어, 구매 자체는 신규 발견과 동일하게 슬롯 여유에 따라서만 판단한다
                isDuplicateVariant = variantId in userRepository.getDiscoveredVariantIds().first()
                isCurrentlyOwned = ownedPlanets.any { it.variantId == variantId }
                if (ownedCount < lab.maxPlanetSlots) {
                    discoveredPlanet to true
                } else {
                    // 슬롯이 가득 차 구매할 수 없는 경우: 여기서 바로 확정하지 않고 "코인으로 받기" 또는
                    // "보유 행성 팔고 구매하기" 중 하나를 다이얼로그에서 고르게 함 (도감 등록·코인 지급은
                    // convertSlotFullToCoin에서, 실제 스왑은 buyDiscoveredPlanet에서 처리). 완전 신규
                    // 발견보다 이미 본 스킨의 코인 전환가를 낮게 잡음(30% vs 50%)
                    isSlotFull = true
                    val rate = if (isDuplicateVariant) GameConstants.DUPLICATE_PLANET_VARIANT_COIN_RATE
                               else GameConstants.SLOT_FULL_DISCOVERY_COIN_RATE
                    slotFullCoins = (buyPrice * rate).toLong()
                    discoveredPlanet to false
                }
            }.getOrNull()
        }

        _uiState.update { it.copy(
            completionResult = ExpeditionCompletionResult(
                expeditionId = expedition.id,
                success = expedition.status == ExpeditionStatus.COMPLETED,
                resources = resources,
                coinsEarned = expedition.coinsEarned,
                discoveredPlanet = planet?.first,
                canBuyPlanet = planet?.second ?: false,
                isDuplicateVariant = isDuplicateVariant,
                isCurrentlyOwned = isCurrentlyOwned,
                isSlotFull = isSlotFull,
                slotFullCoins = slotFullCoins
            )
        )}
    }

    private fun parseResourcesResult(raw: String?): Map<com.doge.simulator.domain.model.ResourceType, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val type = runCatching { com.doge.simulator.domain.model.ResourceType.valueOf(parts[0]) }.getOrNull()
                ?: return@mapNotNull null
            val amount = parts[1].toLongOrNull() ?: return@mapNotNull null
            type to amount
        }.toMap()
    }

    // ── 팀 빌더 ──────────────────────────────────────────────────────
    fun openTeamBuilder() = _uiState.update { it.copy(isTeamBuilderOpen = true) }
    fun closeTeamBuilder() = _uiState.update {
        it.copy(isTeamBuilderOpen = false, dispatchError = null,
            selectedAstronautIds = emptySet(), selectedSpaceshipId = null)
    }

    fun selectCategory(category: ExpeditionCategory) =
        _uiState.update { it.copy(selectedCategory = category, selectedAstronautIds = emptySet()) }

    fun selectTier(tier: Int) = _uiState.update { it.copy(selectedTier = tier) }

    fun toggleAstronaut(id: String) = _uiState.update { state ->
        val ids = state.selectedAstronautIds.toMutableSet()
        if (ids.contains(id)) ids.remove(id) else ids.add(id)
        state.copy(selectedAstronautIds = ids)
    }

    fun selectSpaceship(id: String) = _uiState.update { it.copy(selectedSpaceshipId = id) }

    fun dispatch() {
        val state = _uiState.value
        if (state.selectedSpaceshipId == null) {
            _uiState.update { it.copy(dispatchError = "우주선을 선택해주세요") }
            return
        }
        if (state.selectedAstronautIds.isEmpty()) {
            _uiState.update { it.copy(dispatchError = "우주인을 한 명 이상 선택해주세요") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isDispatching = true) }
            val result = startExpeditionUseCase(
                category = state.selectedCategory,
                tier = state.selectedTier,
                astronautIds = state.selectedAstronautIds.toList(),
                spaceshipId = state.selectedSpaceshipId
            )
            when (result) {
                is StartExpeditionUseCase.Result.Success -> {
                    scheduleExpeditionWorker(result.expedition.id, result.expedition.endTime)
                    _uiState.update { it.copy(isTeamBuilderOpen = false, isDispatching = false,
                        selectedAstronautIds = emptySet(), selectedSpaceshipId = null, dispatchError = null) }
                }
                is StartExpeditionUseCase.Result.CategoryLocked ->
                    _uiState.update { it.copy(isDispatching = false, dispatchError = "해당 탐사 카테고리가 잠겨있습니다") }
                is StartExpeditionUseCase.Result.TierRequirementNotMet ->
                    _uiState.update { it.copy(isDispatching = false, dispatchError = "${result.conditionLabel} 보유 필요") }
                is StartExpeditionUseCase.Result.AstronautNotAvailable ->
                    _uiState.update { it.copy(isDispatching = false, dispatchError = "선택한 우주인이 이미 파견 중입니다") }
                is StartExpeditionUseCase.Result.SpaceshipBusy ->
                    _uiState.update { it.copy(isDispatching = false, dispatchError = "선택한 우주선이 이미 탐사 중입니다") }
                is StartExpeditionUseCase.Result.TeamTooLarge ->
                    _uiState.update { it.copy(isDispatching = false, dispatchError = "우주선 탑승 인원 초과입니다") }
                else ->
                    _uiState.update { it.copy(isDispatching = false, dispatchError = "파견에 실패했습니다") }
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

    // ── 결과 처리 ─────────────────────────────────────────────────────
    fun buyDiscoveredPlanet(planet: Planet, activity: Activity) {
        maybeShowInterstitial(activity) { buyDiscoveredPlanetInternal(planet) }
    }

    private fun buyDiscoveredPlanetInternal(planet: Planet) {
        // 광고 콜백을 거쳐 여러 번 호출될 수 있어(연타 시 각각 별도의 광고 흐름을 탐), 실제
        // 구매 진입점인 여기서 원자적으로 재진입을 막는다
        if (_uiState.value.isBuyingPlanet) return
        _uiState.update { it.copy(isBuyingPlanet = true) }
        viewModelScope.launch {
            val bought = buyPlanetUseCase(planet)
            if (bought) {
                val expeditionId = _uiState.value.completionResult?.expeditionId
                _uiState.update { it.copy(completionResult = null, isSwapPickerOpen = false, isBuyingPlanet = false) }
                expeditionId?.let { expeditionRepository.markResultHandled(it) }
            } else {
                _uiState.update { it.copy(isBuyingPlanet = false) }
            }
        }
    }

    fun dismissResult(activity: Activity) {
        maybeShowInterstitial(activity) { dismissResultInternal() }
    }

    private fun dismissResultInternal() {
        val expeditionId = _uiState.value.completionResult?.expeditionId
        _uiState.update { it.copy(completionResult = null, isSwapPickerOpen = false) }
        if (expeditionId != null) {
            viewModelScope.launch { expeditionRepository.markResultHandled(expeditionId) }
        }
    }

    // 슬롯 풀 상태에서 신규 행성을 "코인으로 받기" 선택 시: 도감 등록 + 코인 지급을 이 시점에 확정
    fun convertSlotFullToCoin(activity: Activity) {
        maybeShowInterstitial(activity) { convertSlotFullToCoinInternal() }
    }

    private fun convertSlotFullToCoinInternal() {
        val result = _uiState.value.completionResult ?: return
        val planet = result.discoveredPlanet ?: return
        viewModelScope.launch {
            userRepository.recordVariantDiscovery(planet.variantId)
            userRepository.addCoins(result.slotFullCoins)
            _uiState.update { it.copy(completionResult = null, isSwapPickerOpen = false) }
            expeditionRepository.markResultHandled(result.expeditionId)
        }
    }

    // 탐사 결과 dismiss 시 빈도 제한(첫 N회 제외, 쿨다운)을 통과하면 전면광고를 먼저 보여주고
    // 닫힌 후 실제 dismiss 로직(after)을 실행. 통과 못하면 바로 실행
    private fun maybeShowInterstitial(activity: Activity, after: () -> Unit) {
        adFrequencyGate.recordExpeditionCompleted()
        if (adFrequencyGate.shouldShowInterstitial()) {
            interstitialAdManager.show(activity) {
                adFrequencyGate.recordInterstitialShown()
                after()
            }
        } else {
            after()
        }
    }

    fun openSwapPicker() = _uiState.update { it.copy(isSwapPickerOpen = true) }
    fun closeSwapPicker() = _uiState.update { it.copy(isSwapPickerOpen = false) }

    // 슬롯 풀 상태에서 보유 행성 하나를 매도 (구매는 별개 액션 — "구매하기" 버튼에서 buyDiscoveredPlanet 호출).
    // 한 번에 안 되면 여러 개 팔아 코인을 모은 뒤 구매하기를 누를 수 있도록 분리
    fun sellOwnedPlanet(planetId: String) {
        viewModelScope.launch {
            val owned = getOwnedPlanetsUseCase().first().firstOrNull { it.id == planetId } ?: return@launch
            sellPlanetUseCase(owned)
        }
    }
}
