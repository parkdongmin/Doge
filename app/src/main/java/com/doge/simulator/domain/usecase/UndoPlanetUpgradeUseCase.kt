package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.repository.PlanetRepository
import javax.inject.Inject

// 강화 실패(Danger Zone) 되돌리기 광고 시청 시 레벨만 복구한다.
// investment는 그대로 둬서 이미 소모된 재료가 환불되지 않도록 한다
class UndoPlanetUpgradeUseCase @Inject constructor(
    private val planetRepository: PlanetRepository
) {
    suspend operator fun invoke(planetId: String, previousLevel: Int, investment: Long) {
        planetRepository.upgradePlanet(planetId, previousLevel, investment)
    }
}
