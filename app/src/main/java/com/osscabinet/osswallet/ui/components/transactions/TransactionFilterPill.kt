package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Floating pill that holds only the timeline selector when collapsed.
 *
 * Gesture mechanics:
 *  - Swipe up: pill expands upward into a floating card, revealing filter controls.
 *    The bottom edge stays anchored; content slides out from inside the pill.
 *  - Long press: presets dropdown.
 *  - Swipe down on the card (or tap scrim): springs back to pill.
 *
 * No full-screen takeover. No header. No back button. Just a floating dialog card.
 *
 * Must be called with Modifier.fillMaxSize() from the parent Box.
 */
@Composable
fun TransactionFilterPill(
    filter: TransactionFilter,
    accounts: List<Account>,
    categories: List<Category>,
    savedFilters: List<Filter>,
    onFilterEvent: (FilterEvent) -> Unit,
    onSaveFilterClick: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 16.dp
) {
    var showDropdown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 0f = compact pill, 1f = floating filter card.
    val morph = remember { Animatable(0f) }
    val f by remember { derivedStateOf { morph.value.coerceIn(0f, 1f) } }

    // Intercept back press when the card is open — dismiss the card, not the screen.
    val isOpen by remember { derivedStateOf { morph.value > 0f } }
    BackHandler(enabled = isOpen) {
        scope.launch {
            morph.animateTo(0f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium))
        }
    }

    val pillH = 56.dp
    val pillHorizMargin = 24.dp

    BoxWithConstraints(modifier = modifier) {
        val maxH = maxHeight
        val maxHPx = with(density) { maxH.toPx() }
        val pillHPx = with(density) { pillH.toPx() }
        val bottomPx = with(density) { bottomPadding.toPx() }

        // Pill rests here (top edge of pill when collapsed).
        val collapsedTopPx = maxHPx - bottomPx - pillHPx

        // Expanded card: 68% of available height, bottom-anchored so the pill
        // grows purely upward without sliding down.
        val expandedH = maxH * 0.8f
        val expandedHPx = with(density) { expandedH.toPx() }
        val expandedTopPx = maxHPx - bottomPx - expandedHPx

        // Actual pixel travel for mapping drag → morph fraction.
        val dragTravelPx = (collapsedTopPx - expandedTopPx).coerceAtLeast(1f)

        fun snapByDelta(rawPixelDelta: Float) {
            // Upward drag (negative delta) opens; downward drag closes.
            val newF = (morph.value - rawPixelDelta / dragTravelPx).coerceIn(0f, 1f)
            scope.launch { morph.snapTo(newF) }
        }

        fun settle() {
            scope.launch {
                val target = if (morph.value >= 0.5f) 1f else 0f
                morph.animateTo(target, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium))
            }
        }

        fun collapse() {
            scope.launch {
                morph.animateTo(0f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium))
            }
        }

        // Bottom stays anchored; top edge lerps upward.
        val offsetYPx = collapsedTopPx + (expandedTopPx - collapsedTopPx) * f
        val surfaceH    = lerp(pillH, expandedH, f)
        val horizMargin = lerp(pillHorizMargin, 8.dp, f)
        val corner      = lerp(28.dp, 20.dp, f)   // stays rounded — it's a floating card
        val shadowElev  = lerp(6.dp, 12.dp, f)    // card lifts as it expands

        // Scrim — tap to dismiss.
        if (f > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = f * 0.4f))
                    .then(
                        if (f > 0.3f) Modifier.pointerInput(Unit) {
                            detectTapGestures { collapse() }
                        } else Modifier
                    )
            )
        }

        Surface(
            shape = RoundedCornerShape(corner),
            color = AppColors.cardSurfaceProminent,
            shadowElevation = shadowElev,
            modifier = Modifier
                .padding(horizontal = horizMargin)
                .fillMaxWidth()
                .height(surfaceH)
                .offset { IntOffset(0, offsetYPx.roundToInt()) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = { settle() },
                        onDragCancel = { settle() },
                        onVerticalDrag = { _, delta ->
                            // Upward drag always expands; downward drag closes when open.
                            if (delta < 0f || morph.value > 0f) snapByDelta(delta)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            if (morph.value < 0.3f && savedFilters.isNotEmpty())
                                showDropdown = true
                        }
                    )
                }
        ) {
            // Content is always in the Surface — the Surface's rounded clip reveals it
            // as the card expands. No alpha fading, no overlays.
            FilterDialogContent(
                filter = filter,
                accounts = accounts,
                categories = categories,
                savedFilters = savedFilters,
                onFilterEvent = onFilterEvent,
                onSaveFilterClick = onSaveFilterClick,
                pillHeight = pillH
            )
        }

        // Presets dropdown — anchored just above the collapsed pill.
        if (showDropdown && savedFilters.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = pillHorizMargin)
                    .padding(bottom = bottomPadding + pillH + 4.dp)
            ) {
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                    modifier = Modifier.background(AppColors.cardSurfaceProminent)
                ) {
                    savedFilters.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.name, style = AppTextStyles.listItemTitle) },
                            onClick = {
                                onFilterEvent(FilterEvent.Apply(preset.filter))
                                showDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Floating filter card content — selector at the top, controls below.
// ---------------------------------------------------------------------------

@Composable
private fun FilterDialogContent(
    filter: TransactionFilter,
    accounts: List<Account>,
    categories: List<Category>,
    savedFilters: List<Filter>,
    onFilterEvent: (FilterEvent) -> Unit,
    onSaveFilterClick: (TransactionFilter) -> Unit,
    pillHeight: Dp
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Center the timeline selector vertically within the pill height for equal bezels.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pillHeight),
            contentAlignment = Alignment.Center
        ) {
            AeonTimelinePeriodSelector(
                currentRange = filter.timelineRange,
                onRangeSelected = { onFilterEvent(FilterEvent.UpdateTimeline(it)) },
                swipeEntireComponent = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp) // Thin horizontal bezel
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.6f))

        // Filter controls — timeline selector suppressed here since it lives above.
        FilterSheetContent(
            filter = filter,
            accounts = accounts,
            allCategories = categories,
            savedFilters = savedFilters,
            onFilterEvent = onFilterEvent,
            onSaveFilterClick = onSaveFilterClick,
            onClose = {},
            backgroundColor = AppColors.cardSurfaceProminent,
            accountNames = remember(accounts) { accounts.associate { it.id to it.name } },
            filterRules = CardFilterRules.DEFAULT.copy(allowTimelineSelection = false)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun TransactionFilterPillPreview() {
    OssWalletTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            TransactionFilterPill(
                filter = TransactionFilter(timelineRange = TimelineRange.AllTime),
                accounts = emptyList(),
                categories = emptyList(),
                savedFilters = emptyList(),
                onFilterEvent = {},
                onSaveFilterClick = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
