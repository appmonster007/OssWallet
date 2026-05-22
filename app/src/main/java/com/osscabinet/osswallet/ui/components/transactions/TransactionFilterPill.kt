package com.osscabinet.ossWallet.ui.components.transactions

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Floating pill at the bottom of the screen.
 *
 * Collapsed: shows only the timeline selector (56 dp tall).
 * Expanded:  grows to 80 % of available height, revealing full filter controls.
 *
 * Gesture contract
 * ────────────────
 * • Drag / flick up on the header   → expand
 * • Drag / flick down on the header → collapse → [onDismiss]
 * • Drag down in expanded content when content is scrolled to the top → collapse → [onDismiss]
 * • Tap outside the pill            → collapse → [onDismiss]
 * • Back press while expanded       → collapse → [onDismiss]
 *
 * Must be given a finite size. Typically called with Modifier.fillMaxSize() from a Dialog overlay.
 */
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
    onAddTag: (String, Int) -> Unit = { _, _ -> },
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 16.dp,
    initialMorph: Float = 0f,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val totalBottomPadding = bottomPadding + navBarPadding

    // 0f = collapsed pill  |  1f = expanded filter card
    val morph = remember { Animatable(initialMorph) }
    val isOpen by remember { derivedStateOf { morph.value > 0f } }

    // Always-fresh reference to the dismiss callback so coroutines launched from
    // remembered lambdas don't hold a stale capture.
    val onDismissState = rememberUpdatedState(onDismiss)

    BackHandler(enabled = isOpen) {
        scope.launch {
            morph.animateTo(0f, pillSpring())
            onDismissState.value()
        }
    }

    val pillH = 56.dp
    val pillHorizMargin = 24.dp

    BoxWithConstraints(modifier = modifier) {
        val maxHPx       = with(density) { maxHeight.toPx() }
        val pillHPx      = with(density) { pillH.toPx() }
        val bottomPx     = with(density) { totalBottomPadding.toPx() }
        val expandedHPx  = maxHPx * 0.8f

        val collapsedTopPx = maxHPx - bottomPx - pillHPx
        val expandedTopPx  = maxHPx - bottomPx - expandedHPx
        // Total pixel distance the pill travels between collapsed and expanded.
        val dragRange = (collapsedTopPx - expandedTopPx).coerceAtLeast(1f)

        // Stable animation functions — keyed on morph + scope so they are recreated only
        // when those objects change (i.e. never in practice).
        val actions = remember(morph, scope, onDismissState) {
            PillActions(morph, scope, onDismissState)
        }

        // NestedScrollConnection: collapses the pill when expanded content reaches its top
        // and the user continues dragging downward.
        val nestedScrollConnection = remember(morph, scope, density, dragRange, actions) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    // available.y > 0 = downward drag the inner content could not absorb (at top).
                    if (available.y > 0f && morph.value > 0f) {
                        val newF = (morph.value - available.y / dragRange).coerceIn(0f, 1f)
                        scope.launch { morph.snapTo(newF) }
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    if (available.y > 0f && morph.value > 0f) {
                        val flickPx = with(density) { 300.dp.toPx() }
                        if (available.y > flickPx) actions.collapse() else actions.settle()
                        return available
                    }
                    return Velocity.Zero
                }
            }
        }

        val f = morph.value.coerceIn(0f, 1f)

        val surfaceH    = lerp(pillH, with(density) { expandedHPx.toDp() }, f)
        val offsetYPx   = collapsedTopPx + (expandedTopPx - collapsedTopPx) * f
        val horizMargin = lerp(pillHorizMargin, 8.dp, f)
        val corner      = lerp(28.dp, 20.dp, f)
        val elevation   = lerp(6.dp, 12.dp, f)

        val surfaceColor  = lerp(AppColors.componentOnCard,                          AppColors.cardSurfaceProminent,                  f)
        val contentColor  = lerp(AppColors.onSelectionContainer,                     AppColors.primary,                               f)
        val inactiveColor = lerp(AppColors.onSelectionContainer.copy(alpha = 0.4f),  AppColors.contentSecondary.copy(alpha = 0.3f),   f)
        val selectorBg    = lerp(AppColors.primaryContainer,                         AppColors.sheetSurface,                          f)

        // ── Dismiss layer ─────────────────────────────────────────────────────
        // Always present so taps outside the pill close the overlay regardless of morph state.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { actions.collapse() } },
        )

        // ── Scrim ──────────────────────────────────────────────────────────────
        // Always in the tree; alpha is driven by morph (no composition churn).
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = f * 0.4f)),
        )

        // ── Pill surface ───────────────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(corner),
            color = surfaceColor,
            shadowElevation = elevation,
            modifier = Modifier
                .nestedScroll(nestedScrollConnection)
                .padding(horizontal = horizMargin)
                .fillMaxWidth()
                .height(surfaceH)
                .offset { IntOffset(0, offsetYPx.roundToInt()) },
        ) {
            Column(Modifier.fillMaxSize()) {

                // ── Header ────────────────────────────────────────────────────
                // This is the sole drag target. The timeline selector inside it only
                // handles horizontal swipes, so vertical drags here are uncontested.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(pillH)
                        // Re-key the gesture handler whenever dragRange changes (layout resize)
                        // so it never holds a stale capture.
                        .pointerInput(dragRange) {
                            val vt = VelocityTracker()
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                vt.resetTracking()
                                vt.addPosition(down.uptimeMillis, down.position)

                                var dragging = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: break
                                    if (!change.pressed) break

                                    vt.addPosition(change.uptimeMillis, change.position)
                                    val dy = change.position.y - change.previousPosition.y

                                    if (!dragging &&
                                        abs(change.position.y - down.position.y) > viewConfiguration.touchSlop
                                    ) {
                                        dragging = true
                                    }

                                    if (dragging) {
                                        change.consume()
                                        val newF = (morph.value - dy / dragRange).coerceIn(0f, 1f)
                                        scope.launch { morph.snapTo(newF) }
                                    }
                                }

                                if (dragging) {
                                    val vy = vt.calculateVelocity().y
                                    val flick = with(density) { 300.dp.toPx() }
                                    when {
                                        vy < -flick -> actions.expand()
                                        vy > flick  -> actions.collapse()
                                        else        -> actions.settle()
                                    }
                                }
                            }
                        },
                ) {
                    AeonTimelinePeriodSelector(
                        currentRange = filter.timelineRange,
                        onRangeSelected = { onFilterEvent(FilterEvent.UpdateTimeline(it)) },
                        swipeEntireComponent = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        containerColor = selectorBg,
                        contentColor = contentColor,
                        inactiveContentColor = inactiveColor,
                    )
                }

                // Divider alpha mirrors morph so it fades in as the pill opens.
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = AppColors.divider.copy(alpha = f * 0.6f),
                )

                // ── Filter controls ───────────────────────────────────────────
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
                    onClose = {},
                    backgroundColor = AppColors.cardSurfaceProminent,
                    accountNames = remember(accounts) { accounts.associate { it.id to it.name } },
                    filterRules = CardFilterRules.DEFAULT.copy(allowTimelineSelection = false),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

private fun pillSpring() = spring<Float>(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)

/** Stable container for the four animation actions so they can be shared across the
 *  pointerInput handler and the NestedScrollConnection without re-creating either. */
private class PillActions(
    private val morph: Animatable<Float, *>,
    private val scope: CoroutineScope,
    private val onDismissState: State<() -> Unit>,
) {
    fun expand()  { scope.launch { morph.animateTo(1f, pillSpring()) } }

    fun collapse() {
        scope.launch {
            morph.animateTo(0f, pillSpring())
            onDismissState.value()
        }
    }

    fun settle() {
        scope.launch {
            val target = if (morph.value >= 0.5f) 1f else 0f
            morph.animateTo(target, pillSpring())
            if (morph.value == 0f) onDismissState.value()
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

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
