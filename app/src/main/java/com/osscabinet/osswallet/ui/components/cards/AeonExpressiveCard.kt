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
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.transactions.countActiveFilters
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Core card shell for the Aeon design system.
 *
 * Owns all card-level UX scaffolding:
 * - Timeline subtitle auto-computed from [filter.timelineRange]
 * - Filter button shown automatically when [filter] + [onFilterChange] are provided
 * - Settings button shown automatically when [displaySettings] slot is provided
 * - [AeonCardSettingsSheet] rendered internally; state never leaks to card composables
 * - [displaySettings] slot is the only place a card exposes delete (via its AlertDialog)
 *
 * Card composables provide only: render data, [config], [callbacks], and [displaySettings] content.
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

    // Filter system — filter button + sheet appear automatically when both are non-null
    filterEnabled: Boolean = true,
    filter: TransactionFilter? = null,
    baseFilter: TransactionFilter = TransactionFilter(),
    filterRules: CardFilterRules? = null,
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    filterSheetTitle: String? = null,
    onFilterChange: ((TransactionFilter) -> Unit)? = null,
    onSaveFilterClick: (TransactionFilter) -> Unit = {},

    // Settings slot — settings button appears automatically when non-null
    // The lambda receives onDismiss and is responsible for its own dialog/sheet UI.
    // This is the correct and only place to expose the card delete action.
    displaySettings: (@Composable (onDismiss: () -> Unit) -> Unit)? = null,

    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "cardScale")

    val showFilterButton = filterEnabled && filter != null && onFilterChange != null
    val showSettingsButton = displaySettings != null

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var showDisplaySettings by rememberSaveable { mutableStateOf(false) }

    val activeFilterCount = filter?.let { countActiveFilters(it, baseFilter, filterRules) } ?: 0
    val effectiveSubtitle = subtitle ?: filter?.timelineRange?.let { timelineLabel(it) }

    Card(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick
                ) else Modifier
            ),
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppColors.cardSurface),
        border = BorderStroke(
            width = if (accentColor != null) 1.dp else 0.5.dp,
            color = when {
                isWarning -> AppColors.error.copy(alpha = 0.3f)
                accentColor != null -> accentColor.copy(alpha = 0.4f)
                else -> AppColors.divider.copy(alpha = 0.15f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeonTheme.dimensions.cardPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(headerModifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
            ) {
                if (icon != null) {
                    Surface(
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg),
                        shape = CircleShape,
                        color = iconColor.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                                tint = iconColor
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
                            letterSpacing = 0.sp
                        ),
                        color = AppColors.contentPrimary
                    )
                    if (effectiveSubtitle != null) {
                        Text(
                            text = effectiveSubtitle,
                            style = AppTextStyles.drawerItemLabel.copy(
                                fontWeight = FontWeight.Light,
                                fontSize = 10.sp
                            ),
                            color = AppColors.contentSecondary
                        )
                    }
                }

                if (showFilterButton || showSettingsButton) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                    ) {
                        if (showFilterButton) {
                            AeonCardFilterButton(
                                onClick = { showFilterSheet = true },
                                activeFilterCount = activeFilterCount
                            )
                        }
                        if (showSettingsButton) {
                            IconButton(
                                onClick = { showDisplaySettings = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Tune,
                                    contentDescription = "Display Settings",
                                    tint = AppColors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))

            content()
        }
    }

    // Overlays rendered outside the Card layout tree so they don't affect Card sizing
    if (showFilterSheet && filter != null && onFilterChange != null) {
        AeonCardSettingsSheet(
            title = filterSheetTitle ?: "$title Filter",
            filter = filter,
            onFilterChange = onFilterChange,
            onDismiss = { showFilterSheet = false },
            onSaveFilterClick = onSaveFilterClick,
            filterContext = filterContext,
            filterRules = filterRules ?: CardFilterRules.DEFAULT,
            baseFilter = baseFilter
        )
    }

    if (showDisplaySettings && displaySettings != null) {
        displaySettings({ showDisplaySettings = false })
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun timelineLabel(range: TimelineRange): String = when (range) {
    is TimelineRange.AllTime -> "All Time"
    is TimelineRange.ThisPeriod -> range.period.label
    is TimelineRange.LastPeriod -> range.period.label
    is TimelineRange.CustomRange -> {
        val fmt = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault())
        val start = range.start?.let { fmt.format(Instant.ofEpochMilli(it)) } ?: "..."
        val end = range.end?.let { fmt.format(Instant.ofEpochMilli(it)) } ?: "..."
        "$start – $end"
    }
}

/**
 * Standard filter button for expressive cards that indicates active filters.
 */
@Composable
fun AeonCardFilterButton(
    onClick: () -> Unit,
    activeFilterCount: Int,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.FilterList,
    activeIcon: ImageVector = Icons.Rounded.FilterAlt
) {
    val isActive = activeFilterCount > 0

    Surface(
        onClick = onClick,
        modifier = modifier.size(28.dp),
        shape = CircleShape,
        color = if (isActive) AppColors.primary.copy(alpha = 0.15f) else AppColors.transparent,
        border = if (isActive) BorderStroke(1.dp, AppColors.primary.copy(alpha = 0.5f)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isActive) activeIcon else icon,
                contentDescription = "Filter",
                tint = if (isActive) AppColors.primary else AppColors.contentSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
