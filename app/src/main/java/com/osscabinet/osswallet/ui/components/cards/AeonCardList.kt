package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.card.CardComputeState
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.ui.components.rememberShimmerBrush
import com.osscabinet.ossWallet.ui.reorderable.ReorderableItem
import com.osscabinet.ossWallet.ui.reorderable.longPressToTrigger
import com.osscabinet.ossWallet.ui.reorderable.rememberReorderableLazyListState
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
 * Hoisted so a screen's top app bar can show a "tick" action while cards are being reordered.
 * Each drop already persists on its own (see [AeonCardList]'s drag handle) — this only tracks
 * whether a reorder is in progress so the tick can appear, and [confirm] just dismisses that
 * indicator. Create with [rememberAeonCardReorderState] and pass the same instance to both the
 * screen's top bar and its [AeonCardList] call.
 */
@Stable
class AeonCardReorderState {
    var isReordering: Boolean by mutableStateOf(false)
        internal set

    /** Dismiss the "reordering" indicator. Does not persist anything — each drop already did. */
    fun confirm() {
        isReordering = false
    }
}

@Composable
fun rememberAeonCardReorderState(): AeonCardReorderState = remember { AeonCardReorderState() }

/**
 * Screen-scoped ambient [AeonCardReorderState]. A screen creates one state instance, provides it
 * once via `CompositionLocalProvider` around both its top app bar and its [AeonCardList] call, and
 * neither needs to be threaded as an explicit parameter through whatever composables sit between
 * them (e.g. `DashboardContent` -> `DashboardGlanceTab`). Mirrors [LocalFilterPillController] /
 * [LocalEditingCardId] in `FilterPillController.kt`, the established pattern for this kind of
 * cross-cutting, screen-level UI state in this package.
 */
val LocalCardReorderState = staticCompositionLocalOf<AeonCardReorderState?> { null }

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
    reorderState: AeonCardReorderState = LocalCardReorderState.current ?: rememberAeonCardReorderState(),
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

    LaunchedEffect(cards) {
        // alwaysPresent cards (e.g. ACCOUNT_HERO) are fixed-position by contract (CardType.kt) and
        // must never enter a user-managed reorder list — filtered here, once, so all three screens
        // get this guarantee regardless of whether their own `cards` list already excludes it.
        val sorted = cards.filterNot { it.cardType.alwaysPresent }.sortedBy { it.order }
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
    val haptic = LocalHapticFeedback.current

    val dragListState = rememberReorderableLazyListState(lazyListState = state) { fromLazy, toLazy ->
        val from = fromLazy - headerOffset
        val to = toLazy - headerOffset
        if ((from in editList.indices) && (to in editList.indices)) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            editList = editList.toMutableList().apply { add(to, removeAt(from)) }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val trackPad = maxHeight * 0.10f

        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
        ) {
            if (header != null) {
                item(key = "__aeon_card_list_header") { header() }
            }

            items(editList, key = { it.cardId }, contentType = { it.cardType }) { cardConfig ->
                ReorderableItem(
                    state = dragListState,
                    key = cardConfig.cardId,
                    modifier = Modifier.reorderableSemantics(
                        lazyListState = state,
                        key = cardConfig.cardId,
                        // TalkBack "Move up"/"Move down" has no drag-stop-equivalent event to hang
                        // persistence off of — each invocation is a complete edit on its own, so it
                        // writes through immediately.
                        onMove = { fromLazy, toLazy ->
                            val from = fromLazy - headerOffset
                            val to = toLazy - headerOffset
                            if ((from in editList.indices) && (to in editList.indices)) {
                                editList = editList.toMutableList().apply { add(to, removeAt(from)) }
                                onReorderRef(editList)
                            }
                        },
                    ),
                ) { _ ->
                    val cardState by remember(cardConfig.cardId) {
                        derivedStateOf { cardDataMapState.value[cardConfig.cardId] }
                    }

                    // Real drag only exists while reordering — persists immediately on drop, same
                    // as AccountListScreen/TemplateListScreen. The top-bar tick button only
                    // dismisses the "reordering" indicator afterwards; it doesn't gate the save.
                    val dragHandleModifier = Modifier.longPressDraggableHandle(
                        onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onDragStopped = { onReorderRef(editList) },
                    )

                    // Collapse/expand is instant, not animated: animateContentSize() clips content
                    // to the animated bounds, which cut off every card's own elevation shadow at a
                    // hard rectangular edge (the shadow's soft blur bleeds a few dp past the card's
                    // own bounds by design).
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (reorderState.isReordering) {
                            CollapsedCardRow(cardConfig = cardConfig, dragHandleModifier = dragHandleModifier)
                        } else {
                            // Long-pressing the icon is the only way to enter reorder mode — scoped
                            // to the icon specifically (via headerModifier) so it never competes with
                            // the header's own long-press-to-flip-to-settings gesture, which was
                            // re-scoped off the icon onto just the title/subtitle area for exactly
                            // this reason (see AeonCardHeader in AeonExpressiveCard.kt).
                            val entryModifier = Modifier.longPressToTrigger(key = cardConfig.cardId) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                reorderState.isReordering = true
                            }
                            CardItem(
                                modifier = entryModifier,
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
                        }
                    }
                }
            }
        }

        LazyColumnScrollbar(
            state = state,
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
 * Compact header-only row shown for every card while the list `isReordering` — icon, title, and a
 * trailing drag-handle glyph, no card-type-specific preview content. The glyph is the real (and
 * only) drag target here, matching the full card's own single-handle design.
 */
@Composable
private fun CollapsedCardRow(cardConfig: CardConfig, dragHandleModifier: Modifier) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        color = AppColors.cardSurface,
        border = BorderStroke(0.5.dp, AppColors.contentPrimary.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeonTheme.dimensions.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
        ) {
            Surface(
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg),
                shape = CircleShape,
                color = AppColors.contentPrimary.copy(alpha = 0.15f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = cardConfig.cardType.toIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                        tint = AppColors.contentPrimary,
                    )
                }
            }
            Text(
                text = cardConfig.title.ifBlank { cardConfig.cardType.displayName },
                style = AppTextStyles.cardSectionTitle,
                color = AppColors.contentPrimary,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(AeonTheme.dimensions.iconSizeLg)
                    .then(dragHandleModifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = AppColors.contentSecondary,
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                )
            }
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
