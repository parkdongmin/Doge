package com.doge.simulator.domain.model.orbit

import kotlin.random.Random

// 매치 상태: 여러 라운드 + SIGNAL 누적. bet은 베팅 없이도(US1 단독 테스트) 매치를 시작할 수
// 있도록 nullable로 둔다 — 실제 베팅 플레이에서는 StartOrbitMatchUseCase가 채워 넣는다.
class OrbitMatchState private constructor(
    private val random: Random,
    firstPlayerOfFirstRound: PlayerSide,
    var bet: OrbitBet?
) {
    val signals: MutableMap<PlayerSide, Int> = mutableMapOf(PlayerSide.PLAYER to 0, PlayerSide.B01 to 0)

    var currentRound: OrbitRoundState = OrbitRoundState.start(firstPlayerOfFirstRound, random)
        internal set // internal: 유닛 테스트에서 SIGNAL 누적/매치 종료 로직을 특정 라운드
        // 결과로 직접 검증할 수 있게 하기 위함. 실제 게임 흐름에서는 onRoundEnded() 내부에서만 갱신된다.

    var matchResult: MatchOutcome? = null
        private set

    val isOver: Boolean get() = matchResult != null

    // 라운드가 종료된 뒤(round.isOver == true) 호출한다: SIGNAL 반영 + 매치 종료 판정 +
    // (매치가 안 끝났다면) 다음 라운드 시작.
    fun onRoundEnded() {
        val round = currentRound
        check(round.isOver) { "라운드가 끝나지 않은 상태에서 onRoundEnded를 호출할 수 없다" }
        val roundWinner = round.winner
        if (roundWinner != null) {
            signals[roundWinner] = (signals[roundWinner] ?: 0) + 1
        }
        when {
            (signals[PlayerSide.PLAYER] ?: 0) >= SIGNALS_TO_WIN -> matchResult = MatchOutcome.WON
            (signals[PlayerSide.B01] ?: 0) >= SIGNALS_TO_WIN -> matchResult = MatchOutcome.LOST
            else -> {
                // 다음 라운드 선공 = 직전 라운드 패자. DRAW(패자 없음)는 직전 선공을 유지한다
                // — 스펙에 명시되지 않은 절차적 세부사항에 대한 합리적 기본값.
                val nextFirstPlayer = roundWinner?.opponent() ?: round.firstPlayerOfRound
                currentRound = OrbitRoundState.start(nextFirstPlayer, random)
            }
        }
    }

    // 매치 도중 이탈: 패배로 즉시 확정한다(FR-016). 이미 종료된 매치에는 영향 없음.
    fun abandon() {
        if (isOver) return
        matchResult = MatchOutcome.LOST
    }

    companion object {
        const val SIGNALS_TO_WIN = 3

        fun start(random: Random = Random.Default, bet: OrbitBet? = null): OrbitMatchState {
            val firstPlayer = if (random.nextBoolean()) PlayerSide.PLAYER else PlayerSide.B01
            return OrbitMatchState(random, firstPlayer, bet)
        }
    }
}
