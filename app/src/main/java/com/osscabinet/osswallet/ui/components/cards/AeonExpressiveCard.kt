package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
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
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.domain.model.settings.CardOpenAnimation
import com.osscabinet.ossWallet.ui.theme.LocalCardOpenAnimation
import com.osscabinet.ossWallet.ui.theme.LocalAeonHaze
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.LocalParentGlassHaze
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.copyStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Encapsulates the interaction state for an Aeon card.
 */
@Stable
class AeonCardState internal constructor(
    val cardId: String?,
    initialFlipped: Boolean = false,
    initialFilterVisible: Boolean = false,
    private val onSessionChange: ((String?) -> Unit)? = null,
    private val sessionState: String? = null
) {
    var isFlippedInternal by mutableStateOf(initialFlipped)
    var isFilterVisibleInternal by mutableStateOf(initialFilterVisible)

    val isFlipped: Boolean
        get() = if (sessionState != null && cardId != null) sessionState == "settings:$cardId" else isFlippedInternal

    val isFilterVisible: Boolean
        get() = if (sessionState != null && cardId != null) sessionState == "filter:$cardId" else isFilterVisibleInternal

    fun flip(flipped: Boolean) {
        if (onSessionChange != null && cardId != null) {
            onSessionChange(if (flipped) "settings:$cardId" else null)
        } else {
            isFlippedInternal = flipped
        }
    }

    fun showFilter(visible: Boolean) {
        if (onSessionChange != null && cardId != null) {
            onSessionChange(if (visible) "filter:$cardId" else null)
        } else {
            isFilterVisibleInternal = visible
        }
    }
}

/**
 * Standardized header component for all Aeon cards.
 */
@Composable
internal fun AeonCardHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color = AppColors.contentSecondary,
    icon: ImageVector? = null,
    iconColor: Color = AppColors.contentPrimary,
    titleColor: Color = AppColors.contentPrimary,
    onIconClick: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    trailingIconColor: Color = AppColors.error,
    onTrailingIconClick: (() -> Unit)? = null,
    subtitleClickable: Boolean = false,
    onSubtitleClick: (() -> Unit)? = null,
    onHeaderClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    showFilterIndicator: Boolean = false,
    accentColor: Color = AppColors.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (icon == null) modifier else Modifier)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onHeaderClick?.invoke() },
                onLongClick = onLongClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
    ) {
        if (icon != null) {
            Surface(
                modifier = Modifier
                    .size(AeonTheme.dimensions.iconSizeLg)
                    .then(modifier)
                    .then(
                        if (onIconClick != null || onHeaderClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onIconClick?.invoke() ?: onHeaderClick?.invoke() })
                        } else Modifier
                    ),
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
                style = AppTextStyles.cardSectionTitle,
                color = titleColor,
            )
            if (subtitle != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = subtitle,
                        style = AppTextStyles.cardPeriodReadout,
                        color = subtitleColor,
                        modifier = Modifier.then(
                            if (subtitleClickable) Modifier
                                .clickable { onSubtitleClick?.invoke() }
                                .aeonDottedUnderline()
                            else Modifier))
                    if (showFilterIndicator) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(color = accentColor, shape = CircleShape)
                        )
                    }
                }
            }
        }

        if (trailingIcon != null) {
            Surface(
                modifier = Modifier
                    .size(AeonTheme.dimensions.iconSizeLg)
                    .clickable(onClick = onTrailingIconClick ?: {}),
                shape = CircleShape,
                color = trailingIconColor.copy(alpha = 0.10f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = "Action",
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                        tint = trailingIconColor,
                    )
                }
            }
        }
    }
}

/**
 * Shared glass surface shell for card sides.
 */
@Composable
private fun AeonCardSideShell(
    modifier: Modifier = Modifier, containerColor: Color, content: @Composable BoxScope.() -> Unit
) {
    val hazeLayers = LocalAeonHaze.current
    val surfaceHazeState = hazeLayers?.states?.getOrNull(HazeLayer.Surface)
    val overlayHazeState = hazeLayers?.states?.getOrNull(HazeLayer.Overlay)
    val cardShape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius)

    val cardStyle = GlassTokens.cardStyle(isSystemInDarkTheme())
    val glassStyle = remember(containerColor, cardStyle) {
        cardStyle.copyStyle(bgColor = containerColor, isDark = null)
    }

    CompositionLocalProvider(
        LocalGlassmorphismEnabled provides true, LocalParentGlassHaze provides surfaceHazeState
    ) {
        GlassSurface(
            modifier = modifier.fillMaxWidth(),
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AeonTheme.dimensions.cardPadding)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Core card shell for the Aeon design system — an Editorial Instrument.
 *
 * Implements a 3D glass flipping architecture with structural parity.
 * - Front side: Primary data visualization and interaction.
 * - Back side: Configuration and metadata (lazy-loaded).
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

    // State
    state: AeonCardState = rememberAeonCardState(),

    // Filter properties
    filterEnabled: Boolean = true,
    filter: TransactionFilter? = null,
    baseFilter: TransactionFilter = TransactionFilter(),
    filterRules: CardFilterRules? = null,
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    onFilterChange: ((TransactionFilter) -> Unit)? = null,
    onSaveFilterClick: (TransactionFilter) -> Unit = {},

    // Settings / Back Side
    displaySettings: (@Composable (onDismiss: () -> Unit) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    settingsContent: (@Composable ColumnScope.() -> Unit)? = null,

    timelineMode: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = if (onClick != null) remember { MutableInteractionSource() } else null
    val isPressed = interactionSource?.collectIsPressedAsState()?.value ?: false
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "cardScale")
    val cardShape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius)

    val openAnim = LocalCardOpenAnimation.current
    val isFlip = openAnim == CardOpenAnimation.FLIP

    // Flip Animation Logic — only the FLIP style rotates the card; the others swap content in place.
    val rotation by animateFloatAsState(
        targetValue = if (state.isFlipped && isFlip) 180f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )
    val isFrontVisible by remember { derivedStateOf { rotation <= 90f } }

    // Discoverability & Intent Feedback (only meaningful while the card is mid-rotation)
    LaunchedEffect(isFrontVisible) {
        if (isFlip && rotation > 0f && rotation < 180f) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val borderModifier = when {
        isWarning -> Modifier.border(BorderStroke(0.5.dp, AppColors.error), cardShape)
        accentColor != null -> Modifier.border(BorderStroke(1.dp, accentColor), cardShape)
        else -> Modifier.aeonHairlineBorder(cardShape)
    }

    // The reading (front) and settings (back) bodies — shared by every open style.
    val frontBody: @Composable BoxScope.() -> Unit = {
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

        Column(modifier = Modifier.fillMaxWidth()) {
            val effectiveSubtitle =
                remember(subtitle, filter?.timelineRange, timelineMode) {
                    subtitle
                        ?: if (!timelineMode) filter?.timelineRange?.let { timelineLabel(it) } else null
                }

            AeonCardHeader(
                title = title,
                titleColor = contentColor,
                subtitle = effectiveSubtitle,
                subtitleColor = subtitleColor,
                icon = icon,
                iconColor = contentColor,
                subtitleClickable = filterEnabled && (filter != null) && (onFilterChange != null),
                onSubtitleClick = { state.showFilter(true) },
                onHeaderClick = onClick,
                onLongClick = {
                    if (settingsContent != null || displaySettings != null) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.flip(true)
                    }
                },
                showFilterIndicator = filter != null && filter != baseFilter,
                accentColor = accentColor ?: AppColors.primary,
                modifier = headerModifier
            )

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

    val backBody: @Composable BoxScope.() -> Unit = {
        val onDismissBack = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            state.flip(false)
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

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationY = if (isFlip) rotation else 0f
                cameraDistance = 20f * density

                // Dynamic visual weight during rotation (FLIP only)
                if (isFlip && rotation > 0.5f && rotation < 179.5f) {
                    val p = if (rotation <= 90f) rotation / 90f else (180f - rotation) / 90f
                    shadowElevation = (2.dp + (6.dp * p)).toPx()
                    alpha = 1f - (p * 0.15f)
                } else {
                    shadowElevation = 2.dp.toPx()
                    alpha = 1f
                }
                clip = true
                shape = cardShape
            }
            .then(borderModifier)
            .then(
                // FLIP resizes the whole card; the others resize via AnimatedContent's SizeTransform.
                if (isFlip) Modifier.animateContentSize(tween(400, easing = FastOutSlowInEasing))
                else Modifier
            )
            .then(
                if ((onClick != null) && !state.isFlipped) Modifier.clickable(
                    interactionSource = interactionSource
                        ?: remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = onClick,
                ) else Modifier
            ),
    ) {
        if (isFlip) {
            if (isFrontVisible) {
                AeonCardSideShell(containerColor = containerColor) { frontBody() }
            } else {
                // BACK SIDE: Identity transform restoration via 180 counter-rotation
                AeonCardSideShell(
                    containerColor = containerColor,
                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                ) { backBody() }
            }
        } else if (settingsContent != null) {
            // Persistent-header model — the header stays fixed as a masthead; only the body swaps
            // (reading <-> settings) with the chosen motion, and the card resizes to fit.
            AeonCardSideShell(containerColor = containerColor) {
                if (!state.isFlipped) {
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
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    val onDismissBack = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        state.flip(false)
                    }
                    // Persistent header: reading affordances vs configuration affordances (title stays).
                    if (!state.isFlipped) {
                        val effectiveSubtitle =
                            remember(subtitle, filter?.timelineRange, timelineMode) {
                                subtitle
                                    ?: if (!timelineMode) filter?.timelineRange?.let { timelineLabel(it) } else null
                            }
                        AeonCardHeader(
                            title = title,
                            titleColor = contentColor,
                            subtitle = effectiveSubtitle,
                            subtitleColor = subtitleColor,
                            icon = icon,
                            iconColor = contentColor,
                            subtitleClickable = filterEnabled && (filter != null) && (onFilterChange != null),
                            onSubtitleClick = { state.showFilter(true) },
                            onHeaderClick = onClick,
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                state.flip(true)
                            },
                            showFilterIndicator = filter != null && filter != baseFilter,
                            accentColor = accentColor ?: AppColors.primary,
                            modifier = headerModifier
                        )
                    } else {
                        AeonCardHeader(
                            title = title,
                            subtitle = "CONFIGURATION",
                            icon = Icons.Default.ChevronLeft,
                            onIconClick = onDismissBack,
                            trailingIcon = if (onDelete != null) Icons.Default.Delete else null,
                            onTrailingIconClick = onDelete,
                        )
                    }

                    Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))

                    AnimatedContent(
                        targetState = state.isFlipped,
                        transitionSpec = { cardOpenTransitionSpec(openAnim) },
                        modifier = Modifier.fillMaxWidth(),
                        label = "cardBody"
                    ) { flipped ->
                        if (!flipped) {
                            Column(modifier = Modifier.fillMaxWidth()) {
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
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
                            ) {
                                settingsContent()
                            }
                        }
                    }
                }
            }
        } else {
            // Custom displaySettings back (rare) — swap the whole side without a shared header.
            AeonCardSideShell(containerColor = containerColor) {
                if (!state.isFlipped) frontBody() else backBody()
            }
        }
    }

    FilterPillOverlay(
        visible = state.isFilterVisible && filter != null && onFilterChange != null,
        filter = filter,
        onFilterChange = onFilterChange,
        onDismiss = { state.showFilter(false) },
        filterRules = filterRules,
        filterContext = filterContext,
        baseFilter = baseFilter,
        onSaveFilterClick = onSaveFilterClick
    )
}

/**
 * Enter/exit transition for a non-flip card open, keyed off the chosen [CardOpenAnimation].
 * FLIP never reaches here (it uses the 3D rotation path); the others swap the reading and settings
 * bodies with a matching motion while [AnimatedContent]'s default [SizeTransform] resizes the card.
 */
private fun AnimatedContentTransitionScope<Boolean>.cardOpenTransitionSpec(
    anim: CardOpenAnimation
): ContentTransform {
    val dur = 420
    // Each branch's ContentTransform carries a default SizeTransform, so the card also
    // resizes between the reading and settings body sizes as the bodies swap.
    return when (anim) {
        // Body slides out to the side and the new body slides in from the other side.
        CardOpenAnimation.PAGE ->
            (slideInHorizontally(tween(dur)) { it } + fadeIn(tween(dur))) togetherWith
                (slideOutHorizontally(tween(dur)) { -it } + fadeOut(tween(dur / 2)))

        // Body clip-reveals downward from under the header (roll down); the outgoing body rolls up.
        CardOpenAnimation.ROLL ->
            (expandVertically(tween(dur), expandFrom = Alignment.Top) + fadeIn(tween(dur))) togetherWith
                (shrinkVertically(tween(dur), shrinkTowards = Alignment.Top) + fadeOut(tween(dur / 2)))

        // A curtain clip-reveals down over the outgoing body as the card expands.
        CardOpenAnimation.DROP ->
            (expandVertically(tween(dur), expandFrom = Alignment.Top) + fadeIn(tween(dur, delayMillis = dur / 4))) togetherWith
                fadeOut(tween(dur / 2))

        else -> // DISSOLVE: fade the body out, resize while empty, then fade the new body in.
            fadeIn(tween(dur / 2, delayMillis = dur / 2)) togetherWith fadeOut(tween(dur / 3))
    }
}

/**
 * Standardizes the synchronization between local state and global card editing sessions.
 */
@Composable
fun rememberAeonCardState(
    cardId: String? = LocalCardId.current,
): AeonCardState {
    val sessionState = LocalEditingCardId.current
    val onSessionChange = LocalOnEditCard.current
    return remember(cardId, sessionState) {
        AeonCardState(cardId, onSessionChange = onSessionChange, sessionState = sessionState)
    }
}

/**
 * Hero card variant for primary dashboard readouts.
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
    state: AeonCardState = rememberAeonCardState(),
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
        state = state,
        displaySettings = displaySettings,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label.uppercase(),
                style = AppTextStyles.overlineLabel,
                color = contentColor.copy(alpha = 0.6f)
            )
            Text(
                text = amount, style = AppTextStyles.heroAmountDisplay, color = amountColor
            )
        }
        if (content != null) {
            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))
            content()
        }
    }
}

// ── Overlay Management ───────────────────────────────────────────────────────

@Composable
private fun FilterPillOverlay(
    visible: Boolean,
    filter: TransactionFilter?,
    onFilterChange: ((TransactionFilter) -> Unit)?,
    onDismiss: () -> Unit,
    filterRules: CardFilterRules?,
    filterContext: FilterSelectionContext,
    baseFilter: TransactionFilter,
    onSaveFilterClick: (TransactionFilter) -> Unit
) {
    if (!visible || filter == null || onFilterChange == null) return

    val currentFilter = rememberUpdatedState(filter)
    val currentOnFilterChange = rememberUpdatedState(onFilterChange)
    val onFilterEvent = remember<(FilterEvent) -> Unit> {
        { event ->
            when (event) {
                is FilterEvent.Apply -> currentOnFilterChange.value.invoke(event.filter)
                is FilterEvent.UpdateTimeline -> {
                    val current = currentFilter.value ?: return@remember
                    currentOnFilterChange.value.invoke(current.copy(timelineRange = event.range))
                }

                is FilterEvent.Reset -> currentOnFilterChange.value.invoke(TransactionFilter())
                else -> Unit
            }
        }
    }

    val filterPillController = LocalFilterPillController.current

    if (filterPillController != null) {
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
                    onDismiss = onDismiss,
                    filterRules = filterRules ?: CardFilterRules.DEFAULT,
                    baseFilter = baseFilter,
                )
            )
        }
        DisposableEffect(Unit) { onDispose { filterPillController.dismiss() } }
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false, dismissOnClickOutside = false
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
                onDismiss = onDismiss,
                modifier = Modifier.fillMaxSize(),
                filterRules = filterRules ?: CardFilterRules.DEFAULT,
                baseFilter = baseFilter,
            )
        }
    }
}

private fun timelineLabel(range: TimelineRange): String {
    val (startMs, endMs) = TimelineService.getDateRange(range)
    if (startMs == null || endMs == null) return "All Time"

    val startCal = Calendar.getInstance().apply { timeInMillis = startMs }
    val endCal = Calendar.getInstance().apply { timeInMillis = endMs }
    val currentYear = Calendar.getInstance()[Calendar.YEAR]

    val resolvedDate =
        if ((startCal[Calendar.DAY_OF_MONTH] == 1) && (endCal[Calendar.DAY_OF_MONTH] == endCal.getActualMaximum(
                Calendar.DAY_OF_MONTH
            )) && (startCal[Calendar.MONTH] == endCal[Calendar.MONTH]) && (startCal[Calendar.YEAR] == endCal[Calendar.YEAR])
        ) {
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(startMs))
        } else {
            val fmt =
                if (startCal.get(Calendar.YEAR) == currentYear && endCal.get(Calendar.YEAR) == currentYear) SimpleDateFormat(
                    "MMM d",
                    Locale.getDefault()
                )
                else SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            "${fmt.format(Date(startMs))} – ${fmt.format(Date(endMs))}"
        }

    val prefix = when (range) {
        is TimelineRange.ThisPeriod -> "${range.period.label} · "
        is TimelineRange.LastPeriod -> "Last ${range.period.label} · "
        else -> ""
    }
    return prefix + resolvedDate
}

// ── Previews ─────────────────────────────────────────────────────────────────

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
                settingsContent = { Text("Configuration Screen") }) {
                Text(
                    "$12,450.00",
                    style = AppTextStyles.heroAmount,
                    color = AppColors.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}
