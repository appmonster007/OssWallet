package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterPill
import com.osscabinet.ossWallet.ui.components.transactions.countActiveFilters
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Core card shell for the Aeon design system.
 *
 * Responsibilities:
 * - Auto-computes a timeline subtitle from [filter.timelineRange]
 * - Shows a filter button and [TransactionFilterPill] overlay when [filter] + [onFilterChange] are provided
 * - Shows a settings button when [displaySettings] is provided
 * - [displaySettings] is the only place a card should expose a delete action
 */
@Composable
fun AeonExpressiveCard(
    title: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconColor: Color = AppColors.primary,
    accentColor: Color? = null,
    isWarning: Boolean = false,
    onClick: (() -> Unit)? = null,

    // Filter — button and pill overlay appear automatically when both are non-null
    filterEnabled: Boolean = true,
    filter: TransactionFilter? = null,
    baseFilter: TransactionFilter = TransactionFilter(),
    filterRules: CardFilterRules? = null,
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    onFilterChange: ((TransactionFilter) -> Unit)? = null,
    onSaveFilterClick: (TransactionFilter) -> Unit = {},

    // Settings — button appears automatically when non-null
    displaySettings: (@Composable (onDismiss: () -> Unit) -> Unit)? = null,

    // Timeline mode — renders AeonTimelinePeriodSelector below the header
    timelineMode: Boolean = false,

    content: @Composable ColumnScope.() -> Unit,
) {
    // Interaction source only created when the card is actually clickable.
    val interactionSource = if (onClick != null) remember { MutableInteractionSource() } else null
    val isPressed = interactionSource?.collectIsPressedAsState()?.value ?: false
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "cardScale")

    val showFilterButton  = filterEnabled && filter != null && onFilterChange != null
    val showSettingsButton = displaySettings != null

    // `remember` (not rememberSaveable) — these are ephemeral overlay flags; restoring them
    // after process death would reopen overlays unexpectedly.
    var showFilterSheet    by remember { mutableStateOf(false) }
    var showDisplaySettings by remember { mutableStateOf(false) }

    val activeFilterCount = filter?.let { countActiveFilters(it, baseFilter, filterRules) } ?: 0

    // timelineLabel allocates Calendar/SimpleDateFormat; cache so it doesn't run every recomposition.
    val effectiveSubtitle = remember(subtitle, filter?.timelineRange, timelineMode) {
        subtitle ?: if (!timelineMode) filter?.timelineRange?.let { timelineLabel(it) } else null
    }

    Card(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (onClick != null && interactionSource != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick,
                ) else Modifier,
            ),
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppColors.cardSurface),
        border = BorderStroke(
            width = if (accentColor != null) 1.dp else 0.5.dp,
            color = when {
                isWarning           -> AppColors.error.copy(alpha = 0.3f)
                accentColor != null -> accentColor.copy(alpha = 0.4f)
                else                -> AppColors.divider.copy(alpha = 0.15f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeonTheme.dimensions.cardPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(headerModifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
            ) {
                if (icon != null) {
                    Surface(
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg),
                        shape = CircleShape,
                        color = iconColor.copy(alpha = 0.15f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                                tint = iconColor,
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AppTextStyles.screenSubTitle.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = AppColors.contentPrimary,
                    )
                    if (effectiveSubtitle != null) {
                        Text(
                            text = effectiveSubtitle,
                            style = AppTextStyles.drawerItemLabel.copy(
                                fontWeight = FontWeight.Light,
                                fontSize = 10.sp,
                            ),
                            color = AppColors.contentSecondary,
                        )
                    }
                }

                if (showFilterButton || showSettingsButton) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs),
                    ) {
                        if (showFilterButton) {
                            AeonCardFilterButton(
                                onClick = { showFilterSheet = true },
                                activeFilterCount = activeFilterCount,
                            )
                        }
                        if (showSettingsButton) {
                            IconButton(
                                onClick = { showDisplaySettings = true },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Tune,
                                    contentDescription = "Display Settings",
                                    tint = AppColors.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))

            if (timelineMode && filter != null && onFilterChange != null) {
                AeonTimelinePeriodSelector(
                    currentRange = filter.timelineRange,
                    onRangeSelected = { onFilterChange(filter.copy(timelineRange = it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))
            }

            content()
        }
    }

    // ── Filter pill overlay ────────────────────────────────────────────────────
    // Rendered outside the Card tree so it doesn't affect Card sizing.
    // Dialog creates a full-screen modal window, blocking the nav pill underneath.
    if (showFilterSheet && filter != null && onFilterChange != null) {
        // Stabilise the event handler: a single stable lambda is created once and reads
        // the latest filter/onFilterChange values via rememberUpdatedState at call time.
        val currentFilter       = rememberUpdatedState(filter)
        val currentOnFilterChange = rememberUpdatedState(onFilterChange)
        val onFilterEvent       = remember<(FilterEvent) -> Unit> {
            { event ->
                when (event) {
                    is FilterEvent.Apply          -> currentOnFilterChange.value(event.filter)
                    is FilterEvent.UpdateTimeline -> currentOnFilterChange.value(currentFilter.value.copy(timelineRange = event.range))
                    is FilterEvent.Reset          -> currentOnFilterChange.value(TransactionFilter())
                    else                          -> Unit
                }
            }
        }

        Dialog(
            onDismissRequest = { showFilterSheet = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false, // pill handles outside-tap itself
            ),
        ) {
            // Disable the Dialog's built-in window dim so our morph-driven scrim
            // inside TransactionFilterPill is the sole overlay.
            val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
            SideEffect { dialogWindow?.setDimAmount(0f) }

            TransactionFilterPill(
                filter = filter,
                accounts = filterContext.accounts,
                categories = filterContext.allCategories,
                allTags = filterContext.allTags,
                onSearchTransactions = filterContext.onSearchTransactions,
                savedFilters = filterContext.savedFilters,
                onFilterEvent = onFilterEvent,
                onSaveFilterClick = onSaveFilterClick,
                onDismiss = { showFilterSheet = false },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (showDisplaySettings && displaySettings != null) {
        displaySettings { showDisplaySettings = false }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun timelineLabel(range: TimelineRange): String {
    val (startMs, endMs) = TimelineService.getDateRange(range)
    if (startMs == null || endMs == null) return "All Time"

    val startCal    = Calendar.getInstance().apply { timeInMillis = startMs }
    val endCal      = Calendar.getInstance().apply { timeInMillis = endMs }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    val resolvedDate = if (
        startCal.get(Calendar.DAY_OF_MONTH) == 1 &&
        endCal.get(Calendar.DAY_OF_MONTH) == endCal.getActualMaximum(Calendar.DAY_OF_MONTH) &&
        startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH) &&
        startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)
    ) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(startMs))
    } else {
        val fmt = if (startCal.get(Calendar.YEAR) == currentYear && endCal.get(Calendar.YEAR) == currentYear)
            SimpleDateFormat("MMM d", Locale.getDefault())
        else
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        "${fmt.format(Date(startMs))} – ${fmt.format(Date(endMs))}"
    }

    val prefix = when (range) {
        is TimelineRange.ThisPeriod -> "${range.period.label} · "
        is TimelineRange.LastPeriod -> "Last ${range.period.label} · "
        else                        -> ""
    }

    return prefix + resolvedDate
}

// ---------------------------------------------------------------------------
// Filter button
// ---------------------------------------------------------------------------

/**
 * Circular filter button that lights up when filters are active.
 */
@Composable
fun AeonCardFilterButton(
    onClick: () -> Unit,
    activeFilterCount: Int,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.FilterList,
    activeIcon: ImageVector = Icons.Rounded.FilterAlt,
) {
    val isActive = activeFilterCount > 0

    Surface(
        onClick = onClick,
        modifier = modifier.size(28.dp),
        shape = CircleShape,
        color = if (isActive) AppColors.primary.copy(alpha = 0.15f) else AppColors.transparent,
        border = if (isActive) BorderStroke(1.dp, AppColors.primary.copy(alpha = 0.5f)) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isActive) activeIcon else icon,
                contentDescription = "Filter",
                tint = if (isActive) AppColors.primary else AppColors.contentSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun AeonExpressiveCardPreview() {
    OssWalletTheme {
        Box(Modifier.padding(16.dp)) {
            AeonExpressiveCard(
                title = "Net Worth",
                subtitle = "Monthly · January 2024",
                icon = Icons.Rounded.AccountBalance,
                filter = TransactionFilter(),
                onFilterChange = {},
                displaySettings = {},
            ) {
                Text(
                    text = "$12,450.00",
                    style = AppTextStyles.heroAmount,
                    color = AppColors.primary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}
