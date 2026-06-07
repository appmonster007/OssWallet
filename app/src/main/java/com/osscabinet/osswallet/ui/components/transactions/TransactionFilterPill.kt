package com.osscabinet.ossWallet.ui.components.transactions

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.card.ScalarRule
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * States for the Transaction Filter Pill
 */
enum class PillState { Collapsed, Expanded }

/**
 * Floating pill at the bottom of the screen.
 *
 * Collapsed: shows only the timeline selector (56 dp tall).
 * Expanded:  grows to 80 % of available height, revealing full filter controls.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionFilterPill(
    filter: TransactionFilter,
    accounts: List<Account>,
    categories: List<Category>,
    allTags: List<Tag> = emptyList(),
    onSearchTransactions: (String) -> Flow<List<Transaction>>,
    savedFilters: List<Filter>,
    onFilterEvent: (FilterEvent) -> Unit,
    onSaveFilterClick: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier,
    onAddTag: (String, Int) -> Unit = { _, _ -> },
    onDismiss: (() -> Unit)? = null,
    bottomPadding: Dp = 16.dp,
    initialMorph: Float = 0f,
    filterRules: CardFilterRules = CardFilterRules.DEFAULT,
    baseFilter: TransactionFilter = TransactionFilter(),
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val totalBottomPadding = bottomPadding + navBarPadding

    // 1. AnchoredDraggable Setup
    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = if (initialMorph > 0.5f) PillState.Expanded else PillState.Collapsed
        )
    }

    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = draggableState,
        positionalThreshold = { distance -> distance * 0.5f },
        animationSpec = spring<Float>(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
    )

    val onDismissState = rememberUpdatedState(onDismiss)
    val actions = remember(draggableState, scope, onDismissState) {
        PillActions(draggableState, scope, onDismissState)
    }

    val f by remember {
        derivedStateOf {
            val anchors = draggableState.anchors
            if (anchors.size < 2) return@derivedStateOf initialMorph

            val collapsed = anchors.positionOf(PillState.Collapsed)
            val expanded = anchors.positionOf(PillState.Expanded)

            if (collapsed.isNaN() || expanded.isNaN()) return@derivedStateOf initialMorph

            val distance = collapsed - expanded
            if (distance == 0f) return@derivedStateOf initialMorph

            val offset = draggableState.offset
            if (offset.isNaN()) return@derivedStateOf initialMorph

            ((collapsed - offset) / distance).coerceIn(0f, 1f)
        }
    }

    val isOpen by remember { derivedStateOf { (f > 0f) || (onDismiss != null) } }

    BackHandler(enabled = draggableState.currentValue == PillState.Expanded) {
        actions.collapse()
    }

    val pillH = 56.dp
    val pillHorizMargin = 24.dp

    BoxWithConstraints(modifier = modifier) {
        val maxHPx = constraints.maxHeight.toFloat()
        val pillHPx = with(density) { pillH.toPx() }
        val bottomPx = with(density) { totalBottomPadding.toPx() }
        val expandedHPx = maxHPx * 0.8f

        val collapsedTopPx = maxHPx - bottomPx - pillHPx
        val expandedTopPx = maxHPx - bottomPx - expandedHPx

        LaunchedEffect(collapsedTopPx, expandedTopPx) {
            draggableState.updateAnchors(
                DraggableAnchors {
                    PillState.Collapsed at collapsedTopPx
                    PillState.Expanded at expandedTopPx
                }
            )
        }

        val nestedScrollConnection = remember(draggableState, scope, actions) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y
                    if (delta < 0 && draggableState.offset > expandedTopPx) {
                        return draggableState.dispatchRawDelta(delta).toOffset()
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    val delta = available.y
                    if (delta > 0) {
                        return draggableState.dispatchRawDelta(delta).toOffset()
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (draggableState.offset > expandedTopPx && draggableState.offset < collapsedTopPx) {
                        actions.settle()
                        return available
                    }
                    return Velocity.Zero
                }
            }
        }

        // UI Visuals
        val surfaceH = lerp(pillH, with(density) { expandedHPx.toDp() }, f)
        val horizMargin = lerp(pillHorizMargin, 8.dp, f)
        val corner = lerp(28.dp, 20.dp, f)
        val elevation = lerp(AeonSize.D8.value, 12.dp, f)
        val surfaceColor = lerp(AppColors.componentOnCard, AppColors.cardSurfaceProminent, f)
        val contentColor = lerp(AppColors.onSelectionContainer, AppColors.primary, f)
        val inactiveColor = lerp(
            AppColors.onSelectionContainer.copy(alpha = 0.4f),
            AppColors.contentSecondary.copy(alpha = 0.3f),
            f
        )
        val selectorBg = lerp(AppColors.primaryContainer, AppColors.sheetSurface, f)

        if (isOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { actions.collapse() } },
            )
        }

        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = statusBarTop)
                .background(Color.Black.copy(alpha = f * 0.4f)),
        )

        GlassSurface(
            modifier = Modifier
                .nestedScroll(nestedScrollConnection)
                .padding(horizontal = horizMargin)
                .fillMaxWidth()
                .height(surfaceH)
                .offset {
                    val y = draggableState.offset
                    val yInt = if (y.isNaN()) {
                        if (draggableState.currentValue == PillState.Collapsed) {
                            collapsedTopPx.roundToInt()
                        } else {
                            expandedTopPx.roundToInt()
                        }
                    } else {
                        y.roundToInt()
                    }
                    IntOffset(0, yInt)
                },
            shape = RoundedCornerShape(corner),
            hazeLayer = HazeLayer.Overlay,
            hazeStyle = GlassTokens.pillStyle(),
            glassVariant = com.osscabinet.ossWallet.ui.theme.GlassVariant.Pill,
            fallbackColor = surfaceColor,
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(pillH)
                        .anchoredDraggable(
                            draggableState,
                            Orientation.Vertical,
                            flingBehavior = flingBehavior
                        ),
                ) {
                    AeonTimelinePeriodSelector(
                        currentRange = filter.timelineRange,
                        onRangeSelected = { onFilterEvent(FilterEvent.UpdateTimeline(it)) },
                        swipeEntireComponent = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        isFlush = true,
                        containerColor = selectorBg,
                        contentColor = contentColor,
                        inactiveContentColor = inactiveColor,
                    )
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = AppColors.divider.copy(alpha = f * 0.6f),
                )

                FilterSheetContent(
                    filter = filter,
                    accounts = accounts,
                    allCategories = categories,
                    allTags = allTags,
                    onSearchTransactions = onSearchTransactions,
                    savedFilters = savedFilters,
                    onFilterEvent = onFilterEvent,
                    onSaveFilterClick = onSaveFilterClick,
                    onAddTag = onAddTag,
                    onClose = { actions.collapse() },
                    backgroundColor = Color.Transparent,
                    accountNames = remember(accounts) {
                        accounts.associateBy({ it.id }, { it.name })
                    },
                    filterRules = filterRules.copy(timelineRule = ScalarRule.Disabled(filter.timelineRange)),
                    baseFilter = baseFilter
                )
            }
        }
    }
}

private fun Float.toOffset() = Offset(0f, this)

@OptIn(ExperimentalFoundationApi::class)
private class PillActions(
    private val state: AnchoredDraggableState<PillState>,
    private val scope: CoroutineScope,
    private val onDismissState: State<(() -> Unit)?>,
) {
    fun expand() {
        scope.launch { state.animateTo(PillState.Expanded) }
    }

    fun collapse() {
        scope.launch {
            state.animateTo(PillState.Collapsed)
            onDismissState.value?.invoke()
        }
    }

    fun settle() {
        scope.launch {
            // Settle to the target value determined by the state.
            state.animateTo(state.targetValue)
            if (state.currentValue == PillState.Collapsed) {
                onDismissState.value?.invoke()
            }
        }
    }
}

@Preview(showBackground = true, name = "Pill — Collapsed")
@Composable
private fun TransactionFilterPillCollapsedPreview() {
    OssWalletTheme {
        Box(Modifier.fillMaxSize()) {
            TransactionFilterPill(
                filter = TransactionFilter(),
                accounts = emptyList(),
                categories = emptyList(),
                savedFilters = emptyList(),
                onSearchTransactions = { kotlinx.coroutines.flow.flowOf(emptyList()) },
                onFilterEvent = {},
                onSaveFilterClick = {},
                initialMorph = 0f,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(showBackground = true, name = "Pill — Expanded")
@Composable
private fun TransactionFilterPillExpandedPreview() {
    OssWalletTheme {
        Box(Modifier.fillMaxSize()) {
            TransactionFilterPill(
                filter = TransactionFilter(),
                accounts = emptyList(),
                categories = emptyList(),
                savedFilters = emptyList(),
                onSearchTransactions = { kotlinx.coroutines.flow.flowOf(emptyList()) },
                onFilterEvent = {},
                onSaveFilterClick = {},
                initialMorph = 1f,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
