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
 * Standardized Haze infrastructure for all Aeon screens.
 * Enforces an N-Layer Glass Architecture automatically:
 *
 * Each layer blurs everything below it and registers itself as a source for everything above it.
 * Level 0: Static Background (Color + Contours) captured for all higher states.
 */
@Composable
fun AeonHazeProvider(
    backgroundColor: Color = AppColors.screenBackground,
    drawContours: Boolean = true,
    content: @Composable (AeonHazeLayers) -> Unit
) {
    val existing = LocalAeonHaze.current
    val hazeLayers = remember(existing) { existing ?: AeonHazeLayers() }

    CompositionLocalProvider(LocalAeonHaze provides hazeLayers) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Level 0: Static Background Capture
            // We register the background as a source for EVERY state in the pool.
            val sourceModifier = hazeLayers.states.fold(Modifier as Modifier) { mod, state ->
                mod.hazeSource(state)
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
 * Implements auto-incrementing depth for N-layer glass stacking.
 *
 * @param hazeLayer The semantic depth of this surface. If null, uses [LocalHazeDepth].
 *                  Surface (0) blurs background.
 *                  Overlay (1) blurs background + Surface.
 *                  Drawer (2) blurs background + Surface + Overlay.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    hazeState: HazeState? = null,
    hazeLayer: Int? = null,
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
    val depth = hazeLayer ?: LocalHazeDepth.current

    // 1. BLUR LOGIC: Blur the state assigned to this depth.
    val effectiveHazeState = hazeState ?: layers?.states?.getOrNull(depth)

    // 2. SOURCE LOGIC: Register this surface as a source for EVERY state at a higher depth.
    val sourceModifier = if (layers != null && depth >= 0) {
        layers.states.drop(depth + 1).fold(Modifier as Modifier) { mod, state ->
            mod.hazeSource(state)
        }
    } else Modifier

    Box(modifier = modifier.then(sourceModifier)) {
        // Shadow on its own RenderNode
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
                )
        ) {
            // 3. INCREMENT DEPTH: Nested children will automatically blur this surface.
            CompositionLocalProvider(LocalHazeDepth provides depth + 1) {
                content()
            }
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
                    borderColor = Color.White.copy(alpha = 0.15f),
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
