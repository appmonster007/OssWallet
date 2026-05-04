package com.osscabinet.ossWallet.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
 * - Duration: 350-400ms for large surface transitions
 * - Easing: Standard emphasized decelerate for enter, emphasized accelerate for exit
 * - No bounce: Smooth easing curves only (0 bounce)
 * - Slide offsets: Full width enter, partial width for depth on exit
 */

// Material 3 Expressive duration
private const val EXPRESSIVE_DURATION = 350
private const val FADE_DURATION = 200

// Material 3 Expressive easing curves (no bounce)
// Standard emphasized decelerate: cubic-bezier(0.2, 0.0, 0, 1.0) - smooth slow down
private val emphasizedDecelerate = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.0f, 0f, 1.0f)
// Standard emphasized accelerate: cubic-bezier(0.4, 0.0, 1, 1.0) - smooth speed up
private val emphasizedAccelerate = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0.0f, 1f, 1.0f)

/**
 * Standard screen enter - slides in from right with fade and subtle scale
 * No bounce, smooth decelerated motion
 */
val EnterTransitionSpec: EnterTransition =
    slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(
            durationMillis = EXPRESSIVE_DURATION,
            easing = emphasizedDecelerate
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
 * Standard screen exit - slides out to left with fade
 * Partial slide to show depth, faster fade
 */
val ExitTransitionSpec: ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 5 },
        animationSpec = tween(
            durationMillis = EXPRESSIVE_DURATION,
            easing = emphasizedAccelerate
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = FADE_DURATION,
            easing = emphasizedAccelerate
        )
    )

/**
 * Pop enter (going back) - slides in from left with fade
 * No bounce, smooth decelerated motion
 */
val PopEnterTransitionSpec: EnterTransition =
    slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth / 5 },
        animationSpec = tween(
            durationMillis = EXPRESSIVE_DURATION,
            easing = emphasizedDecelerate
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = EXPRESSIVE_DURATION,
            easing = emphasizedDecelerate
        )
    )

/**
 * Pop exit (going back) - slides out to right with fade and scale
 * No bounce, smooth accelerated motion
 */
val PopExitTransitionSpec: ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(
            durationMillis = EXPRESSIVE_DURATION,
            easing = emphasizedAccelerate
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
