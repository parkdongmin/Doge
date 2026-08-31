package com.doge.simulator.presentation.tutorial

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

// 튜토리얼 스포트라이트가 밝힐 수 있는 화면 요소들.
enum class TutorialTarget {
    EXPLORE_FIRST_CATEGORY, // 탐사 탭의 첫 번째 카테고리 카드 (광물 탐사)
}

// 여러 화면에 흩어진 타깃 컴포저블의 화면 좌표를, MainScreen에 올라가는 오버레이가
// 읽을 수 있도록 공유하는 레지스트리. 값은 boundsInWindow() 기준 Rect.
class TutorialTargetRegistry {
    private val bounds = mutableStateMapOf<TutorialTarget, Rect>()

    fun put(target: TutorialTarget, rect: Rect) { bounds[target] = rect }
    fun remove(target: TutorialTarget) { bounds.remove(target) }
    fun get(target: TutorialTarget?): Rect? = target?.let { bounds[it] }
}

val LocalTutorialTargets = compositionLocalOf<TutorialTargetRegistry?> { null }

/** 이 컴포저블의 화면 좌표를 튜토리얼 레지스트리에 등록한다. 레지스트리가 없으면 no-op. */
fun Modifier.tutorialTarget(
    registry: TutorialTargetRegistry?,
    target: TutorialTarget
): Modifier {
    if (registry == null) return this
    return this.onGloballyPositioned { registry.put(target, it.boundsInWindow()) }
}
