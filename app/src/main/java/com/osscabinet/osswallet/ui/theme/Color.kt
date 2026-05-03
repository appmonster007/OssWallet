package com.osscabinet.ossWallet.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// ARCHITECT SLATE COLOR PALETTE (MAXIMIZED TONAL CONTRAST)
// Re-engineered for high-fidelity component separation following the Amoled pattern.
// 
// Contrast Strategy:
// Light: Pure White Base + Muted Slate Containers (Shadowless depth)
// Dark: Deep Carbon Base + Elevated Slate Containers (Vibrant depth)
// ============================================================================

// --- LIGHT THEME (Pristine Architect) ---

val PrimaryLight = Color(0xFF006874) 
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFF9EEFFD)
val OnPrimaryContainerLight = Color(0xFF001F24)

val SecondaryLight = Color(0xFF4D616C) 
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD0E6F2)
val OnSecondaryContainerLight = Color(0xFF081E27)

val TertiaryLight = Color(0xFF5D5B7D) 
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFE3DFFF)
val OnTertiaryContainerLight = Color(0xFF191836)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

// Refined Light Neutrals - Maximizing the White vs Slate gap
val BackgroundLight = Color(0xFFFFFFFF) // Absolute white background
val OnBackgroundLight = Color(0xFF171C1E)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF171C1E)

val SurfaceVariantLight = Color(0xFFDCE4E9)
val OnSurfaceVariantLight = Color(0xFF3F484C)
val OutlineLight = Color(0xFF6F797D)
val OutlineVariantLight = Color(0xFFBFC8CC)

val InverseSurfaceLight = Color(0xFF2C3133)
val InverseOnSurfaceLight = Color(0xFFEFF1F2)

// Surface Container Family (Architect Separation)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFE7EDF0) // Pronounced container separation
val SurfaceContainerLight = Color(0xFFDEE5E9)
val SurfaceContainerHighLight = Color(0xFFD6DDE2)
val SurfaceContainerHighestLight = Color(0xFFCED6DB)

// --- DARK THEME (Carbon Slate) ---

val PrimaryDark = Color(0xFF84D2E0) 
val OnPrimaryDark = Color(0xFF00363D)
val PrimaryContainerDark = Color(0xFF004F58)
val OnPrimaryContainerDark = Color(0xFF9EEFFD)

val SecondaryDark = Color(0xFFB4CAD6) 
val OnSecondaryDark = Color(0xFF1F333C)
val SecondaryContainerDark = Color(0xFF354A53)
val OnSecondaryContainerDark = Color(0xFFD0E6F2)

val TertiaryDark = Color(0xFFC6C2EA) 
val OnTertiaryDark = Color(0xFF2F2D4D)
val TertiaryContainerDark = Color(0xFF454364)
val OnTertiaryContainerDark = Color(0xFFE3DFFF)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Refined Dark Neutrals - Deep carbon base for card pop
val BackgroundDark = Color(0xFF07090A) // Deep Carbon background
val OnBackgroundDark = Color(0xFFDEE3E5)
val SurfaceDark = Color(0xFF07090A)
val OnSurfaceDark = Color(0xFFDEE3E5)

val SurfaceVariantDark = Color(0xFF3F484C)
val OnSurfaceVariantDark = Color(0xFFBFC8CC)
val OutlineDark = Color(0xFF899296)
val OutlineVariantDark = Color(0xFF3F484C)

val InverseSurfaceDark = Color(0xFFDEE3E5)
val InverseOnSurfaceDark = Color(0xFF07090A)

// Surface Container Family (Carbon Elevation)
val SurfaceContainerLowestDark = Color(0xFF040607)
val SurfaceContainerLowDark = Color(0xFF1B2327) // Strong tonal "lift" for cards
val SurfaceContainerDark = Color(0xFF242C31)
val SurfaceContainerHighDark = Color(0xFF2E363B)
val SurfaceContainerHighestDark = Color(0xFF384046)

// --- SPECIAL COLORS ---

val PositiveAmountColor = Color(0xFF00C853)
val NegativeAmountColor = Color(0xFFBA1A1A)

val SuccessColor = Color(0xFF00C853)
val WarningColor = Color(0xFFFF9100)
val InfoColor = Color(0xFF2979FF)
val AccentBlue = Color(0xFF2979FF)
val AccentOrange = Color(0xFFFF9100)
val AccentPurple = Color(0xFFD500F9)
