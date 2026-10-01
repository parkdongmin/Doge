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
    GameDialog(
        title = "자리를 비운 사이",
        onDismissRequest = onClaimFree,
        dismissOnClickOutside = false,
        buttons = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                GameButton(
                    text = "광고 보고 %,d 받기".format((coins * GameConstants.OFFLINE_PROFIT_AD_MULTIPLIER).toLong()),
                    onClick = onClaimWithAd,
                    modifier = Modifier.fillMaxWidth(),
                    style = GameButtonStyle.Primary,
                    size = GameButtonSize.Large,
                    leadingIcon = R.drawable.ic_ui_ad
                )
                GameButton(
                    text = "그냥 %,d 받기".format(coins),
                    onClick = onClaimFree,
                    modifier = Modifier.fillMaxWidth(),
                    style = GameButtonStyle.Neutral,
                    size = GameButtonSize.Large
                )
            }
        }
    ) {
        Text(
            "%,d 코인을 벌었어요!".format(coins),
            color = GoldAccent,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
