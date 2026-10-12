package com.doge.simulator.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doge.simulator.domain.model.BeginnerMission
import com.doge.simulator.domain.model.BeginnerMissionStatus
import com.doge.simulator.ui.theme.GoldAccent
import com.doge.simulator.ui.theme.SpaceMid
import com.doge.simulator.ui.theme.SpaceNavy
import com.doge.simulator.ui.theme.Spacing
import com.doge.simulator.ui.theme.TextPrimary
import com.doge.simulator.ui.theme.TextSecondary

// 하단 탭 바 바로 위의 초보 임무 띠. 지금 할 일 하나만 — 임무 이름, 무엇이 몇/몇인지, 보상.
// 완료 상태여도 무엇을 해서 완료인지(예: 보유 우주선 2/2척)가 항상 보이게 한다 —
// 임무보다 먼저 해둔 사람이 "내가 뭘 했길래 완료지?" 하지 않도록.
@Composable
fun BeginnerMissionStrip(
    status: BeginnerMissionStatus?,
    justClaimed: BeginnerMission?,
    onClaim: () -> Unit,
    onGo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val complete = status?.isComplete == true && justClaimed == null
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xs),
        shape = RoundedCornerShape(10.dp),
        color = SpaceNavy,
        border = BorderStroke(1.dp, if (complete || justClaimed != null) GoldAccent else SpaceMid)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (justClaimed != null) {
                    Text(
                        "보상을 받았어요",
                        color = GoldAccent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        rewardText(justClaimed),
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                } else {
                    if (status == null) return@Column
                    val mission = status.mission
                    Text(
                        "임무 ${status.index + 1}/${status.total} · ${mission.title}",
                        color = if (complete) GoldAccent else TextPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${mission.progressLabel} ${status.current}/${mission.target}${mission.unit}" +
                            (if (complete) " ✓" else "") + " · 보상 ${rewardText(mission)}",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2
                    )
                }
            }
            if (justClaimed == null) {
                if (complete) {
                    GameButton(text = "받기", onClick = onClaim, style = GameButtonStyle.Gold)
                } else {
                    GameButton(text = "바로가기", onClick = onGo, style = GameButtonStyle.Neutral)
                }
            }
        }
    }
}

private fun rewardText(mission: BeginnerMission): String =
    (listOf("코인 ${"%,d".format(mission.rewardCoins)}") +
        mission.rewardResources.map { (type, amount) -> "${type.displayName} $amount" })
        .joinToString(" + ")
