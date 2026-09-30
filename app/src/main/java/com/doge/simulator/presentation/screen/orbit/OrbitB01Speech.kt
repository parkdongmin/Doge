package com.doge.simulator.presentation.screen.orbit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doge.simulator.R
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.domain.model.orbit.PlayerSide
import com.doge.simulator.domain.usecase.orbit.PlayOrbitCardUseCase
import com.doge.simulator.presentation.viewmodel.OrbitRoundEndInfo
import com.doge.simulator.ui.theme.*

// B-01 아바타 + 말풍선. 휴게실/베팅/게임/결과 화면 공용.
// 이름표만 있던 B-01에 캐릭터성을 주기 위한 것 — 플레이어를 "함장님"이라 부르는 정중한 로봇 말투.
// highlighted: 게임 중 B-01 차례일 때 말풍선 테두리를 금색으로.
// showAvatar: 휴게실처럼 장면 일러스트 안에 B-01이 이미 있으면 아바타를 빼고 말풍선만.
@Composable
fun B01SpeechRow(
    line: String,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 48.dp,
    highlighted: Boolean = false,
    showAvatar: Boolean = true
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (showAvatar) {
            Image(
                painter = painterResource(R.drawable.ch_b_01),
                contentDescription = null,
                modifier = Modifier.size(avatarSize)
            )
            Spacer(Modifier.width(Spacing.sm))
        }
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = SpaceNavy),
            shape = RoundedCornerShape(topStart = 2.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp),
            border = BorderStroke(1.dp, if (highlighted) GoldAccent else SpaceBlue)
        ) {
            Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "B-01",
                        color = GoldAccent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(Spacing.xxs))
                Text(line, color = TextPrimary, style = BodyReading)
            }
        }
    }
}

// 대사 풀. 화면에서는 상황(키)이 바뀔 때만 remember(key) { pool.random() }로 한 번 뽑는다 —
// 재구성마다 새로 뽑으면 대사가 깜빡이며 바뀐다.
object B01Lines {
    val lounge = listOf(
        "다음 목적지까지 시간이 좀 남았습니다. 한 판 하시겠습니까, 함장님?",
        "카드는 섞어 두었습니다. 언제든 앉으십시오, 함장님.",
        "오늘의 승률을 계산해 두었습니다. 결과는… 비밀입니다."
    )

    val bet = listOf(
        "좋은 승부가 되었으면 합니다, 함장님.",
        "베팅 금액을 정해 주십시오. 저는 준비되어 있습니다.",
        "위험도가 높을수록 저도 실수를 줄이겠습니다."
    )

    val thinking = listOf("확률을 계산합니다…", "잠시만요, 함장님…", "연산 중입니다…")

    val resultWon = listOf(
        "좋은 승부였습니다, 함장님.",
        "제 연산이 부족했군요. 축하드립니다, 함장님."
    )

    val resultLost = listOf(
        "…다음엔 더 좋은 결과를 기대하겠습니다.",
        "이번엔 제가 이겼군요. 다음 판도 기다리겠습니다, 함장님."
    )

    fun roundEnd(info: OrbitRoundEndInfo): List<String> = when (info.winner) {
        PlayerSide.PLAYER -> listOf("이번 라운드는 함장님 승리입니다.", "…당했군요.")
        PlayerSide.B01 -> listOf("이번 라운드는 제가 가져가겠습니다.", "계산대로입니다, 함장님.")
        null -> listOf("무승부군요.")
    }

    fun roundStart(playerFirst: Boolean): List<String> =
        if (playerFirst) listOf("함장님 선공입니다.", "먼저 하십시오, 함장님.")
        else listOf("제가 먼저 하겠습니다.")

    // B-01이 방금 낸 카드에 대한 한마디(이제 함장님 차례일 때).
    fun afterOwnPlay(s: PlayOrbitCardUseCase.PlayedCardSummary): List<String> = when {
        s.blockedByShield -> listOf("…쉴드에 막혔군요.")
        s.card.type == OrbitCardType.SCOUT_DRONE -> listOf("빗나갔군요. 다음엔 맞히겠습니다.")
        s.card.type == OrbitCardType.PROBE -> listOf("호각이군요, 함장님.")
        s.card.type == OrbitCardType.SENSOR -> listOf("함장님의 카드, 잘 봤습니다.")
        s.card.type == OrbitCardType.SHIELD -> listOf("방어 태세를 갖추겠습니다.")
        s.card.type == OrbitCardType.EMP -> listOf("판을 한번 흔들어 보죠.")
        s.card.type == OrbitCardType.WARP_GATE -> listOf("서로 카드를 바꿔 보죠, 함장님.")
        s.card.type == OrbitCardType.AI_CORE -> listOf("제 코어는 아무 일도 하지 않습니다. 아직은요.")
        else -> listOf("함장님 차례입니다.")
    }

    // 함장님이 방금 낸 카드에 대한 반응(B-01 차례, 생각하는 동안).
    fun afterPlayerPlay(s: PlayOrbitCardUseCase.PlayedCardSummary): List<String> = when {
        s.blockedByShield -> listOf("제 쉴드가 막았습니다.")
        s.card.type == OrbitCardType.SCOUT_DRONE -> listOf("아쉽군요, 함장님.")
        s.card.type == OrbitCardType.PROBE -> listOf("호각입니다.")
        s.card.type == OrbitCardType.SENSOR -> listOf("제 카드를 보셨군요…")
        s.card.type == OrbitCardType.SHIELD -> listOf("쉴드라… 신중하시군요.")
        s.card.type == OrbitCardType.EMP -> listOf("EMP라… 흥미롭군요.")
        s.card.type == OrbitCardType.WARP_GATE -> listOf("제 카드를 가져가셨군요.")
        s.card.type == OrbitCardType.AI_CORE -> listOf("AI CORE를 버리시다니… 동족으로서 유감입니다.")
        else -> thinking
    }
}
