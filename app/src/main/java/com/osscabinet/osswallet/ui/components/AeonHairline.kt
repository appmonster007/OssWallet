package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.LocalThemeMode

/**
 * 1px hairline-gradient border for solid Editorial-Instrument panels.
 *
 * The gradient catches a faint [accent] rim at the top-left and fades into the
 * neutral [base] hairline — the same "edge lit" look as the HTML comps. Reads
 * the live accent token, so it automatically picks up the identity palette
 * (champagne/bronze) once that ships.
 */
@Composable
fun Modifier.aeonHairlineBorder(
    shape: Shape,
    width: Dp = 1.dp,
    accent: Color = AppColors.accent,
    base: Color = AppColors.outlineVariant,
): Modifier {
    val isDark = LocalThemeMode.current.isDarkTheme
    val brush = Brush.linearGradient(
        colors = listOf(
            accent.copy(alpha = if (isDark) 0.26f else 0.20f),
            base.copy(alpha = if (isDark) 0.22f else 0.55f),
            base.copy(alpha = if (isDark) 0.06f else 0.20f),
        ),
    )
    return this.border(width, brush, shape)
}

/**
 * Faint dotted underline used to signal a tappable mono readout (e.g. the card
 * period readout). Drawn under the full width of the element it modifies.
 */
@Composable
fun Modifier.aeonDottedUnderline(
    color: Color = AppColors.outlineVariant,
    strokeWidth: Dp = 1.dp,
): Modifier {
    return this.drawBehind {
        val y = size.height
        val sw = strokeWidth.toPx()
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = sw,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(2f * sw, 3f * sw), 0f,
            ),
        )
    }
}

/**
 * Dashed hairline divider drawn along the top edge of the element it modifies —
 * the editorial "ledger rule" used above Net totals and twin-column splits in
 * the H comps (`.net` / `.nw-flows` border-top: 1px dashed).
 */
@Composable
fun Modifier.aeonDashedTopRule(
    color: Color = AppColors.outlineVariant,
    strokeWidth: Dp = 1.dp,
): Modifier {
    return this.drawBehind {
        val sw = strokeWidth.toPx()
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = sw,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(4f * sw, 4f * sw), 0f,
            ),
        )
    }
}
