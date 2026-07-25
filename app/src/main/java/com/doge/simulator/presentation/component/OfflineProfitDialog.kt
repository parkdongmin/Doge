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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.doge.simulator.R
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.ui.theme.ButtonDepth
import com.doge.simulator.ui.theme.ButtonPadding
import com.doge.simulator.ui.theme.GoldAccent
import com.doge.simulator.ui.theme.SpaceBlue
import com.doge.simulator.ui.theme.SpaceLight
import com.doge.simulator.ui.theme.SpaceNavy
import com.doge.simulator.ui.theme.Spacing
import com.doge.simulator.ui.theme.TextPrimary
import com.doge.simulator.ui.theme.TextSecondary

@Composable
fun OfflineProfitDialog(
    coins: Long,
    onClaimWithAd: () -> Unit,
    onClaimFree: () -> Unit
) {
    Dialog(
        onDismissRequest = onClaimFree,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SpaceNavy,
            border = BorderStroke(1.dp, SpaceBlue.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Image(
                        painter = painterResource(R.drawable.ic_ui_planet),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("자리를 비운 사이", color = TextPrimary, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                }
                Text(
                    "%,d 코인을 벌었어요!".format(coins),
                    color = GoldAccent,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onClaimWithAd,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SpaceLight, contentColor = TextPrimary),
                    border = ButtonDepth.highlightBorder,
                    elevation = ButtonDepth.elevation(),
                    contentPadding = ButtonPadding.fullWidthCta
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Image(
                            painter = painterResource(R.drawable.ic_ui_ad),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("광고 보고 %,d 받기".format((coins * GameConstants.OFFLINE_PROFIT_AD_MULTIPLIER).toLong()))
                    }
                }
                OutlinedButton(
                    onClick = onClaimFree,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    contentPadding = ButtonPadding.fullWidthCta
                ) {
                    Text("그냥 %,d 받기".format(coins))
                }
            }
        }
    }
}
