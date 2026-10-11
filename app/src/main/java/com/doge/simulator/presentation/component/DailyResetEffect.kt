package com.doge.simulator.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.Calendar
import kotlinx.coroutines.delay

// 하루 횟수 제한(광고 등)의 남은 횟수를 화면에 띄워 둘 때 쓴다. 값은 게이트가 읽을 때 기기 로컬
// 자정 기준으로 리셋되지만, 화면이 그 값을 다시 읽지 않으면 "내일 다시"에 묶인 채로 남는다(0회면
// 버튼도 비활성이라 눌러서 갱신할 수도 없음). 그래서 active인 동안 앱 복귀(ON_RESUME)와 로컬 자정에
// onRefresh를 부른다.
@Composable
fun DailyResetEffect(active: Boolean = true, onRefresh: () -> Unit) {
    val refresh by rememberUpdatedState(onRefresh)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, active) {
        val observer = LifecycleEventObserver { _, event ->
            if (active && event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        refresh()
        while (true) {
            // 자정 직후 1초 여유 — 경계에서 아직 전날로 읽히는 일을 피한다
            delay(millisUntilNextLocalMidnight() + 1_000L)
            refresh()
        }
    }
}

private fun millisUntilNextLocalMidnight(): Long {
    val now = Calendar.getInstance()
    val midnight = (now.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return (midnight.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
}
