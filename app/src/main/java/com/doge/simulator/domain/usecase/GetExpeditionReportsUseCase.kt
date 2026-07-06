package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.ExpeditionReport
import com.doge.simulator.domain.repository.StoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExpeditionReportsUseCase @Inject constructor(
    private val storyRepository: StoryRepository
) {
    fun all(): Flow<List<ExpeditionReport>> = storyRepository.getAllReports()
    fun unread(): Flow<List<ExpeditionReport>> = storyRepository.getUnreadReports()
    fun unreadCount(): Flow<Int> = storyRepository.getUnreadCount()
}
