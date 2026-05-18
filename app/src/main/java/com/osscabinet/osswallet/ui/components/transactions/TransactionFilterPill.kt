package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Floating pill at the bottom of the transaction screen.
 *
 * Collapsed: a compact, centered pill showing only the timeline selector.
 *   - Long-press: opens presets dropdown.
 *   - Swipe up: pill floats to top while morphing into the full-screen filter.
 *
 * Expanded: full-screen filter sheet.
 *   - Back button or swipe-down on the top bar: collapses back to pill.
 *
 * Must be placed inside a Box with Modifier.fillMaxSize() so the expansion can
 * reach the full screen. bottomPadding controls how high the pill sits above the
 * navigation bar when collapsed.
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
    var isExpanded by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }

    val dragOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // No bounce — overshoot makes corner radius go negative.
    val expandFraction by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pill_expand"
    )

    // Always clamp so downstream lerp calls are safe even if the spring overshoots slightly.
    val f = expandFraction.coerceIn(0f, 1f)

    // Pill geometry constants
    val pillH = 92.dp
    val pillHorizMargin = 20.dp  // margin each side → pill is narrower than the screen

    BoxWithConstraints(modifier = modifier) {
        val maxH = maxHeight
        val maxHPx = with(density) { maxH.toPx() }
        val pillHPx = with(density) { pillH.toPx() }
        val bottomPx = with(density) { bottomPadding.toPx() }

        // Y of the pill's top edge when collapsed, measured from the top of this Box.
        val collapsedTopPx = maxHPx - bottomPx - pillHPx

        // Interpolated values — always computed from the clamped fraction.
        val surfaceH = lerp(pillH, maxH, f)
        val horizMargin = lerp(pillHorizMargin, 0.dp, f)
        // Corner: 28dp pill → 0dp full-screen, clamped to avoid negatives.
        val corner = ((1f - f) * 28f).coerceAtLeast(0f).dp
        val shadowElev = lerp(8.dp, 0.dp, f)
        // Y offset: pill rests at collapsedTopPx, expands toward 0.
        val offsetYPx = collapsedTopPx * (1f - f)
        // Live upward drag feedback only while collapsed.
        val liveTranslation = if (!isExpanded) dragOffset.value else 0f

        // Scrim appears as the pill expands.
        if (f > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = f * 0.5f))
                    .then(
                        if (f > 0.4f) Modifier.pointerInput(Unit) {
                            detectTapGestures { isExpanded = false }
                        } else Modifier
                    )
            )
        }

        // The single morphing surface — pill shape when collapsed, full-screen when expanded.
        Surface(
            shape = RoundedCornerShape(corner),
            color = AppColors.sheetSurface,
            shadowElevation = shadowElev,
            modifier = Modifier
                .padding(horizontal = horizMargin)     // shrinks pill width, disappears when expanded
                .fillMaxWidth()
                .height(surfaceH)
                .offset { IntOffset(0, (offsetYPx + liveTranslation).roundToInt()) }
                .then(
                    if (!isExpanded) Modifier
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragOffset.value < -swipeThresholdPx) {
                                        isExpanded = true
                                        scope.launch { dragOffset.snapTo(0f) }
                                    } else {
                                        scope.launch {
                                            dragOffset.animateTo(
                                                0f,
                                                spring(stiffness = Spring.StiffnessLow)
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        dragOffset.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                                    }
                                },
                                onVerticalDrag = { _, delta ->
                                    scope.launch {
                                        // Only allow upward drags (negative delta).
                                        dragOffset.snapTo((dragOffset.value + delta).coerceAtMost(0f))
                                    }
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
            Crossfade(targetState = isExpanded, label = "pill_crossfade") { expanded ->
                if (expanded) {
                    FullScreenFilterContent(
                        filter = filter,
                        accounts = accounts,
                        categories = categories,
                        savedFilters = savedFilters,
                        onFilterEvent = onFilterEvent,
                        onSaveFilterClick = onSaveFilterClick,
                        onClose = { isExpanded = false }
                    )
                } else {
                    PillContent(
                        filter = filter,
                        onFilterEvent = onFilterEvent
                    )
                }
            }
        }

        // Presets dropdown — floats just above the collapsed pill position.
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

@Composable
private fun PillContent(
    filter: TransactionFilter,
    onFilterEvent: (FilterEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Grip handle — hints at swipe-up interaction.
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(3.dp)
                .background(AppColors.divider.copy(alpha = 0.45f), RoundedCornerShape(1.5.dp))
        )

        Spacer(Modifier.height(8.dp))

        AeonTimelinePeriodSelector(
            currentRange = filter.timelineRange,
            onRangeSelected = { onFilterEvent(FilterEvent.UpdateTimeline(it)) },
            swipeEntireComponent = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FullScreenFilterContent(
    filter: TransactionFilter,
    accounts: List<Account>,
    categories: List<Category>,
    savedFilters: List<Filter>,
    onFilterEvent: (FilterEvent) -> Unit,
    onSaveFilterClick: (TransactionFilter) -> Unit,
    onClose: () -> Unit
) {
    var dragAccum by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val collapseThresholdPx = with(density) { 80.dp.toPx() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar — back button + swipe-down-to-close.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.sheetSurface)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 4.dp, vertical = 8.dp)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        if (delta > 0) {
                            dragAccum += delta
                            if (dragAccum > collapseThresholdPx) {
                                onClose()
                                dragAccum = 0f
                            }
                        }
                    },
                    onDragStopped = { dragAccum = 0f }
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
