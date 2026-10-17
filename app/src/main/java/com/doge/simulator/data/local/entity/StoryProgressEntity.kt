package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "story_progress_table")
data class StoryProgressEntity(
    @PrimaryKey val id: Int = 1,
    val totalRecordsCompleted: Int = 0,
    val currentChapter: Int = 1,
    val firstRuinsCompleted: Boolean = false,
    val firstAlienCivCompleted: Boolean = false,
    val firstT3Completed: Boolean = false,
    val firstT5Completed: Boolean = false,
    val firstT10Completed: Boolean = false,
    val storyCompleted: Boolean = false,
    // 2부 (2026-10-14) — 의미는 도메인 StoryProgress 참고
    val firstT11Completed: Boolean = false,
    val firstT12Completed: Boolean = false,
    val firstT13Completed: Boolean = false,
    val firstT15Completed: Boolean = false,
    val part2Completed: Boolean = false,
    val part2IntroShown: Boolean = false,
    val settlementLogCursor: Int = 0
)
