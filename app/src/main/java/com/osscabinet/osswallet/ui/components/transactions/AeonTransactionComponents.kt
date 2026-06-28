package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.ConfirmedTransactionUiModel
import com.osscabinet.ossWallet.domain.model.PlannedPaymentUiModel
import com.osscabinet.ossWallet.domain.model.settings.FastScrollSide
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListUiState
import com.osscabinet.ossWallet.ui.theme.AppColors
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import my.nanihadesuka.compose.ScrollbarLayoutSide

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AeonTransactionList(
    listState: TransactionListUiState,
    onTransactionEvent: (TransactionItemEvent) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    useNoteAsHeader: Boolean = false,
    showHeaderOpeningBalance: Boolean = false,
    showHeaderClosingBalance: Boolean = true,
    showItemOpeningBalance: Boolean = false,
    showItemClosingBalance: Boolean = true,
    showOnlyActiveCurrencies: Boolean = false,
    showCurrencyBreakdown: Boolean = true,
    allTxnBreakdown: Boolean = true,
    showAccount: Boolean = true,
    showCategory: Boolean = true,
    showNote: Boolean = true,
    showTags: Boolean = true,
    showTimestamp: Boolean = true,
    showIcon: Boolean = true,
    baseCurrency: String,
    revealedCardIds: List<String> = emptyList(),
    highlightedId: String? = null,
    scrollToId: String? = null,
    onScrollComplete: () -> Unit = {},
    hasStickyHeader: Boolean = true,
    fastScrollSide: FastScrollSide = FastScrollSide.RIGHT,
) {
    val scrollIndex = remember(listState, scrollToId) {
        if (scrollToId == null) return@remember -1
        var currentIndex = 0
        for (group in listState.groups) {
            // Header item
            currentIndex++
            for (record in group.items) {
                if (record.record.id == scrollToId) {
                    return@remember currentIndex
                }
                currentIndex++
            }
        }
        -1
    }

    LaunchedEffect(scrollIndex) {
        if (scrollIndex != -1) {
            lazyListState.animateScrollToItem(scrollIndex)
            onScrollComplete()
        }
    }

    val indexToTitle = remember(listState.groups) {
        buildMap<Int, String> {
            var i = 0
            listState.groups.forEach { group ->
                val label = group.headerData.subtitle
                put(i++, label)
                repeat(group.items.size) { put(i++, label) }
            }
            if (i > 0) put(i, this[i - 1] ?: "")
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val trackPad = maxHeight * 0.10f

        // Full-height list — unaffected by scrollbar inset
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            listState.groups.forEach { group ->
                val headerContent: @Composable () -> Unit = {
                    TransactionHeader(
                        title = group.headerData.title,
                        subtitle = group.headerData.subtitle,
                        incomeByCurrency = group.headerData.incomeByCurrency,
                        expenseByCurrency = group.headerData.expenseByCurrency,
                        openingBalancesByCurrency = group.headerData.openingBalancesByCurrency,
                        closingBalancesByCurrency = group.headerData.closingBalancesByCurrency,
                        allCurrencies = group.headerData.allCurrencies,
                        incomeBase = group.headerData.incomeBase,
                        expenseBase = group.headerData.expenseBase,
                        netBase = group.headerData.netBase,
                        totalOpeningBalanceBase = group.headerData.totalOpeningBalanceBase,
                        totalClosingBalanceBase = group.headerData.totalClosingBalanceBase,
                        baseCurrency = baseCurrency,
                        showOpeningBalance = if (showCurrencyBreakdown && !allTxnBreakdown) false else showHeaderOpeningBalance,
                        showClosingBalance = if (showCurrencyBreakdown && !allTxnBreakdown) false else showHeaderClosingBalance,
                        showOnlyActiveCurrencies = !allTxnBreakdown || showOnlyActiveCurrencies,
                        showCurrencyBreakdown = showCurrencyBreakdown
                    )
                }

                val headerKey = "header_${group.key}"
                if (hasStickyHeader) {
                    stickyHeader(key = headerKey, contentType = "header") { headerContent() }
                } else {
                    item(key = headerKey, contentType = "header") { headerContent() }
                }

                itemsIndexed(
                    group.items,
                    key = { _, item -> item.record.id },
                    contentType = { _, _ -> "transaction" }
                ) { index, item ->
                    val record = item.record
                    val icon = getIconByName(record.categoryIcon)
                    val iconBg = Color(record.categoryColor)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val isHighlighted = record.id == highlightedId
                        val blinkAlpha = remember { androidx.compose.animation.core.Animatable(0f) }

                        LaunchedEffect(isHighlighted) {
                            if (isHighlighted) {
                                blinkAlpha.animateTo(0.4f, tween(200))
                                kotlinx.coroutines.delay(600)
                                blinkAlpha.animateTo(0f, tween(200))
                            } else {
                                blinkAlpha.snapTo(0f)
                            }
                        }

                        SwipeableTransactionItem(
                            record = record,
                            isRevealed = revealedCardIds.contains(record.id),
                            onEvent = onTransactionEvent,
                            modifier = Modifier.background(AppColors.primary.copy(alpha = blinkAlpha.value))
                        ) {
                            AeonTransactionItem(
                                category = record.categoryName,
                                account = record.accountName,
                                amount = item.formattedAmount,
                                convertedAmount = item.formattedConvertedAmount,
                                balanceBefore = item.formattedBalanceBefore,
                                balanceAfter = item.formattedBalanceAfter,
                                type = if (record.amount.signum() >= 0) CashFlowType.INCOME else CashFlowType.EXPENSE,
                                timestamp = item.formattedDate,
                                note = record.note,
                                tags = record.tags,
                                icon = icon,
                                iconBg = iconBg,
                                useNoteAsHeader = useNoteAsHeader,
                                showOpeningBalance = showItemOpeningBalance && (record is ConfirmedTransactionUiModel),
                                showClosingBalance = showItemClosingBalance && (record is ConfirmedTransactionUiModel),
                                status = (record as? ConfirmedTransactionUiModel)?.status
                                    ?: com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED,
                                showAccount = showAccount,
                                showCategory = showCategory,
                                showNote = showNote,
                                showTags = showTags,
                                showTimestamp = showTimestamp,
                                showIcon = showIcon,
                                showDividers = index < group.items.size - 1,
                                isPlanned = record is PlannedPaymentUiModel
                            ) { onTransactionEvent(TransactionItemEvent.Click(record.id)) }
                        }
                    }
                }
            }
            item(key = "footer_divider", contentType = "divider") {
                // Min height keeps averageItemSize stable for the scrollbar thumb position calc.
                // Without it, this near-zero-height item causes the thumb to snap to the bottom.
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = AppColors.divider.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Scrollbar-only overlay, inset to 80% height. Thumb size is fixed so the
        // mismatched viewport doesn't affect positioning. The empty Box content has
        // no pointer handling so all list touches pass through to the LazyColumn above.
        LazyColumnScrollbar(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = trackPad),
            settings = ScrollbarSettings.Default.copy(
                thumbSelectedColor = AppColors.primary,
                thumbUnselectedColor = AppColors.primary.copy(alpha = 0.5f),
                thumbThickness = 8.dp,
                thumbMinLength = 0.1f,
                thumbMaxLength = 0.1f,
                thumbShape = RoundedCornerShape(12.dp),
                side = if (fastScrollSide == FastScrollSide.LEFT) ScrollbarLayoutSide.Start
                       else ScrollbarLayoutSide.End
            ),
            indicatorContent = { index: Int, isThumbSelected: Boolean ->
                if (isThumbSelected) {
                    val haptic = LocalHapticFeedback.current
                    val title = indexToTitle[index] ?: ""

                    LaunchedEffect(title) {
                        if (title.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }

                    if (title.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(
                                    start = if (fastScrollSide == FastScrollSide.LEFT) 36.dp else 16.dp,
                                    end = if (fastScrollSide == FastScrollSide.RIGHT) 36.dp else 16.dp
                                )
                                .background(AppColors.primary, RoundedCornerShape(24.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
