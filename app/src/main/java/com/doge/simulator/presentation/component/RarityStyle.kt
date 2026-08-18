package com.doge.simulator.presentation.component

import androidx.compose.ui.graphics.Color
import com.doge.simulator.domain.model.RarityTier

val rarityOrder = listOf(
    RarityTier.COMMON,
    RarityTier.UNCOMMON,
    RarityTier.RARE,
    RarityTier.EPIC,
    RarityTier.LEGENDARY
)

val rarityColor = mapOf(
    RarityTier.COMMON    to Color(0xFF9EA3A8),
    RarityTier.UNCOMMON  to Color(0xFF5DBF7A),
    RarityTier.RARE      to Color(0xFF5B9CF6),
    RarityTier.EPIC      to Color(0xFFB07FE0),
    RarityTier.LEGENDARY to Color(0xFFE8A84C)
)

val rarityLabel = mapOf(
    RarityTier.COMMON    to "COMMON",
    RarityTier.UNCOMMON  to "UNCOMMON",
    RarityTier.RARE      to "RARE",
    RarityTier.EPIC      to "EPIC",
    RarityTier.LEGENDARY to "LEGENDARY"
)
