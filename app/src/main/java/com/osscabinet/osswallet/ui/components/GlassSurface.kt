package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.LocalAeonHaze
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Canonical glass surface shell for the Aeon design system.
 *
 * Encodes the two-sibling RenderNode pattern required for glass and shadow to coexist:
 * the shadow lives on its own [Spacer] graphicsLayer so hazeEffect's internal RenderEffect
 * cannot silently drop it. The background/blur/border modifier chain sits on the inner Box.
 *
 * Always renders [fallbackColor] as the surface background regardless of whether glass is
 * enabled or [hazeState] is null, so the component degrades gracefully in Compose Previews
 * and on screens that haven't wired up a hazeSource.
 *
 * The border and hazeEffect are gated on both [LocalGlassmorphismEnabled] and a non-null
 * [hazeState] so visual glass chrome never appears without the actual blur effect behind it.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    hazeState: HazeState? = null,
    hazeLayer: HazeLayer? = null,
    hazeStyle: HazeStyle = GlassTokens.style(),
    fallbackColor: Color = AppColors.cardSurfaceProminent,
    shadowElevation: Dp = 0.dp,
    ambientShadowAlpha: Float = 0.06f,
    spotShadowAlpha: Float = 0.12f,
    borderColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glassEnabled = LocalGlassmorphismEnabled.current
    val layers = LocalAeonHaze.current

    val effectiveHazeState = hazeState ?: when (hazeLayer) {
        HazeLayer.Surface -> layers?.surfaceState
        HazeLayer.Overlay -> layers?.overlayState
        null -> null
    }

    // Automatic recursive blur: Surfaces (Layer 1) must be captured as sources for Overlays (Layer 2).
    val autoSourceState = if (hazeLayer == HazeLayer.Surface) layers?.overlayState else null

    Box(
        modifier = modifier.then(
            if (autoSourceState != null) Modifier.hazeSource(autoSourceState)
            else Modifier
        )
    ) {
        // Shadow on its own RenderNode — hazeEffect installs a RenderEffect that silently
        // drops shadowElevation when both share the same graphicsLayer node.
        if (glassEnabled && shadowElevation > 0.dp) {
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        this.shape = shape
                        clip = false
                        this.shadowElevation = shadowElevation.toPx()
                        ambientShadowColor = Color.Black.copy(alpha = ambientShadowAlpha)
                        spotShadowColor = Color.Black.copy(alpha = spotShadowAlpha)
                    }
            )
        }
        Box(
            modifier = Modifier
                .graphicsLayer { this.shape = shape; clip = true }
                .then(
                    if (glassEnabled && effectiveHazeState != null) {
                        Modifier.hazeEffect(effectiveHazeState, hazeStyle)
                    } else {
                        Modifier.background(fallbackColor, shape)
                    }
                )
                .then(
                    if (glassEnabled && effectiveHazeState != null && borderColor != null)
                        Modifier.border(0.5.dp, borderColor, shape)
                    else Modifier
                ),
            content = content
        )
    }
}
