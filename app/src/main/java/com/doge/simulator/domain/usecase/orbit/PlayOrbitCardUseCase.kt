package com.doge.simulator.domain.usecase.orbit

import com.doge.simulator.domain.ai.B01Memory
import com.doge.simulator.domain.model.orbit.OrbitCard
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.domain.model.orbit.OrbitRoundState
import com.doge.simulator.domain.model.orbit.PlayerSide
import javax.inject.Inject

// PlayOrbitCardUseCase가 요구하는 카드별 추가 입력. SENSOR/PROBE/WARP_GATE/AI_CORE/CAPTAIN은
// 1:1 대전이라 대상이 상대(또는 자신)로 고정되어 추가 입력이 필요 없다.
sealed class OrbitCardEffectInput {
    data class ScoutGuess(val guessedPower: Int) : OrbitCardEffectInput()
    data class EmpTarget(val target: PlayerSide) : OrbitCardEffectInput()
    data object None : OrbitCardEffectInput()
}

// 턴 1회(이미 드로우해 손패 2장인 상태)에서 카드 한 장을 사용하는 로직 전체.
// 8종 카드 이펙트(FR-005), SHIELD 무효화(FR-006), AI_CORE 강제 사용, CAPTAIN OUT 규칙을 담당한다.
class PlayOrbitCardUseCase @Inject constructor() {

    sealed class Result {
        data class Applied(val summary: PlayedCardSummary) : Result()
        data object InvalidMove : Result()
    }

    data class PlayedCardSummary(
        val by: PlayerSide,
        val card: OrbitCard,
        val outSide: PlayerSide? = null,
        // SENSOR를 "플레이어가" 사용했을 때만 채워진다 — 화면에 일시적으로 보여줄 값(FR-004).
        val revealedOpponentCard: OrbitCard? = null,
        // 대상이 SHIELD로 보호돼 있어 효과가 전혀 적용되지 않았는지(FR-006) — 화면에서
        // "막혔다"는 걸 알려주기 위한 값. 카드 자체는 정상적으로 소모된다.
        val blockedByShield: Boolean = false,
        // SHIELD로 막힌 것도 아닌데 효과가 그냥 안 일어난 경우(PROBE 동점, SCOUT DRONE
        // 오답)의 사유. 이게 없으면 "카드는 냈는데 화면상 아무 일도 안 일어나서" 왜 그런지
        // 알 수 없다는 피드백이 있었음.
        val noEffectNote: String? = null
    )

    operator fun invoke(
        round: OrbitRoundState,
        actingSide: PlayerSide,
        card: OrbitCard,
        input: OrbitCardEffectInput = OrbitCardEffectInput.None,
        b01Memory: B01Memory
    ): Result {
        if (round.isOver || round.currentTurn != actingSide) return Result.InvalidMove
        val actor = round.player(actingSide)
        if (card !in actor.hand) return Result.InvalidMove

        // AI_CORE 강제 사용 규칙: 손에 AI_CORE + (EMP 또는 WARP_GATE)가 함께 있으면 반드시
        // AI_CORE를 내야 한다.
        val mustPlayAiCore = actor.hand.any { it.type == OrbitCardType.AI_CORE } &&
            actor.hand.any { it.type == OrbitCardType.EMP || it.type == OrbitCardType.WARP_GATE }
        if (mustPlayAiCore && card.type != OrbitCardType.AI_CORE) return Result.InvalidMove

        // EMP는 덱에 최소 1장 남아있어야 사용 가능
        if (card.type == OrbitCardType.EMP && round.deck.remainingCount < 1) return Result.InvalidMove

        actor.hand.remove(card)
        actor.cardsUsedThisRound.add(card)
        round.deck.markUsed(card)

        val opponentSide = actingSide.opponent()
        val opponent = round.player(opponentSide)
        val opponentShielded = opponent.shieldActive

        var outSide: PlayerSide? = null
        var revealedOpponentCard: OrbitCard? = null
        var blockedByShield = false
        var noEffectNote: String? = null

        when (card.type) {
            OrbitCardType.SCOUT_DRONE -> {
                if (opponentShielded) {
                    blockedByShield = true
                } else {
                    val guess = (input as? OrbitCardEffectInput.ScoutGuess)?.guessedPower
                    if (guess != null && guess != 1 &&
                        opponent.hand.isNotEmpty() && opponent.hand.first().power == guess
                    ) {
                        opponent.markOut()
                        outSide = opponentSide
                    } else {
                        noEffectNote = "추측이 빗나갔어요"
                    }
                }
            }
            OrbitCardType.SENSOR -> {
                if (opponentShielded) {
                    blockedByShield = true
                } else if (opponent.hand.isNotEmpty()) {
                    val seen = opponent.hand.first()
                    if (actingSide == PlayerSide.B01) {
                        b01Memory.reveal(seen)
                    } else {
                        revealedOpponentCard = seen
                    }
                }
            }
            OrbitCardType.PROBE -> {
                if (opponentShielded) {
                    blockedByShield = true
                } else if (actor.hand.isNotEmpty() && opponent.hand.isNotEmpty()) {
                    val myPower = actor.hand.first().power
                    val oppPower = opponent.hand.first().power
                    when {
                        myPower < oppPower -> { actor.markOut(); outSide = actingSide }
                        oppPower < myPower -> { opponent.markOut(); outSide = opponentSide }
                        else -> noEffectNote = "동점이라 아무 일도 없었어요"
                    }
                }
            }
            OrbitCardType.SHIELD -> {
                actor.shieldActive = true
            }
            OrbitCardType.EMP -> {
                val target = (input as? OrbitCardEffectInput.EmpTarget)?.target ?: opponentSide
                val targetShielded = round.player(target).shieldActive
                if (target != actingSide && targetShielded) {
                    blockedByShield = true
                } else {
                    val targetState = round.player(target)
                    val discarded = targetState.hand.firstOrNull()
                    if (discarded != null) {
                        targetState.hand.remove(discarded)
                        round.deck.markUsed(discarded)
                        if (discarded.type == OrbitCardType.CAPTAIN) {
                            targetState.markOut()
                            outSide = target
                        }
                        val redraw = round.deck.draw()
                        if (redraw != null) targetState.hand.add(redraw)
                        // EMP로 교체된 카드는 완전히 새로 뽑힌 카드라 더 이상 확신할 수 없다.
                        if (target == PlayerSide.PLAYER) b01Memory.forget()
                    }
                }
            }
            OrbitCardType.WARP_GATE -> {
                if (opponentShielded) {
                    blockedByShield = true
                } else if (actor.hand.isNotEmpty() && opponent.hand.isNotEmpty()) {
                    val playerState = round.player(PlayerSide.PLAYER)
                    val b01State = round.player(PlayerSide.B01)
                    val playerCardBefore = playerState.hand.removeAt(0)
                    val b01CardBefore = b01State.hand.removeAt(0)
                    playerState.hand.add(b01CardBefore)
                    b01State.hand.add(playerCardBefore)
                    // 교환은 "버림"이 아니므로 CAPTAIN이 넘어가도 OUT 아님. 스왑 직후 플레이어의
                    // 새 카드(= b01CardBefore)는 B-01 스스로 원래 갖고 있던 카드이므로, 누가 이
                    // 카드를 냈든 B-01은 항상 정확히 안다.
                    b01Memory.reveal(b01CardBefore)
                }
            }
            OrbitCardType.AI_CORE -> Unit
            OrbitCardType.CAPTAIN -> {
                actor.markOut()
                outSide = actingSide
            }
        }

        if (outSide != null) {
            round.finishWithOut(loser = outSide)
        } else {
            round.endTurnAndSwitchIfNotOver()
            // 플레이어가 정상적으로 턴을 마치면(WARP_GATE 제외 — 그 경우는 위에서 이미 정확히
            // 알게 됨) 남은 손패 구성을 더 이상 확신할 수 없다 — "카드가 변경됐는지 여부"를
            // 보수적으로(항상 모른다로) 취급한다.
            if (actingSide == PlayerSide.PLAYER && card.type != OrbitCardType.WARP_GATE) {
                b01Memory.forget()
            }
        }

        return Result.Applied(
            PlayedCardSummary(actingSide, card, outSide, revealedOpponentCard, blockedByShield, noEffectNote)
        )
    }
}
