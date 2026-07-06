package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "astronaut_table")
data class AstronautEntity(
    @PrimaryKey val id: String,
    val name: String,
    val specialty: String,
    val level: Int,
    val status: String,
    val trainingEndTime: Long?,
    val hiredAt: Long
)
