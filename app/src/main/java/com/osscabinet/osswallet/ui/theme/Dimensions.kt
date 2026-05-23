package com.osscabinet.ossWallet.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

/**
 * AEON DESIGN LANGUAGE - DIMENSION SYSTEM
 *
 * This system defines the spatial DNA of the OssWallet app.
 * It uses a semantic hierarchy to ensure consistency across different screens and components.
 *
 * LAYOUT LAYERING DIAGRAM:
 * ┌──────────────────────────────────────────────────────────┐ ▲
 * │ Screen (screenPadding)                                   │ │
 * │  ┌────────────────────────────────────────────────────┐  │ │
 * │  │ Card (cardCornerRadius)                            │  │ │
 * │  │  ┌──────────────────────────────────────────────┐  │  │ │
 * │  │  │ Content (cardPadding)                        │  │  │ │
 * │  │  │  ┌──────────┐        ┌────────────────────┐  │  │  │ │
 * │  │  │  │ Icon (Md)│ (Sp.Md) │ Text Label         │  │  │  │ │
 * │  │  │  └──────────┘        └────────────────────┘  │  │  │ │
 * │  │  │          (spacingLg)                         │  │  │ │
 * │  │  │  ┌────────────────────────────────────────┐  │  │  │ │
 * │  │  │  │ Inner Component (componentCornerRadius) │  │  │  │ │
 * │  │  │  └────────────────────────────────────────┘  │  │  │ │
 * │  │  │          (spacingMd)                         │  │  │ │
 * │  │  │  ┌───────────────────────┐                  │  │  │ │
 * │  │  │  │ Chip (chipCornerRadius)│                  │  │  │ │
 * │  │  │  │ (chipPadding)          │                  │  │  │ │
 * │  │  │  └───────────────────────┘                  │  │  │ │
 * │  │  └──────────────────────────────────────────────┘  │  │ │
 * │  └────────────────────────────────────────────────────┘  │ │
 * └──────────────────────────────────────────────────────────┘ ▼
 *
 * USAGE GUIDELINES:
 * - Layout Paddings: Shared across Dashboard, Analytics, Details, and Entry screens.
 * - Card vs. Chip: Use cardPadding/cardCornerRadius for structural containers, 
 *   and chipPadding/chipCornerRadius for small interactive metadata tags.
 * - Semantic Spacing: Use for gaps between sibling elements (Arrangement.spacedBy).
 * - Icon Sizes: Use for all vector icons to maintain visual weight.
 * - Corner Radii: Higher values for outer containers, lower for nested elements.
 */
data class AeonDimensions(
    /**
     * LAYOUT PADDINGS
     * High-level structural margins. Ensure consistency across:
     * - Dashboard & Analytics
     * - Account/Loan Details
     * - All Entry/Form Screens
     */
    // Outer edge of all primary screens
    val screenPadding: Dp = AeonSize.D18.value,
    // Internal content margin of a standard Card
    val cardPadding: Dp = AeonSize.D16.value,
    // Internal padding for chips and small action items
    val chipPadding: Dp = AeonSize.D10.value,
    // Horizontal padding for items in a LazyColumn
    val listPadding: Dp = AeonSize.D12.value,
    // Spacing between items in a Grid
    val gridPadding: Dp = AeonSize.D12.value,

    /**
     * SEMANTIC SPACING
     * Vertical and horizontal gaps between related components.
     */
    // Micro-adjustments (e.g., icon and text in a chip)
    val spacingXs: Dp = AeonSize.D4.value,
    // Tight groups (e.g., label and its value)
    val spacingSm: Dp = AeonSize.D8.value,
    // Standard component gap (e.g., between fields in a form)
    val spacingMd: Dp = AeonSize.D12.value,
    // Comfortable section gap
    val spacingLg: Dp = AeonSize.D16.value,
    // Large structural breaks
    val spacingXl: Dp = AeonSize.D24.value,
    // Major screen divisions
    val spacing2xl: Dp = AeonSize.D32.value,
    // Hero sections or large empty states
    val spacing3xl: Dp = AeonSize.D48.value,

    /**
     * ICON SIZES
     * Standardized bounding boxes for vector assets.
     */
    // Tiny inline indicators
    val iconSizeXs: Dp = AeonSize.D12.value,
    // Secondary actions or small list icons
    val iconSizeSm: Dp = AeonSize.D18.value,
    // Primary list items or action buttons
    val iconSizeMd: Dp = AeonSize.D24.value,
    // Highlighted headers or feature cards
    val iconSizeLg: Dp = AeonSize.D32.value,
    // Large profile icons or hero cards
    val iconSizeXl: Dp = AeonSize.D44.value,
    // Decorative feature icons
    val iconSize2xl: Dp = AeonSize.D52.value,
    // Massive empty state or landing icons
    val iconSize3xl: Dp = AeonSize.D64.value,

    /**
     * CORNER RADII
     * The curvature that defines the Aeon look.
     */
    // Main structural cards (Accounts, Budgets, Loans)
    val cardCornerRadius: Dp = AeonSize.D32.value,
    // Full-screen bottom sheets and alert dialogs
    val dialogCornerRadius: Dp = AeonSize.D32.value,
    // Generic grouping containers (Surfaces inside screens)
    val containerCornerRadius: Dp = AeonSize.D24.value,
    // Interactive elements (Chips, Input fields)
    val componentCornerRadius: Dp = AeonSize.D16.value,
    // Specific radius for action chips/mini buttons
    val chipCornerRadius: Dp = AeonSize.D32.value,
    // Main action buttons (Floating action or Submit)
    val buttonCornerRadius: Dp = AeonSize.D28.value
)

/**
 * EXAMPLE SCREEN ARCHITECTURE (LOAN DETAIL)
 * 
 * ┌────────────────────────────────────────────────────────────┐
 * │ (x) Loan Detail (Top Bar)                                  │
 * ├────────────────────────────────────────────────────────────┤
 * │ (screenPadding)                                            │
 * │  ┌──────────────────────────────────────────────────────┐  │
 * │  │ IMPACT CARD (cardCornerRadius)                       │  │
 * │  │ ┌──────────────────────────────────────────────────┐ │  │
 * │  │ │ (cardPadding)                                    │ │  │
 * │  │ │ (Icon-Lg) (Title-Lg)                             │ │  │
 * │  │ │ (spacingLg)                                      │ │  │
 * │  │ │ (Amount-Headline)                                │ │  │
 * │  │ └──────────────────────────────────────────────────┘ │  │
 * │  └──────────────────────────────────────────────────────┘  │
 * │  (spacingXl)                                               │
 * │  ┌──────────────────────────────────────────────────────┐  │
 * │  │ CONTEXT SECTION (spacingMd gap)                      │  │
 * │  │ ┌───────────┐ (sp.Md) ┌───────────┐                  │  │
 * │  │ │ (Icon-Md) │         │ (Icon-Md) │                  │  │
 * │  │ │ Next Pay  │         │ Interest  │                  │  │
 * │  │ └───────────┘         └───────────┘                  │  │
 * │  └──────────────────────────────────────────────────────┘  │
 * │  (spacing2xl)                                              │
 * │  (--- HorizontalDivider ---)                               │
 * │  (spacingMd)                                               │
 * │  LazyColumn {                                              │
 * │    (listPadding) items...                                  │
 * │  }                                                         │
 * └────────────────────────────────────────────────────────────┘
 */

val LocalAeonDimensions = staticCompositionLocalOf { AeonDimensions() }
