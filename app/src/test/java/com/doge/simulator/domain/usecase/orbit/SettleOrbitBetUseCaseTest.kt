package com.doge.simulator.domain.usecase.orbit

import com.doge.simulator.domain.model.orbit.MatchOutcome
import com.doge.simulator.domain.model.orbit.OrbitBet
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.domain.model.orbit.OrbitMatchState
import com.doge.simulator.domain.model.orbit.OrbitRiskTier
import com.doge.simulator.domain.model.orbit.OrbitRoundState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class SettleOrbitBetUseCaseTest {

    @Test
    fun `winning settles a reward proportional to the risk tier multiplier`() = runBlocking {
        val repo = FakeOrbitUserRepository(initialCoins = 0L)
        val useCase = SettleOrbitBetUseCase(repo)
        val bet = OrbitBet(amount = 1_000L, riskTier = OrbitRiskTier.CHALLENGE)
        val match = winByThreeRounds(bet)

        val settlement = useCase.settleFinished(match)

        assertEquals(MatchOutcome.WON, settlement?.outcome)
        assertEquals(1_000L, settlement?.betAmount)
        assertEquals((1_000L * OrbitRiskTier.CHALLENGE.winRewardMultiplier).toLong(), settlement?.netChange)
        assertEquals((1_000L * OrbitRiskTier.CHALLENGE.winRewardMultiplier).toLong(), repo.coins)
    }

    @Test
    fun `losing confirms the loss without any further deduction`() = runBlocking {
        val repo = FakeOrbitUserRepository(initialCoins = 500L)
        val useCase = SettleOrbitBetUseCase(repo)
        val bet = OrbitBet(amount = 1_000L, riskTier = OrbitRiskTier.SAFE)
        val match = loseByThreeRounds(bet)

        val settlement = useCase.settleFinished(match)

        assertEquals(MatchOutcome.LOST, settlement?.outcome)
        assertEquals(1_000L, settlement?.betAmount)
        assertEquals(-1_000L, settlement?.netChange)
        assertEquals(500L, repo.coins) // 이미 시작 시 차감됐다는 전제 — 여기서 추가 차감 없음
    }

    @Test
    fun `abandoning a match confirms a loss immediately without touching the wallet again`() = runBlocking {
        val repo = FakeOrbitUserRepository(initialCoins = 500L)
        val useCase = SettleOrbitBetUseCase(repo)
        val bet = OrbitBet(amount = 500L, riskTier = OrbitRiskTier.SAFE)
        val match = OrbitMatchState.start(Random(1), bet = bet)

        val settlement = useCase.settleAbandoned(match)

        assertEquals(MatchOutcome.LOST, settlement?.outcome)
        assertEquals(500L, settlement?.betAmount)
        assertEquals(-500L, settlement?.netChange)
        assertEquals(MatchOutcome.LOST, match.matchResult)
        assertEquals(500L, repo.coins)
    }

    private fun winByThreeRounds(bet: OrbitBet): OrbitMatchState {
        val useCase = ResolveOrbitRoundEndUseCase()
        val match = OrbitMatchState.start(Random(1), bet = bet)
        repeat(3) {
            val round = OrbitRoundState.forTest(
                playerHand = listOf(OrbitCardType.CAPTAIN),
                b01Hand = listOf(OrbitCardType.SCOUT_DRONE)
            )
            useCase(round)
            match.currentRound = round
            match.onRoundEnded()
        }
        return match
    }

    private fun loseByThreeRounds(bet: OrbitBet): OrbitMatchState {
        val useCase = ResolveOrbitRoundEndUseCase()
        val match = OrbitMatchState.start(Random(1), bet = bet)
        repeat(3) {
            val round = OrbitRoundState.forTest(
                playerHand = listOf(OrbitCardType.SCOUT_DRONE),
                b01Hand = listOf(OrbitCardType.CAPTAIN)
            )
            useCase(round)
            match.currentRound = round
            match.onRoundEnded()
        }
        return match
    }
}
