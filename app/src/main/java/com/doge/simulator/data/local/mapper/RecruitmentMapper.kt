package com.doge.simulator.data.local.mapper

import com.doge.simulator.data.local.entity.RecruitmentCandidateEntity
import com.doge.simulator.domain.model.AstronautGrade
import com.doge.simulator.domain.model.AstronautSpecialty
import com.doge.simulator.domain.model.RecruitmentCandidate

fun RecruitmentCandidateEntity.toDomain() = RecruitmentCandidate(
    id = id,
    name = name,
    specialty = AstronautSpecialty.valueOf(specialty),
    grade = AstronautGrade.valueOf(grade),
    proficiency = proficiency
)

fun RecruitmentCandidate.toEntity(slotIndex: Int) = RecruitmentCandidateEntity(
    slotIndex = slotIndex,
    id = id,
    name = name,
    specialty = specialty.name,
    grade = grade.name,
    proficiency = proficiency
)