package com.osscabinet.ossWallet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.R

/**
 * All supported app fonts. Add new entries here to make them available throughout the app —
 * no other typography code needs to change.
 *
 * [displayFamily] is used for headings/display text; [bodyFamily] for body/label text.
 * For [SYSTEM] both resolve to [FontFamily.Default].
 */
enum class AppFont(
    val displayName: String,
    val displayFamily: FontFamily,
    val bodyFamily: FontFamily
) {
    MANROPE(
        displayName = "Manrope",
        displayFamily = FontFamily(Font(R.font.manrope)),
        bodyFamily = FontFamily.Default
    ),
    INTER(
        displayName = "Inter",
        displayFamily = FontFamily.Default,
        bodyFamily = FontFamily.Default
    ),
    SYSTEM(
        displayName = "System Default",
        displayFamily = FontFamily.Default,
        bodyFamily = FontFamily.Default
    );

    companion object {
        val DEFAULT = MANROPE
        fun fromName(name: String): AppFont = entries.find { it.name == name } ?: DEFAULT
    }
}

// Typographic scale: sizes, weights, spacing — defined once with no font concern.
// Font selection only injects fontFamily via createTypography(); nothing else changes.
private val displayScale = object {
    val large = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum"
    )
    val medium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum"
    )
    val small = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum"
    )
    val headlineLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum"
    )
    val headlineMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum"
    )
    val headlineSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum"
    )
    val titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
        fontFeatureSettings = "tnum"
    )
    val titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        fontFeatureSettings = "tnum"
    )
    val titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        fontFeatureSettings = "tnum"
    )
}

private val bodyScale = object {
    val large = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        fontFeatureSettings = "tnum"
    )
    val medium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        fontFeatureSettings = "tnum"
    )
    val small = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        fontFeatureSettings = "tnum"
    )
    val labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        fontFeatureSettings = "tnum"
    )
    val labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        fontFeatureSettings = "tnum"
    )
    val labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        fontFeatureSettings = "tnum"
    )
}


// Only fontFamily is injected here. All other properties come from the scale objects above.
fun createTypography(font: AppFont): Typography = Typography(
    displayLarge = displayScale.large.copy(fontFamily = font.displayFamily),
    displayMedium = displayScale.medium.copy(fontFamily = font.displayFamily),
    displaySmall = displayScale.small.copy(fontFamily = font.displayFamily),
    headlineLarge = displayScale.headlineLarge.copy(fontFamily = font.displayFamily),
    headlineMedium = displayScale.headlineMedium.copy(fontFamily = font.displayFamily),
    headlineSmall = displayScale.headlineSmall.copy(fontFamily = font.displayFamily),
    titleLarge = displayScale.titleLarge.copy(fontFamily = font.displayFamily),
    titleMedium = displayScale.titleMedium.copy(fontFamily = font.displayFamily),
    titleSmall = displayScale.titleSmall.copy(fontFamily = font.displayFamily),
    bodyLarge = bodyScale.large.copy(fontFamily = font.bodyFamily),
    bodyMedium = bodyScale.medium.copy(fontFamily = font.bodyFamily),
    bodySmall = bodyScale.small.copy(fontFamily = font.bodyFamily),
    labelLarge = bodyScale.labelLarge.copy(fontFamily = font.bodyFamily),
    labelMedium = bodyScale.labelMedium.copy(fontFamily = font.bodyFamily),
    labelSmall = bodyScale.labelSmall.copy(fontFamily = font.bodyFamily),
)

val Typography.labelExtraSmall: TextStyle
    get() = labelSmall.copy(fontSize = 9.sp)

// =============================================================================
// APP TEXT STYLES
// Single entry point for all inline text style expressions in @Composable
// functions. MaterialTheme.typography must NOT be accessed directly in
// composables — use AppTextStyles.xxx instead.
//
// Tokens are grouped by semantic role. Where multiple call-sites share the
// same expression, a single token covers all of them.
// =============================================================================

object AppTextStyles {

    // ── Transaction Headers ──────────────────────────────────────────────────
    // Section date / group title row above a list of transactions.
    val transactionGroupTitle: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

    // Amount displayed on the transaction row (positive / negative numeral).
    val transactionAmount: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)

    // Secondary meta-data line under the amount (e.g. converted currency).
    val transactionAmountMeta: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)

    // ── Transaction List Items ───────────────────────────────────────────────
    // Primary label of a transaction row (payee / description).
    val transactionTitle: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodyMedium

    // Secondary detail on a transaction row (account, category).
    val transactionSubtitle: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodySmall

    // Tiny badge / status label on a transaction row.
    val transactionBadge: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)

    // Overdue / upcoming marker micro-label.
    val transactionStatusMicro: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp)

    // Swipe-action label on a transaction row.
    val transactionSwipeAction: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall

    // ── Card Titles & Hero Amounts ───────────────────────────────────────────
    // Dashboard / analytics card title.
    val cardTitle: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)

    // Smaller card section sub-title.
    val cardSectionTitle: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)

    // Large hero number (e.g. total net worth on the dashboard).
    val heroAmount: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)

    // Extra-large display hero number (e.g. account balance on detail screen).
    val heroAmountDisplay: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)

    // Supporting label beneath a hero amount.
    val heroAmountLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall

    // Secondary figure inside a card (e.g. expense sub-total).
    val cardSecondaryAmount: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)

    // Inline delta / change indicator.
    val cardDelta: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)

    // ── Form Labels & Entry Fields ───────────────────────────────────────────
    // Section header label inside a form (e.g. "Amount", "Account").
    val formSectionLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)

    // Hint / placeholder text inside an entry field.
    val formFieldHint: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall

    // Primary value text inside an entry field.
    val formFieldValue: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)

    // Supporting note / description beneath a form field.
    val formFieldNote: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodySmall

    // Switch / toggle row label.
    val formSwitchLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodyMedium

    // ── Chips & Tags ─────────────────────────────────────────────────────────
    // Text inside a filter or action chip.
    val chipLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)

    // Text inside a tag badge.
    val tagLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall

    // Chip label with extra-bold weight (e.g. active filter chip).
    val chipLabelBold: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

    // ── Navigation & Screen Headers ──────────────────────────────────────────
    // Top-level screen title in a TopAppBar.
    val screenTitle: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.titleLarge

    // Sub-page / section screen title.
    val screenSubTitle: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.titleMedium

    // Drawer navigation item label.
    val drawerItemLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelLarge

    // Bottom nav / pill navigation label.
    val navLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall

    // ── Analytics & Charts ───────────────────────────────────────────────────
    // Chart axis label (small, along X or Y axis).
    val chartAxisLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)

    // Chart tooltip primary value.
    val chartTooltipValue: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)

    // Chart legend item label.
    val chartLegendLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp
            )

    // Chart empty-state message.
    val chartEmptyMessage: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodySmall

    // Treemap tile primary label.
    val treemapTileLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)

    // Treemap tile secondary / amount label.
    val treemapTileAmount: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)

    // Timeline period selector tab label.
    val timelineTabLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                fontSize = 8.sp
            )

    // Timeline active period value (e.g. "Mar 2025").
    val timelinePeriodValue: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)

    // ── List Items ───────────────────────────────────────────────────────────
    // Primary label of a generic list row.
    val listItemTitle: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodyLarge

    // Secondary / supporting text of a generic list row.
    val listItemSubtitle: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodySmall

    // Section header above a group of list items.
    val listSectionHeader: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)

    // Management list item title (accounts, budgets, categories…).
    val managementItemTitle: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)

    // Management list item secondary meta line.
    val managementItemSubtitle: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodySmall

    // ── Numerals & Calculator ────────────────────────────────────────────────
    // Primary amount displayed in the calculator / entry amount area.
    val calculatorDisplay: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)

    // Calculator numpad digit key label.
    val calculatorKey: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.headlineSmall

    // Calculator operator / action key label.
    val calculatorOperator: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)

    // ── Footer & Meta ────────────────────────────────────────────────────────
    // Timestamp or date meta label (small, secondary).
    val metaLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelExtraSmall

    // Caption / footnote text (below cards, under data).
    val caption: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.bodySmall

    // Section divider / overline label (all-caps style).
    val overlineLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

    // ── Overlines & Table Headers ────────────────────────────────────────────
    // ALL-CAPS section header with heavy tracking (e.g. "ASSET CONFIGURATION").
    val sectionOverline: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )

    // ALL-CAPS table column header with maximum tracking (e.g. "CURRENCY", "VALUE").
    val tableColumnHeader: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

    // ── Extended Hero Amounts ────────────────────────────────────────────────
    // displayMedium hero number (adaptive display, one step larger than heroAmountDisplay).
    val heroAmountMedium: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)

    // displayLarge hero number (adaptive display, largest size).
    val heroAmountLarge: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold)

    // ── Buttons ─────────────────────────────────────────────────────────────
    // Primary action button label.
    val buttonLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)

    // ── Keypad ───────────────────────────────────────────────────────────────
    // Large numpad / converter key button label (headlineMedium).
    val keyButtonLabel: TextStyle
        @Composable @ReadOnlyComposable get() =
            MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
}

