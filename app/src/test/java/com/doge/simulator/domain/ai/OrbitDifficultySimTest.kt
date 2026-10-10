package com.doge.simulator.domain.ai

import com.doge.simulator.domain.model.orbit.MatchOutcome
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.domain.model.orbit.OrbitMatchState
import com.doge.simulator.domain.model.orbit.OrbitRiskTier
import com.doge.simulator.domain.model.orbit.PlayerSide
import com.doge.simulator.domain.usecase.orbit.OrbitCardEffectInput
import com.doge.simulator.domain.usecase.orbit.PlayOrbitCardUseCase
import com.doge.simulator.domain.usecase.orbit.ResolveOrbitRoundEndUseCase
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

// 난이도별 매치 승률 시뮬레이션. 실제 규칙(PlayOrbitCardUseCase·ResolveOrbitRoundEndUseCase·OrbitMatchState)으로
// 3선승 매치를 끝까지 돌리고, 플레이어는 같은 AI에 실수율을 줘서 실력 단계를 흉내 낸다.
// 플레이어 쪽 기억은 사람이 알 수 있는 정보(내 SENSOR 결과, WARP 교환, PROBE 동점, 상대가 낸 카드)로만 갱신
class OrbitDifficultySimTest {

    private val play = PlayOrbitCardUseCase()
    private val resolve = ResolveOrbitRoundEndUseCase()

    private fun playMatch(b01Mistake: Float, playerMistake: Float, rng: Random, b01Gap: Double = B01OrbitAi.DEFAULT_MAX_MISTAKE_GAP): MatchOutcome {
        val match = OrbitMatchState.start(rng)
        while (!match.isOver) {
            val round = match.currentRound
            val b01Memory = B01Memory()
            val playerMemory = B01Memory() // 플레이어가 아는 B-01 카드
            while (!round.isOver) {
                if (!round.drawForCurrentPlayer()) { resolve(round); break }
                val side = round.currentTurn
                val decision = if (side == PlayerSide.B01) {
                    B01OrbitAi.decide(round, b01Memory, b01Mistake, rng, maxMistakeGap = b01Gap)
                } else {
                    B01OrbitAi.decide(round, playerMemory, playerMistake, rng, self = PlayerSide.PLAYER)
                }
                val playerHandBefore = round.player(PlayerSide.PLAYER).hand.toList()
                val raw = play(round, side, decision.card, decision.input, b01Memory)
                check(raw is PlayOrbitCardUseCase.Result.Applied) {
                    "InvalidMove side=$side hand=${round.player(side).hand.map { it.type }} deck=${round.deck.remainingCount} card=${decision.card.type} input=${decision.input}"
                }
                val result = raw
                val s = result.summary
                if (round.isOver) break

                // ── 플레이어 기억 갱신 (사람이 볼 수 있는 정보만) ──
                when (decision.card.type) {
                    OrbitCardType.SENSOR -> if (side == PlayerSide.PLAYER && !s.blockedByShield) {
                        s.revealedOpponentCard?.let { playerMemory.reveal(it) }
                    }
                    OrbitCardType.WARP_GATE -> if (!s.blockedByShield) {
                        // 교환 뒤 B-01은 플레이어가 원래 들고 있던 카드를 갖는다
                        val mine = if (side == PlayerSide.PLAYER) {
                            playerHandBefore.toMutableList().apply { remove(decision.card) }.first()
                        } else playerHandBefore.first()
                        playerMemory.reveal(mine)
                    }
                    OrbitCardType.PROBE -> if (!s.blockedByShield && s.noEffectNote != null) {
                        playerMemory.reveal(round.player(PlayerSide.B01).hand.first())
                    }
                    OrbitCardType.EMP -> if (side == PlayerSide.B01 &&
                        (decision.input as? OrbitCardEffectInput.EmpTarget)?.target == PlayerSide.B01
                    ) playerMemory.forget()
                    else -> Unit
                }
                if (side == PlayerSide.B01 && decision.card.type != OrbitCardType.WARP_GATE) {
                    val known = playerMemory.knownPlayerCard
                    if (known == null || known.type == decision.card.type) playerMemory.forget()
                }
            }
            match.onRoundEnded()
            match.startNextRoundIfNotOver()
        }
        return requireNotNull(match.matchResult)
    }

    // 베팅 1회당 기대 수익 = 승률 × 배율 − 1 (베팅액은 시작 때 빠지고, 이기면 베팅액 × 배율을 받음).
    // 시드 고정이라 결과는 매번 같다. 지켜야 할 것:
    //  - 안정은 어떤 실력이든 기대 수익이 플러스가 아니다 ("안정만 최대 베팅으로 반복"이 정답이 되지 않게)
    //  - 보통 실력 기준 고위험이 안정보다 기대 수익이 낮지 않다 (어려운 만큼 보상)
    @Test
    fun difficultyTiersStayBalanced() {
        val players = listOf("고수" to 0f, "보통" to 0.2f, "초보" to 0.4f)
        val games = 4000
        val ev = mutableMapOf<Pair<OrbitRiskTier, String>, Double>()
        println("난이도 | 배율 | 본전 승률 | " + players.joinToString(" | ") { "${it.first} 승률(기대값)" })
        for (tier in OrbitRiskTier.entries) {
            val cells = players.mapIndexed { pi, (name, mistake) ->
                val rng = Random(1000 + tier.ordinal * 10 + pi)
                val wins = (1..games).count { playMatch(tier.aiMistakeRate, mistake, rng, tier.aiMaxMistakeGap) == MatchOutcome.WON }
                val p = wins.toDouble() / games
                ev[tier to name] = p * tier.winRewardMultiplier - 1
                "${"%.1f".format(p * 100)}% (${"%+.0f".format(ev.getValue(tier to name) * 100)}%)"
            }
            println("${tier.displayName} | ${tier.winRewardMultiplier} | ${"%.1f".format(100 / tier.winRewardMultiplier)}% | ${cells.joinToString(" | ")}")
        }
        players.forEach { (name, _) ->
            assertTrue("안정 기대 수익이 플러스($name)", ev.getValue(OrbitRiskTier.SAFE to name) <= 0.0)
        }
        assertTrue("보통 실력에서 고위험이 안정보다 손해",
            ev.getValue(OrbitRiskTier.HIGH_RISK to "보통") >= ev.getValue(OrbitRiskTier.SAFE to "보통"))
    }
}
