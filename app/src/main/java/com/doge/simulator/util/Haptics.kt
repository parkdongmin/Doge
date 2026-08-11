package com.doge.simulator.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class UpgradeHaptic { SUCCESS, FAIL, DANGER_FAIL }

// Compose의 LocalHapticFeedback는 LongPress/TextHandleMove 두 종류밖에 없어 성공·실패를
// 촉감으로 구분할 수 없다. 사운드가 아직 없는 상태에서 강화 결과의 임팩트를 대신 전달하기 위해
// 결과별로 다른 진동 패턴을 직접 재생한다
fun Context.vibrateUpgradeResult(type: UpgradeHaptic) {
    val vibrator = vibratorOrNull() ?: return
    val pattern = when (type) {
        UpgradeHaptic.SUCCESS -> longArrayOf(0, 40)
        UpgradeHaptic.FAIL -> longArrayOf(0, 30, 60, 30)
        UpgradeHaptic.DANGER_FAIL -> longArrayOf(0, 60, 80, 140)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(pattern, -1)
    }
}

private fun Context.vibratorOrNull(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
} else {
    @Suppress("DEPRECATION")
    getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
}
