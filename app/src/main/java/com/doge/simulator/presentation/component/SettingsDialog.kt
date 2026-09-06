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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.doge.simulator.ui.theme.BodyReading
import com.doge.simulator.ui.theme.GoldAccent
import com.doge.simulator.ui.theme.SpaceBlue
import com.doge.simulator.ui.theme.SpaceDark
import com.doge.simulator.ui.theme.SpaceMid
import com.doge.simulator.ui.theme.SpaceNavy
import com.doge.simulator.ui.theme.Spacing
import com.doge.simulator.ui.theme.TextPrimary
import com.doge.simulator.ui.theme.TextSecondary
import kotlinx.coroutines.delay

// 자산 탭 헤더의 "설정" 버튼으로 여는 다이얼로그. 배경 음악 토글 + 내 ID(문의용) + 로그아웃.
// 톤·폭은 [[InfoDialog]]와 맞춘다 (화면 88%, SpaceNavy, SpaceBlue 테두리).
@Composable
fun SettingsDialog(
    uid: String?,
    bgmEnabled: Boolean,
    onBgmChange: (Boolean) -> Unit,
    onSignOutClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var justCopied by remember { mutableStateOf(false) }
    LaunchedEffect(justCopied) {
        if (justCopied) {
            delay(1500)
            justCopied = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(vertical = Spacing.xxl),
            shape = RoundedCornerShape(16.dp),
            color = SpaceNavy,
            border = BorderStroke(1.dp, SpaceBlue)
        ) {
            Column(modifier = Modifier.padding(Spacing.xl)) {
                Text(
                    "설정",
                    color = GoldAccent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(Spacing.lg))

                // 배경 음악
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "배경 음악",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = bgmEnabled,
                        onCheckedChange = onBgmChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SpaceDark,
                            checkedTrackColor = GoldAccent,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SpaceNavy,
                            uncheckedBorderColor = SpaceMid
                        )
                    )
                }

                // Pixabay 음원 라이선스 필수 표기 (이미지와 달리 오디오는 크레딧 필요)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "출처: Music by Maksim Chubrey from Pixabay",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )

                Spacer(Modifier.height(Spacing.md))
                HorizontalDivider(color = SpaceMid)
                Spacer(Modifier.height(Spacing.md))

                // 내 ID (문의용)
                Text(
                    "내 ID",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    "문의할 때 이 ID를 함께 보내주시면 확인이 빨라요.",
                    color = TextSecondary,
                    style = BodyReading
                )
                Spacer(Modifier.height(Spacing.sm))
                if (uid != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = SpaceBlue.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                uid,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            TextButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("Doge ID", uid))
                                    justCopied = true
                                }
                            ) {
                                Text(
                                    if (justCopied) "복사됨" else "복사",
                                    color = GoldAccent,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "로그인 정보를 불러올 수 없어요.",
                        color = TextSecondary,
                        style = BodyReading
                    )
                }

                Spacer(Modifier.height(Spacing.md))
                HorizontalDivider(color = SpaceMid)
                Spacer(Modifier.height(Spacing.md))

                // 로그아웃
                OutlinedButton(
                    onClick = onSignOutClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SpaceMid),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("로그아웃", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(Modifier.height(Spacing.sm))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("닫기", color = GoldAccent, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
