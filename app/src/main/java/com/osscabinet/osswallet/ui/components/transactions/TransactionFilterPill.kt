package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Floating pill that holds only the timeline selector when collapsed.
 *
 * Gesture mechanics:
 *  - Swipe up: pill moves up in real-time and morphs into the full-screen filter as
 *    the finger moves. Release past 50 % → springs to full screen. Release below → snaps back.
 *  - Long press: presets dropdown.
 *  - Expanded / back button / swipe down on top bar: real-time collapse, springs back to pill.
 *
 * Must be called with Modifier.fillMaxSize() from the parent Box so the expansion can
 * cover the whole screen. Pass bottomPadding so the pill floats above the nav bar.
 */
@Composable
fun TransactionFilterPill(
    filter: TransactionFilter,
    accounts: List<Account>,
    categories: List<Category>,
    savedFilters: List<Filter>,
    onFilterEvent: (FilterEvent) -> Unit,
    onSaveFilterClick: (TransactionFilter) -> Unit,
    bottomPadding: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 0f = compact pill at bottom, 1f = full-screen filter.
    // Driven in real-time by gesture; springs to 0 or 1 on release.
    val morph = remember { Animatable(0f) }

    // Safe fraction clamped to [0,1] so lerp calls never produce negative sizes.
    val f by remember { derivedStateOf { morph.value.coerceIn(0f, 1f) } }

    // Content switch at the midpoint so the filter form appears on a nearly-full surface.
    val showExpanded by remember { derivedStateOf { morph.value > 0.5f } }

    val pillH = 76.dp   // compact: just enough for AeonTimelinePeriodSelector
    val pillHorizMargin = 20.dp

    BoxWithConstraints(modifier = modifier) {
        val maxH = maxHeight
        val maxHPx = with(density) { maxH.toPx() }
        val pillHPx = with(density) { pillH.toPx() }
        val bottomPx = with(density) { bottomPadding.toPx() }

        // Y of the pill's top edge when fully collapsed.
        val collapsedTopPx = maxHPx - bottomPx - pillHPx

        // The pixel distance the pill travels from rest to screen-top — used to
        // convert raw drag pixels into a 0–1 fraction.
        val travelPx = collapsedTopPx

        // --- helpers that must capture travelPx and live inside BoxWithConstraints ---

        fun snapByDelta(rawPixelDelta: Float) {
            // rawPixelDelta: negative = dragging up, positive = dragging down.
            // Upward drag increases morph; downward drag decreases it.
            val newF = (morph.value - rawPixelDelta / travelPx).coerceIn(0f, 1f)
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

        // Interpolated visual properties
        val surfaceH      = lerp(pillH, maxH, f)
        val horizMargin   = lerp(pillHorizMargin, 0.dp, f)
        val corner        = ((1f - f) * 28f).coerceAtLeast(0f).dp
        val shadowElev    = lerp(8.dp, 0.dp, f)
        val offsetYPx     = collapsedTopPx * (1f - f)

        // Scrim
        if (f > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = f * 0.5f))
                    .then(
                        if (f > 0.4f) Modifier.pointerInput(Unit) {
                            detectTapGestures { collapse() }
                        } else Modifier
                    )
            )
        }

        // The single morphing surface.
        Surface(
            shape = RoundedCornerShape(corner),
            color = AppColors.sheetSurface,
            shadowElevation = shadowElev,
            modifier = Modifier
                .padding(horizontal = horizMargin)
                .fillMaxWidth()
                .height(surfaceH)
                .offset { IntOffset(0, offsetYPx.roundToInt()) }
                .then(
                    // Gesture modifiers are only active while in pill mode.
                    if (!showExpanded) Modifier
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { settle() },
                                onDragCancel = { settle() },
                                onVerticalDrag = { _, delta ->
                                    // Only respond to upward drags (or cancelling a partial drag).
                                    if (delta < 0f || morph.value > 0f) snapByDelta(delta)
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    if (savedFilters.isNotEmpty()) showDropdown = true
                                }
                            )
                        }
                    else Modifier
                )
        ) {
            if (showExpanded) {
                FullScreenFilterContent(
                    filter = filter,
                    accounts = accounts,
                    categories = categories,
                    savedFilters = savedFilters,
                    onFilterEvent = onFilterEvent,
                    onSaveFilterClick = onSaveFilterClick,
                    onClose = ::collapse,
                    onDragDelta = ::snapByDelta,
                    onDragEnd = ::settle
                )
            } else {
                PillContent(filter = filter, onFilterEvent = onFilterEvent)
            }
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
// Pill content — just the timeline selector, no extras.
// ---------------------------------------------------------------------------

@Composable
private fun PillContent(
    filter: TransactionFilter,
    onFilterEvent: (FilterEvent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        AeonTimelinePeriodSelector(
            currentRange = filter.timelineRange,
            onRangeSelected = { onFilterEvent(FilterEvent.UpdateTimeline(it)) },
            swipeEntireComponent = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ---------------------------------------------------------------------------
// Full-screen filter content — shown after the pill finishes expanding.
// ---------------------------------------------------------------------------

@Composable
private fun FullScreenFilterContent(
    filter: TransactionFilter,
    accounts: List<Account>,
    categories: List<Category>,
    savedFilters: List<Filter>,
    onFilterEvent: (FilterEvent) -> Unit,
    onSaveFilterClick: (TransactionFilter) -> Unit,
    onClose: () -> Unit,
    // onDragDelta: raw pixel delta from the top-bar drag; caller converts to fraction.
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar — drag downward to collapse, back button to close.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.sheetSurface)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 4.dp, vertical = 8.dp)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        // Only pass downward (positive) drag deltas to the morph driver.
                        if (delta > 0f) onDragDelta(delta)
                    },
                    onDragStopped = { onDragEnd() }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close filter",
                    tint = AppColors.contentPrimary
                )
            }
            Text(
                "Filter",
                style = AppTextStyles.screenTitle.copy(fontWeight = FontWeight.Bold),
                color = AppColors.contentPrimary
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))

        FilterSheetContent(
            filter = filter,
            accounts = accounts,
            allCategories = categories,
            savedFilters = savedFilters,
            onFilterEvent = onFilterEvent,
            onSaveFilterClick = onSaveFilterClick,
            onClose = onClose,
            backgroundColor = AppColors.sheetSurface,
            accountNames = remember(accounts) { accounts.associate { it.id to it.name } }
        )
    }
}
