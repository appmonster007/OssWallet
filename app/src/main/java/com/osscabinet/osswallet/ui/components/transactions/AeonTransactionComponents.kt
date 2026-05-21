package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.osscabinet.ossWallet.ui.theme.*
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionListState
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AeonTransactionList(
    listState: TransactionListState,
    onTransactionEvent: (TransactionItemEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    useNoteAsHeader: Boolean = false,
    showOpeningBalance: Boolean = false,
    showClosingBalance: Boolean = true,
    showOnlyActiveCurrencies: Boolean = false,
    showCurrencyBreakdown: Boolean = true,
    baseCurrency: String,
    revealedCardIds: List<Long> = emptyList()
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        listState.groups.forEach { group ->
            stickyHeader {
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
                    showOpeningBalance = showOpeningBalance,
                    showClosingBalance = showClosingBalance,
                    showOnlyActiveCurrencies = showOnlyActiveCurrencies,
                    showCurrencyBreakdown = showCurrencyBreakdown
                )
            }

            items(group.transactions, key = { it.item.transaction.id }) { enriched ->
                val tx = enriched.item.transaction
                val icon = getIconByName(enriched.categoryIcon)
                val iconBg = Color(enriched.categoryColor)

                SwipeableTransactionItem(
                    transaction = tx,
                    isRevealed = revealedCardIds.contains(tx.id),
                    onEvent = onTransactionEvent
                ) {
                    AeonTransactionItem(
                        category = enriched.categoryName,
                        account = enriched.accountName,
                        amount = formatCurrencyRight(
                            tx.amount,
                            tx.currency
                        ),
                        convertedAmount = if ((tx.currency != baseCurrency) && (enriched.amountInBase != null)) {
                            formatCurrencyRight(enriched.amountInBase, baseCurrency)
                        } else null,
                        balanceBefore = formatCurrencyRight(
                            enriched.item.balanceBefore,
                            enriched.accountCurrency
                        ),
                        balanceAfter = formatCurrencyRight(
                            enriched.item.balanceAfter,
                            enriched.accountCurrency
                        ),
                        type = if (tx.amount.signum() >= 0) CashFlowType.INCOME else CashFlowType.EXPENSE,
                        timestamp = com.osscabinet.ossWallet.ui.util.TimeUtils.formatDateTime(tx.timestamp),
                        note = tx.note,
                        tags = tx.tags,
                        icon = icon,
                        iconBg = iconBg,
                        useNoteAsHeader = useNoteAsHeader,
                        showOpeningBalance = showOpeningBalance,
                        showClosingBalance = showClosingBalance,
                        status = tx.status
                    ) { onTransactionEvent(TransactionItemEvent.Click(tx.id)) }
                }
            }
        }
        item {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = AppColors.divider.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AeonTransactionItem(
    category: String,
    account: String,
    amount: String,
    modifier: Modifier = Modifier,
    convertedAmount: String? = null,
    balanceBefore: String,
    balanceAfter: String,
    type: CashFlowType,
    timestamp: String,
    note: String? = null,
    tags: Set<String> = emptySet(),
    icon: ImageVector? = null,
    iconBg: Color? = null,
    useNoteAsHeader: Boolean = false,
    showOpeningBalance: Boolean = false,
    showClosingBalance: Boolean = true,
    showDividers: Boolean = true,
    status: com.osscabinet.ossWallet.domain.model.TransactionStatus = com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED,
    containerColor: Color = AppColors.panelSurface,
    onClick: () -> Unit
) {
    val isNoteHeader = useNoteAsHeader && !note.isNullOrBlank()
    val headerText = if (isNoteHeader) note else category
    val bottomText = if (isNoteHeader) category else note

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RectangleShape
    ) {
        Column {
            if (showDividers) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.5.dp,
                    color = AppColors.divider.copy(alpha = 0.5f)
                )
            }

            Column(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.listPadding, vertical = AeonTheme.dimensions.spacingSm)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = (iconBg ?: AppColors.primaryContainer).copy(
                                alpha = 0.2f
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon ?: Icons.Rounded.Category,
                                    contentDescription = null,
                                    tint = iconBg ?: AppColors.primary,
                                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                                )
                            }
                        }

                        // Status indicator
                        if (status != com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED) {
                            Surface(
                                modifier = Modifier
                                    .size(AeonTheme.dimensions.iconSizeXs)
                                    .align(Alignment.BottomStart),
                                shape = CircleShape,
                                color = when (status) {
                                    com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED -> PositiveAmountColor

                                    com.osscabinet.ossWallet.domain.model.TransactionStatus.RECONCILE -> AccentBlue

                                    com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED -> AppColors.transparent
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (status) {
                                            com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED -> Icons.Rounded.Check
                                            else -> Icons.Rounded.Sync
                                        },
                                        contentDescription = null,
                                        tint = ColorOnDark,
                                        modifier = Modifier.size(6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingSm))

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                headerText,
                                style = AppTextStyles.transactionTitle.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                maxLines = if (isNoteHeader) Int.MAX_VALUE else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                account,
                                style = AppTextStyles.transactionSwipeAction.copy(
                                    fontSize = 10.sp
                                ),
                                color = AppColors.border,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!bottomText.isNullOrBlank()) {
                                Text(
                                    bottomText,
                                    style = AppTextStyles.caption.copy(
                                        fontSize = 11.sp
                                    ),
                                    color = AppColors.contentSecondary.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 2.dp),
                                    maxLines = if (!isNoteHeader) Int.MAX_VALUE else 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = amount,
                                style = AppTextStyles.listItemTitle.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = if (type == CashFlowType.INCOME) PositiveAmountColor else AppColors.error
                                )
                            )

                            if (convertedAmount != null) {
                                Text(
                                    text = convertedAmount,
                                    style = AppTextStyles.transactionSwipeAction.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    color = (if (type == CashFlowType.INCOME) PositiveAmountColor else AppColors.error).copy(alpha = 0.8f)
                                )
                            }

                            if (showOpeningBalance || showClosingBalance) {
                                val balanceText = when {
                                    showOpeningBalance && showClosingBalance -> "$balanceBefore → $balanceAfter"
                                    showOpeningBalance -> balanceBefore
                                    else -> balanceAfter
                                }
                                Text(
                                    balanceText,
                                    style = AppTextStyles.transactionAmountMeta,
                                    color = AppColors.contentSecondary.copy(alpha = 0.8f)
                                )
                            }

                            Text(
                                timestamp,
                                style = AppTextStyles.transactionAmountMeta,
                                color = AppColors.contentSecondary.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                if (tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = AeonTheme.dimensions.spacingSm),
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs),
                        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(AeonTheme.dimensions.spacingXs),
                                color = AppColors.secondaryContainer.copy(alpha = 0.3f),
                            ) {
                                Text(
                                    text = tag,
                                    style = AppTextStyles.transactionSwipeAction.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.onSecondaryContainer
                                    ),
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingXs, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (showDividers) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.5.dp,
                    color = AppColors.divider.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonTransactionItemPreview() {
    OssWalletTheme {
        AeonTransactionItem(
            category = "Food & Drinks",
            account = "Main Wallet",
            amount = "-$15.50",
            balanceBefore = "$100.00",
            balanceAfter = "$84.50",
            type = CashFlowType.EXPENSE,
            timestamp = "10:30 AM",
            note = "Lunch at Subway",
            tags = setOf("Lunch", "Work"),
            icon = Icons.Rounded.Category,
            iconBg = Color(0xFF4CAF50),
            onClick = {}
        )
    }
}
