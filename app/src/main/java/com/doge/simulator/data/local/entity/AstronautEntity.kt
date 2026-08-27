package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "astronaut_table")
data class AstronautEntity(
    @PrimaryKey val id: String,
    val name: String,
    val specialty: String,
    val grade: String,
    val proficiency: Int,
    val status: String,
    val trainingEndTime: Long?,
    val trainingType: String?,
    val hiredAt: Long
)