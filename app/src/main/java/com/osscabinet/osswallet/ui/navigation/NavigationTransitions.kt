package com.osscabinet.ossWallet.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * Material 3 Expressive Navigation Transitions
 *
 * Guidelines followed:
 * - Duration: 350-400ms for expressive feel (Material 3 recommends 350ms+ for prominent motion)
 * - Easing: Standard emphasized decelerate curve (fast out, slow in)
 * - Spring: Low stiffness, medium damping for natural, bouncy feel
 * - Combined: Slide + Fade + subtle Scale for depth perception
 */

// Material 3 Expressive duration (400ms for large surface transitions)
private const val EXPRESSIVE_DURATION = 400
private const val FADE_DURATION = 250

// Material 3 Expressive easing curves
// Standard emphasized decelerate: cubic-bezier(0.2, 0.0, 0, 1.0)
private val emphasizedDecelerate = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.0f, 0f, 1.0f)
// Standard emphasized accelerate: cubic-bezier(0.4, 0.0, 1, 1.0)
private val emphasizedAccelerate = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0.0f, 1f, 1.0f)

/**
 * Standard screen enter transition - slides in from right with fade
 */
val EnterTransitionSpec: EnterTransition =
    slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = EXPRESSIVE_DURATION,
            easing = emphasizedDecelerate
        )
    ) + scaleIn(
        initialScale = 0.95f,
        animationSpec = tween(
            durationMillis = EXPRESSIVE_DURATION,
            easing = emphasizedDecelerate
        )
    )

/**
 * Standard screen exit transition - slides out to left with fade
 */
val ExitTransitionSpec: ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 4 },
        animationSpec = tween(
            durationMillis = EXPRESSIVE_DURATION,
            easing = emphasizedDecelerate
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = FADE_DURATION,
            easing = emphasizedAccelerate
        )
    )

/**
 * Pop enter transition (when going back) - slides in from left with fade
 */
val PopEnterTransitionSpec: EnterTransition =
    slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth / 4 },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = EXPRESSIVE_DURATION,
            easing = emphasizedDecelerate
        )
    )

/**
 * Pop exit transition (when going back) - slides out to right with fade
 */
val PopExitTransitionSpec: ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = FADE_DURATION,
            easing = emphasizedAccelerate
        )
    ) + scaleOut(
        targetScale = 0.95f,
        animationSpec = tween(
            durationMillis = EXPRESSIVE_DURATION,
            easing = emphasizedAccelerate
        )
    )

/**
 * Fade-only transition for start destination (Dashboard)
 */
val FadeEnterTransitionSpec: EnterTransition = fadeIn(
    animationSpec = tween(
        durationMillis = EXPRESSIVE_DURATION,
        easing = emphasizedDecelerate
    )
)

val FadeExitTransitionSpec: ExitTransition = fadeOut(
    animationSpec = tween(
        durationMillis = FADE_DURATION,
        easing = emphasizedAccelerate
    )
)

/**
 * Bottom sheet style enter - slides up from bottom with fade
 */
val BottomSheetEnterSpec: EnterTransition =
    slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = EXPRESSIVE_DURATION,
            easing = emphasizedDecelerate
        )
    ) + scaleIn(
        initialScale = 0.92f,
        animationSpec = tween(
            durationMillis = EXPRESSIVE_DURATION,
            easing = emphasizedDecelerate
        )
    )

/**
 * Bottom sheet style exit - slides down with fade
 */
val BottomSheetExitSpec: ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(
            durationMillis = EXPRESSIVE_DURATION,
            easing = emphasizedDecelerate
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = FADE_DURATION,
            easing = emphasizedAccelerate
        )
    ) + scaleOut(
        targetScale = 0.92f,
        animationSpec = tween(
            durationMillis = EXPRESSIVE_DURATION,
            easing = emphasizedAccelerate
        )
    )
