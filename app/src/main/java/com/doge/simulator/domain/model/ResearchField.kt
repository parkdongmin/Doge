package com.doge.simulator.domain.model

import androidx.annotation.DrawableRes
import com.doge.simulator.R

enum class ResearchField(
    val displayName: String,
    val description: String,
    val icon: String,
    @DrawableRes val iconRes: Int? = null
) {
    EXPLORATION_TECH("탐사 기술", "탐사 카테고리 해금", "🔭", R.drawable.ic_research_explore_skills),
    CELESTIAL_ANALYSIS("천체 분석", "희귀 행성 발견 확률 증가, 행성 슬롯 확장", "🌌", R.drawable.ic_research_celestial_analysis),
    HR_MANAGEMENT("인사 관리", "우주인 고용 한도 및 훈련 슬롯 확장", "👥", R.drawable.ic_research_human_management),
    SPACE_ENGINEERING("우주 공학", "보유 우주선 수 확장", "🚀", R.drawable.ic_research_space_engineering)
}
