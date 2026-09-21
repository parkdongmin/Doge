package com.doge.simulator.domain.model.orbit

import kotlin.random.Random

// 매치 내 한 라운드의 진행 상태. deck/players는 도메인 내부에서 직접 조작되며, 이 클래스
// 자체는 Android 의존이 없는 순수 Kotlin이라 유닛 테스트에서 자유롭게 구성할 수 있다.
class OrbitRoundState internal constructor(
    val deck: OrbitDeck,
    val players: Map<PlayerSide, OrbitPlayerState>,
    val firstPlayerOfRound: PlayerSide
) {
    var currentTurn: PlayerSide = firstPlayerOfRound
        private set

    // 이번 턴에 이미 드로우했는지 여부. PlayOrbitCardUseCase가 InvalidMove를 반환하면(예:
    // 덱이 비어 EMP를 쓸 수 없는 상황) 턴이 넘어가지 않는데, 이때 호출자가 "아직 이 사람
    // 턴이네" 하고 drawForCurrentPlayer()를 또 부르면 한 턴에 카드를 여러 장 뽑아 손패가
    // 2장이라는 불변식이 깨진다(B01OrbitAi.decide()의 check(hand.size==2)가 터져 앱이
    // 죽는 원인이었음). 이 플래그로 같은 턴에서의 중복 드로우를 막는다.
    var hasDrawnThisTurn: Boolean = false
        private set

    var endReason: RoundEndReason? = null
        private set

    var winner: PlayerSide? = null
        private set

    val isOver: Boolean get() = endReason != null

    fun player(side: PlayerSide): OrbitPlayerState = players.getValue(side)

    // 자기 턴에 드로우한다. SHIELD는 "자신의 다음 턴이 시작될 때까지" 유지되므로, 이 시점에
    // 만료시킨다. 덱이 비어 있으면 false를 반환하고 호출자는 즉시 덱 소진 판정으로 넘어가야
    // 한다(FR-008). 이미 이번 턴에 드로우했다면 다시 뽑지 않고 true를 반환한다.
    fun drawForCurrentPlayer(): Boolean {
        if (hasDrawnThisTurn) return true
        val actor = player(currentTurn)
        actor.shieldActive = false
        val card = deck.draw() ?: return false
        actor.hand.add(card)
        hasDrawnThisTurn = true
        return true
    }

    fun endTurnAndSwitchIfNotOver() {
        if (isOver) return
        currentTurn = currentTurn.opponent()
        hasDrawnThisTurn = false
    }

    fun finishWithOut(loser: PlayerSide) {
        endReason = RoundEndReason.OUT
        winner = loser.opponent()
    }

    fun finishDeckExhausted(winner: PlayerSide) {
        endReason = RoundEndReason.DECK_EXHAUSTED
        this.winner = winner
    }

    fun finishDraw() {
        endReason = RoundEndReason.DRAW
        winner = null
    }

    companion object {
        fun start(firstPlayerOfRound: PlayerSide, random: Random = Random.Default): OrbitRoundState {
            val deck = OrbitDeck.newShuffled(random)
            val players = mapOf(PlayerSide.PLAYER to OrbitPlayerState(), PlayerSide.B01 to OrbitPlayerState())
            // 각자 1장씩 배분 — 나머지 15장이 드로우 덱이 된다.
            players.getValue(PlayerSide.PLAYER).hand.add(requireNotNull(deck.draw()))
            players.getValue(PlayerSide.B01).hand.add(requireNotNull(deck.draw()))
            return OrbitRoundState(deck, players, firstPlayerOfRound)
        }

        // 테스트 전용: 정해진 손패/잔여 덱으로 라운드를 직접 구성한다.
        fun forTest(
            playerHand: List<OrbitCardType>,
            b01Hand: List<OrbitCardType>,
            remainingDeck: List<OrbitCardType> = emptyList(),
            turn: PlayerSide = PlayerSide.PLAYER
        ): OrbitRoundState {
            val deck = OrbitDeck.forTest(remainingDeck)
            val players = mapOf(
                PlayerSide.PLAYER to OrbitPlayerState().apply { hand.addAll(playerHand.map { OrbitCard(it) }) },
                PlayerSide.B01 to OrbitPlayerState().apply { hand.addAll(b01Hand.map { OrbitCard(it) }) }
            )
            return OrbitRoundState(deck, players, firstPlayerOfRound = turn)
        }
    }
}
