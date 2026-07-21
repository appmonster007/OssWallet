package com.osscabinet.ossWallet.ui.screens.shortcut

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.QuickAddActivity

fun buildShortcutIconBitmap(
    painter: Painter,
    density: Density,
    bgColor: Color
): Bitmap {
    val sizePx = (108 * density.density).toInt().coerceAtLeast(108)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val composeCanvas = Canvas(android.graphics.Canvas(bitmap))

    val iconSize = sizePx * 0.45f
    val iconOffset = (sizePx - iconSize) / 2f

    CanvasDrawScope().draw(
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = composeCanvas,
        size = Size(sizePx.toFloat(), sizePx.toFloat())
    ) {
        drawCircle(color = bgColor, radius = sizePx / 2f, center = center)
        translate(left = iconOffset, top = iconOffset) {
            with(painter) {
                draw(size = Size(iconSize, iconSize), colorFilter = ColorFilter.tint(Color.White))
            }
        }
    }

    return bitmap
}

fun createTemplateHomeScreenShortcut(
    context: Context,
    template: Template,
    iconBitmap: Bitmap
) {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return

    val shortcutIntent = Intent(context, QuickAddActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        putExtra(QuickAddActivity.EXTRA_TEMPLATE_ID, template.id)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    val info = ShortcutInfoCompat.Builder(context, "template_${template.id}_${System.currentTimeMillis()}")
        .setShortLabel(template.name)
        .setLongLabel("Quick Add: ${template.name}")
        .setIcon(IconCompat.createWithAdaptiveBitmap(iconBitmap))
        .setIntent(shortcutIntent)
        .build()

    ShortcutManagerCompat.requestPinShortcut(context, info, null)
}
