package com.doge.simulator.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetTrait
import com.doge.simulator.domain.model.PlanetTraitTable
import com.doge.simulator.domain.model.TraitAxis
import com.doge.simulator.domain.model.TraitType
import com.doge.simulator.domain.model.traitEffects
import com.doge.simulator.ui.theme.BodyReading
import com.doge.simulator.ui.theme.LabelTiny
import com.doge.simulator.ui.theme.Spacing
import com.doge.simulator.ui.theme.StatusGreen
import com.doge.simulator.ui.theme.StatusRed
import com.doge.simulator.ui.theme.TextPrimary
import com.doge.simulator.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.roundToInt

// ── 2부 행성 특성 표시 ────────────────────────────────────────────────
// 문구는 specs/004-part2-planet-traits/contracts/part2-ui-contract.md 3~5절. 용어는 앱에 이미 있는 말
// (생산량, 악재/호재, 악재 확률, 고장, 자원 드롭량, 강화 성공률, 시세, 매도가)을 그대로 쓰고,
// 특성의 Ⅰ·Ⅱ·Ⅲ은 "등급"이라고 부른다. 색은 역할로만 — 좋은 특성 초록, 나쁜 특성 빨강(금색은 쓰지 않음)

private val TRAIT_DESCRIPTIONS: Map<TraitType, String> = mapOf(
    TraitType.HIGH_PRODUCTION to "생산량이 많아요",
    TraitType.LOW_PRODUCTION to "생산량이 적어요",
    TraitType.STABLE_ENVIRONMENT to "악재 확률이 낮아요",
    TraitType.UNSTABLE_ENVIRONMENT to "악재 확률이 높아요",
    TraitType.STURDY_STRUCTURE to "큰 악재에도 잘 고장 나지 않아요",
    TraitType.WEAK_STRUCTURE to "작은 악재에도 고장 나요",
    TraitType.BOOM_PRONE to "호재가 뜨면 생산량이 더 크게 올라요",
    TraitType.RICH_RESOURCES to "자원 드롭량이 많아요",
    TraitType.BARREN_RESOURCES to "자원 드롭량이 적어요",
    TraitType.UPGRADE_APTITUDE to "강화 성공률이 높아요",
    TraitType.UPGRADE_INAPTITUDE to "강화 성공률이 낮아요"
)

private fun traitColor(trait: PlanetTrait): Color = if (trait.type.isGood) StatusGreen else StatusRed

// 소수점이 있을 때만 한 자리까지 — 22%, 12.5%
private fun formatNumber(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "%.1f".format(rounded)
}

private fun signed(percent: Double): String = (if (percent >= 0) "+" else "−") + formatNumber(abs(percent)) + "%"

// 이 행성 기준 실제 숫자 — 특성 하나만 붙었을 때의 변화로 보여 준다(한 축엔 특성이 최대 1개라 실제 값과 같다)
fun traitNumberText(planet: Planet, trait: PlanetTrait): String {
    val value = PlanetTraitTable.value(planet.type, trait)
    return when (trait.type.axis) {
        TraitAxis.PRODUCTION -> "생산량 ${signed(value * 100)}"
        TraitAxis.BAD_CHANCE -> {
            val after = (planet.eventRate + if (trait.type.isGood) -value else value).roundToInt().coerceIn(0, 100)
            "악재 확률 ${planet.eventRate}% → $after%"
        }
        TraitAxis.BREAKDOWN_THRESHOLD -> {
            val base = GameConstants.PLANET_BREAKDOWN_DELTA_THRESHOLD * 100
            "${formatNumber(base + value * 100)}% 이상 악재부터 고장 (기본 ${formatNumber(base)}%)"
        }
        TraitAxis.BOOM -> "호재 상승폭 ${signed(value * 100)}"
        TraitAxis.RESOURCE_DROP -> "자원 드롭량 ×${formatNumber(if (trait.type.isGood) 1.0 + value else value)}"
        TraitAxis.UPGRADE_SUCCESS -> "강화 성공률 ×${"%.2f".format(value).trimEnd('0').trimEnd('.')}"
    }
}

fun traitTitle(trait: PlanetTrait): String =
    if (trait.type.isGood) "${trait.type.displayName} ${trait.intensityLabel}" else trait.type.displayName

// "특성 시세 +35%" — 발견 창 가격 줄 옆
fun traitPriceText(planet: Planet): String = "특성 시세 ${signed(planet.traitEffects.priceAdjustment * 100)}"

@Composable
fun PlanetTraitRow(planet: Planet, trait: PlanetTrait, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                traitTitle(trait),
                color = traitColor(trait),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            // 적합도는 좋은 특성에만 있다 — 나쁜 특성은 행성과 상관없이 같은 값
            PlanetTraitTable.fit(planet.type, trait.type)?.takeIf { trait.type.isGood }?.let { fit ->
                Text("적합도 ${fit.label}", color = TextSecondary, style = LabelTiny)
            }
        }
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            "${TRAIT_DESCRIPTIONS[trait.type].orEmpty()} · ${traitNumberText(planet, trait)}",
            color = TextSecondary,
            style = BodyReading
        )
    }
}

// 특성 목록 + 도움말(ⓘ). 특성이 없는 행성이면 아무것도 그리지 않는다
@Composable
fun PlanetTraitList(planet: Planet, modifier: Modifier = Modifier) {
    if (planet.traits.isEmpty()) return
    var showInfo by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "특성",
                color = TextPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            HelpChip(onClick = { showInfo = true }, contentDescription = "특성 설명")
        }
        planet.traits.forEach { PlanetTraitRow(planet, it) }
    }
    if (showInfo) PlanetTraitInfoDialog(planet, onDismiss = { showInfo = false })
}

@Composable
fun PlanetTraitInfoDialog(planet: Planet, onDismiss: () -> Unit) {
    InfoDialog(title = "특성", onDismiss = onDismiss) {
        InfoEntry("특성", "T11부터 찾은 행성엔 특성이 붙어요. 좋은 특성도, 나쁜 특성도 있어요.")
        InfoEntry("적합도", "같은 특성이라도 행성마다 잘 맞는 정도가 달라요. 적합도가 높을수록 효과가 커요.")
        InfoEntry("등급", "좋은 특성엔 Ⅰ·Ⅱ·Ⅲ 등급이 있어요. 등급이 높을수록 효과가 크고, 티어가 높을수록 잘 나와요.")
        InfoEntry("시세", "특성 효과만큼 시세가 오르거나 내려요. 매도가에도 그대로 반영돼요.")
        Spacer(Modifier.height(Spacing.xs))
        planet.traits.forEach { trait ->
            InfoEntry(traitTitle(trait), traitNumberText(planet, trait))
        }
    }
}

// 행성 목록·바꾸기 창용 짧은 이름 칩(최대 2개). 아이콘 없이 글자만
@Composable
fun PlanetTraitChips(traits: List<PlanetTrait>, modifier: Modifier = Modifier) {
    if (traits.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        traits.forEach { trait ->
            val color = traitColor(trait)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = color.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
            ) {
                Text(
                    traitTitle(trait),
                    color = color,
                    style = LabelTiny,
                    // 글자가 테두리에 붙어 보이지 않게 등급 칩(RarityChip)보다 살짝 넉넉히
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}
