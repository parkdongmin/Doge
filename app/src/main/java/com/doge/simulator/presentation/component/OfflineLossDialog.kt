package com.doge.simulator.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.doge.simulator.R
import com.doge.simulator.ui.theme.ButtonDepth
import com.doge.simulator.ui.theme.ButtonPadding
import com.doge.simulator.ui.theme.SpaceLight
import com.doge.simulator.ui.theme.SpaceNavy
import com.doge.simulator.ui.theme.Spacing
import com.doge.simulator.ui.theme.StatusRed
import com.doge.simulator.ui.theme.TextPrimary
import com.doge.simulator.ui.theme.TextSecondary

// 방치 중 보유 행성들의 순수익이 마이너스로(생산 중단 행성 누적) 쌓였을 때 알려주는 다이얼로그.
// 손해를 "2배로 만들기" 같은 광고 옵션은 말이 안 되므로 확인 버튼 하나만 제공.
// 시각적 무게는 OfflineProfitDialog와 맞추고, 빨강은 손실 금액 숫자에만 사용.
@Composable
fun OfflineLossDialog(
    coins: Long,
    onAcknowledge: () -> Unit
) {
    Dialog(
        onDismissRequest = onAcknowledge,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SpaceNavy,
            border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Image(
                        painter = painterResource(R.drawable.ic_ui_danger),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("자리를 비운 사이", color = TextPrimary, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                }
                Text(
                    "-%,d 코인".format(coins),
                    color = StatusRed,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "생산이 중단된 행성 때문에 손실이 발생했어요",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onAcknowledge,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SpaceLight, contentColor = TextPrimary),
                    border = ButtonDepth.highlightBorder,
                    elevation = ButtonDepth.elevation(),
                    contentPadding = ButtonPadding.fullWidthCta
                ) {
                    Text("확인")
                }
            }
        }
    }
}
