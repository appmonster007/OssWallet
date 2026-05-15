package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.theme.AeonTheme

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
    AeonContextGrid {
        val accountIcon = getAccountIconByName(selectedAccount?.icon)
        EntryActionCard(
            icon = accountIcon,
            label = if (isTransfer) "FROM" else "ACCOUNT",
            value = selectedAccount?.name ?: "Select",
            onClick = onAccountClick,
            modifier = Modifier.weight(1f),
            iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            iconTint = MaterialTheme.colorScheme.primary
        )

        if (isTransfer) {
            val targetDisplay = targetAccount?.name ?: "Select"
            val targetIcon = getAccountIconByName(targetAccount?.icon)
            EntryActionCard(
                icon = targetIcon,
                label = "TO",
                value = targetDisplay,
                onClick = onTargetAccountClick,
                modifier = Modifier.weight(1f),
                iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.tertiary
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

            EntryActionCard(
                icon = categoryIcon,
                label = if (isSplitMode) "TRANSACTION" else "CATEGORY",
                value = displayCategoryName,
                onClick = if (isSplitMode) onSplitClick else onCategoryClick,
                modifier = Modifier.weight(1f),
                iconContainerColor = if (isSplitMode) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                },
                iconTint = if (isSplitMode) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                }
            )
        }
    }
}

/**
 * Data class representing the shared metadata state for entry screens.
 */
data class AeonMetadataState(
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val statusLabel: String = "Completed",
    val statusIcon: String = "CheckCircle",
    val statusColor: String = "#4CAF50",
    val isWarrantyEnabled: Boolean = false,
    val warrantyExpiration: Long = System.currentTimeMillis(),
    val location: Pair<Double, Double>? = null,
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
    is24Hour: Boolean,
    onShowLinkedTransactions: () -> Unit,
    onRemoveLink: (Long) -> Unit,
    onPaymentMethodClick: () -> Unit,
    onStatusClick: () -> Unit,
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
            is24Hour = is24Hour,
            onShowSheet = onShowLinkedTransactions,
            onRemoveLink = onRemoveLink
        )

        // Payment Method and Status row
        val statusColor = Color(android.graphics.Color.parseColor(state.statusColor))
        val statusIcon = AppIconMap[state.statusIcon] ?: Icons.Rounded.CheckCircle
        val paymentIcon = AppIconMap[state.paymentMethod.iconName] ?: Icons.Rounded.Payments

        EntryPaymentMethodStatusRow(
            paymentMethod = state.paymentMethod.displayName,
            onPaymentMethodClick = onPaymentMethodClick,
            statusLabel = state.statusLabel,
            onStatusClick = onStatusClick,
            paymentMethodIcon = paymentIcon,
            statusIcon = statusIcon,
            statusIconTint = statusColor,
            statusContainerColor = statusColor.copy(alpha = 0.1f)
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = AeonTheme.dimensions.spacingXs),
            thickness = (AeonTheme.dimensions.spacingXs / 8),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
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
            coords = state.location,
            onShowLocationSheet = onLocationClick,
            label = locationLabel
        )
    }
}
