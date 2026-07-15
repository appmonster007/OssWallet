package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.AeonGlassHost
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.components.aeonDottedUnderline
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterPill
import com.osscabinet.ossWallet.ui.theme.AccountStructureColors
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.DeepTealGreen
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.LocalAeonHaze
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.LocalParentGlassHaze
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.copyStyle
import dev.chrisbanes.haze.hazeEffect
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Core card shell for the Aeon design system — an Editorial Instrument.
 *
 * Self-contained header: title + an in-body period readout, no filter/config icon buttons.
 * - Auto-computes the period readout from filter's timeline range
 * - When [filter] + [onFilterChange] are provided, tapping the period readout opens the
 *   [TransactionFilterPill] overlay
 * - When [displaySettings] is provided, long-pressing the header opens it; it is the only
 *   place a card should expose a delete action
 *
 * Cards are glassmorphic panels that blur the screen background via [HazeLayer.Surface].
 * Glass is disabled for nested card content via [LocalGlassmorphismEnabled] to maintain readability.
 */
@Composable
fun AeonExpressiveCard(
    title: String,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color = AppColors.contentSecondary,
    icon: ImageVector? = null,
    accentColor: Color? = null,
    isWarning: Boolean = false,
    onClick: (() -> Unit)? = null,

    containerColor: Color = AppColors.cardSurface,
    contentColor: Color = AppColors.contentPrimary,
    backgroundIcon: ImageVector? = null,

    // Filter — tappable period readout + pill overlay appear automatically when both are non-null
    filterEnabled: Boolean = true,
    filter: TransactionFilter? = null,
    baseFilter: TransactionFilter = TransactionFilter(),
    filterRules: CardFilterRules? = null,
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    onFilterChange: ((TransactionFilter) -> Unit)? = null,
    onSaveFilterClick: (TransactionFilter) -> Unit = {},

    // Settings — button appears automatically when non-null
    displaySettings: (@Composable (onDismiss: () -> Unit) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    settingsContent: (@Composable ColumnScope.() -> Unit)? = null,

    // Timeline mode — renders AeonTimelinePeriodSelector below the header
    timelineMode: Boolean = false,

    content: @Composable ColumnScope.() -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val hazeLayers = LocalAeonHaze.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val interactionSource = if (onClick != null) remember { MutableInteractionSource() } else null
    val isPressed = interactionSource?.collectIsPressedAsState()?.value ?: false
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "cardScale")

    val cardShape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius)

    val periodTappable = filterEnabled && (filter != null) && (onFilterChange != null)
    val hasSettings = (displaySettings != null) || (settingsContent != null)

    val editingCardSession = LocalEditingCardId.current
    val cardId = LocalCardId.current
    val onEditSession = LocalOnEditCard.current

    val (showFilterSheet, setShowFilterSheet) = rememberSyncedCardState(
        cardId = cardId,
        tag = "filter",
        sessionState = editingCardSession,
        onSessionChange = onEditSession,
    )

    val (isFlipped, setIsFlipped) = rememberSyncedCardState(
        cardId = cardId,
        tag = "settings",
        sessionState = editingCardSession,
        onSessionChange = onEditSession,
    )

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )

    val isFrontVisible by remember { derivedStateOf { rotation <= 90f } }

    val cardStyle = GlassTokens.cardStyle(isDark)
    val glassStyle = remember(containerColor, isDark, cardStyle) {
        cardStyle.copyStyle(
            bgColor = containerColor,
            isDark = isDark
        )
    }

    // Discoverability & Intent Feedback
    // Guard skips the first composition to avoid a spurious haptic if the card enters
    // with a non-zero, non-180 rotation (e.g., restored from saved state).
    var initialComposition by remember { mutableStateOf(value = true) }
    LaunchedEffect(isFrontVisible) {
        if (initialComposition) {
            initialComposition = false; return@LaunchedEffect
        }
        if (rotation > 0f && rotation < 180f) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // timelineLabel allocates Calendar/SimpleDateFormat; cache so it doesn't run every recomposition.
    val effectiveSubtitle = remember(subtitle, filter?.timelineRange, timelineMode) {
        subtitle ?: if (!timelineMode) filter?.timelineRange?.let { timelineLabel(it) } else null
    }

    val borderModifier = when {
        isWarning -> Modifier.border(BorderStroke(0.5.dp, AppColors.error), cardShape)
        accentColor != null -> Modifier.border(BorderStroke(1.dp, accentColor), cardShape)
        else -> Modifier.aeonHairlineBorder(cardShape)
    }

    val headerLongPress = if (hasSettings) {
        Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                setIsFlipped(true)
            },
        )
    } else Modifier

    val surfaceHazeState = hazeLayers?.states?.getOrNull(HazeLayer.Surface)
    val overlayHazeState = hazeLayers?.states?.getOrNull(HazeLayer.Overlay)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationY = rotation
                cameraDistance = 20f * density 

                // Only perform expensive shadow/alpha math during active rotation
                if (rotation > 0.5f && rotation < 179.5f) {
                    val progress = if (rotation <= 90f) {
                        (rotation - 0.5f) / 89.5f
                    } else {
                        (179.5f - rotation) / 89.5f
                    }
                    shadowElevation = (2.dp + (8.dp * progress)).toPx()
                    alpha = 1f - (progress * 0.15f)
                } else {
                    shadowElevation = 2.dp.toPx()
                    alpha = 1f
                }

                clip = true
                shape = cardShape
            }
            .then(borderModifier)
            .then(
                if (rotation in 0.01f..179.99f) {
                    Modifier
                } else {
                    Modifier.animateContentSize(
                        animationSpec = tween(
                            durationMillis = 400,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                },
            )
            .then(
                if ((onClick != null) && (interactionSource != null) && !isFlipped) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick,
                ) else Modifier,
            ),
    ) {
        CompositionLocalProvider(
            LocalGlassmorphismEnabled provides true,
            LocalParentGlassHaze provides surfaceHazeState
        ) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShape,
                hazeState = surfaceHazeState,
                hazeStyle = glassStyle,
                fallbackColor = containerColor,
            ) {
                AeonGlassHost(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aeonHazeSource(overlayHazeState)
                ) {
                    if (isFrontVisible) {
                        // Front Side
                        Box(modifier = Modifier.fillMaxWidth()) {
                            backgroundIcon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .offset(x = 24.dp, y = 24.dp)
                                        .size(180.dp)
                                        .graphicsLayer { alpha = 0.08f },
                                    tint = contentColor,
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AeonTheme.dimensions.cardPadding),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(headerLongPress),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
                                ) {
                                    if (icon != null) {
                                        Surface(
                                            modifier = Modifier
                                                .size(AeonTheme.dimensions.iconSizeLg)
                                                .then(headerModifier),
                                            shape = CircleShape,
                                            color = contentColor.copy(alpha = 0.15f),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                                                    tint = contentColor,
                                                )
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            style = AppTextStyles.cardSectionTitle,
                                            color = contentColor,
                                        )
                                        if (effectiveSubtitle != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (periodTappable) {
                                                    Text(
                                                        text = effectiveSubtitle,
                                                        style = AppTextStyles.cardPeriodReadout,
                                                        color = subtitleColor,
                                                        modifier = Modifier
                                                            .clickable { setShowFilterSheet(true) }
                                                            .aeonDottedUnderline(),
                                                    )
                                                } else {
                                                    Text(
                                                        text = effectiveSubtitle,
                                                        style = AppTextStyles.cardPeriodReadout,
                                                        color = subtitleColor,
                                                    )
                                                }

                                                // Filter active indicator
                                                if (filter != null && filter != baseFilter) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .background(
                                                                color = accentColor ?: AppColors.primary,
                                                                shape = CircleShape
                                                            )
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
                                        isFlush = true
                                    )
                                    Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))
                                }

                                content()
                            }
                        }
                    } else {
                        // Back Side
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    rotationY = 180f
                                }
                        ) {
                            val onDismissBack = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                setIsFlipped(false)
                            }

                            if (settingsContent != null) {
                                AeonCardSettingsScaffold(
                                    title = title,
                                    onDismiss = onDismissBack,
                                    onDelete = onDelete,
                                ) {
                                    settingsContent()
                                }
                            } else {
                                displaySettings?.invoke { onDismissBack() }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Filter pill overlay ────────────────────────────────────────────────────
    // rememberUpdatedState / remember must be called unconditionally (Compose rule).
    val currentFilter = rememberUpdatedState(filter)
    val currentOnFilterChange = rememberUpdatedState(onFilterChange)
    val onFilterEvent = remember<(FilterEvent) -> Unit> {
        { event ->
            when (event) {
                is FilterEvent.Apply -> currentOnFilterChange.value?.invoke(event.filter)
                is FilterEvent.UpdateTimeline -> currentOnFilterChange.value?.invoke(
                    currentFilter.value?.copy(timelineRange = event.range) ?: return@remember
                )

                is FilterEvent.Reset -> currentOnFilterChange.value?.invoke(TransactionFilter())
                else -> Unit
            }
        }
    }

    val filterPillController = LocalFilterPillController.current

    if (showFilterSheet && filter != null && onFilterChange != null) {
        if (filterPillController != null) {
            // Controller path: pill renders at screen level in the main window so
            // LocalAeonHaze is accessible and real glass blur works.
            SideEffect {
                filterPillController.show(
                    FilterPillRequest(
                        filter = filter,
                        accounts = filterContext.accounts,
                        categories = filterContext.allCategories,
                        allTags = filterContext.allTags,
                        savedFilters = filterContext.savedFilters,
                        onSearchTransactions = filterContext.onSearchTransactions,
                        onFilterEvent = onFilterEvent,
                        onSaveFilterClick = onSaveFilterClick,
                        onDismiss = { setShowFilterSheet(false) },
                        filterRules = filterRules ?: CardFilterRules.DEFAULT,
                        baseFilter = baseFilter,
                    )
                )
            }
            DisposableEffect(Unit) {
                onDispose { filterPillController.dismiss() }
            }
        } else {
            // Dialog fallback: screens that don't provide LocalFilterPillController
            // (Analytics, etc.) keep the existing behaviour unchanged.
            Dialog(
                onDismissRequest = { setShowFilterSheet(false) },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false,
                ),
            ) {
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
                    onDismiss = { setShowFilterSheet(false) },
                    modifier = Modifier.fillMaxSize(),
                    filterRules = filterRules ?: CardFilterRules.DEFAULT,
                    baseFilter = baseFilter,
                )
            }
        }
    }

    // Removed showDisplaySettings dialog logic as it's now handled by flipping
}

/**
 * A specialized version of [AeonExpressiveCard] for "Hero" sections that standardizes
 * the large amount display and overline label.
 */
@Composable
fun AeonHeroCard(
    title: String,
    amount: String,
    label: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentColor: Color? = null,
    amountColor: Color = AppColors.contentPrimary,
    containerColor: Color = AppColors.cardSurface,
    contentColor: Color = AppColors.contentPrimary,
    backgroundIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    displaySettings: (@Composable (onDismiss: () -> Unit) -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    AeonExpressiveCard(
        title = title,
        subtitle = subtitle,
        icon = icon,
        accentColor = accentColor,
        containerColor = containerColor,
        contentColor = contentColor,
        backgroundIcon = backgroundIcon,
        modifier = modifier,
        onClick = onClick,
        displaySettings = displaySettings,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label.uppercase(),
                style = AppTextStyles.overlineLabel,
                color = contentColor.copy(alpha = 0.6f)
            )
            Text(
                text = amount,
                style = AppTextStyles.heroAmountDisplay,
                color = amountColor
            )
        }
        if (content != null) {
            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Unifies local state and global session state (e.g. from DashboardViewModel) for card edits.
 */
@Composable
private fun rememberSyncedCardState(
    cardId: String?,
    tag: String,
    sessionState: String?,
    onSessionChange: ((String?) -> Unit)?
): Pair<Boolean, (Boolean) -> Unit> {
    var internalState by remember { mutableStateOf(false) }

    val isActive = if (sessionState != null && cardId != null) {
        sessionState == "$tag:$cardId"
    } else {
        internalState
    }

    val setter: (Boolean) -> Unit = { active ->
        if (onSessionChange != null && cardId != null) {
            onSessionChange(if (active) "$tag:$cardId" else null)
        } else {
            internalState = active
        }
    }

    return isActive to setter
}

private fun timelineLabel(range: TimelineRange): String {
    val (startMs, endMs) = TimelineService.getDateRange(range)
    if (startMs == null || endMs == null) return "All Time"

    val startCal = Calendar.getInstance().apply { timeInMillis = startMs }
    val endCal = Calendar.getInstance().apply { timeInMillis = endMs }
    val currentYear = Calendar.getInstance()[Calendar.YEAR]

    val resolvedDate = if (
        (startCal[Calendar.DAY_OF_MONTH] == 1) &&
                (endCal[Calendar.DAY_OF_MONTH] == endCal.getActualMaximum(Calendar.DAY_OF_MONTH)) &&
                (startCal[Calendar.MONTH] == endCal[Calendar.MONTH]) &&
                (startCal[Calendar.YEAR] == endCal[Calendar.YEAR])
    ) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(startMs))
    } else {
        val fmt =
            if (startCal.get(Calendar.YEAR) == currentYear && endCal.get(Calendar.YEAR) == currentYear)
                SimpleDateFormat("MMM d", Locale.getDefault())
            else
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        "${fmt.format(Date(startMs))} – ${fmt.format(Date(endMs))}"
    }

    val prefix = when (range) {
        is TimelineRange.ThisPeriod -> "${range.period.label} · "
        is TimelineRange.LastPeriod -> "Last ${range.period.label} · "
        else -> ""
    }

    return prefix + resolvedDate
}

// ---------------------------------------------------------------------------
// Filter button
// ---------------------------------------------------------------------------

/**
 * Circular filter button that lights up when filters are active.
 */


// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "Light Theme")
@Composable
private fun AeonExpressiveCardLightPreview() {
    OssWalletTheme(theme = "Light") {
        Box(
            Modifier
                .background(AppColors.background)
                .padding(16.dp)
        ) {
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

@Preview(showBackground = true, name = "Dark Theme")
@Composable
private fun AeonExpressiveCardDarkPreview() {
    OssWalletTheme(theme = "Dark") {
        Box(
            Modifier
                .background(AppColors.background)
                .padding(16.dp)
        ) {
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

@Preview(showBackground = true, name = "Amoled Theme")
@Composable
private fun AeonExpressiveCardAmoledPreview() {
    OssWalletTheme(theme = "Amoled") {
        Box(
            Modifier
                .background(AppColors.background)
                .padding(16.dp)
        ) {
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

@Preview(showBackground = true, name = "Accent Color")
@Composable
private fun AeonExpressiveCardAccentPreview() {
    OssWalletTheme {
        Box(
            Modifier
                .background(AppColors.background)
                .padding(16.dp)
        ) {
            AeonExpressiveCard(
                title = "Savings Goal",
                subtitle = "85% of $10,000",
                icon = Icons.Rounded.AccountBalance,
                accentColor = AccountStructureColors.savings,
                filter = TransactionFilter(),
                onFilterChange = {},
            ) {
                Text(
                    text = "$8,500.00",
                    style = AppTextStyles.heroAmount,
                    color = AccountStructureColors.savings,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

// ── AeonHeroCard Previews ──────────────────────────────────────────────────

@Preview(showBackground = true, name = "Hero - Light")
@Composable
private fun AeonHeroCardLightPreview() {
    OssWalletTheme(theme = "Light") {
        Box(
            Modifier
                .background(AppColors.background)
                .padding(16.dp)
        ) {
            AeonHeroCard(
                title = "Net Worth",
                amount = "$1,245,800.50",
                label = "Total Balance",
                icon = Icons.Rounded.AccountBalance,
            )
        }
    }
}

@Preview(showBackground = true, name = "Hero - Dark")
@Composable
private fun AeonHeroCardDarkPreview() {
    OssWalletTheme(theme = "Dark") {
        Box(
            Modifier
                .background(AppColors.background)
                .padding(16.dp)
        ) {
            AeonHeroCard(
                title = "Wealth Fund",
                amount = "$245,800.50",
                label = "Total Balance",
                icon = Icons.Rounded.AccountBalance,
                accentColor = DeepTealGreen,
            )
        }
    }
}
