package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "research_lab_table")
data class ResearchLabEntity(
    @PrimaryKey val id: Int = 1,
    val explorationTechLevel: Int,
    val celestialAnalysisLevel: Int,
    val hrLevel: Int,
    val engineeringLevel: Int
)
