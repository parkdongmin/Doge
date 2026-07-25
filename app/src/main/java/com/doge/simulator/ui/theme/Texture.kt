package com.doge.simulator.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.random.Random

private const val GRAIN_SIZE = 64

// 앱 전체가 공유하는 단일 노이즈 텍스처 — 카드마다 새로 생성하지 않는다
private val grainBitmap: ImageBitmap by lazy {
    val random = Random(20260725) // 고정 시드: 매 실행마다 같은 패턴
    val pixels = IntArray(GRAIN_SIZE * GRAIN_SIZE) {
        val v = random.nextInt(256)
        (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    Bitmap.createBitmap(pixels, GRAIN_SIZE, GRAIN_SIZE, Bitmap.Config.ARGB_8888).asImageBitmap()
}

private val grainBrush: Brush by lazy {
    ShaderBrush(ImageShader(grainBitmap, TileMode.Repeated, TileMode.Repeated))
}

/**
 * 카드/패널 배경에 은은한 비네트 + 그레인 질감을 입힌다.
 * 버튼에는 쓰지 않는다 — 버튼은 가독성을 위해 플랫 단색을 유지한다.
 *
 * BlendMode.Overlay는 오프스크린 레이어 위에서 합성해야 정상 동작한다 — 지정하지 않으면
 * 스크롤 중 하드웨어 레이어가 임시로 씌워질 때만 우연히 보이고, 스크롤을 멈추면
 * (레이어가 빠지면서) 그레인이 사라지는 문제가 생긴다. graphicsLayer로 항상 오프스크린
 * 합성을 강제해서 스크롤 상태와 무관하게 일관되게 보이도록 한다.
 */
fun Modifier.textured(
    shape: Shape,
    baseColor: Color,
    highlightColor: Color = SpaceLight,
    grainAlpha: Float = 0.08f
): Modifier = this
    .clip(shape)
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithCache {
        val vignette = Brush.radialGradient(
            colors = listOf(highlightColor.copy(alpha = 0.08f), baseColor, baseColor),
            center = Offset(size.width / 2f, -size.height * 0.15f),
            radius = maxOf(size.width, size.height) * 1.1f
        )
        onDrawWithContent {
            drawRect(vignette)
            drawContent()
            drawRect(brush = grainBrush, blendMode = BlendMode.Overlay, alpha = grainAlpha)
        }
    }
