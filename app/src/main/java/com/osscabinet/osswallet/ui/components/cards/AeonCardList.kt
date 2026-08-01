package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.osscabinet.ossWallet.domain.card.CardComputeState
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.ui.components.rememberShimmerBrush
import com.osscabinet.ossWallet.ui.reorderable.ReorderableItem
import com.osscabinet.ossWallet.ui.reorderable.longPressDraggableHandle
import com.osscabinet.ossWallet.ui.reorderable.rememberReorderableLazyListState
import com.osscabinet.ossWallet.ui.reorderable.reorderable
import com.osscabinet.ossWallet.ui.reorderable.reorderableSemantics
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings

/**
 * Visual/interaction phase for the reorderable list.
 *
 * IDLE     — normal dashboard, cards full-size, tapping a card opens it.
 * WIGGLING — every card is collapsed to a compact header row; nothing is being dragged yet.
 * DRAGGING — a press-hold-move on a collapsed row is actively repositioning it.
 *
 * Entering (long-press any card -> WIGGLING) and exiting (tap outside -> IDLE, persisting the
 * buffered order) are the only transitions that write through `onReorder`; every swap while
 * WIGGLING/DRAGGING only mutates the in-memory `editList`.
 */
private enum class CardReorderMode { IDLE, WIGGLING, DRAGGING }

@Composable
fun AeonCardList(
    cards: List<CardConfig>,
    cardInput: CardInput,
    cardUpdateHandler: CardUpdateHandler,
    onReorder: (List<CardConfig>) -> Unit,
    onDelete: ((String) -> Unit)?,
    onRecordsClick: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(AeonTheme.dimensions.screenPadding),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd),
    animateCards: Boolean = false,
    filterContext: FilterSelectionContext = FilterSelectionContext(),
    callbacks: AeonInteractionCallbacks = AeonInteractionCallbacks(),
    baseFilter: TransactionFilter = TransactionFilter(),
    screenRules: CardFilterRules = CardFilterRules.DEFAULT,
    disableFilterSheet: Boolean = false,
    header: @Composable (LazyItemScope.() -> Unit)? = null,
    cardDataMap: ImmutableMap<String, CardComputeState> = persistentMapOf(),
    state: LazyListState = rememberLazyListState(),
    scrollToId: String? = null,
    highlightedId: String? = null,
    onScrollComplete: () -> Unit = {},
) {
    // Locally-owned visual order, decoupled from `cards`. A swap during drag must reorder this
    // list synchronously so the LazyColumn re-lays-out in the same frame the finger crosses a
    // threshold — waiting on `onReorder` to round-trip through the repository/Room and back into
    // `cards` is an I/O-bound async hop per swap, which is what made items appear not to move.
    // Mirrors the `editList` pattern already used in AccountListScreen/TemplateListScreen.
    var editList by remember { mutableStateOf(emptyList<CardConfig>()) }
    var reorderMode by remember { mutableStateOf(CardReorderMode.IDLE) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(cards) {
        val sorted = cards.sortedBy { it.order }
        val sameIds = editList.map { it.cardId }.toSet() == sorted.map { it.cardId }.toSet()
        editList = if (editList.isEmpty() || !sameIds) {
            sorted
        } else {
            // Preserve the in-progress/just-dropped visual order; only field values changed upstream.
            val orderMap = editList.withIndex().associate { it.value.cardId to it.index }
            sorted.sortedBy { orderMap[it.cardId] ?: Int.MAX_VALUE }
        }
    }

    val onReorderRef by rememberUpdatedState(onReorder)
    val cardDataMapState = rememberUpdatedState(cardDataMap)
    val headerOffset = if (header != null) 1 else 0

    // Exit wiggle mode: `persist = true` writes the buffered `editList` through to the repository
    // (tap-outside, nav-away, or dropping a card back to WIGGLING all persist); `persist = false`
    // discards it and snaps back to the last-persisted order (app backgrounded mid-edit).
    fun exitReorderMode(persist: Boolean) {
        if (persist) {
            onReorderRef(editList)
        } else {
            editList = cards.sortedBy { it.order }
        }
        reorderMode = CardReorderMode.IDLE
    }

    // Navigating away from this screen removes AeonCardList from composition before any
    // tap-outside gesture can fire — treat that the same as tap-outside: persist, don't discard.
    DisposableEffect(Unit) {
        onDispose { if (reorderMode != CardReorderMode.IDLE) onReorderRef(editList) }
    }

    // Backgrounding mid-edit discards the buffered order rather than silently persisting a reorder
    // the user never explicitly confirmed by exiting wiggle mode.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (reorderMode != CardReorderMode.IDLE) {
            editList = cards.sortedBy { it.order }
            reorderMode = CardReorderMode.IDLE
        }
    }

    val reorderState = rememberReorderableLazyListState(
        lazyListState = state,
        swapTravelRatio = 0.4f,
        onAccessibilityMove = {
            // TalkBack "Move up"/"Move down" bypasses wiggle mode entirely and persists immediately
            // per action — there's no drag-stop-equivalent event to hang buffered persistence off of.
            onReorderRef(editList)
        },
    ) { fromLazy, toLazy ->
        val from = fromLazy - headerOffset
        val to = toLazy - headerOffset
        if ((from in editList.indices) && (to in editList.indices)) {
            editList = editList.toMutableList().apply { add(to, removeAt(from)) }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val trackPad = maxHeight * 0.10f

        // Tap-outside-to-exit scrim: sits behind the LazyColumn so item-level gestures (drag
        // handles, card taps) still get first crack at any touch that lands on a card; only taps
        // that fall through an empty stretch of the list reach this and exit wiggle mode.
        if (reorderMode != CardReorderMode.IDLE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(reorderMode) {
                        detectTapGestures { exitReorderMode(persist = true) }
                    }
            )
        }

        LazyColumn(
            state = reorderState.lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .reorderable(reorderState),
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
        ) {
            if (header != null) {
                item(key = "__aeon_card_list_header") { header() }
            }

            items(editList, key = { it.cardId }, contentType = { it.cardType }) { cardConfig ->
                val draggingActive = reorderState.draggedKey != null
                val isDragging = reorderState.isDragging(cardConfig.cardId)
                val alpha by animateFloatAsState(
                    targetValue = if (draggingActive && !isDragging) 0.75f else 1.0f,
                    label = "card_drag_alpha"
                )

                // IDLE: long-press anywhere on the row enters wiggle mode (collapses every card)
                // without starting a drag — a descendant long-press consumer (e.g. the card header's
                // existing "flip to settings" gesture) still wins if it's the one under the finger.
                // WIGGLING/DRAGGING: press-hold-then-move on the (now collapsed) row starts/continues
                // the actual drag — a second, distinct gesture from the one that entered the mode.
                val rowModifier = if (reorderMode == CardReorderMode.IDLE) {
                    Modifier.pointerInput(cardConfig.cardId) {
                        detectTapGestures(onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            reorderMode = CardReorderMode.WIGGLING
                        })
                    }
                } else {
                    Modifier.longPressDraggableHandle(
                        state = reorderState,
                        key = cardConfig.cardId,
                        onDragStarted = { reorderMode = CardReorderMode.DRAGGING },
                        // Drop returns to WIGGLING (still collapsed), not IDLE — persistence only
                        // happens when the user exits wiggle mode entirely.
                        onDragStopped = { reorderMode = CardReorderMode.WIGGLING },
                    )
                }

                ReorderableItem(
                    state = reorderState,
                    key = cardConfig.cardId,
                    modifier = Modifier
                        .then(rowModifier)
                        .reorderableSemantics(state = reorderState, key = cardConfig.cardId)
                        .graphicsLayer { this.alpha = alpha },
                ) { _ ->
                    if (reorderMode == CardReorderMode.IDLE) {
                        val cardState by remember(cardConfig.cardId) {
                            derivedStateOf { cardDataMapState.value[cardConfig.cardId] }
                        }

                        CardItem(
                            modifier = Modifier,
                            cardConfig = cardConfig,
                            cardState = cardState,
                            cardInput = cardInput,
                            cardUpdateHandler = cardUpdateHandler,
                            onDelete = onDelete,
                            onRecordsClick = onRecordsClick,
                            callbacks = callbacks,
                            baseFilter = baseFilter,
                            screenRules = screenRules,
                            filterContext = filterContext,
                            disableFilterSheet = disableFilterSheet,
                            animateCards = animateCards,
                            scrollToId = scrollToId,
                            highlightedId = highlightedId,
                            onScrollComplete = onScrollComplete,
                        )
                    } else {
                        CollapsedCardRow(cardConfig = cardConfig)
                    }
                }
            }
        }

        LazyColumnScrollbar(
            state = reorderState.lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = trackPad),
            settings = ScrollbarSettings.Default.copy(
                thumbSelectedColor = AppColors.primary,
                thumbUnselectedColor = AppColors.primary.copy(alpha = 0.5f),
                thumbThickness = 6.dp,
                thumbShape = CircleShape,
                thumbMinLength = 0.1f,
                thumbMaxLength = 0.1f
            )
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun CardItem(
    modifier: Modifier,
    cardConfig: CardConfig,
    cardState: CardComputeState?,
    cardInput: CardInput,
    cardUpdateHandler: CardUpdateHandler,
    onDelete: ((String) -> Unit)?,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks,
    baseFilter: TransactionFilter,
    screenRules: CardFilterRules,
    filterContext: FilterSelectionContext,
    disableFilterSheet: Boolean,
    animateCards: Boolean,
    scrollToId: String?,
    highlightedId: String?,
    onScrollComplete: () -> Unit,
) {
    val card = rememberCardWithCallbacks(
        config = cardConfig,
        state = cardState,
        input = cardInput,
        updateHandler = cardUpdateHandler,
        onDelete = onDelete,
        onRecordsClick = onRecordsClick,
        callbacks = callbacks,
        filterSheetEnabled = !disableFilterSheet,
    )

    val effectiveCard = remember(card, screenRules, baseFilter, disableFilterSheet) {
        card.copyWithRules(
            rules = card.filterRules.layeredWith(screenRules),
            baseFilter = baseFilter,
            filterSheetEnabled = !disableFilterSheet
        )
    }

    UnifiedCardRenderer(
        cardWithCallbacks = effectiveCard,
        props = CardLayoutProps(
            modifier = Modifier.fillMaxWidth(),
            headerModifier = modifier,
            filterContext = filterContext,
            inheritedFilter = cardInput.filter,
            screenOwnsTimeline = cardInput.screenOwnsTimeline,
            animate = animateCards,
            scrollToId = scrollToId,
            highlightedId = highlightedId,
            onScrollComplete = onScrollComplete
        )
    )
}


/**
 * Uniform compact row shown for every card type while the list is in wiggle/drag mode — icon,
 * title, and a trailing drag-handle glyph, no card-type-specific preview content. Deliberately
 * shares `AeonCardHeader` with the full card's header so icon/title rendering stays identical
 * between the expanded and collapsed states. The drag/long-press gesture itself is attached one
 * level up, on the enclosing `ReorderableItem`, so the whole row is draggable — not just this row.
 */
@Composable
private fun CollapsedCardRow(cardConfig: CardConfig) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        color = AppColors.cardSurface,
        border = BorderStroke(0.5.dp, AppColors.contentPrimary.copy(alpha = 0.08f)),
    ) {
        Box(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
            AeonCardHeader(
                title = cardConfig.title.ifBlank { cardConfig.cardType.displayName },
                icon = cardConfig.cardType.toIcon(),
                trailingIcon = Icons.Rounded.DragHandle,
                trailingIconColor = AppColors.contentSecondary,
            )
        }
    }
}

@Composable
fun CardErrorState(message: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.errorContainer.copy(alpha = 0.2f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            AppColors.error.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(AeonTheme.dimensions.spacingMd),
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = AppColors.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message ?: "Failed to load card",
                style = AppTextStyles.transactionTitle,
                color = AppColors.onErrorContainer
            )
        }
    }
}

@Composable
fun CardErrorContent(message: String?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = AppColors.error,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = message ?: "Failed to load card",
            style = AppTextStyles.transactionTitle,
            color = AppColors.error,
        )
    }
}

@Composable
fun CardContentShimmer() {
    val brush = rememberShimmerBrush(horizontal = true, label = "card_content_shimmer")
    Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .height(28.dp)
                .background(brush, RoundedCornerShape(4.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(brush, RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius))
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CardErrorStatePreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CardErrorState(message = "Unable to compute net worth")
        }
    }
}
