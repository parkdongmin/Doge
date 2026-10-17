package com.doge.simulator.presentation.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.doge.simulator.ui.theme.BodyReading
import com.doge.simulator.ui.theme.TextSecondary

// 플레이 스토어에 새 버전이 있을 때 메인 화면에서 한 번 띄우는 안내(MainActivity가 확인).
// [업데이트]는 구글 업데이트 화면으로 넘어가고, [나중에]는 이번 실행 동안 다시 안 띄운다
@Composable
fun AppUpdateDialog(onUpdate: () -> Unit, onLater: () -> Unit) {
    GameDialog(
        title = "새 버전이 나왔어요",
        onDismissRequest = onLater,
        dismissOnClickOutside = false,
        buttons = {
            GameDialogButtons(
                confirmText = "업데이트",
                onConfirm = onUpdate,
                dismissText = "나중에",
                onDismiss = onLater
            )
        }
    ) {
        Text(
            "업데이트하면 새 내용과 수정 사항이 적용돼요. 여러 기기에서 플레이한다면 모든 기기를 업데이트해 주세요.",
            color = TextSecondary,
            style = BodyReading
        )
    }
}
