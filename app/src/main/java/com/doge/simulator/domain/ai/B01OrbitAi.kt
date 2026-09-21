package com.doge.simulator.domain.ai

import com.doge.simulator.domain.model.orbit.OrbitCard
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.domain.model.orbit.OrbitRoundState
import com.doge.simulator.domain.model.orbit.PlayerSide
import com.doge.simulator.domain.usecase.orbit.OrbitCardEffectInput
import kotlin.random.Random

data class B01Decision(val card: OrbitCard, val input: OrbitCardEffectInput)

// B-01의 턴 판단. 플레이어의 실제 손패는 절대 참조하지 않고(FR-010), 공개 정보(사용된 카드,
// B01Memory에 기록된 확실한 정보, 상대 SHIELD 상태)만 사용한다. mistakeRate 확률로 최적이
// 아닌 대안을 선택해 "가끔 실수하는" 난이도를 구현한다(FR-011).
//
// 참고: 여기서의 "최적"은 완전한 게임 이론적 해가 아니라 단순한 휴리스틱 점수다. ORBIT
// 자체가 커스텀 룰의 새 게임이라 완전 해석(게임 트리 탐색)은 이번 구현 범위를 벗어나며,
// 스펙이 요구하는 것은 "계산은 하되 가끔 대안을 섞는" 체감 난이도이지 증명 가능한 최적
// 플레이가 아니다(spec.md Assumptions 참고).
object B01OrbitAi {

    fun decide(
        round: OrbitRoundState,
        memory: B01Memory,
        mistakeRate: Float,
        random: Random = Random.Default
    ): B01Decision {
        val hand = round.player(PlayerSide.B01).hand
        check(hand.size == 2) { "AI는 드로우 이후(손패 2장)에만 판단한다" }

        val mustPlayAiCore = hand.any { it.type == OrbitCardType.AI_CORE } &&
            hand.any { it.type == OrbitCardType.EMP || it.type == OrbitCardType.WARP_GATE }
        if (mustPlayAiCore) {
            val core = hand.first { it.type == OrbitCardType.AI_CORE }
            return B01Decision(core, OrbitCardEffectInput.None)
        }

        val candidates = hand.map { c -> c to scoreCandidate(round, memory, c) }
        val best = candidates.maxByOrNull { it.second.score }!!
        val chosen = if (candidates.size > 1 && random.nextFloat() < mistakeRate) {
            candidates.first { it.first != best.first }
        } else {
            best
        }
        return B01Decision(chosen.first, chosen.second.input)
    }

    private data class Scored(val score: Int, val input: OrbitCardEffectInput)

    private fun otherCard(hand: List<OrbitCard>, excluding: OrbitCard): OrbitCard {
        val copy = hand.toMutableList()
        copy.remove(excluding)
        return copy.first()
    }

    private fun scoreCandidate(round: OrbitRoundState, memory: B01Memory, card: OrbitCard): Scored {
        val hand = round.player(PlayerSide.B01).hand
        val opponent = round.player(PlayerSide.PLAYER)
        val opponentShielded = opponent.shieldActive

        return when (card.type) {
            OrbitCardType.SCOUT_DRONE -> {
                val guess = bestScoutGuess(round, memory)
                val likelyHit = memory.knownPlayerCard?.power == guess
                Scored(
                    score = if (opponentShielded) 0 else if (likelyHit) 90 else 20,
                    input = OrbitCardEffectInput.ScoutGuess(guess)
                )
            }
            OrbitCardType.SENSOR -> Scored(if (opponentShielded) 0 else 40, OrbitCardEffectInput.None)
            OrbitCardType.PROBE -> {
                val known = memory.knownPlayerCard
                val myOther = otherCard(hand, card)
                val score = when {
                    opponentShielded -> 0
                    known != null && myOther.power > known.power -> 85
                    known != null && myOther.power < known.power -> 5
                    else -> 45
                }
                Scored(score, OrbitCardEffectInput.None)
            }
            OrbitCardType.SHIELD -> Scored(35, OrbitCardEffectInput.None)
            OrbitCardType.EMP -> {
                val known = memory.knownPlayerCard
                val myOther = otherCard(hand, card)
                val target = when {
                    known != null && known.power >= OrbitCardType.WARP_GATE.power -> PlayerSide.PLAYER
                    myOther.type == OrbitCardType.SCOUT_DRONE -> PlayerSide.B01
                    else -> PlayerSide.PLAYER
                }
                val targetShielded = round.player(target).shieldActive
                Scored(if (targetShielded) 5 else 55, OrbitCardEffectInput.EmpTarget(target))
            }
            OrbitCardType.WARP_GATE -> {
                val known = memory.knownPlayerCard
                val myOther = otherCard(hand, card)
                val score = when {
                    opponentShielded -> 0
                    known != null && known.power > myOther.power -> 80
                    known != null -> 15
                    else -> 30
                }
                Scored(score, OrbitCardEffectInput.None)
            }
            OrbitCardType.AI_CORE -> Scored(50, OrbitCardEffectInput.None)
            // CAPTAIN을 직접 내면 즉시 OUT되므로 다른 선택지가 있는 한 최후의 수단으로만 취급.
            OrbitCardType.CAPTAIN -> Scored(1, OrbitCardEffectInput.None)
        }
    }

    private fun bestScoutGuess(round: OrbitRoundState, memory: B01Memory): Int {
        memory.knownPlayerCard?.let { if (it.power != 1) return it.power }
        // 알려진 정보가 없으면, 아직 다 나오지 않았을 가능성이 가장 높은 카드 종류를 추정한다
        // (Power 1인 SCOUT_DRONE 자신은 지목 대상에서 제외).
        val used = round.deck.usedCardsSnapshot().map { it.type }
        val selfHandTypes = round.player(PlayerSide.B01).hand.map { it.type }
        val remainingByType = OrbitCardType.entries
            .filter { it != OrbitCardType.SCOUT_DRONE }
            .associateWith { type ->
                type.count - used.count { it == type } - selfHandTypes.count { it == type }
            }
        val guessType = remainingByType.filterValues { it > 0 }.maxByOrNull { it.value }?.key
            ?: OrbitCardType.CAPTAIN
        return guessType.power
    }
}
