package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resource_table")
data class ResourceEntity(
    @PrimaryKey val type: String,
    val amount: Long
)
