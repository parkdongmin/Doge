package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "story_event_table")
data class StoryEventEntity(
    @PrimaryKey val id: String,
    val expeditionId: String,
    val title: String,
    val description: String,
    val choice1Label: String,
    val choice1ResourceType: String,
    val choice1Amount: Long,
    val choice2Label: String,
    val choice2ResourceType: String,
    val choice2Amount: Long,
    val choice3Label: String?,
    val choice3ResourceType: String?,
    val choice3Amount: Long?,
    // -1 = 미선택, 0/1/2 = 선택됨
    val selectedChoiceIndex: Int = -1,
    val isDeparture: Boolean = false,
    val outcomeNote: String? = null
)
