package com.doge.simulator.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.doge.simulator.domain.model.AstronautGrade
import com.doge.simulator.domain.model.Spaceship
import com.doge.simulator.ui.theme.SpaceMid
import com.doge.simulator.ui.theme.Spacing
import kotlin.math.roundToInt

val AstronautGrade.color: Color
    get() = when (this) {
        AstronautGrade.INTERN -> Color(0xFF9EA3A8)
        AstronautGrade.REGULAR -> Color(0xFF5DBF7A)
        AstronautGrade.SENIOR -> Color(0xFF5B9CF6)
        AstronautGrade.VETERAN -> Color(0xFFB07FE0)
        AstronautGrade.LEGEND -> Color(0xFFE8A84C)
    }

// 등급이 5단계 중 몇 번째인지 칸 5개로 보여준다 — "수석 대원"이 높은 건지 이름만으론 알 수 없다는
// 피드백 대응. 별 글리프는 픽셀 폰트에 없어 시스템 폰트로 대체돼 튀므로 네모 칸을 직접 그린다
@Composable
fun GradePips(grade: AstronautGrade, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        AstronautGrade.entries.forEach { step ->
            Box(
                Modifier
                    .size(5.dp)
                    .background(if (step.ordinal <= grade.ordinal) grade.color else SpaceMid)
            )
        }
    }
}

// 모집 센터 등장 확률(%). spawnWeight 합이 100이라 그대로 퍼센트
val AstronautGrade.spawnPercent: Int
    get() = spawnWeight * 100 / AstronautGrade.entries.sumOf { it.spawnWeight }

// 우주선 스탯을 원시 숫자(속도 40 · 적재 50) 대신 실제 효과로 — 숫자만으론 좋은 건지 알 수 없었다
fun durationEffectLabel(reduction: Double): String = "−${(reduction * 100).roundToInt()}%"

fun cargoEffectLabel(bonus: Double): String {
    val pct = (bonus * 100).roundToInt()
    return if (pct >= 0) "+$pct%" else "−${-pct}%"
}

val Spaceship.effectSummary: String
    get() = "시간 ${durationEffectLabel(durationReduction)} · 자원 ${cargoEffectLabel(cargoGainOverScout)}"

// 등급 배지: 등급 이름 + 서열 칸. 우주인 센터 보유·모집 카드에서 공유
@Composable
fun GradeBadge(grade: AstronautGrade) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = grade.color.copy(alpha = 0.18f)
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                grade.displayName,
                color = grade.color,
                style = MaterialTheme.typography.labelSmall
            )
            GradePips(grade)
        }
    }
}
