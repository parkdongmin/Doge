package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.repository.AstronautRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

// 우주인 방출 — 인턴을 뽑았다가 고용 한도가 차면 빼낼 방법이 연구소 강화뿐이라 추가. 환급은 없다
// (영입→방출 반복으로 이득을 볼 구멍을 아예 두지 않음). 대기 중인 우주인만 가능하고, 마지막 1명은
// 남긴다 — 코인까지 바닥난 상태에서 0명이 되면 탐사를 못 보내 진행이 막힐 수 있다
class DismissAstronautUseCase @Inject constructor(
    private val astronautRepository: AstronautRepository
) {
    sealed class Result {
        object Success : Result()
        object NotIdle : Result()
        object LastAstronaut : Result()
        object NotFound : Result()
    }

    suspend operator fun invoke(astronaut: Astronaut): Result {
        val astronauts = astronautRepository.getAstronauts().first()
        // 화면의 astronaut는 지난 상태일 수 있어 저장소 최신값으로 판단
        val current = astronauts.firstOrNull { it.id == astronaut.id } ?: return Result.NotFound
        if (current.status != AstronautStatus.IDLE) return Result.NotIdle
        if (astronauts.size <= 1) return Result.LastAstronaut
        astronautRepository.dismiss(current.id)
        return Result.Success
    }
}
