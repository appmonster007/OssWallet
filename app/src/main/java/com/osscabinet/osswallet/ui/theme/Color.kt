package com.osscabinet.ossWallet.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.osscabinet.ossWallet.domain.formatter.ThemeMode
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.settings.CardOpenAnimation
import com.osscabinet.ossWallet.domain.model.settings.UiIdentity

// --- AMOLED THEME ---

val PrimaryAmoled = Color(0xFFDDE2FA)
val OnPrimaryAmoled = Color(0xFF000000)
val PrimaryContainerAmoled = Color(0xFF2F3A88)
val OnPrimaryContainerAmoled = Color(0xFFDDE2FA)

val BackgroundAmoled = Color(0xFF000000)
val OnBackgroundAmoled = Color(0xFFFFFFFF)
val SurfaceAmoled = Color(0xFF050509)
val OnSurfaceAmoled = Color(0xFFFFFFFF)

val SurfaceVariantAmoled = Color(0xFF11111A)
val OnSurfaceVariantAmoled = Color(0xFF9091A0)
val OutlineAmoled = Color(0x24FFFFFF) // rgba(255,255,255,0.14)
val OutlineVariantAmoled = Color(0x1AFFFFFF) // rgba(255,255,255,0.10)

val SurfaceContainerLowestAmoled = Color(0xFF000000)
val SurfaceContainerLowAmoled = Color(0xFF0A0C10) // N-4
val SurfaceContainerAmoled = Color(0xFF0E1015)    // N-6
val SurfaceContainerHighAmoled = Color(0xFF191C20) // N-10
val SurfaceContainerHighestAmoled = Color(0xFF1D2024) // N-12

// --- H "EDITORIAL INSTRUMENT" PALETTE ---
// Warm-black / ivory / champagne (dark + amoled) and ivory / ink / bronze (light).
// Neutrals are fixed under H; only the accent (primary) is dynamic-accent eligible.

// Dark
val HDarkBackground = Color(0xFF0B0B0A)
val HDarkSurfaceContainerLowest = Color(0xFF0E0E0D)
val HDarkSurfaceContainerLow = Color(0xFF111110)
val HDarkSurfaceContainer = Color(0xFF131312)       // panel / card body
val HDarkSurfaceContainerHigh = Color(0xFF1A1A18)
val HDarkSurfaceContainerHighest = Color(0xFF201F1D)
val HDarkSurfaceVariant = Color(0xFF1A1A18)
val HDarkOnSurface = Color(0xFFECE8DD)              // ivory
val HDarkOnSurfaceVariant = Color(0xFF9A958A)
val HDarkOutline = Color(0x2EECE8DD)               // ivory ~18%
val HDarkOutlineVariant = Color(0x17ECE8DD)        // ivory ~9% hairline
val HDarkAccent = Color(0xFFD6C189)                // champagne
val HDarkOnAccent = Color(0xFF2A2410)
val HDarkAccentContainer = Color(0xFF3A3320)
val HDarkOnAccentContainer = Color(0xFFF0E4C0)

// Light
val HLightBackground = Color(0xFFF2EFE7)
val HLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val HLightSurfaceContainerLow = Color(0xFFFAF8F1)
val HLightSurfaceContainer = Color(0xFFFAF8F1)      // panel / card body
val HLightSurfaceContainerHigh = Color(0xFFF4F1E8)
val HLightSurfaceContainerHighest = Color(0xFFEEEADF)
val HLightSurfaceVariant = Color(0xFFEAE6DA)
val HLightOnSurface = Color(0xFF1B1914)             // ink
val HLightOnSurfaceVariant = Color(0xFF6B6658)
val HLightOutline = Color(0xFFC9C2B2)
val HLightOutlineVariant = Color(0xFFDED9CC)        // hairline
val HLightAccent = Color(0xFF8A6F3C)               // bronze
val HLightOnAccent = Color(0xFFFFFFFF)
val HLightAccentContainer = Color(0xFFE8DCC0)
val HLightOnAccentContainer = Color(0xFF3A2E14)

// AMOLED (true black, panels lifted slightly, hairlines brightened)
val HAmoledBackground = Color(0xFF000000)
val HAmoledSurfaceContainerLowest = Color(0xFF000000)
val HAmoledSurfaceContainerLow = Color(0xFF080807)
val HAmoledSurfaceContainer = Color(0xFF0C0C0B)     // panel / card body
val HAmoledSurfaceContainerHigh = Color(0xFF141413)
val HAmoledSurfaceContainerHighest = Color(0xFF1A1A18)
val HAmoledSurfaceVariant = Color(0xFF141413)
val HAmoledOutline = Color(0x33ECE8DD)             // ivory ~20%
val HAmoledOutlineVariant = Color(0x1FECE8DD)      // ivory ~12% hairline

// Semantic feedback (pos / neg) under H
val HPositiveDark = Color(0xFF7FBF8E)
val HNegativeDark = Color(0xFFD88379)
val HPositiveLight = Color(0xFF2C6E49)
val HNegativeLight = Color(0xFF9B3B30)

// Quiet secondary/tertiary roles — warm neutrals so nothing falls back to Material purple.
// (M3 leaves these at default values unless explicitly overridden in the scheme.)
val HDarkSecondary = Color(0xFFC4BDAE)
val HDarkOnSecondary = Color(0xFF2A2820)
val HDarkSecondaryContainer = Color(0xFF26241E)
val HDarkOnSecondaryContainer = Color(0xFFE4DECF)
val HLightSecondary = Color(0xFF5C5648)
val HLightOnSecondary = Color(0xFFFFFFFF)
val HLightSecondaryContainer = Color(0xFFE6E1D4)
val HLightOnSecondaryContainer = Color(0xFF3A352A)

// Error family aligned to the H negative tone (destructive == negative in a finance app).
val HDarkError = Color(0xFFD88379)
val HDarkOnError = Color(0xFF2A1410)
val HDarkErrorContainer = Color(0xFF4A1F18)
val HDarkOnErrorContainer = Color(0xFFF0C0B8)
val HLightError = Color(0xFF9B3B30)
val HLightOnError = Color(0xFFFFFFFF)
val HLightErrorContainer = Color(0xFFF0D8D2)
val HLightOnErrorContainer = Color(0xFF3A140E)

// --- GLASS THEME PREPARATION ---
// Tokens for upcoming glass implementation as per colors.md

val GlassBlurLight = 24.dp
val GlassBlurDark = 28.dp

val GlassSurfaceOpacityLight = 0.62f
val GlassSurfaceOpacityDark = 0.09f
val GlassSurface3OpacityLight = 0.42f
val GlassSurface3OpacityDark = 0.07f

val GlassBorderOpacityLight = 0.85f
val GlassBorderOpacityDark = 0.22f

// --- SPECIAL COLORS ---

val PositiveAmountColor = Color(0xFF1E9B4F) // Aligned with colors.md Light
val NegativeAmountColor = Color(0xFFC8463F) // Aligned with colors.md Light
val PositiveAmountDark = Color(0xFF4DCE85)  // Dark-theme positive amount
val NegativeAmountDark = Color(0xFFE27974)  // Dark-theme negative amount

val SuccessColor = Color(0xFF22C55E)
val WarningColor = Color(0xFFF59E0B)
val InfoColor = Color(0xFF3B82F6)
val AccentBlue = Color(0xFF3B82F6)
val AccentOrange = Color(0xFFF59E0B)
val AccentPurple = Color(0xFFA855F7)
val AccentDeepPurple = Color(0xFF673AB7)

// --- STATIC UTILITY COLORS ---
// True absolute colors needed for contrast-on-custom-accent logic and
// structural rendering that cannot be expressed as theme tokens.

val ColorTransparent = Color.Transparent       // No fill / no overlay
val ColorScrimDark = Color(0x66000000)        // 40 % black overlay for FAB scrim
val ColorScrimPill = Color.Black              // Pill nav scrim base (animated alpha)
val ColorOnDark =
    Color.White              // Content rendered directly on a dark custom-accent surface
val ColorOnLight =
    Color.Black              // Content rendered directly on a light custom-accent surface
val NearBlack = Color(0xFF1A1A1A)             // Near-black ink / structural dark surfaces

// Neutral status color (paused / skipped / inactive states)
val NeutralStatusColor = Color(0xFF9E9E9E)     // Material Gray-500

// Light neutral background used in preview wrappers and structural scaffolding
val PreviewLightGray = Color(0xFFF5F5F5)

// --- CHART STATIC COLORS ---
// Used as default parameter values or rendering constants in chart composables.
// These are deliberately static — charts render data-driven colors passed in,
// and these serve only as fallbacks or structural rendering aids.

val ChartGridColor = Color(0x1A9E9E9E)    // 10 % gray grid lines
val ChartZeroLineColor = Color(0x4D9E9E9E)    // 30 % gray zero axis
val ChartDotHighlight = Color.White          // dot highlight ring on line charts
val ChartOverlayDark = Color(0x0A000000)    // 4 % black depth shadow on treemap/pie
val ChartGlossHighlight = Color(0x26FFFFFF)    // 15 % white gloss on treemap cells
val ChartGlossGradEnd = Color(0x1AFFFFFF)    // 10 % white gloss gradient end
val ChartRimHighlight = Color(0x4DFFFFFF)    // 30 % white rim on treemap/pie
val ChartPieInnerShadow = Color(0x0D000000)    // 5 % black inner shadow on pie center

// Default chart series colors (used as parameter defaults only)
val ChartColorBlue = Color(0xFF2979FF)
val ChartColorGreen = Color(0xFF00C853)
val ChartColorRed = Color(0xFFB3261E)
val ChartColorOrange = Color(0xFFFF5722)

// Category icon / badge palette — semantic colors for category icon tints in UI samples
val CategoryBrown = Color(0xFF795548)
val CategoryPink = Color(0xFFE91E63)

// Tag / category preset palette (user-facing color swatches)
val TagPresetColors = listOf(
    Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFFAB47BC), Color(0xFF7E57C2),
    Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF29B6F6), Color(0xFF26C6DA),
    Color(0xFF26A69A), Color(0xFF66BB6A), Color(0xFF9CCC65), Color(0xFFD4E157),
    Color(0xFFFFEE58), Color(0xFFFFCA28), Color(0xFFFFA726), Color(0xFFFF7043)
)

val BrandPresetColorHexes = listOf(
    "#004D5F", "#1A237E", "#1B5E20", "#B71C1C", "#4A148C",
    "#E65100", "#006064", "#212121", "#795548", "#607D8B"
)

const val DefaultAccentColorHex = "#004D5F"
val DefaultAccentColor = Color(0xFF004D5F)  // Color form of DefaultAccentColorHex
val DeepTealGreen = Color(0xFF004D40)        // Deep teal-green for accent/preview use

val AccountStructureType.color: Color
    get() = Color(this.colorHex.toColorInt())

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

val LocalThemeMode = staticCompositionLocalOf<ThemeMode> {
    error("No ThemeMode provided. Wrap your UI in OssWalletTheme.")
}

val LocalUiIdentity = staticCompositionLocalOf {
    UiIdentity.DEFAULT
}

/** The user's chosen card-open animation, provided by [OssWalletTheme] from settings. */
val LocalCardOpenAnimation = staticCompositionLocalOf {
    CardOpenAnimation.DEFAULT
}

object AppColors {

    // ── Accent / Brand ──────────────────────────────────────────────────────
    // Primary brand color. Use for buttons, FABs, active icons, and links.
    // Semantic aliases (accent*) and raw M3 names (primary*) point to the same tokens.
    val accent: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.primary
    val onAccent: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onPrimary
    val accentContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.primaryContainer
    val onAccentContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onPrimaryContainer
    val primary: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.primary
    val onPrimary: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onPrimary
    val primaryContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.primaryContainer
    val onPrimaryContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onPrimaryContainer

    // ── Secondary & Tertiary ────────────────────────────────────────────────
    val secondary: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.secondary
    val onSecondary: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onSecondary
    val secondaryContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.secondaryContainer
    val onSecondaryContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onSecondaryContainer
    val tertiary: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.tertiary
    val onTertiary: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onTertiary
    val tertiaryContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.tertiaryContainer
    val onTertiaryContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onTertiaryContainer

    // ── Error & Feedback ────────────────────────────────────────────────────
    val error: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.error
    val onError: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onError
    val errorContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.errorContainer
    val onErrorContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onErrorContainer

    // ── Semantic Feedback (colors.md 2.7) ──────────────────────────────────
    // Positive (Income/Profit) and Negative (Expense/Loss) colors.
    val positive: Color
        @Composable @ReadOnlyComposable get() {
            val mode = LocalThemeMode.current
            if (LocalUiIdentity.current == UiIdentity.H) {
                return if (mode.isDarkTheme || mode.isAmoled) HPositiveDark else HPositiveLight
            }
            return when {
                mode.isAmoled -> Color(0xFF5BE093)
                mode.isDarkTheme -> Color(0xFF4DCE85)
                else -> Color(0xFF1E9B4F)
            }
        }
    val negative: Color
        @Composable @ReadOnlyComposable get() {
            val mode = LocalThemeMode.current
            if (LocalUiIdentity.current == UiIdentity.H) {
                return if (mode.isDarkTheme || mode.isAmoled) HNegativeDark else HNegativeLight
            }
            return when {
                mode.isAmoled -> Color(0xFFF08782)
                mode.isDarkTheme -> Color(0xFFE27974)
                else -> Color(0xFFC8463F)
            }
        }

    // ── Screen Background ───────────────────────────────────────────────────
    // Outermost scaffold fill behind all content.
    val screenBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.background
    val onScreenBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onBackground
    val background: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.background
    val onBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onBackground

    // ── Surface Hierarchy ───────────────────────────────────────────────────
    // Surfaces ordered from lowest to highest visual elevation.
    // Change one semantic value to retheme every instance of that surface role.
    //
    //  recessedSurface       surfaceContainerLowest  sunken wells (darker than screen in dark)
    //  cardSurface           surfaceContainer        standard card background
    //  sheetSurface          surfaceContainerLow     bottom sheets, dialogs (M3 spec)
    //  cardSurfaceRaised     surfaceContainerHigh    slightly raised cards
    //  cardSurfaceProminent  surfaceContainerHigh    elevated / highlighted cards
    //  sheetSurfaceRaised    surfaceContainerHigh    raised sheet sections
    //  componentOnCardSubtle surfaceContainerHighest subtle chips on a card (one step above card)
    //  componentOnCard       surfaceContainerHighest chips, tags on a card
    //  panelSurface          surfaceContainer        list rows, form containers
    //  drawerSurface         surfaceContainerLow     navigation drawer (M3 spec; glass is primary)
    //  popupSurface          surfaceContainerHigh    dropdowns, overlays (float above content)
    //  inputSurface          surfaceContainerHighest text field backgrounds (M3 filled field spec)
    //  insetSurface          surfaceVariant          section dividers, progress tracks
    //
    val recessedSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerLowest
    val inputSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerHighest
    val cardSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainer
    val cardSurfaceRaised: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerHigh
    val sheetSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerLow
    val cardSurfaceProminent: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerHigh
    val sheetSurfaceRaised: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerHigh
    val componentOnCardSubtle: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerHighest
    val componentOnCard: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerHighest
    val panelSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainer
    val onPanelSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onSurface
    val drawerSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerLow
    val popupSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerHigh
    val insetSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceVariant

    // Raw M3 surface tokens (for component params that require the M3 name)
    val surface: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surface
    val onSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onSurface
    val surfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceVariant
    val onSurfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onSurfaceVariant
    val surfaceContainerLowest: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerLowest
    val surfaceContainerLow: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerLow
    val surfaceContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainer
    val surfaceContainerHigh: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerHigh
    val surfaceContainerHighest: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceContainerHighest
    val inverseSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.inverseSurface
    val inverseOnSurface: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.inverseOnSurface
    val inversePrimary: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.inversePrimary
    val scrim: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.scrim

    // ── Content / Text ──────────────────────────────────────────────────────
    // Foreground colors for text and icons on any surface.
    val contentPrimary: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onSurface
    val contentSecondary: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onSurfaceVariant

    // ── Borders & Dividers ──────────────────────────────────────────────────
    // divider — subtle low-contrast separator (list row lines, card edges)
    // border  — prominent high-contrast outline (focused inputs, card frames)
    val divider: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.outlineVariant
    val border: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.outline
    val outline: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.outline
    val outlineVariant: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.outlineVariant

    // ── Selection ───────────────────────────────────────────────────────────
    // Highlight for selected list items, active chips, and chosen options.
    val selectionContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.primaryContainer
    val onSelectionContainer: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onPrimaryContainer

    // ── Toggle / Switch ─────────────────────────────────────────────────────
    val switchTrackOn: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.primaryContainer
    val switchTrackOff: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceVariant
    val switchThumbOn: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onPrimaryContainer
    val switchThumbOff: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onSurfaceVariant

    // ── Form Entry Components ───────────────────────────────────────────────
    // Icon container tints in entry / edit forms.
    val formIconContainerPrimary: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.primaryContainer
    val formIconContainerSecondary: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.secondaryContainer
    val formIconContainerTertiary: Color @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.tertiaryContainer

    // ── Absolute / Transparent ──────────────────────────────────────────────
    // Not theme tokens — safe to use where a hard "no fill" value is required.
    val transparent: Color get() = Color.Transparent
}

/**
 * Lifts a 24-bit RGB int (e.g. `Category.colorHex`, which is signed and may have
 * a zero alpha byte) into an opaque [Color]. Centralises the `or (0xFF shl 24)`
 * bit-trick so UI files don't have to reason about ARGB packing.
 */
fun Int.toOpaqueColor(): Color = Color(this or (0xFF shl 24))
