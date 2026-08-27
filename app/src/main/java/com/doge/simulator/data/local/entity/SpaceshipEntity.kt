package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "spaceship_table")
data class SpaceshipEntity(
    @PrimaryKey val id: String,
    val name: String,
    val grade: Int,
    val crewCapacity: Int,
    val speed: Int,
    val cargo: Int,
    val successRate: Float,
    val purchasedAt: Long
)
