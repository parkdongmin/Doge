package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.marketValue
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.UserRepository
import javax.inject.Inject

class SellPlanetUseCase @Inject constructor(
    private val planetRepository: PlanetRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(planet: Planet) {
        // 삭제가 실제로 일어났을 때만 대금을 지급 — 연타 등으로 같은 행성을 두 번 매도
        // 시도해도 두 번째 호출은 삭제할 행 자체가 없어 코인이 중복 지급되지 않는다
        if (!planetRepository.sellPlanet(planet.id)) return

        // marketValue는 0 이상(악재가 겹쳐도 시세는 0에서 바닥). 수수료 5% 떼고 지급 — 항상 0 이상
        val baseValue = planet.marketValue
        val fee = (baseValue * GameConstants.SELL_FEE_RATE).toLong()
        val proceeds = baseValue - fee

        userRepository.addCoins(proceeds)
    }
}
