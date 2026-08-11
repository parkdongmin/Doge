package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.model.preciseProduction
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.ResourceRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

// 앱 세션 전반의 60초 방치수익 루프(ExploreViewModel)와 ON_RESUME 시점의 미수령 수익
// 자동 수집(AppSessionViewModel)이 거의 동시에 호출될 수 있다. 두 호출자 모두 락 획득 전에
// planets 리스트를 스냅샷하므로, 단순히 호출을 직렬화하는 것만으로는 뒤에 대기한 호출이 여전히
// 같은(오래된) lastProfitTime을 보고 같은 경과 구간을 중복 지급하는 것을 막지 못한다. 그래서
// 락 안에서 DB의 최신 상태를 다시 읽어와 사용한다 — 먼저 실행된 호출이 이미 lastProfitTime을
// 갱신했다면 뒤에 대기한 호출은 그 갱신된 값을 보고 경과 시간을 0에 가깝게 계산하게 된다.
// 여러 곳에서 주입돼도 같은 락을 공유하도록 Singleton으로 스코프한다
@Singleton
class CollectProfitUseCase @Inject constructor(
    private val planetRepository: PlanetRepository,
    private val userRepository: UserRepository,
    private val resourceRepository: ResourceRepository
) {
    private val mutex = Mutex()

    // multiplier: 오프라인 수익 2배 리워드 광고 시청 시 코인에만 적용 (자원 드랍은 배율 미적용)
    suspend operator fun invoke(planets: List<Planet>, multiplier: Double = 1.0) = mutex.withLock {
        val now = System.currentTimeMillis()
        var totalEarned = 0L

        val ownedIds = planets.map { it.id }.toSet()
        val currentPlanets = planetRepository.getOwnedPlanets().first().filter { it.id in ownedIds }

        currentPlanets.forEach { planet ->
            val rawElapsed = (now - planet.lastProfitTime) / 60_000L
            val elapsedMinutes = minOf(rawElapsed, GameConstants.MAX_OFFLINE_MINUTES)
            if (elapsedMinutes <= 0) return@forEach

            val earned = (planet.preciseProduction * elapsedMinutes).toLong()
            totalEarned += earned

            planetRepository.updatePlanetProfit(
                planetId = planet.id,
                totalProfit = planet.totalProfit + earned,
                lastProfitTime = now
            )

            val meta = PlanetMetaDataTable.data[planet.type]
            val rarityMultiplier = GameConstants.RARITY_RESOURCE_MULTIPLIER[meta?.rarity] ?: 1.0
            val levelMultiplier = GameConstants.planetLevelMultiplier(planet.level)

            meta?.resourceDrops.orEmpty().forEach { (type, baseDropChance) ->
                val amount = rollResourceAmount(elapsedMinutes, baseDropChance, rarityMultiplier, levelMultiplier)
                if (amount > 0L) resourceRepository.add(type, amount)
            }
        }

        if (totalEarned > 0) userRepository.addCoins((totalEarned * multiplier).toLong())
    }

    // 분당 dropChance(%) × 등급 배율 × 강화 레벨 배율을 경과 시간에 대한 기댓값으로 환산하여 정수 개수를 산출
    private fun rollResourceAmount(
        elapsedMinutes: Long,
        baseDropChancePercent: Int,
        rarityMultiplier: Double,
        levelMultiplier: Double
    ): Long {
        val effectiveChance = baseDropChancePercent * rarityMultiplier * levelMultiplier
        val expected = elapsedMinutes * effectiveChance / 100.0
        val whole = expected.toLong()
        val fractional = expected - whole
        return whole + if (Random.nextDouble() < fractional) 1L else 0L
    }
}
