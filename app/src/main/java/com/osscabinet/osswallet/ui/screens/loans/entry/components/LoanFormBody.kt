package com.osscabinet.ossWallet.ui.screens.loans.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*

@Composable
fun LoanFormBody(
    loanType: LoanType,
    onTypeSelected: (LoanType) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    amount: String,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    supportedCurrencies: List<String>,
    onAmountClick: () -> Unit,
    startDateMillis: Long,
    onStartDateClick: () -> Unit,
    interestRate: String,
    onInterestClick: () -> Unit,
    linkedAccountId: String,
    accounts: List<Account>,
    onAccountClick: () -> Unit,
    hasSettlementDate: Boolean,
    settlementDateMillis: Long,
    onSettlementClick: () -> Unit,
    selectedCategoryIds: Set<Long>,
    categories: List<Category>,
    onCategoryClick: () -> Unit,
    counterparty: String,
    onCounterpartyChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    onHasSettlementDateChange: (Boolean) -> Unit,
    selectedTags: Set<String>,
    onShowTagSheet: () -> Unit,
    onRemoveTag: (String) -> Unit,
    selectedPaymentMethods: Set<PaymentMethod>,
    onPaymentMethodClick: () -> Unit,
    selectedStatuses: Set<TransactionStatus>,
    onStatusClick: () -> Unit,
    linkedTransactionIds: Set<Long>,
    onLinkRecordClick: () -> Unit,
    filterId: Long?,
    onFilterClick: () -> Unit,
    availableFilters: List<Filter>,
    counterpartySuggestions: List<String>,
    noteSuggestions: List<String>,
    modifier: Modifier = Modifier,
    isFilterShared: Boolean = false,
    filterUsageCount: Int = 0,
    onMakeFilterUnique: () -> Unit = {}
) {
    val amountColor = if (loanType == LoanType.LENT) Color(0xFF004D40) else Color(0xFFE65100)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // IMPACT: Header card
        AeonImpactHeaderCard(
            selectedType = loanType,
            onTypeSelected = onTypeSelected,
            types = listOf(
                Triple(Icons.Rounded.ArrowUpward, "Lent", LoanType.LENT),
                Triple(Icons.Rounded.ArrowDownward, "Borrowed", LoanType.BORROWED)
            ),
            amount = amount,
            currency = currency,
            onCurrencyClick = { onCurrencyChange(currency) },
            onAmountClick = onAmountClick,
            name = name,
            onNameChange = onNameChange,
            nameLabel = "LOAN NAME",
            amountColor = amountColor,
            supportedCurrencies = supportedCurrencies
        )

        // CONTEXT: Action cards grid
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Row 1: Start Date and Interest Rate
            AeonContextGrid {
                EntryDateCard(
                    icon = Icons.Rounded.CalendarToday,
                    label = "START DATE",
                    dateMillis = startDateMillis,
                    onClick = onStartDateClick,
                    modifier = Modifier.weight(1f)
                )

                EntryActionCard(
                    icon = Icons.Rounded.Percent,
                    label = "INTEREST",
                    value = if (interestRate.isEmpty()) "0%" else "$interestRate%",
                    onClick = onInterestClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }

            // Row 2: Account and Category
            AeonContextGrid {
                val accountObj = accounts.find { it.id == linkedAccountId }
                EntryActionCard(
                    icon = getAccountIconByName(accountObj?.icon),
                    label = "ACCOUNT",
                    value = accountObj?.name ?: "Select",
                    onClick = onAccountClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )

                val categoryObj = if (selectedCategoryIds.size == 1) categories.find { it.id == selectedCategoryIds.first() } else null
                EntryActionCard(
                    icon = categoryObj?.let { getIconByName(it.iconName) } ?: Icons.Rounded.Category,
                    label = "CATEGORY",
                    value = when {
                        selectedCategoryIds.isEmpty() || (categories.isNotEmpty() && selectedCategoryIds.size == categories.size) -> "All Categories"
                        categoryObj != null -> categoryObj.name
                        else -> "${selectedCategoryIds.size} Selected"
                    },
                    onClick = onCategoryClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }

            // Row 3: Filter (Full Width)
            val filterObj = availableFilters.find { it.id == filterId }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EntryActionCard(
                    icon = Icons.Rounded.FilterList,
                    label = "FILTER",
                    value = filterObj?.name ?: "None",
                    onClick = onFilterClick,
                    modifier = Modifier.fillMaxWidth(),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
                
                if (filterId != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isFilterShared) "Shared with ${filterUsageCount - 1} other(s)" else "Unique Filter",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (isFilterShared) {
                            TextButton(
                                onClick = onMakeFilterUnique,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Make Unique", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // DETAIL: Filter section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "FILTERS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )

            // Payment Methods
            EntryActionCard(
                icon = Icons.Rounded.Payments,
                label = "PAYMENT METHODS",
                value = if (selectedPaymentMethods.isEmpty() || selectedPaymentMethods.size == PaymentMethod.entries.size) "All Methods" else "${selectedPaymentMethods.size} Selected",
                onClick = onPaymentMethodClick,
                modifier = Modifier.fillMaxWidth(),
                iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.primary
            )

            // Transaction Status
            EntryActionCard(
                icon = Icons.Rounded.CheckCircle,
                label = "TRANSACTION STATUS",
                value = if (selectedStatuses.isEmpty() || selectedStatuses.size == TransactionStatus.entries.size) "All Statuses" else "${selectedStatuses.size} Selected",
                onClick = onStatusClick,
                modifier = Modifier.fillMaxWidth(),
                iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.secondary
            )

            // Linked Transactions
            EntryActionCard(
                icon = Icons.Rounded.Link,
                label = "LINKED TRANSACTIONS",
                value = if (linkedTransactionIds.isEmpty()) "None" else "${linkedTransactionIds.size} Linked",
                onClick = onLinkRecordClick,
                modifier = Modifier.fillMaxWidth(),
                iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.tertiary
            )
        }

        // DETAIL: Metadata section
        AeonDetailSection(
            note = note,
            onNoteChange = onNoteChange,
            payee = counterparty,
            onPayeeChange = onCounterpartyChange,
            payeeLabel = if (loanType == LoanType.LENT) "BORROWER" else "LENDER",
            payeeSuggestions = counterpartySuggestions,
            selectedTags = selectedTags,
            onTagClick = onShowTagSheet,
            onRemoveTag = onRemoveTag,
            noteSuggestions = noteSuggestions,
            additionalContent = {
                // Settlement Date
                EntrySwitchRow(
                    label = "Has Settlement Date",
                    checked = hasSettlementDate,
                    onCheckedChange = onHasSettlementDateChange,
                    icon = Icons.Rounded.EventAvailable
                )

                if (hasSettlementDate) {
                    EntryDateCard(
                        icon = Icons.Rounded.CalendarToday,
                        label = "SETTLEMENT DATE",
                        dateMillis = settlementDateMillis,
                        onClick = onSettlementClick
                    )
                }
            }
        )
    }
}
