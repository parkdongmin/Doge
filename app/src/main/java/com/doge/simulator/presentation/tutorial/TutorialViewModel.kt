package com.doge.simulator.presentation.tutorial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.data.local.TutorialPrefs
import com.doge.simulator.domain.usecase.GetActiveExpeditionsUseCase
import com.doge.simulator.domain.usecase.GetOwnedPlanetsUseCase
import com.doge.simulator.presentation.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// 첫 진입 튜토리얼의 현재 단계.
sealed interface TutorialStep {
    data object None : TutorialStep

    // Part 1 — 탐사 파견 안내. page 0 = 인트로, page 1 = 카테고리 하이라이트.
    data class Part1(val page: Int) : TutorialStep

    // Part 2 — 첫 행성 획득 후 방치수익 안내.
    data object Part2 : TutorialStep

    // 맥락형 원샷 — 해당 화면 첫 방문 시 1회.
    data object HqIntro : TutorialStep
    data object UpgradeIntro : TutorialStep

    companion object { const val PART1_PAGES = 2 }
}

@HiltViewModel
class TutorialViewModel @Inject constructor(
    getActiveExpeditionsUseCase: GetActiveExpeditionsUseCase,
    getOwnedPlanetsUseCase: GetOwnedPlanetsUseCase,
    private val prefs: TutorialPrefs
) : ViewModel() {

    // prefs가 반응형이 아니라, dismiss 시 이 값을 올려 step 흐름을 다시 계산시킨다.
    private val revision = MutableStateFlow(0)
    private val part1Page = MutableStateFlow(0)

    // MainScreen이 현재 라우트를 밀어넣는다 — 화면별 맥락형 버블 판정에 필요.
    private val currentRoute = MutableStateFlow<String?>(null)
    fun onRouteChanged(route: String?) { currentRoute.value = route }

    val step: StateFlow<TutorialStep> = combine(
        getActiveExpeditionsUseCase(),
        getOwnedPlanetsUseCase(),
        currentRoute,
        part1Page,
        revision
    ) { expeditions, planets, route, page, _ ->
        when {
            // startedFresh = 이 기기에서 완전 신규로 시작(스타터 자산 지급받음).
            // 클라우드 복원 기존 유저는 이 값이 false라 튜토리얼 전체가 안 뜬다.
            !prefs.startedFresh -> TutorialStep.None

            // Part 1: 아직 안 봤고, 아직 탐사를 한 번도 안 보낸 상태 · 탐사 탭에서.
            !prefs.part1Done && expeditions.isEmpty() && route == NavRoutes.Explore.route ->
                TutorialStep.Part1(page.coerceIn(0, TutorialStep.PART1_PAGES - 1))

            // Part 1을 끝내기 전엔 나머지 안내를 얹지 않는다.
            !prefs.part1Done -> TutorialStep.None

            // Part 2: 행성 탭에서. 스타터 행성 덕에 Part 1 직후 바로 뜬다.
            !prefs.part2Done && planets.isNotEmpty() && route == NavRoutes.Planet.route ->
                TutorialStep.Part2

            // 맥락형 원샷 — 해당 화면 첫 방문.
            !prefs.hqIntroDone && route == NavRoutes.HQ.route ->
                TutorialStep.HqIntro

            !prefs.upgradeIntroDone && route == NavRoutes.PlanetDetail.route ->
                TutorialStep.UpgradeIntro

            else -> TutorialStep.None
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TutorialStep.None)

    /** Part 1 인트로 → 카테고리 하이라이트로 진행. */
    fun advancePart1() {
        part1Page.value = (part1Page.value + 1).coerceAtMost(TutorialStep.PART1_PAGES - 1)
    }

    fun dismiss(step: TutorialStep) {
        when (step) {
            is TutorialStep.Part1 -> prefs.part1Done = true
            TutorialStep.Part2 -> prefs.part2Done = true
            TutorialStep.HqIntro -> prefs.hqIntroDone = true
            TutorialStep.UpgradeIntro -> prefs.upgradeIntroDone = true
            TutorialStep.None -> Unit
        }
        revision.value++
    }
}
