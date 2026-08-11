package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.StoryContent
import com.doge.simulator.domain.model.StoryEvent
import com.doge.simulator.domain.repository.ExpeditionRepository
import com.doge.simulator.domain.repository.ResourceRepository
import com.doge.simulator.domain.repository.StoryRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

class ChooseStoryEventUseCase @Inject constructor(
    private val storyRepository: StoryRepository,
    private val resourceRepository: ResourceRepository,
    private val expeditionRepository: ExpeditionRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(event: StoryEvent, choiceIndex: Int) {
        val choice = event.choiceAt(choiceIndex) ?: return
        val (_, resourceType, amount) = choice

        // 이벤트당 한 번만 선택이 반영되도록 먼저 원자적으로 확정 — 연타 등으로 같은 선택이
        // 두 번 들어와도 두 번째 호출은 여기서 막혀 보상·페널티가 중복 적용되지 않는다
        if (!storyRepository.claimEventChoice(event.id, event.expeditionId, choiceIndex)) return

        // 마무리 선택의 "자원을 더 싣는다"는 위험이 있다 — 실패하면 자원 대신 코인을 잃고,
        // 후속 이벤트도 발생하지 않는다
        if (event.isDeparture && choiceIndex == 1) {
            if (Random.nextFloat() < GameConstants.DEPARTURE_LOOT_SUCCESS_RATE) {
                if (amount > 0) resourceRepository.add(resourceType, amount)
                chainFollowUpEvent(event)
            } else {
                val penalty = (amount * 80).coerceAtLeast(150L)
                val actualPenalty = userRepository.deductCoinsClamped(penalty)
                val flavor = StoryContent.departureFailureFlavors.random()
                val description = if (actualPenalty > 0) "$flavor (코인 -$actualPenalty)" else flavor
                storyRepository.setEventOutcomeNote(event.id, event.expeditionId, description)
            }
            return
        }

        if (amount > 0) resourceRepository.add(resourceType, amount)
    }

    private suspend fun chainFollowUpEvent(event: StoryEvent) {
        val category = expeditionRepository.getAll().first()
            .firstOrNull { it.id == event.expeditionId }?.category
        val followUp = category?.let { StoryContent.randomEvent(it, event.expeditionId) }
        if (followUp != null) storyRepository.addEvent(followUp)
    }
}
