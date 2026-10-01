package com.doge.simulator.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doge.simulator.R
import com.doge.simulator.ui.theme.*

// 게임 버튼 — 색을 꽉 채운 몸통 + 아래에 한 톤 어두운 두께(립)를 깔아 튀어나온 블록처럼 보이게 하고,
// 누르면 몸통이 립 두께만큼 내려앉는다. 글자색만 바꾼 TextButton·테두리만 있는 버튼이 "서비스 앱" 같다는
// 피드백으로 도입(2026-10-01, 예전 주식왕 매수 창 버튼 참고). 앱의 모든 버튼은 이걸 쓴다.
// 문구는 동사 하나로 짧게("매도", "강화", "확인") — 같은 동작은 화면이 달라도 같은 단어.
//
// 색 = 역할: Danger(빨강) 잃는 행동 / Primary(파랑) 확인·일반 / Gold(금색) 코인 쓰는 구매·강화 /
// Neutral(남색) 취소·보조.
// 카드 한 장에 강조색(금색·빨강) 버튼은 최대 하나 — 둘 이상이면(우주인 훈련 기초/심화) 남색으로 두고 금액만 금색.
// 줄마다 반복되는 버튼(자산 자원 판매)도 남색, 최종 확인 창의 버튼만 강조색.
enum class GameButtonStyle(val face: Color, val lip: Color, val text: Color) {
    Danger(StatusRed, StatusRedDim, Color.White),
    Primary(SpaceAccent, Color(0xFF1B3570), Color.White),
    Gold(GoldAccent, GoldDim, SpaceDark),
    Neutral(SpaceMid, Color(0xFF0A1430), TextPrimary)
}

enum class GameButtonSize(val lip: Dp, val corner: Dp, val padH: Dp, val padV: Dp, val minWidth: Dp) {
    Small(lip = 3.dp, corner = 6.dp, padH = 14.dp, padV = 7.dp, minWidth = 64.dp),
    Large(lip = 4.dp, corner = 8.dp, padH = 20.dp, padV = 11.dp, minWidth = 96.dp)
}

private val DisabledFace = Color(0xFF2A3350)
private val DisabledLip = Color(0xFF161C30)

@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GameButtonStyle = GameButtonStyle.Primary,
    size: GameButtonSize = GameButtonSize.Small,
    enabled: Boolean = true,
    // 글자 앞 픽셀 아이콘(광고 버튼의 ic_ui_ad 등).
    @DrawableRes leadingIcon: Int? = null,
    // 금액 — 글자 뒤에 코인 아이콘 + 숫자로(앱 전체 금액 표기와 같게). 글자 없이 금액만 두려면 text = "".
    coinAmount: Long? = null,
    // 글자 아래 작은 보조 줄(행성 강화 "Lv.1 → Lv.2", 훈련 "4시간" 등).
    subText: String? = null,
    // 보조 줄 뒤에 붙는 금액(훈련 "4시간 · 🪙300").
    subCoinAmount: Long? = null
) {
    GameButtonBox(onClick, modifier, style, size, enabled) { textColor, textShadow ->
        // 금색 버튼은 바탕이 금색이라 금액을 글자색(어두운색)으로, 나머지는 금색으로 강조.
        val coinColor = if (style == GameButtonStyle.Gold || !enabled) textColor else GoldAccent
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (leadingIcon != null) {
                Image(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    // 꺼진 버튼이면 아이콘도 흐리게 — 글자만 흐려지고 아이콘은 원색으로 남아 어색했다.
                    modifier = Modifier.size(if (size == GameButtonSize.Large) 18.dp else 14.dp).alpha(if (enabled) 1f else 0.35f)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (text.isNotEmpty()) {
                        Text(
                            text,
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            style = (if (size == GameButtonSize.Large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium)
                                .copy(shadow = textShadow)
                        )
                    }
                    if (coinAmount != null) {
                        CoinAmount(coinAmount, coinColor, textShadow, large = size == GameButtonSize.Large, dimmed = !enabled)
                    }
                }
                if (subText != null || subCoinAmount != null) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 윗줄과 같은 픽셀 글씨 계열로(예전엔 숫자 글꼴이라 한 버튼 안에서 글씨가 따로 놀았다).
                        if (subText != null) {
                            Text(
                                subText,
                                color = textColor.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow)
                            )
                        }
                        if (subText != null && subCoinAmount != null) {
                            Text(
                                "  ·  ",
                                color = textColor.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (subCoinAmount != null) {
                            CoinAmount(subCoinAmount, coinColor, textShadow, large = false, dimmed = !enabled)
                        }
                    }
                }
            }
        }
    }
}

// 버튼 안 금액 — 코인 아이콘 + 숫자(창 명판·베팅 창과 같은 표기).
@Composable
private fun CoinAmount(amount: Long, color: Color, shadow: Shadow?, large: Boolean, dimmed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Image(
            painter = painterResource(R.drawable.ic_ui_coin),
            contentDescription = "코인",
            modifier = Modifier.size(if (large) 16.dp else 13.dp).alpha(if (dimmed) 0.35f else 1f)
        )
        Text(
            "%,d".format(amount),
            color = color,
            fontWeight = FontWeight.Bold,
            style = (if (large) NumericSmall else NumericXSmall).copy(shadow = shadow)
        )
    }
}

// 버튼 몸통·립·눌림만 그리고 안쪽은 호출하는 쪽이 채운다(스토리 선택지처럼 왼쪽 글자 + 오른쪽 보상 등).
// content에는 글자색과 흰 글씨용 그림자를 넘겨준다.
@Composable
fun GameButtonBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GameButtonStyle = GameButtonStyle.Primary,
    size: GameButtonSize = GameButtonSize.Small,
    enabled: Boolean = true,
    content: @Composable (textColor: Color, textShadow: Shadow?) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sunk = if (pressed && enabled) size.lip else 0.dp
    val face = if (enabled) style.face else DisabledFace
    val lip = if (enabled) style.lip else DisabledLip
    val textColor = if (enabled) style.text else TextDisabled
    // 흰 글씨는 아래로 어두운 그림자를 깔아 어떤 바탕색 위에서도 또렷하게(시안의 글자 외곽선 대신).
    val textShadow = if (enabled && style.text == Color.White) {
        Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 2f), blurRadius = 0f)
    } else null

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = size.minWidth)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .drawBehind {
                val lipPx = size.lip.toPx()
                val sunkPx = sunk.toPx()
                val r = CornerRadius(size.corner.toPx())
                // 립: 버튼 전체 영역을 어두운 색으로 — 몸통이 위에 얹혀 아래쪽 두께만 보인다.
                drawRoundRect(lip, cornerRadius = r)
                // 몸통
                val faceH = this.size.height - lipPx
                drawRoundRect(face, topLeft = Offset(0f, sunkPx), size = Size(this.size.width, faceH), cornerRadius = r)
                // 몸통 위쪽 안쪽에 밝은 선 한 줄 — 빛 받는 윗면.
                if (enabled) {
                    val inset = r.x * 0.6f
                    drawRect(
                        Color.White.copy(alpha = 0.28f),
                        topLeft = Offset(inset, sunkPx + 1.dp.toPx()),
                        size = Size(this.size.width - inset * 2, 1.dp.toPx())
                    )
                }
            }
            .padding(top = sunk, bottom = size.lip - sunk)
            .padding(horizontal = size.padH, vertical = size.padV),
        contentAlignment = Alignment.Center
    ) {
        content(textColor, textShadow)
    }
}

// 다이얼로그·창 맨 아래 버튼 줄 — 시안(주식왕 매수 창)처럼 보조(왼쪽, 남색) / 주(오른쪽) 두 칸을 같은 폭으로.
// 보조 버튼이 없으면 주 버튼 하나가 전체 폭.
@Composable
fun GameDialogButtons(
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmStyle: GameButtonStyle = GameButtonStyle.Primary,
    confirmEnabled: Boolean = true,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    dismissEnabled: Boolean = true
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (dismissText != null && onDismiss != null) {
            GameButton(
                text = dismissText,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                style = GameButtonStyle.Neutral,
                size = GameButtonSize.Large,
                enabled = dismissEnabled
            )
        }
        GameButton(
            text = confirmText,
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            style = confirmStyle,
            size = GameButtonSize.Large,
            enabled = confirmEnabled
        )
    }
}
