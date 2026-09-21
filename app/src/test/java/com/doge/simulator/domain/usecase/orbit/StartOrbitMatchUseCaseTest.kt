package com.doge.simulator.domain.usecase.orbit

import com.doge.simulator.domain.model.orbit.OrbitRiskTier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartOrbitMatchUseCaseTest {

    @Test
    fun `starting a match deducts the bet amount immediately`() = runBlocking {
        val repo = FakeOrbitUserRepository(initialCoins = 1_000L)
        val useCase = StartOrbitMatchUseCase(repo)

        val result = useCase(amount = 500L, riskTier = OrbitRiskTier.SAFE)

        assertTrue(result is StartOrbitMatchUseCase.Result.Success)
        assertEquals(500L, repo.coins)
    }

    @Test
    fun `insufficient balance fails without deducting anything`() = runBlocking {
        val repo = FakeOrbitUserRepository(initialCoins = 100L)
        val useCase = StartOrbitMatchUseCase(repo)

        val result = useCase(amount = 500L, riskTier = OrbitRiskTier.SAFE)

        assertEquals(StartOrbitMatchUseCase.Result.InsufficientCoins, result)
        assertEquals(100L, repo.coins)
    }
}
