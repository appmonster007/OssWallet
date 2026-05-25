package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews

private fun parseAccountColor(colorHex: String?): Color? =
    colorHex?.runCatching { Color(android.graphics.Color.parseColor(this)) }?.getOrNull()

private fun parseCategoryColor(colorHex: Int?): Color? =
    colorHex?.let { Color(it) }

/**
 * Shared Context grid for Account and Category/Target account selection.
 */
@Composable
fun AeonEntryContextRow(
    isTransfer: Boolean,
    selectedAccount: Account?,
    onAccountClick: () -> Unit,
    selectedCategory: Category? = null,
    onCategoryClick: () -> Unit = {},
    targetAccount: Account? = null,
    onTargetAccountClick: () -> Unit = {},
    isSplitMode: Boolean = false,
    splitCount: Int = 0,
    onSplitClick: () -> Unit = {}
) {
    EntryActionRow {
        val accountIcon = getAccountIconByName(selectedAccount?.icon)
        val accountColor = parseAccountColor(selectedAccount?.colorHex) ?: AppColors.primary
        EntryActionCard(
            icon = accountIcon,
            label = if (isTransfer) "FROM" else "ACCOUNT",
            value = selectedAccount?.name ?: "Select",
            onClick = onAccountClick,
            modifier = Modifier.weight(1f),
            iconContainerColor = accountColor.copy(alpha = 0.1f),
            iconTint = accountColor
        )

        if (isTransfer) {
            val targetDisplay = targetAccount?.name ?: "Select"
            val targetIcon = getAccountIconByName(targetAccount?.icon)
            val targetColor = parseAccountColor(targetAccount?.colorHex) ?: AppColors.tertiary
            EntryActionCard(
                icon = targetIcon,
                label = "TO",
                value = targetDisplay,
                onClick = onTargetAccountClick,
                modifier = Modifier.weight(1f),
                iconContainerColor = targetColor.copy(alpha = 0.1f),
                iconTint = targetColor
            )
        } else {
            val displayCategoryName = if (isSplitMode) {
                "Split Transaction ($splitCount)"
            } else {
                selectedCategory?.name ?: "Unknown"
            }
            val categoryIcon = if (isSplitMode) {
                Icons.Rounded.Splitscreen
            } else {
                getIconByName(selectedCategory?.iconName)
            }
            val categoryColor = if (isSplitMode) AppColors.primary
            else parseCategoryColor(selectedCategory?.colorHex) ?: AppColors.tertiary

            EntryActionCard(
                icon = categoryIcon,
                label = if (isSplitMode) "TRANSACTION" else "CATEGORY",
                value = displayCategoryName,
                onClick = if (isSplitMode) onSplitClick else onCategoryClick,
                modifier = Modifier.weight(1f),
                iconContainerColor = categoryColor.copy(alpha = 0.1f),
                iconTint = categoryColor
            )
        }
    }
}

/**
 * Data class representing the shared metadata state for entry screens.
 */
data class AeonMetadataState(
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val status: TransactionStatus = TransactionStatus.CLEARED,
    val isWarrantyEnabled: Boolean = false,
    val warrantyExpiration: Long = System.currentTimeMillis(),
    val gpsLocation: String? = null,
    val locationName: String? = null,
    val linkedTransactions: List<Transaction> = emptyList()
)

/**
 * Common metadata section containing Linked Transactions, Payment Method, Status, Warranty, and Location.
 */
@Composable
fun AeonEntryMetadataSection(
    state: AeonMetadataState,
    accounts: List<Account>,
    categories: List<Category>,
    allTags: List<com.osscabinet.ossWallet.domain.model.Tag> = emptyList(),
    onShowLinkedTransactions: () -> Unit,
    onRemoveLink: (Long) -> Unit,
    onPaymentMethodClick: () -> Unit,
    onStatusClick: (TransactionStatus) -> Unit,
    onWarrantyToggle: (Boolean) -> Unit,
    onWarrantyDateClick: () -> Unit,
    onLocationClick: () -> Unit,
    locationLabel: String = "Location",
    additionalTopContent: @Composable (ColumnScope.() -> Unit)? = null,
    additionalBottomContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
        additionalTopContent?.invoke(this)

        // Linked Transactions
        EntryLinkedTransactionsSection(
            linkedTransactions = state.linkedTransactions,
            accounts = accounts,
            categories = categories,
            allTags = allTags,
            onShowSheet = onShowLinkedTransactions,
            onRemoveLink = onRemoveLink
        )

        // Payment Method and Status row
        val statusColor = Color(android.graphics.Color.parseColor(state.status.colorHex))
        val statusIcon = AppIconMap[state.status.iconName] ?: Icons.Rounded.CheckCircle
        val paymentIcon = AppIconMap[state.paymentMethod.iconName] ?: Icons.Rounded.Payments

        EntryPaymentMethodStatusRow(
            paymentMethod = state.paymentMethod.displayName,
            onPaymentMethodClick = onPaymentMethodClick,
            statusLabel = state.status.displayName,
            onStatusClick = { onStatusClick(TransactionStatus.next(state.status)) },
            paymentMethodIcon = paymentIcon,
            statusIcon = statusIcon,
            statusIconTint = statusColor,
            statusContainerColor = statusColor.copy(alpha = 0.1f)
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = AeonTheme.dimensions.spacingXs),
            thickness = (AeonTheme.dimensions.spacingXs / 8),
            color = AppColors.divider.copy(alpha = 0.2f)
        )

        additionalBottomContent?.invoke(this)

        // Warranty
        EntryWarrantySection(
            enabled = state.isWarrantyEnabled,
            onToggle = onWarrantyToggle,
            expirationMillis = state.warrantyExpiration,
            onDateClick = onWarrantyDateClick
        )

        // Location
        EntryLocationRow(
            gpsLocation = state.gpsLocation,
            locationName = state.locationName,
            onShowLocationSheet = onLocationClick,
            label = locationLabel
        )
    }
}

@ThemePreviews
@Composable
fun AeonEntryContextRowPreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AeonEntryContextRow(
                isTransfer = false,
                selectedAccount = Account(
                    id = "1",
                    name = "Savings",
                    colorHex = "#4CAF50",
                    currencyCode = "USD",
                    structureType = AccountStructureType.SAVINGS,
                    icon = "AccountBalance"
                ),
                onAccountClick = {},
                selectedCategory = Category(
                    id = 1,
                    name = "Food",
                    iconName = "Restaurant",
                    colorHex = -1,
                    isDefault = false,
                    parentId = null
                ),
                onCategoryClick = {}
            )
            AeonEntryContextRow(
                isTransfer = true,
                selectedAccount = Account(
                    id = "1",
                    name = "Savings",
                    colorHex = "#4CAF50",
                    currencyCode = "USD",
                    structureType = AccountStructureType.SAVINGS,
                    icon = "AccountBalance"
                ),
                onAccountClick = {},
                targetAccount = Account(
                    id = "2",
                    name = "Checking",
                    colorHex = "#2196F3",
                    currencyCode = "USD",
                    structureType = AccountStructureType.CHECKING,
                    icon = "AccountBalance"
                ),
                onTargetAccountClick = {}
            )
        }
    }
}

@ThemePreviews
@Composable
fun AeonEntryMetadataSectionPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AeonEntryMetadataSection(
                state = AeonMetadataState(),
                accounts = emptyList(),
                categories = emptyList(),
                onShowLinkedTransactions = {},
                onRemoveLink = {},
                onPaymentMethodClick = {},
                onStatusClick = {},
                onWarrantyToggle = {},
                onWarrantyDateClick = {},
                onLocationClick = {}
            )
        }
    }
}
