package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "recruitment_meta_table")
data class RecruitmentMetaEntity(
    @PrimaryKey val id: Int = 1,
    val lastRefreshTime: Long
)