package com.osscabinet.ossWallet.ui.components.management

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
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

    // Categories — always collapsed to the top-level (root) category chip; tapping a chip
    // drills one level down into its children, however deep the hierarchy goes. Expansion is
    // hoisted here (rather than kept per-node) so expanded children flatten into the SAME
    // FlowRow as their siblings, instead of being boxed into their own sub-row.
    val childrenByParentId = remember(allCategories) {
        allCategories.filter { it.parentId != null }.groupBy { it.parentId!! }
    }
    val knownCategoryIds = remember(allCategories) {
        allCategories.mapTo(mutableSetOf()) { it.id }
    }
    var expandedCategoryIds by remember { mutableStateOf(emptySet<Long>()) }
    val categoryRoots = remember(allCategories, filter.categoryIds, childrenByParentId, knownCategoryIds) {
        allCategories.filter {
            (it.parentId == null || it.parentId !in knownCategoryIds) &&
                hasSelectedDescendant(it, childrenByParentId, filter.categoryIds)
        }
    }
    val categoryItems = remember(categoryRoots, childrenByParentId, filter.categoryIds, expandedCategoryIds) {
        flattenCategoryItems(
            categories = categoryRoots,
            childrenByParentId = childrenByParentId,
            selectedIds = filter.categoryIds,
            expandedIds = expandedCategoryIds,
            onToggle = { id, expand ->
                expandedCategoryIds = if (expand) expandedCategoryIds + id else expandedCategoryIds - id
            }
        )
    }
    val categoriesIsEmpty = filter.categoryIds.isEmpty() ||
        (allCategories.isNotEmpty() && filter.categoryIds.size == allCategories.size)

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
                if (group.label == "ACCOUNTS") {
                    ScopeSection(
                        label = "CATEGORIES",
                        icon = Icons.Rounded.Category,
                        allText = "All Categories",
                        isEmpty = categoriesIsEmpty,
                        items = categoryItems
                    )
                }
            }
        }
    }
}

private fun hasSelectedDescendant(
    category: Category,
    childrenByParentId: Map<Long, List<Category>>,
    selectedIds: Set<Long>
): Boolean =
    category.id in selectedIds ||
        childrenByParentId[category.id].orEmpty().any { hasSelectedDescendant(it, childrenByParentId, selectedIds) }

/** Sum of selected ids across the whole subtree rooted at [category] — all nested levels. */
private fun selectedDescendantCount(
    category: Category,
    childrenByParentId: Map<Long, List<Category>>,
    selectedIds: Set<Long>
): Int {
    val self = if (category.id in selectedIds) 1 else 0
    return self + childrenByParentId[category.id].orEmpty()
        .sumOf { selectedDescendantCount(it, childrenByParentId, selectedIds) }
}

/**
 * Flattens [categories] (and, for any that are expanded, their relevant children —
 * recursively) into a single list of chip composables, so an expanded parent's children land
 * as ordinary siblings in the caller's one [FlowRow] rather than boxed into their own
 * sub-layout. That's what lets e.g. "Transport" wrap onto the same line as "Food & Drink"'s
 * last expanded child instead of always starting a new row.
 */
private fun flattenCategoryItems(
    categories: List<Category>,
    childrenByParentId: Map<Long, List<Category>>,
    selectedIds: Set<Long>,
    expandedIds: Set<Long>,
    onToggle: (id: Long, expand: Boolean) -> Unit
): List<@Composable () -> Unit> {
    val items = mutableListOf<@Composable () -> Unit>()
    for (category in categories) {
        val relevantChildren = childrenByParentId[category.id].orEmpty()
            .filter { hasSelectedDescendant(it, childrenByParentId, selectedIds) }

        if (relevantChildren.isEmpty()) {
            items += {
                ScopeChip(
                    text = category.name,
                    color = Color(category.colorHex),
                    icon = AppIconMap[category.iconName]
                )
            }
        } else if (category.id in expandedIds) {
            items += {
                CategoryBreadcrumbChip(category = category, onClick = { onToggle(category.id, false) })
            }
            items += flattenCategoryItems(
                categories = relevantChildren,
                childrenByParentId = childrenByParentId,
                selectedIds = selectedIds,
                expandedIds = expandedIds,
                onToggle = onToggle
            )
        } else {
            val count = selectedDescendantCount(category, childrenByParentId, selectedIds)
            items += {
                CategoryChip(category = category, count = count, onClick = { onToggle(category.id, true) })
            }
        }
    }
    return items
}

/** Collapsed category chip; tap drills one level down into its children. */
@Composable
private fun CategoryChip(category: Category, count: Int, onClick: () -> Unit) {
    val categoryColor = Color(category.colorHex)
    Surface(
        onClick = onClick,
        color = AppColors.cardSurfaceProminent.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(categoryColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val icon = AppIconMap[category.iconName]
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(categoryColor, CircleShape)
                    )
                }
            }
            Text(
                text = category.name,
                style = AppTextStyles.caption.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.2.sp
                ),
                color = AppColors.contentPrimary.copy(alpha = 0.8f)
            )
            if (count > 1) {
                Surface(
                    color = categoryColor.copy(alpha = 0.18f),
                    shape = CircleShape
                ) {
                    Text(
                        text = count.toString(),
                        style = AppTextStyles.caption.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        ),
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/** Expanded category's breadcrumb chip; tap collapses it back to [CategoryChip]. */
@Composable
private fun CategoryBreadcrumbChip(category: Category, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = AppColors.cardSurfaceProminent.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "‹",
                style = AppTextStyles.caption.copy(fontWeight = FontWeight.Black, fontSize = 12.sp),
                color = AppColors.contentSecondary.copy(alpha = 0.7f)
            )
            Text(
                text = category.name,
                style = AppTextStyles.caption.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.2.sp
                ),
                color = AppColors.contentSecondary.copy(alpha = 0.7f)
            )
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
                    categoryIds = setOf(201L, 202L, 203L, 204L, 301L, 102L),
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
                    // Parent "Food & Drink" — all 4 children selected -> collapses to one chip
                    Category(
                        id = 200L,
                        name = "Food & Drink",
                        colorHex = 0xFFF2914D.toInt(),
                        iconName = "Fastfood",
                        isDefault = false,
                        parentId = null
                    ),
                    Category(
                        id = 201L,
                        name = "Dining",
                        colorHex = 0xFFF2914D.toInt(),
                        iconName = "Restaurant",
                        isDefault = false,
                        parentId = 200L
                    ),
                    Category(
                        id = 202L,
                        name = "Groceries",
                        colorHex = 0xFF4CC987.toInt(),
                        iconName = "ShoppingCart",
                        isDefault = false,
                        parentId = 200L
                    ),
                    Category(
                        id = 203L,
                        name = "Coffee",
                        colorHex = 0xFFB5793A.toInt(),
                        iconName = "LocalCafe",
                        isDefault = false,
                        parentId = 200L
                    ),
                    Category(
                        id = 204L,
                        name = "Takeout",
                        colorHex = 0xFFE85A94.toInt(),
                        iconName = "Fastfood",
                        isDefault = false,
                        parentId = 200L
                    ),
                    // Parent "Transport" — only "Fuel" selected -> still a tappable chip, expands to "Fuel"
                    Category(
                        id = 300L,
                        name = "Transport",
                        colorHex = 0xFF4F9AE0.toInt(),
                        iconName = "DirectionsCar",
                        isDefault = false,
                        parentId = null
                    ),
                    Category(
                        id = 301L,
                        name = "Fuel",
                        colorHex = 0xFF4F9AE0.toInt(),
                        iconName = "LocalGasStation",
                        isDefault = false,
                        parentId = 300L
                    ),
                    Category(
                        id = 302L,
                        name = "Public Transit",
                        colorHex = 0xFF4F9AE0.toInt(),
                        iconName = "DirectionsBus",
                        isDefault = false,
                        parentId = 300L
                    ),
                    // Top-level category with no children -> renders as a plain chip
                    Category(
                        id = 102L,
                        name = "Travel",
                        colorHex = 0xFF00FFFF.toInt(),
                        iconName = "Flight",
                        isDefault = false,
                        parentId = null
                    )
                )
            )
        }
    }
}
