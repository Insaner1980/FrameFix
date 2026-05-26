package com.example.model

import androidx.compose.ui.graphics.Color

sealed class AspectRatio(
    val label: String,
    val widthRatio: Float,
    val heightRatio: Float
) {
    object Ratio9_16 : AspectRatio("9:16", 9f, 16f)
    object Ratio4_5 : AspectRatio("4:5", 4f, 5f)
    object Ratio3_4 : AspectRatio("3:4", 3f, 4f)
    object Ratio2_3 : AspectRatio("2:3", 2f, 3f)
    object Ratio1_1 : AspectRatio("1:1", 1f, 1f)
    object Ratio16_9 : AspectRatio("16:9", 16f, 9f)
    object Ratio4_3 : AspectRatio("4:3", 4f, 3f)
    object Ratio3_2 : AspectRatio("3:2", 3f, 2f)
    class Custom(var w: Float, var h: Float) : AspectRatio("Custom", w, h)

    val ratioValue: Float
        get() = if (heightRatio > 0f) widthRatio / heightRatio else 1f
}

enum class LayoutMode {
    FIT, FILL
}

enum class BackgroundType {
    BLUR, BLACK, WHITE, CUSTOM
}
