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
// 점수는 "이번 라운드를 이길 가능성" 단위(100 = 확실한 승리, −100 = 확실한 패배)로 맞춘다.
// 낸 카드의 효과 + 손에 남기는 카드의 가치를 더해 두 후보를 비교한다. 상대 카드는 기억이 있으면
// 그 카드로, 없으면 "아직 안 나온 카드"(전체 − 사용됨 − 내 손패)에서 균등하게 뽑혔다고 본다.
//
// 예전엔 카드마다 고정 점수(PROBE 45, WARP 30 등)라 내 남은 카드를 거의 안 봤다 — SCOUT+PROBE에서
// PROBE를 내 Power 1로 비교해 자멸하거나, WARP로 SCOUT를 넘겨줘 상대가 그걸로 B-01 카드를 맞히는
// 판단이 나왔다(고위험에서도 "실수가 잦다"는 피드백의 원인).
object B01OrbitAi {

    // 실수라도 최선보다 이만큼 이상 나쁜 수는 고르지 않는다 — "가끔 최선이 아닌 수"와 "자살"은 다르다.
    // 난이도마다 따로 준다(OrbitRiskTier.aiMaxMistakeGap): 안정은 눈에 보이는 실수도, 고위험은 거의 차이 없는 수 사이에서만
    const val DEFAULT_MAX_MISTAKE_GAP = 60.0

    fun decide(
        round: OrbitRoundState,
        memory: B01Memory,
        mistakeRate: Float,
        random: Random = Random.Default,
        maxMistakeGap: Double = DEFAULT_MAX_MISTAKE_GAP,
        // 시뮬레이션에서 같은 판단으로 플레이어 쪽을 두게 할 때만 바꾼다. 게임에선 항상 B01
        self: PlayerSide = PlayerSide.B01
    ): B01Decision {
        val hand = round.player(self).hand
        check(hand.size == 2) { "AI는 드로우 이후(손패 2장)에만 판단한다" }

        val mustPlayAiCore = hand.any { it.type == OrbitCardType.AI_CORE } &&
            hand.any { it.type == OrbitCardType.EMP || it.type == OrbitCardType.WARP_GATE }
        if (mustPlayAiCore) {
            val core = hand.first { it.type == OrbitCardType.AI_CORE }
            return B01Decision(core, OrbitCardEffectInput.None)
        }

        // CAPTAIN은 내는 즉시 자멸(OUT)이라 다른 카드가 하나라도 있으면 후보에서 뺀다.
        // (덱이 빈 EMP도 이제 효과 없이 낼 수 있어 AI_CORE 강제 외엔 손패 두 장 모두 합법 수다)
        val candidatePool = hand.filterNot { it.type == OrbitCardType.CAPTAIN }.ifEmpty { hand }
        val odds = opponentCardOdds(round, memory, self)
        val candidates = candidatePool.map { c -> c to scoreCandidate(round, odds, memory, c, self) }
        // 인덱스로 "최적"과 "그 외"를 구분한다 — OrbitCard는 type만으로 동등성을 판단하는
        // data class라 같은 종류 2장이면 값 비교로는 구분할 수 없다(실기기 크래시 원인).
        val bestIndex = candidates.indices.maxByOrNull { candidates[it].second.score }!!
        val alternativeIndex = candidates.indices.firstOrNull { it != bestIndex }
        val chosenIndex = if (
            alternativeIndex != null &&
            candidates[bestIndex].second.score - candidates[alternativeIndex].second.score < maxMistakeGap &&
            random.nextFloat() < mistakeRate
        ) alternativeIndex else bestIndex
        val chosen = candidates[chosenIndex]
        return B01Decision(chosen.first, chosen.second.input)
    }

    private data class Scored(val score: Double, val input: OrbitCardEffectInput)

    private fun otherCard(hand: List<OrbitCard>, excluding: OrbitCard): OrbitCard {
        val copy = hand.toMutableList()
        copy.remove(excluding)
        return copy.first()
    }

    // 상대 손패의 카드 종류별 확률. 기억이 있으면 확정, 없으면 아직 안 보인 카드에서 균등
    private fun opponentCardOdds(round: OrbitRoundState, memory: B01Memory, self: PlayerSide): Map<OrbitCardType, Double> {
        memory.knownPlayerCard?.let { return mapOf(it.type to 1.0) }
        return unseenOdds(round, self)
    }

    // 전체 − 사용된 카드 − B-01 손패. 상대 손패와 드로우 더미가 모두 여기서 나온다
    private fun unseenOdds(round: OrbitRoundState, self: PlayerSide): Map<OrbitCardType, Double> {
        val used = round.deck.usedCardsSnapshot().map { it.type }
        val mine = round.player(self).hand.map { it.type }
        val counts = OrbitCardType.entries.associateWith { type ->
            (type.count - used.count { it == type } - mine.count { it == type }).coerceAtLeast(0)
        }.filterValues { it > 0 }
        val total = counts.values.sum().toDouble()
        if (total <= 0.0) return emptyMap()
        return counts.mapValues { it.value / total }
    }

    private fun Map<OrbitCardType, Double>.probability(predicate: (OrbitCardType) -> Boolean): Double =
        entries.filter { predicate(it.key) }.sumOf { it.value }

    // 손에 남기는 카드의 가치 — 덱이 다 떨어지면 남은 카드 Power로 승부가 나므로, 덱이 1장 이하면
    // (상대가 마지막 장을 뽑고 나면 바로 비교) 높은 카드를 쥐는 게 훨씬 중요해진다
    private fun keepValue(round: OrbitRoundState, card: OrbitCardType): Double {
        val weight = if (round.deck.remainingCount <= 1) 12.0 else 3.0
        return card.power * weight
    }

    // 상대가 B-01 손패를 정확히 알게 되는 경우의 위험 — 다음 턴에 SCOUT를 쥐면(지금 들고 있거나 뽑으면)
    // 맞혀서 B-01을 OUT시킨다. B-01 카드가 SCOUT(Power 1, 지목 불가)면 위험 없음
    private fun exposedRisk(round: OrbitRoundState, myCard: OrbitCardType, opponentHoldsScout: Double, self: PlayerSide): Double {
        if (myCard == OrbitCardType.SCOUT_DRONE) return 0.0
        val pile = round.deck.remainingCount
        val drawScout = if (pile <= 0) 0.0 else unseenOdds(round, self).probability { it == OrbitCardType.SCOUT_DRONE }
        val pScout = opponentHoldsScout + (1 - opponentHoldsScout) * drawScout
        return 100.0 * pScout
    }

    private fun scoreCandidate(
        round: OrbitRoundState,
        odds: Map<OrbitCardType, Double>,
        memory: B01Memory,
        card: OrbitCard,
        self: PlayerSide
    ): Scored {
        val hand = round.player(self).hand
        val other = otherCard(hand, card).type
        val opponentShielded = round.player(self.opponent()).shieldActive
        val keep = keepValue(round, other)

        return when (card.type) {
            OrbitCardType.SCOUT_DRONE -> {
                // Power 1(SCOUT)은 지목할 수 없으니 그 외에서 가장 가능성 높은 카드를 고른다
                val best = odds.filterKeys { it != OrbitCardType.SCOUT_DRONE }.maxByOrNull { it.value }
                val guess = best?.key ?: OrbitCardType.CAPTAIN
                val hit = if (opponentShielded) 0.0 else (best?.value ?: 0.0)
                Scored(100.0 * hit + keep, OrbitCardEffectInput.ScoutGuess(guess.power))
            }
            OrbitCardType.SENSOR -> {
                // 이미 아는 카드를 다시 볼 필요는 없다. 다음 턴에 정보를 써먹을 카드(SCOUT·PROBE·EMP)를 쥐면 더 가치 있음
                val info = when {
                    opponentShielded || memory.knownPlayerCard != null -> 0.0
                    other in setOf(OrbitCardType.SCOUT_DRONE, OrbitCardType.PROBE, OrbitCardType.EMP) -> 30.0
                    else -> 18.0
                }
                Scored(info + keep, OrbitCardEffectInput.None)
            }
            OrbitCardType.PROBE -> {
                // 내 남은 카드와 상대 카드 비교 — 낮으면 내가 OUT
                val effect = if (opponentShielded) 0.0 else {
                    val win = odds.probability { it.power < other.power }
                    val lose = odds.probability { it.power > other.power }
                    100.0 * (win - lose)
                }
                Scored(effect + keep, OrbitCardEffectInput.None)
            }
            OrbitCardType.SHIELD -> Scored(20.0 + keep, OrbitCardEffectInput.None)
            OrbitCardType.EMP -> scoreEmp(round, odds, other, opponentShielded, self)
            OrbitCardType.WARP_GATE -> {
                if (opponentShielded) return Scored(keep, OrbitCardEffectInput.None)
                // 교환하면 B-01은 상대 카드 X를, 상대는 내 남은 카드(other)를 갖고, 상대는 X를 정확히 안다.
                // 특히 other가 SCOUT면 상대가 그걸로 바로 X를 맞혀 B-01을 OUT시킨다
                val opponentHoldsScout = if (other == OrbitCardType.SCOUT_DRONE) 1.0 else 0.0
                val score = odds.entries.sumOf { (x, p) ->
                    p * (keepValue(round, x) - exposedRisk(round, x, opponentHoldsScout, self))
                }
                Scored(score, OrbitCardEffectInput.None)
            }
            // AI_CORE는 효과가 없다 — 남기는 카드의 가치만으로 비교
            OrbitCardType.AI_CORE -> Scored(keep, OrbitCardEffectInput.None)
            // candidatePool에서 빠지므로 다른 카드가 없을 때(강제)만 여기 온다
            OrbitCardType.CAPTAIN -> Scored(-1000.0, OrbitCardEffectInput.None)
        }
    }

    private fun scoreEmp(
        round: OrbitRoundState,
        odds: Map<OrbitCardType, Double>,
        other: OrbitCardType,
        opponentShielded: Boolean,
        self: PlayerSide
    ): Scored {
        val keep = keepValue(round, other)
        // 덱이 비면 EMP는 효과 없이 소모만 된다 — 남기는 카드의 가치만
        if (round.deck.remainingCount < 1) return Scored(keep, OrbitCardEffectInput.EmpTarget(self.opponent()))
        // 상대에게: 상대가 CAPTAIN이면 버려져 즉시 승리. 아니면 상대 카드를 새로 뽑게 해 흔드는 정도
        val onOpponent = if (opponentShielded) keep else {
            val captain = odds[OrbitCardType.CAPTAIN] ?: 0.0
            val disruption = 4.0 * odds.entries.sumOf { (t, p) -> p * t.power } / OrbitCardType.CAPTAIN.power
            100.0 * captain + disruption + keep
        }
        // 나에게: 남은 카드를 버리고 새로 뽑는다. CAPTAIN을 버리면 자멸
        val onSelf = if (other == OrbitCardType.CAPTAIN) -1000.0 else {
            val draw = unseenOdds(round, self)
            draw.entries.sumOf { (t, p) -> p * (if (t == OrbitCardType.CAPTAIN) keepValue(round, t) - 10 else keepValue(round, t)) }
        }
        return if (onSelf > onOpponent) {
            Scored(onSelf, OrbitCardEffectInput.EmpTarget(self))
        } else {
            Scored(onOpponent, OrbitCardEffectInput.EmpTarget(self.opponent()))
        }
    }
}
