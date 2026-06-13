package com.osscabinet.ossWallet.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.dashedBorder(
    color: Color,
    width: Dp = 1.dp,
    shape: Shape,
    on: Dp = 4.dp,
    off: Dp = 4.dp
): Modifier = this.drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(on.toPx(), off.toPx()), 0f)
    )
    drawOutline(
        outline = outline,
        color = color,
        style = stroke
    )
}
