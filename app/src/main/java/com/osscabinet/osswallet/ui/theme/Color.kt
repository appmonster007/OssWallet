package com.osscabinet.ossWallet.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ============================================================================
// ARCHITECT SLATE COLOR PALETTE (MAXIMIZED TONAL CONTRAST)
// Re-engineered for strict adherence to the Material 3 Expressive Tonal Loop.
// 
// Contrast Strategy:
// Light: Pure White Base + Pronounced Slate Containers
// Dark: Midnight Base + Elevated Charcoal Containers
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
val BackgroundLight = Color(0xFFFFFFFF) 
val OnBackgroundLight = Color(0xFF101416)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0x00101416)

val SurfaceVariantLight = Color(0xFFCAD4D9) // Level 3: Darkest Slate for recessed units
val OnSurfaceVariantLight = Color(0xFF3F484C)
val OutlineLight = Color(0xFF6F797D)
val OutlineVariantLight = Color(0xFFBFC8CC)

val InverseSurfaceLight = Color(0xFF2C3133)
val InverseOnSurfaceLight = Color(0xFFEFF1F2)

// Surface Container Family (Level 1-2 separation)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0x00000000)  // Level 1: Initial elevation
val SurfaceContainerLight = Color(0xFFE2E9ED)     
val SurfaceContainerHighLight = Color(0xFFD6DFE5) // Level 2: Secondary elevation
val SurfaceContainerHighestLight = Color(0xFFCAD5DC)

// --- DARK THEME (Midnight Slate) ---

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

// Refined Dark Neutrals - Deep Midnight for card pop
val BackgroundDark = Color(0xFF050708) 
val OnBackgroundDark = Color(0xFFE2E3E5)
val SurfaceDark = Color(0xFF050708)
val OnSurfaceDark = Color(0xFFE2E3E5)

val SurfaceVariantDark = Color(0xFF121619) // Level 3: Recessed "cut-out" units
val OnSurfaceVariantDark = Color(0xFFBFC8CC)
val OutlineDark = Color(0xFF70797D)
val OutlineVariantDark = Color(0xFF3F484C)

val InverseSurfaceDark = Color(0xFFDEE3E5)
val InverseOnSurfaceDark = Color(0xFF050708)

// Surface Container Family (Carbon Elevation)
val SurfaceContainerLowestDark = Color(0xFF020304)
val SurfaceContainerLowDark = Color(0xFF1D2429)  // Level 1: Elevated card
val SurfaceContainerDark = Color(0xFF262E34)
val SurfaceContainerHighDark = Color(0xFF303940) // Level 2: Highlighted row
val SurfaceContainerHighestDark = Color(0xFF3B464E)

// --- SPECIAL COLORS ---

val PositiveAmountColor = Color(0xFF22C55E) // Emerald 500
val NegativeAmountColor = Color(0xFFEF4444) // Red 500

val SuccessColor = Color(0xFF22C55E)
val WarningColor = Color(0xFFF59E0B)
val InfoColor = Color(0xFF3B82F6)
val AccentBlue = Color(0xFF3B82F6)
val AccentOrange = Color(0xFFF59E0B)
val AccentPurple = Color(0xFFA855F7)

// --- STATIC UTILITY COLORS ---
// True absolute colors needed for contrast-on-custom-accent logic and
// structural rendering that cannot be expressed as theme tokens.

val ColorTransparent = Color.Transparent       // No fill / no overlay
val ColorScrimDark  = Color(0x66000000)        // 40 % black overlay for FAB scrim
val ColorScrimPill  = Color.Black              // Pill nav scrim base (animated alpha)
val ColorOnDark     = Color.White              // Content rendered directly on a dark custom-accent surface
val ColorOnLight    = Color.Black              // Content rendered directly on a light custom-accent surface

// Neutral status color (paused / skipped / inactive states)
val NeutralStatusColor = Color(0xFF9E9E9E)     // Material Gray-500

// --- CHART STATIC COLORS ---
// Used as default parameter values or rendering constants in chart composables.
// These are deliberately static — charts render data-driven colors passed in,
// and these serve only as fallbacks or structural rendering aids.

val ChartGridColor      = Color(0x1A9E9E9E)    // 10 % gray grid lines
val ChartZeroLineColor  = Color(0x4D9E9E9E)    // 30 % gray zero axis
val ChartDotHighlight   = Color.White          // dot highlight ring on line charts
val ChartOverlayDark    = Color(0x0A000000)    // 4 % black depth shadow on treemap/pie
val ChartGlossHighlight = Color(0x26FFFFFF)    // 15 % white gloss on treemap cells
val ChartGlossGradEnd   = Color(0x1AFFFFFF)    // 10 % white gloss gradient end
val ChartRimHighlight   = Color(0x4DFFFFFF)    // 30 % white rim on treemap/pie
val ChartPieInnerShadow = Color(0x0D000000)    // 5 % black inner shadow on pie center

// Default chart series colors (used as parameter defaults only)
val ChartColorBlue      = Color(0xFF2979FF)
val ChartColorGreen     = Color(0xFF00C853)
val ChartColorRed       = Color(0xFFB3261E)
val ChartColorOrange    = Color(0xFFFF5722)

// Tag / category preset palette (user-facing color swatches)
val TagPresetColors = listOf(
    Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFFAB47BC), Color(0xFF7E57C2),
    Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF29B6F6), Color(0xFF26C6DA),
    Color(0xFF26A69A), Color(0xFF66BB6A), Color(0xFF9CCC65), Color(0xFFD4E157),
    Color(0xFFFFEE58), Color(0xFFFFCA28), Color(0xFFFFA726), Color(0xFFFF7043)
)

// =============================================================================
// APP COLORS
// Single entry point for all dynamic theme colors in @Composable functions.
// MaterialTheme.colorScheme must NOT appear outside Color.kt and Theme.kt.
//
// Prefer semantic aliases over raw M3 token names — semantic aliases let you
// retheme a whole UI role (e.g. all card backgrounds) by changing one line.
// Raw M3 tokens are kept for component params that require the M3 name.
// =============================================================================

val LocalAppColorScheme = staticCompositionLocalOf<ColorScheme> {
    error("No AppColorScheme provided. Wrap your UI in OssWalletTheme.")
}

object AppColors {

    // ── Accent / Brand ──────────────────────────────────────────────────────
    // Primary brand color. Use for buttons, FABs, active icons, and links.
    // Semantic aliases (accent*) and raw M3 names (primary*) point to the same tokens.
    val accent              : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.primary
    val onAccent            : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onPrimary
    val accentContainer     : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.primaryContainer
    val onAccentContainer   : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onPrimaryContainer
    val primary             : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.primary
    val onPrimary           : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onPrimary
    val primaryContainer    : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.primaryContainer
    val onPrimaryContainer  : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onPrimaryContainer

    // ── Secondary & Tertiary ────────────────────────────────────────────────
    val secondary            : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.secondary
    val onSecondary          : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onSecondary
    val secondaryContainer   : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.secondaryContainer
    val onSecondaryContainer : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onSecondaryContainer
    val tertiary             : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.tertiary
    val onTertiary           : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onTertiary
    val tertiaryContainer    : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.tertiaryContainer
    val onTertiaryContainer  : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onTertiaryContainer

    // ── Error & Feedback ────────────────────────────────────────────────────
    val error                : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.error
    val onError              : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onError
    val errorContainer       : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.errorContainer
    val onErrorContainer     : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onErrorContainer

    // ── Screen Background ───────────────────────────────────────────────────
    // Outermost scaffold fill behind all content.
    val screenBackground     : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.background
    val onScreenBackground   : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onBackground
    val background           : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.background
    val onBackground         : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onBackground

    // ── Surface Hierarchy ───────────────────────────────────────────────────
    // Surfaces ordered from lowest to highest visual elevation.
    // Change one semantic value to retheme every instance of that surface role.
    //
    //  recessedSurface      surfaceContainerLowest  sunken wells, inset inputs
    //  inputSurface         surfaceContainerLow     text field backgrounds
    //  cardSurface          surfaceContainerLow     standard card background
    //  cardSurfaceRaised    surfaceContainer        slightly raised cards
    //  sheetSurface         surfaceContainer        bottom sheets, dialogs
    //  cardSurfaceProminent surfaceContainerHigh    elevated / highlighted cards
    //  sheetSurfaceRaised   surfaceContainerHigh    raised sheet sections
    //  componentOnCardSubtle surfaceContainerHigh   subtle chips on cards
    //  componentOnCard      surfaceContainerHighest chips, tags on a card
    //  panelSurface         surface                 list rows, form containers
    //  drawerSurface        surface                 navigation drawer panel
    //  popupSurface         surface                 dropdowns, overlays
    //  insetSurface         surfaceVariant          section dividers, progress tracks
    //
    val recessedSurface          : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerLowest
    val inputSurface             : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerLow
    val cardSurface              : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainer
    val cardSurfaceRaised        : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainer
    val sheetSurface             : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainer
    val cardSurfaceProminent     : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerHigh
    val sheetSurfaceRaised       : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerHigh
    val componentOnCardSubtle    : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerHigh
    val componentOnCard          : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerHighest
    val panelSurface             : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surface
    val drawerSurface            : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surface
    val popupSurface             : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surface
    val insetSurface             : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceColorAtElevation(20.dp)

    // Raw M3 surface tokens (for component params that require the M3 name)
    val surface                  : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surface
    val onSurface                : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onSurface
    val surfaceVariant           : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceVariant
    val onSurfaceVariant         : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onSurfaceVariant
    val surfaceContainerLowest   : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerLowest
    val surfaceContainerLow      : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerLow
    val surfaceContainer         : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainer
    val surfaceContainerHigh     : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerHigh
    val surfaceContainerHighest  : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerHighest
    val inverseSurface           : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.inverseSurface
    val inverseOnSurface         : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.inverseOnSurface
    val inversePrimary           : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.inversePrimary
    val scrim                    : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.scrim

    // ── Content / Text ──────────────────────────────────────────────────────
    // Foreground colors for text and icons on any surface.
    val contentPrimary           : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onSurface
    val contentSecondary         : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onSurfaceVariant

    // ── Borders & Dividers ──────────────────────────────────────────────────
    // divider — subtle low-contrast separator (list row lines, card edges)
    // border  — prominent high-contrast outline (focused inputs, card frames)
    val divider                  : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.outlineVariant
    val border                   : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.outline
    val outline                  : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.outline
    val outlineVariant           : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.outlineVariant

    // ── Selection ───────────────────────────────────────────────────────────
    // Highlight for selected list items, active chips, and chosen options.
    val selectionContainer       : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.primaryContainer
    val onSelectionContainer     : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onPrimaryContainer

    // ── Toggle / Switch ─────────────────────────────────────────────────────
    val switchTrackOn            : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.primaryContainer
    val switchTrackOff           : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceVariant
    val switchThumbOn            : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onPrimaryContainer
    val switchThumbOff           : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onSurfaceVariant

    // ── Form Entry Components ───────────────────────────────────────────────
    // Icon container tints in entry / edit forms.
    val formIconContainerPrimary   : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.primary
    val formIconContainerSecondary : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.secondary
    val formIconContainerTertiary  : Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.tertiary

    // ── Absolute / Transparent ──────────────────────────────────────────────
    // Not theme tokens — safe to use where a hard "no fill" value is required.
    val transparent                : Color get() = Color.Transparent
}
