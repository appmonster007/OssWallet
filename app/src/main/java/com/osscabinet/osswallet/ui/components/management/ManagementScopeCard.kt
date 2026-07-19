package com.osscabinet.ossWallet.ui.components.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.formatCurrency
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ManagementScopeCard(
    filter: TransactionFilter,
    allAccounts: List<Account>,
    allCategories: List<Category>,
    modifier: Modifier = Modifier
) {
    val primaryColor = AppColors.primary
    val secondaryColor = AppColors.secondary
    val positiveColor = AppColors.positive
    val negativeColor = AppColors.negative
    val accentColor = AppColors.accent
    val contentSecondaryColor = AppColors.contentSecondary

    val sections = remember(
        filter,
        allAccounts,
        allCategories,
        primaryColor,
        secondaryColor,
        positiveColor,
        negativeColor,
        accentColor,
        contentSecondaryColor
    ) {
        buildList {
            // Timeline/Period
            if (filter.timelineRange != TimelineRange.AllTime) {
                add(
                    ScopeGroup(
                        label = "PERIOD",
                        icon = Icons.Rounded.CalendarToday,
                        allText = "All Time",
                        isEmpty = false,
                        items = listOf(
                            ScopeItem(
                                text = filter.timelineRange.toDisplayString(),
                                color = primaryColor
                            )
                        )
                    )
                )
            }

            // Accounts
            add(
                ScopeGroup(
                    label = "ACCOUNTS",
                    icon = Icons.Rounded.AccountBalance,
                    allText = "All Accounts",
                    isEmpty = filter.accountIds.isEmpty() || (allAccounts.isNotEmpty() && filter.accountIds.size == allAccounts.size),
                    items = allAccounts.filter { it.id in filter.accountIds }.map { account ->
                        ScopeItem(
                            text = account.name,
                            color = try {
                                Color(account.colorHex.toColorInt())
                            } catch (_: Exception) {
                                primaryColor
                            },
                            icon = AppIconMap[account.icon]
                        )
                    }
                )
            )

            // Categories
            add(
                ScopeGroup(
                    label = "CATEGORIES",
                    icon = Icons.Rounded.Category,
                    allText = "All Categories",
                    isEmpty = filter.categoryIds.isEmpty() || (allCategories.isNotEmpty() && filter.categoryIds.size == allCategories.size),
                    items = allCategories.filter { it.id in filter.categoryIds }.map { category ->
                        ScopeItem(
                            text = category.name,
                            color = Color(category.colorHex),
                            icon = AppIconMap[category.iconName]
                        )
                    }
                )
            )

            // Payee
            if (!filter.queryPayee.isNullOrBlank()) {
                add(
                    ScopeGroup(
                        label = "PAYEE",
                        icon = Icons.Rounded.Person,
                        allText = "",
                        isEmpty = false,
                        items = listOf(
                            ScopeItem(
                                text = filter.queryPayee + if (filter.isPayeeRegex) " (Regex)" else "",
                                color = primaryColor
                            )
                        )
                    )
                )
            }

            // Note
            if (!filter.queryNote.isNullOrBlank()) {
                add(
                    ScopeGroup(
                        label = "NOTE",
                        icon = Icons.AutoMirrored.Rounded.Notes,
                        allText = "",
                        isEmpty = false,
                        items = listOf(
                            ScopeItem(
                                text = filter.queryNote + if (filter.isNoteRegex) " (Regex)" else "",
                                color = primaryColor
                            )
                        )
                    )
                )
            }

            // Amount Range
            if (filter.minAmount != null || filter.maxAmount != null) {
                val minText = filter.minAmount?.let { formatCurrency(it) }
                val maxText = filter.maxAmount?.let { formatCurrency(it) }
                val rangeText = when {
                    minText != null && maxText != null -> "$minText - $maxText"
                    minText != null -> "Min $minText"
                    maxText != null -> "Max $maxText"
                    else -> ""
                }
                add(
                    ScopeGroup(
                        label = "AMOUNT",
                        icon = Icons.Rounded.AttachMoney,
                        allText = "",
                        isEmpty = false,
                        items = listOf(ScopeItem(text = rangeText, color = primaryColor))
                    )
                )
            }

            // Tags
            add(
                ScopeGroup(
                    label = "TAGS",
                    icon = Icons.AutoMirrored.Rounded.Label,
                    allText = "No Tags",
                    isEmpty = filter.tags.isEmpty(),
                    items = filter.tags.map { ScopeItem(text = it, color = secondaryColor) }
                )
            )

            // Transaction Types
            if (filter.transactionTypes.isNotEmpty()) {
                add(
                    ScopeGroup(
                        label = "TYPES",
                        icon = Icons.Rounded.SyncAlt,
                        allText = "All Types",
                        isEmpty = false,
                        items = filter.transactionTypes.map { type ->
                            ScopeItem(
                                text = type.name,
                                color = when (type) {
                                    TransactionType.EXPENSE -> negativeColor
                                    TransactionType.INCOME -> positiveColor
                                    TransactionType.TRANSFER_INFLOW, TransactionType.TRANSFER_OUTFLOW -> accentColor
                                }
                            )
                        }
                    )
                )
            }

            // Payment Methods
            if (filter.paymentMethods.isNotEmpty()) {
                add(
                    ScopeGroup(
                        label = "METHODS",
                        icon = Icons.Rounded.CreditCard,
                        allText = "All Methods",
                        isEmpty = false,
                        items = filter.paymentMethods.map { method ->
                            ScopeItem(
                                text = method.displayName,
                                color = try {
                                    Color(
                                        method.colorHex?.toColorInt() ?: primaryColor.value.toLong()
                                            .toInt()
                                    )
                                } catch (_: Exception) {
                                    primaryColor
                                },
                                icon = AppIconMap[method.iconName]
                            )
                        }
                    )
                )
            }

            // Currencies
            if (filter.currencies.isNotEmpty()) {
                add(
                    ScopeGroup(
                        label = "CURRENCIES",
                        icon = Icons.Rounded.Payments,
                        allText = "All Currencies",
                        isEmpty = false,
                        items = filter.currencies.map { ScopeItem(text = it, color = primaryColor) }
                    )
                )
            }

            // Statuses
            if (filter.statuses.isNotEmpty()) {
                add(
                    ScopeGroup(
                        label = "STATUSES",
                        icon = Icons.AutoMirrored.Rounded.PlaylistAddCheck,
                        allText = "All Statuses",
                        isEmpty = false,
                        items = filter.statuses.map { status ->
                            ScopeItem(
                                text = status.name,
                                color = when (status) {
                                    TransactionStatus.CLEARED -> positiveColor
                                    TransactionStatus.UNCLEARED -> contentSecondaryColor
                                    TransactionStatus.RECONCILE -> accentColor
                                }
                            )
                        }
                    )
                )
            }

            // Warranty
            val hasWarranty = filter.hasWarranty
            if (hasWarranty != null) {
                add(
                    ScopeGroup(
                        label = "WARRANTY",
                        icon = Icons.Rounded.VerifiedUser,
                        allText = "",
                        isEmpty = false,
                        items = listOf(
                            ScopeItem(
                                text = if (hasWarranty) "Has Warranty" else "No Warranty",
                                color = if (hasWarranty) positiveColor else contentSecondaryColor
                            )
                        )
                    )
                )
            }

            // Warranty Expiration
            if (filter.warrantyExpirationStart != null || filter.warrantyExpirationEnd != null) {
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val start = filter.warrantyExpirationStart?.let { dateFormat.format(Date(it)) }
                val end = filter.warrantyExpirationEnd?.let { dateFormat.format(Date(it)) }
                val range = when {
                    start != null && end != null -> "$start - $end"
                    start != null -> "From $start"
                    end != null -> "Until $end"
                    else -> ""
                }
                add(
                    ScopeGroup(
                        label = "WARRANTY EXPIRES",
                        icon = Icons.Rounded.Event,
                        allText = "",
                        isEmpty = false,
                        items = listOf(ScopeItem(text = range, color = primaryColor))
                    )
                )
            }

            // Linked Transactions
            if (filter.linkedTransactionIds.isNotEmpty()) {
                add(
                    ScopeGroup(
                        label = "LINKED ITEMS",
                        icon = Icons.Rounded.Link,
                        allText = "",
                        isEmpty = false,
                        items = listOf(
                            ScopeItem(
                                text = "${filter.linkedTransactionIds.size} transactions",
                                color = secondaryColor
                            )
                        )
                    )
                )
            }

            // Future Transactions
            if (!filter.timelineRange.excludeFuture) {
                add(
                    ScopeGroup(
                        label = "FUTURE",
                        icon = Icons.AutoMirrored.Rounded.Forward,
                        allText = "",
                        isEmpty = false,
                        items = listOf(ScopeItem(text = "Including Future", color = primaryColor))
                    )
                )
            }
        }
    }

    AeonExpressiveCard(
        title = "SCOPE & CONTEXT",
        icon = Icons.Rounded.FilterAlt,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg),
            modifier = modifier.fillMaxWidth()
        ) {
            sections.forEach { group ->
                ScopeSection(
                    label = group.label,
                    icon = group.icon,
                    allText = group.allText,
                    isEmpty = group.isEmpty,
                    items = group.items.map { item ->
                        { ScopeChip(text = item.text, color = item.color, icon = item.icon) }
                    }
                )
            }
        }
    }
}

private data class ScopeGroup(
    val label: String,
    val icon: ImageVector,
    val allText: String,
    val isEmpty: Boolean,
    val items: List<ScopeItem>
)

private data class ScopeItem(
    val text: String,
    val color: Color,
    val icon: ImageVector? = null
)

@Preview(showBackground = true)
@Composable
fun ManagementScopeCardPreview() {
    OssWalletTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ManagementScopeCard(
                filter = TransactionFilter(
                    timelineRange = TimelineRange.ThisPeriod(
                        ThisPeriodType.MONTH,
                        excludeFuture = false
                    ),
                    accountIds = setOf("1"),
                    categoryIds = setOf(101L, 102L),
                    tags = setOf("Vacation", "Work"),
                    transactionTypes = setOf(TransactionType.EXPENSE),
                    queryPayee = "Amazon",
                    minAmount = BigDecimal("50"),
                    paymentMethods = setOf(PaymentMethod.CREDIT_CARD),
                    hasWarranty = true,
                    linkedTransactionIds = setOf(1L, 2L, 3L)
                ),
                allAccounts = listOf(
                    Account(
                        id = "1",
                        name = "Main Account",
                        colorHex = "#FF0000",
                        currencyCode = "USD",
                        structureType = AccountStructureType.SAVINGS,
                        icon = "AccountBalance"
                    ),
                    Account(
                        id = "2",
                        name = "Savings",
                        colorHex = "#00FF00",
                        currencyCode = "USD",
                        structureType = AccountStructureType.SAVINGS,
                        icon = "AccountBalance"
                    )
                ),
                allCategories = listOf(
                    Category(
                        id = 101L,
                        name = "Food",
                        colorHex = 0xFFFF00FF.toInt(),
                        iconName = "Fastfood",
                        isDefault = false,
                        parentId = null
                    ),
                    Category(
                        id = 102L,
                        name = "Travel",
                        colorHex = 0xFF00FFFF.toInt(),
                        iconName = "Flight",
                        isDefault = false,
                        parentId = null
                    ),
                    Category(
                        id = 103L,
                        name = "Rent",
                        colorHex = 0xFF0000FF.toInt(),
                        iconName = "Home",
                        isDefault = false,
                        parentId = null
                    )
                )
            )
        }
    }
}
