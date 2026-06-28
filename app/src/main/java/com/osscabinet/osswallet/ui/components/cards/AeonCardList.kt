package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.osscabinet.ossWallet.ui.components.rememberShimmerBrush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.card.CardComputeState
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import com.osscabinet.ossWallet.domain.card.CardFilterRules
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardUpdateHandler
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.CardConfig
import com.osscabinet.ossWallet.ui.reorderable.ReorderableItem
import com.osscabinet.ossWallet.ui.reorderable.draggableHandle
import com.osscabinet.ossWallet.ui.components.toIcon
import com.osscabinet.ossWallet.ui.reorderable.rememberReorderableLazyListState
import com.osscabinet.ossWallet.ui.reorderable.reorderable
import com.osscabinet.ossWallet.ui.reorderable.reorderableSemantics
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Composable
fun AeonCardList(
    cards: List<CardConfig>,
    cardInput: CardInput,
    cardUpdateHandler: CardUpdateHandler,
    onReorder: (List<CardConfig>) -> Unit,
    onDelete: (String) -> Unit,
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
    onScrollComplete: () -> Unit = {}
) {
    val sortedCards = remember(cards) { cards.sortedBy { it.order } }
    val sortedCardsRef by rememberUpdatedState(sortedCards)
    val onReorderRef by rememberUpdatedState(onReorder)
    val cardDataMapState = rememberUpdatedState(cardDataMap)
    val headerOffset = if (header != null) 1 else 0

    val reorderState = rememberReorderableLazyListState(
        lazyListState = state,
        swapTravelRatio = 0.45f,
        onMove = { fromLazy, toLazy ->
            val from = fromLazy - headerOffset
            val to = toLazy - headerOffset
            val current = sortedCardsRef
            if (from in current.indices && to in current.indices) {
                val newList = current.toMutableList().apply { add(to, removeAt(from)) }
                onReorderRef(newList)
            }
        }
    )

    LazyColumn(
        state = reorderState.lazyListState,
        modifier = modifier.reorderable(reorderState),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
    ) {
        if (header != null) {
            item(key = "__aeon_card_list_header") { header() }
        }

        items(sortedCards, key = { it.cardId }, contentType = { it.cardType }) { cardConfig ->
            val draggingActive = reorderState.draggedKey != null
            val isDragging = reorderState.isDragging(cardConfig.cardId)
            val alpha by animateFloatAsState(
                targetValue = if (draggingActive && !isDragging) 0.75f else 1.0f,
                label = "card_drag_alpha"
            )

            ReorderableItem(
                state = reorderState,
                key = cardConfig.cardId,
                modifier = Modifier
                    .reorderableSemantics(state = reorderState, key = cardConfig.cardId)
                    .graphicsLayer { this.alpha = alpha },
            ) { _ ->
                val dragHandleModifier = Modifier.draggableHandle(
                    state = reorderState,
                    key = cardConfig.cardId,
                )
                val cardState by remember(cardConfig.cardId) {
                    derivedStateOf { cardDataMapState.value[cardConfig.cardId] }
                }

                CardItem(
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
                    dragHandleModifier = dragHandleModifier,
                    animateCards = animateCards,
                    scrollToId = scrollToId,
                    highlightedId = highlightedId,
                    onScrollComplete = onScrollComplete,
                )
            }
        }
    }
}

@Composable
private fun CardItem(
    cardConfig: CardConfig,
    cardState: CardComputeState?,
    cardInput: CardInput,
    cardUpdateHandler: CardUpdateHandler,
    onDelete: (String) -> Unit,
    onRecordsClick: (TransactionFilter) -> Unit,
    callbacks: AeonInteractionCallbacks,
    baseFilter: TransactionFilter,
    screenRules: CardFilterRules,
    filterContext: FilterSelectionContext,
    disableFilterSheet: Boolean,
    dragHandleModifier: Modifier,
    animateCards: Boolean,
    scrollToId: String?,
    highlightedId: String?,
    onScrollComplete: () -> Unit,
) {
    when (val cs = cardState) {
        is CardComputeState.Loading, null -> AeonExpressiveCard(
            title = cardConfig.cardType.displayName,
            icon = cardConfig.cardType.toIcon(),
            headerModifier = dragHandleModifier,
        ) {
            CardContentShimmer()
        }

        is CardComputeState.Error -> AeonExpressiveCard(
            title = cardConfig.cardType.displayName,
            icon = cardConfig.cardType.toIcon(),
            headerModifier = dragHandleModifier,
            isWarning = true,
        ) {
            CardErrorContent(cs.message)
        }

        is CardComputeState.Success -> {
            val card = rememberCardWithCallbacks(
                config = cardConfig,
                renderData = cs.data,
                input = cardInput,
                updateHandler = cardUpdateHandler,
                onDelete = onDelete,
                onRecordsClick = onRecordsClick,
                callbacks = callbacks,
                filterSheetEnabled = !disableFilterSheet,
            )
            // null only for a config/data type mismatch — programming error
            if (card == null) {
                AeonExpressiveCard(
                    title = cardConfig.cardType.displayName,
                    icon = cardConfig.cardType.toIcon(),
                    headerModifier = dragHandleModifier,
                    isWarning = true,
                ) {
                    CardErrorContent(null)
                }
            } else {
                val effectiveCard = remember(card, screenRules, baseFilter, disableFilterSheet) {
                    card.copyWithRules(
                        rules = card.filterRules.layeredWith(screenRules),
                        baseFilter = baseFilter,
                        filterSheetEnabled = !disableFilterSheet
                    )
                }
                UnifiedCardRenderer(
                    cardWithCallbacks = effectiveCard,
                    modifier = Modifier.fillMaxWidth(),
                    headerModifier = dragHandleModifier,
                    filterContext = filterContext,
                    inheritedFilter = cardInput.filter,
                    screenOwnsTimeline = cardInput.screenOwnsTimeline,
                    animate = animateCards,
                    scrollToId = scrollToId,
                    highlightedId = highlightedId,
                    onScrollComplete = onScrollComplete
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

@Composable
fun CardShimmerPlaceholder() {
    val brush = rememberShimmerBrush(horizontal = true, label = "card_shimmer")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(brush, RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius))
    )
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
