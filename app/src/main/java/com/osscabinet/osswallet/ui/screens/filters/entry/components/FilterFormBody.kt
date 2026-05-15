package com.osscabinet.ossWallet.ui.screens.filters.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryEvent
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme

@Composable
fun FilterFormBody(
    state: FilterEntryState,
    onEvent: (FilterEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = AeonTheme.dimensions
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dims.screenPadding, vertical = dims.spacingLg),
            verticalArrangement = Arrangement.spacedBy(dims.spacingLg)
        ) {
            // IMPACT: Header card (Filter Name)
            AeonImpactHeaderCard(
                selectedType = if (state.selectedTransactionTypes.size == 1) state.selectedTransactionTypes.first() else TransactionType.EXPENSE,
                onTypeSelected = { onEvent(FilterEntryEvent.ShowTypeSheet(true)) },
                types = listOf(
                    Triple(Icons.Rounded.Remove, "Expense", TransactionType.EXPENSE),
                    Triple(Icons.Rounded.Add, "Income", TransactionType.INCOME)
                ),
                amount = "",
                currency = "",
                onCurrencyClick = { },
                onAmountClick = { },
                name = state.name,
                onNameChange = { onEvent(FilterEntryEvent.SetName(it)) },
                nameLabel = "FILTER NAME",
                showTypeSelector = true,
                showAmount = false,
                showCurrency = false
            )

            // CONTEXT: Action cards grid
            Column(verticalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
                Text(
                    "CONTEXT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                
                AeonContextGrid {
                    // Accounts
                    val accountObj = if (state.selectedAccountIds.size == 1) state.accounts.find { it.id == state.selectedAccountIds.first() } else null
                    EntryActionCard(
                        icon = getAccountIconByName(accountObj?.icon),
                        label = "ACCOUNTS",
                        value = when {
                            state.selectedAccountIds.isEmpty() || (state.accounts.isNotEmpty() && state.selectedAccountIds.size == state.accounts.size) -> "All Accounts"
                            accountObj != null -> accountObj.name
                            else -> "${state.selectedAccountIds.size} Selected"
                        },
                        onClick = { onEvent(FilterEntryEvent.ShowAccountSheet(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    // Categories
                    val categoryObj = if (state.selectedCategoryIds.size == 1) state.categories.find { it.id == state.selectedCategoryIds.first() } else null
                    EntryActionCard(
                        icon = categoryObj?.let { getIconByName(it.iconName) } ?: Icons.Rounded.Category,
                        label = "CATEGORIES",
                        value = when {
                            state.selectedCategoryIds.isEmpty() || (state.categories.isNotEmpty() && state.selectedCategoryIds.size == state.categories.size) -> "All Categories"
                            categoryObj != null -> categoryObj.name
                            else -> "${state.selectedCategoryIds.size} Selected"
                        },
                        onClick = { onEvent(FilterEntryEvent.ShowCategorySheet(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // FILTERS: Multi-selection filters
            Column(verticalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
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
                    value = if (state.selectedPaymentMethods.isEmpty() || state.selectedPaymentMethods.size == PaymentMethod.entries.size) "All Methods" else "${state.selectedPaymentMethods.size} Selected",
                    onClick = { onEvent(FilterEntryEvent.ShowPaymentMethodSheet(true)) },
                    modifier = Modifier.fillMaxWidth(),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )

                // Transaction Status
                EntryActionCard(
                    icon = Icons.Rounded.CheckCircle,
                    label = "TRANSACTION STATUS",
                    value = if (state.selectedStatuses.isEmpty() || state.selectedStatuses.size == TransactionStatus.entries.size) "All Statuses" else "${state.selectedStatuses.size} Selected",
                    onClick = { onEvent(FilterEntryEvent.ShowStatusSheet(true)) },
                    modifier = Modifier.fillMaxWidth(),
                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )

                // Linked Transactions
                EntryActionCard(
                    icon = Icons.Rounded.Link,
                    label = "LINKED TRANSACTIONS",
                    value = if (state.linkedTransactionIds.isEmpty()) "None" else "${state.linkedTransactionIds.size} Linked",
                    onClick = { onEvent(FilterEntryEvent.ShowLinkSheet(true)) },
                    modifier = Modifier.fillMaxWidth(),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )

                // Currencies
                EntryActionCard(
                    icon = Icons.Rounded.AttachMoney,
                    label = "CURRENCIES",
                    value = if (state.selectedCurrencies.isEmpty()) "All Currencies" else "${state.selectedCurrencies.size} Selected",
                    onClick = { onEvent(FilterEntryEvent.ShowCurrencySheet(true)) },
                    modifier = Modifier.fillMaxWidth(),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }

            // SEARCH: Query filters
            Column(verticalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
                Text(
                    "SEARCH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )

                // Note Query
                EntryEditorialInput(
                    label = "NOTE QUERY",
                    value = state.queryNote,
                    onValueChange = { onEvent(FilterEntryEvent.SetQueryNote(it)) },
                    icon = Icons.Rounded.Note,
                    placeholder = "Search in notes...",
                    additionalContent = {
                        EntrySwitchRow(
                            label = "Regex",
                            checked = state.isNoteRegex,
                            onCheckedChange = { onEvent(FilterEntryEvent.SetNoteRegex(it)) },
                            icon = Icons.Rounded.Code
                        )
                    }
                )

                // Payee Query
                EntryEditorialInput(
                    label = "PAYEE QUERY",
                    value = state.queryPayee,
                    onValueChange = { onEvent(FilterEntryEvent.SetQueryPayee(it)) },
                    icon = Icons.Rounded.Person,
                    placeholder = "Search in payees...",
                    additionalContent = {
                        EntrySwitchRow(
                            label = "Regex",
                            checked = state.isPayeeRegex,
                            onCheckedChange = { onEvent(FilterEntryEvent.SetPayeeRegex(it)) },
                            icon = Icons.Rounded.Code
                        )
                    }
                )
            }

            // RANGE: Amount and date ranges
            Column(verticalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
                Text(
                    "RANGE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )

                AeonContextGrid {
                    // Min Amount
                    EntryActionCard(
                        icon = Icons.Rounded.ArrowDownward,
                        label = "MIN AMOUNT",
                        value = if (state.minAmount.isEmpty()) "Any" else state.minAmount,
                        onClick = { onEvent(FilterEntryEvent.ShowMinAmountSheet(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    // Max Amount
                    EntryActionCard(
                        icon = Icons.Rounded.ArrowUpward,
                        label = "MAX AMOUNT",
                        value = if (state.maxAmount.isEmpty()) "Any" else state.maxAmount,
                        onClick = { onEvent(FilterEntryEvent.ShowMaxAmountSheet(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // OPTIONS: Additional options
            Column(verticalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
                Text(
                    "OPTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )

                // Warranty
                EntryActionCard(
                    icon = Icons.Rounded.Verified,
                    label = "WARRANTY",
                    value = when (state.hasWarranty) {
                        null -> "Any"
                        true -> "Has Warranty"
                        false -> "No Warranty"
                    },
                    onClick = { onEvent(FilterEntryEvent.SetHasWarranty(
                        when (state.hasWarranty) {
                            null -> true
                            true -> false
                            false -> null
                        }
                    )) },
                    modifier = Modifier.fillMaxWidth(),
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )

                if (state.hasWarranty == true) {
                    AeonContextGrid {
                        EntryDateCard(
                            icon = Icons.Rounded.CalendarToday,
                            label = "WARRANTY START",
                            dateMillis = state.warrantyExpirationStart ?: System.currentTimeMillis(),
                            onClick = { onEvent(FilterEntryEvent.ShowWarrantyStartPicker(true)) },
                            modifier = Modifier.weight(1f)
                        )

                        EntryDateCard(
                            icon = Icons.Rounded.CalendarToday,
                            label = "WARRANTY END",
                            dateMillis = state.warrantyExpirationEnd ?: System.currentTimeMillis(),
                            onClick = { onEvent(FilterEntryEvent.ShowWarrantyEndPicker(true)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Exclude Future
                EntrySwitchRow(
                    label = "Exclude Future",
                    checked = state.excludeFuture,
                    onCheckedChange = { onEvent(FilterEntryEvent.SetExcludeFuture(it)) },
                    icon = Icons.Rounded.Schedule,
                    subtitle = "Don't include future transactions"
                )
            }

            // DETAIL: Metadata section
            AeonDetailSection(
                note = "",
                onNoteChange = { },
                showPayee = false,
                selectedTags = state.selectedTags,
                onTagClick = { onEvent(FilterEntryEvent.ShowTagSheet(true)) },
                onRemoveTag = { tag -> onEvent(FilterEntryEvent.SetTags(state.selectedTags - tag)) },
                noteSuggestions = emptyList()
            )
        }

        FilterEntrySheets(state = state, onEvent = onEvent)
    }
}
