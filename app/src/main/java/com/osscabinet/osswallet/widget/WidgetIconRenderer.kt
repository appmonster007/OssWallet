package com.osscabinet.ossWallet.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath

/**
 * Draws one of the app's category [ImageVector] icons (the same ones the cards show, from
 * `getIconByName`) into a [Bitmap] so a RemoteViews ImageView can display it. Widgets cannot host
 * Compose, and RemoteViews cannot take a vector painter directly, so the vector's path data is
 * painted onto a plain Android canvas with a single tint colour.
 *
 * Only filled paths are drawn, which is all the Material icon set the app uses contains; strokes
 * and gradients are ignored.
 */
object WidgetIconRenderer {

    fun render(icon: ImageVector, sizePx: Int, tint: Int): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(size / icon.viewportWidth, size / icon.viewportHeight)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = tint
        }
        drawGroup(canvas, icon.root, paint, tint)
        return bitmap
    }

    private fun drawGroup(canvas: Canvas, group: VectorGroup, paint: Paint, tint: Int) {
        canvas.save()
        val matrix = Matrix().apply {
            postTranslate(-group.pivotX, -group.pivotY)
            postScale(group.scaleX, group.scaleY)
            postRotate(group.rotation)
            postTranslate(group.pivotX + group.translationX, group.pivotY + group.translationY)
        }
        canvas.concat(matrix)
        if (group.clipPathData.isNotEmpty()) {
            canvas.clipPath(group.clipPathData.toPath().asAndroidPath())
        }
        for (node: VectorNode in group) {
            when (node) {
                is VectorGroup -> drawGroup(canvas, node, paint, tint)
                is VectorPath -> if (node.fill != null) drawPath(canvas, node, paint, tint)
            }
        }
        canvas.restore()
    }

    private fun drawPath(canvas: Canvas, node: VectorPath, paint: Paint, tint: Int) {
        val path = Path().also { node.pathData.toPath(it) }.asAndroidPath()
        path.fillType = if (node.pathFillType == PathFillType.EvenOdd) {
            android.graphics.Path.FillType.EVEN_ODD
        } else {
            android.graphics.Path.FillType.WINDING
        }
        paint.color = tint
        paint.alpha = (node.fillAlpha * android.graphics.Color.alpha(tint)).toInt().coerceIn(0, 255)
        canvas.drawPath(path, paint)
    }
}
