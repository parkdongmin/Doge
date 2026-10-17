package com.doge.simulator.presentation.tutorial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.data.local.TutorialPrefs
import com.doge.simulator.domain.model.Part2StoryContent
import com.doge.simulator.domain.repository.StoryRepository
import com.doge.simulator.domain.usecase.GetActiveExpeditionsUseCase
import com.doge.simulator.domain.usecase.GetOwnedPlanetsUseCase
import com.doge.simulator.presentation.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    // 스토리 2부 안내 — 1부 완주 뒤 탐사 탭에서 한 번. page 0~4 = 관제탑 안내 5줄(Part2StoryContent.part2IntroLines)
    data class Part2StoryIntro(val page: Int) : TutorialStep

    companion object { const val PART1_PAGES = 2 }
}

private data class TutorialFlags(
    val startedFresh: Boolean,
    val part1Done: Boolean,
    val part2Done: Boolean,
    val hqIntroDone: Boolean,
    val upgradeIntroDone: Boolean,
)

@HiltViewModel
class TutorialViewModel @Inject constructor(
    getActiveExpeditionsUseCase: GetActiveExpeditionsUseCase,
    getOwnedPlanetsUseCase: GetOwnedPlanetsUseCase,
    private val prefs: TutorialPrefs,
    private val storyRepository: StoryRepository
) : ViewModel() {

    private val part1Page = MutableStateFlow(0)
    private val part2StoryPage = MutableStateFlow(0)

    // 2부 안내를 띄울 때인지 — 1부를 완주했고, 아직 안 봤고, 1부 완주 기록을 이미 읽었을 때(기록을 읽기 전에
    // 안내가 먼저 나오면 엔딩의 반전을 관제탑이 먼저 말해 버린다). "봤음"은 기기 설정이 아니라 게임 데이터에
    // 저장해 기기를 바꾸거나 클라우드에서 복원해도 다시 뜨지 않는다
    private val part2IntroDue = combine(
        storyRepository.getProgress(),
        storyRepository.getUnreadReports()
    ) { progress, unread ->
        progress.part1Completed && !progress.part2IntroShown && unread.none { it.isStoryEnding }
    }

    // MainScreen이 현재 라우트를 밀어넣는다 — 화면별 맥락형 버블 판정에 필요.
    private val currentRoute = MutableStateFlow<String?>(null)
    fun onRouteChanged(route: String?) { currentRoute.value = route }

    private val flags = combine(
        prefs.startedFresh, prefs.part1Done, prefs.part2Done, prefs.hqIntroDone, prefs.upgradeIntroDone
    ) { fresh, p1, p2, hq, up -> TutorialFlags(fresh, p1, p2, hq, up) }

    private val baseStep = combine(
        flags,
        getActiveExpeditionsUseCase(),
        getOwnedPlanetsUseCase(),
        currentRoute,
        part1Page
    ) { f, expeditions, planets, route, page ->
        when {
            // startedFresh = 이 기기에서 완전 신규로 시작(스타터 자산 지급받음).
            // 클라우드 복원 기존 유저는 이 값이 false라 튜토리얼 전체가 안 뜬다.
            !f.startedFresh -> TutorialStep.None

            // Part 1: 아직 안 봤고, 아직 탐사를 한 번도 안 보낸 상태 · 탐사 탭에서.
            !f.part1Done && expeditions.isEmpty() && route == NavRoutes.Explore.route ->
                TutorialStep.Part1(page.coerceIn(0, TutorialStep.PART1_PAGES - 1))

            // Part 1을 끝내기 전엔 나머지 안내를 얹지 않는다.
            !f.part1Done -> TutorialStep.None

            // Part 2: 행성 탭에서. 스타터 행성 덕에 Part 1 직후 바로 뜬다.
            !f.part2Done && planets.isNotEmpty() && route == NavRoutes.Planet.route ->
                TutorialStep.Part2

            // 맥락형 원샷 — 해당 화면 첫 방문.
            !f.hqIntroDone && route == NavRoutes.HQ.route ->
                TutorialStep.HqIntro

            !f.upgradeIntroDone && route == NavRoutes.PlanetDetail.route ->
                TutorialStep.UpgradeIntro

            else -> TutorialStep.None
        }
    }

    // 2부 안내는 첫 진입 튜토리얼(startedFresh 기준)과 무관하게 판정한다 — 클라우드에서 복원한 기존 플레이어는
    // startedFresh가 false라 첫 진입 튜토리얼이 통째로 꺼져 있다
    val step: StateFlow<TutorialStep> = combine(baseStep, part2IntroDue, currentRoute, part2StoryPage) { base, due, route, page ->
        if (due && route == NavRoutes.Explore.route) TutorialStep.Part2StoryIntro(page.coerceIn(0, Part2StoryContent.part2IntroLines.lastIndex))
        else base
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TutorialStep.None)

    /** Part 1 인트로 → 카테고리 하이라이트로 진행. */
    fun advancePart1() {
        part1Page.value = (part1Page.value + 1).coerceAtMost(TutorialStep.PART1_PAGES - 1)
    }

    fun advancePart2StoryIntro() {
        part2StoryPage.value = (part2StoryPage.value + 1).coerceAtMost(Part2StoryContent.part2IntroLines.lastIndex)
    }

    fun dismiss(step: TutorialStep) {
        when (step) {
            is TutorialStep.Part2StoryIntro -> viewModelScope.launch { storyRepository.markPart2IntroShown() }
            is TutorialStep.Part1 -> prefs.markPart1Done()
            TutorialStep.Part2 -> prefs.markPart2Done()
            TutorialStep.HqIntro -> prefs.markHqIntroDone()
            TutorialStep.UpgradeIntro -> prefs.markUpgradeIntroDone()
            TutorialStep.None -> Unit
        }
    }
}
