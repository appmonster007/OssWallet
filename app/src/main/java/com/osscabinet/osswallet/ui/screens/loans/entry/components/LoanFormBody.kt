package com.osscabinet.ossWallet.ui.screens.loans.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.LoanType
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
    onPaymentMethodClick: () -> Unit,
    onStatusClick: () -> Unit,
    onLinkRecordClick: () -> Unit,
    counterpartySuggestions: List<String>,
    noteSuggestions: List<String>,
    modifier: Modifier = Modifier
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
                        selectedCategoryIds.isEmpty() -> "Select"
                        categoryObj != null -> categoryObj.name
                        else -> "${selectedCategoryIds.size} Selected"
                    },
                    onClick = onCategoryClick,
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }
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
                AeonEntryMetadataSection(
                    state = AeonMetadataState(),
                    accounts = accounts,
                    categories = categories,
                    is24Hour = false,
                    onShowLinkedTransactions = onLinkRecordClick,
                    onRemoveLink = { /* Handled by linking sheet */ },
                    onPaymentMethodClick = onPaymentMethodClick,
                    onStatusClick = onStatusClick,
                    onWarrantyToggle = {},
                    onWarrantyDateClick = {},
                    onLocationClick = {},
                    additionalBottomContent = {
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
        )
    }
}
