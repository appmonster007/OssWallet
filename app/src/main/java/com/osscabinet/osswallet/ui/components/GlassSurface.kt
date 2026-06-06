package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.osscabinet.ossWallet.ui.theme.AeonHazeLayers
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.LocalAeonHaze
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.LocalHazeDepth
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Safe wrapper for [hazeSource] that prevents recursive drawing crashes in Haze 1.7.2+.
 */
fun Modifier.aeonHazeSource(state: HazeState?): Modifier = if (state != null) {
    this.hazeSource(state)
} else this

/**
 * Standardized Haze infrastructure for all Aeon screens.
 *
 * Always creates a fresh [AeonHazeLayers] pool — each screen owns its own capture state.
 * The background Box is the sole hazeSource registrar for all depth levels.
 * GlassSurface elements are pure consumers (hazeEffect only).
 */
@Composable
fun AeonHazeProvider(
    backgroundColor: Color = AppColors.screenBackground,
    drawContours: Boolean = true,
    content: @Composable (AeonHazeLayers) -> Unit
) {
    val hazeLayers = remember { AeonHazeLayers() }

    CompositionLocalProvider(LocalAeonHaze provides hazeLayers) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Static background: sole hazeSource for Surface, Overlay, and Drawer states.
            val sourceModifier = hazeLayers.states.take(3).fold(Modifier as Modifier) { mod, state ->
                mod.aeonHazeSource(state)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(sourceModifier)
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                )
                if (drawContours) ContourBackground()
            }

            content(hazeLayers)
        }
    }
}

/**
 * Canonical glass surface shell for the Aeon design system.
 * Pure hazeEffect consumer — never registers as hazeSource.
 *
 * @param hazeLayer The semantic depth of this surface. If null, uses [LocalHazeDepth].
 *                  Surface (0) blurs background.
 *                  Overlay (1) blurs background + any explicitly registered surface content.
 *                  Drawer (2) blurs all screen content (captured via NavHost hazeSource).
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    hazeState: HazeState? = null,
    hazeLayer: Int? = null,
    hazeStyle: HazeStyle = GlassTokens.style(),
    fallbackColor: Color = AppColors.cardSurfaceProminent,
    shadowElevation: Dp? = null,
    ambientShadowAlpha: Float = 0.1f,
    spotShadowAlpha: Float = 0.2f,
    borderColor: Color? = null,
    borderAlpha: Float? = null,
    borderWidth: Dp? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val glassEnabled = LocalGlassmorphismEnabled.current
    val layers = LocalAeonHaze.current
    val depth = hazeLayer ?: LocalHazeDepth.current
    val config = GlassTokens.config(depth)

    // Default glass rim that catches light at the edges
    val effectiveBorderAlpha = borderAlpha ?: config.borderAlpha
    val glassRimColor = if (isDark) {
        Color.White.copy(alpha = effectiveBorderAlpha)
    } else {
        // In light mode, we use a more defined border to prevent blending
        AppColors.outlineVariant.copy(alpha = effectiveBorderAlpha * 2.5f) // definition boost
    }
    val effectiveBorderColor = borderColor ?: glassRimColor
    val effectiveBorderWidth = borderWidth ?: 0.5.dp

    // Theme-aware shadow intensity
    val effectiveAmbientAlpha = if (isDark) ambientShadowAlpha else ambientShadowAlpha * 1.5f
    val effectiveSpotAlpha = if (isDark) spotShadowAlpha else spotShadowAlpha * 1.5f
    val effectiveShadowElevation = shadowElevation ?: config.shadowElevation.dp

    // Blur the state assigned to this depth. GlassSurface is a pure consumer — no hazeSource.
    val effectiveHazeState = hazeState ?: layers?.states?.getOrNull(depth)

    Box(
        modifier = modifier,
        propagateMinConstraints = true
    ) {
        // Shadow on its own RenderNode
        if (glassEnabled && effectiveShadowElevation > 0.dp) {
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        this.shape = shape
                        clip = false
                        this.shadowElevation = effectiveShadowElevation.toPx()
                        ambientShadowColor = Color.Black.copy(alpha = effectiveAmbientAlpha)
                        spotShadowColor = Color.Black.copy(alpha = effectiveSpotAlpha)
                    }
            )
        }
        
        // Background layer (Blur/Glass) + Content Container
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
                    if (glassEnabled && effectiveHazeState != null)
                        Modifier.border(effectiveBorderWidth, effectiveBorderColor, shape)
                    else Modifier
                )
        ) {
            content()
        }
    }
}

/**
 * A custom glass bottom sheet that stays in the same window hierarchy.
 * This is essential for glassmorphism because ModalBottomSheet creates a new window,
 * which loses access to the Haze background capture.
 */
@Composable
fun AeonGlassSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeLayer: Int = HazeLayer.Overlay,
    content: @Composable ColumnScope.() -> Unit
) {
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
    ) {
        var visible by remember { mutableStateOf(false) }
        SideEffect { visible = true }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = modifier
            ) {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    hazeLayer = hazeLayer,
                    hazeStyle = GlassTokens.sheetStyle(),
                    fallbackColor = AppColors.sheetSurface,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Standard Drag Handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 4.dp)
                                    .background(
                                        AppColors.outlineVariant.copy(alpha = 0.3f),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                        content()
                    }
                }
            }
        }
    }
}
