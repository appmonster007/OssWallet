package com.osscabinet.ossWallet.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// MATERIAL 3 EXPRESSIVE COLOR SCHEME
// Following Material 3 Expressive guidelines with HCT color space
// 60-30-10 rule: 60% Neutral/Surface, 30% Primary/Secondary, 10% Tertiary/Accents
// ============================================================================

// LIGHT THEME - Expressive with proper HCT-based tones
// Primary (30%): Vibrant teal-green for growth/finance (Tone 80)
val PrimaryLight = Color(0xFF006A5D)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFF74F8D9)
val OnPrimaryContainerLight = Color(0xFF00201A)

// Secondary (30%): Warm amber-brown for complementary contrast (Tone 70)
val SecondaryLight = Color(0xFF6D5A3A)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFF4DDB5)
val OnSecondaryContainerLight = Color(0xFF231900)

// Tertiary (10%): Vibrant purple-violet for expressive accents (Tone 60)
val TertiaryLight = Color(0xFF5A3A6D)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFDDC0F6)
val OnTertiaryContainerLight = Color(0xFF1A0029)

// Error: High-chroma red for warnings (Tone 30)
val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)

// Neutral (60%): Grounded neutrals for surfaces/backgrounds
val BackgroundLight = Color(0xFFFBFDF9)  // Tone 98
val OnBackgroundLight = Color(0xFF191C1A)  // Tone 10
val SurfaceLight = Color(0xFFFBFDF9)  // Tone 98
val OnSurfaceLight = Color(0xFF191C1A)  // Tone 10

// Surface Variants - Tinted with primary for cohesive mood
val SurfaceVariantLight = Color(0xFFDBE5DE)  // Tone 92, slight primary tint
val OnSurfaceVariantLight = Color(0xFF3F4944)
val OutlineLight = Color(0xFF6F7974)
val OutlineVariantLight = Color(0xFFBFC9C2)
val InverseSurfaceLight = Color(0xFF2E312E)
val InverseOnSurfaceLight = Color(0xFFF0F1EC)

// Scrim & Shadow
val ScrimLight = Color(0xFF000000)
val ShadowLight = Color(0xFF000000)

// Container variants
val PrimaryFixedLight = Color(0xFF74F8D9)
val OnPrimaryFixedLight = Color(0xFF00201A)
val PrimaryFixedDimLight = Color(0xFF52DBC0)
val OnPrimaryFixedVariantLight = Color(0xFF005138)

val SecondaryFixedLight = Color(0xFFF4DDB5)
val OnSecondaryFixedLight = Color(0xFF231900)
val SecondaryFixedDimLight = Color(0xFFD7C19A)
val OnSecondaryFixedVariantLight = Color(0xFF524325)

val TertiaryFixedLight = Color(0xFFDDC0F6)
val OnTertiaryFixedLight = Color(0xFF1A0029)
val TertiaryFixedDimLight = Color(0xFFC0A5DA)
val OnTertiaryFixedVariantLight = Color(0xFF432954)

// DARK THEME - Expressive with proper HCT-based tones (avoid neon eye strain)
// Primary (30%): Light vibrant teal (Tone 80 for dark mode)
val PrimaryDark = Color(0xFF52DBC0)
val OnPrimaryDark = Color(0xFF00382F)
val PrimaryContainerDark = Color(0xFF005138)
val OnPrimaryContainerDark = Color(0xFF74F8D9)

// Secondary (30%): Light vibrant amber (Tone 70 for dark mode)
val SecondaryDark = Color(0xFFD7C19A)
val OnSecondaryDark = Color(0xFF3B2F15)
val SecondaryContainerDark = Color(0xFF524325)
val OnSecondaryContainerDark = Color(0xFFF4DDB5)

// Tertiary (10%): Light vibrant purple (Tone 60 for dark mode)
val TertiaryDark = Color(0xFFC0A5DA)
val OnTertiaryDark = Color(0xFF2F1643)
val TertiaryContainerDark = Color(0xFF432954)
val OnTertiaryContainerDark = Color(0xFFDDC0F6)

// Error: Light vibrant red (Tone 80 for dark mode to avoid neon)
val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)

// Neutral (60%): Deep neutrals for surfaces/backgrounds
val BackgroundDark = Color(0xFF191C1A)  // Tone 10
val OnBackgroundDark = Color(0xFFE1E3DF)  // Tone 90
val SurfaceDark = Color(0xFF191C1A)  // Tone 10
val OnSurfaceDark = Color(0xFFE1E3DF)  // Tone 90

// Surface Variants - Tinted with primary for cohesive mood
val SurfaceVariantDark = Color(0xFF3F4944)  // Tone 30, slight primary tint
val OnSurfaceVariantDark = Color(0xFFBFC9C2)
val OutlineDark = Color(0xFF8A938C)
val OutlineVariantDark = Color(0xFF3F4944)
val InverseSurfaceDark = Color(0xFFE1E3DF)
val InverseOnSurfaceDark = Color(0xFF191C18)

// Scrim & Shadow
val ScrimDark = Color(0xFF000000)
val ShadowDark = Color(0xFF000000)

// Container variants
val PrimaryFixedDark = Color(0xFF74F8D9)
val OnPrimaryFixedDark = Color(0xFF00201A)
val PrimaryFixedDimDark = Color(0xFF52DBC0)
val OnPrimaryFixedVariantDark = Color(0xFF005138)

val SecondaryFixedDark = Color(0xFFF4DDB5)
val OnSecondaryFixedDark = Color(0xFF231900)
val SecondaryFixedDimDark = Color(0xFFD7C19A)
val OnSecondaryFixedVariantDark = Color(0xFF524325)

val TertiaryFixedDark = Color(0xFFDDC0F6)
val OnTertiaryFixedDark = Color(0xFF1A0029)
val TertiaryFixedDimDark = Color(0xFFC0A5DA)
val OnTertiaryFixedVariantDark = Color(0xFF432954)

// ============================================================================
// SPECIAL COLORS - Following meaningful color usage guidelines
// ============================================================================

// Financial indicators - High contrast for accessibility (not using tertiary)
val PositiveAmountColor = Color(0xFF00C853)  // High-chroma green (Tone 60)
val NegativeAmountColor = Color(0xFFB3261E)  // Error color for consistency

// Accent colors (10%) - For expressive moments of delight
val AccentBlue = Color(0xFF2979FF)    // Tone 60
val AccentOrange = Color(0xFFFF9100)  // Tone 60
val AccentPurple = Color(0xFFD500F9)  // Tone 60

// Gradient colors for expressive backgrounds
val GradientStart = Color(0xFF006A5D)  // Primary
val GradientEnd = Color(0xFF52DBC0)    // Primary Dark

// Semantic colors - Using standard mental models
val SuccessColor = Color(0xFF00C853)    // Green for success
val WarningColor = Color(0xFFFF9100)    // Orange for warning
val InfoColor = Color(0xFF2979FF)      // Blue for info
