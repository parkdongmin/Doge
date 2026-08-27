package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "recruitment_candidate_table")
data class RecruitmentCandidateEntity(
    @PrimaryKey val slotIndex: Int,
    val id: String,
    val name: String,
    val specialty: String,
    val grade: String,
    val proficiency: Int
)