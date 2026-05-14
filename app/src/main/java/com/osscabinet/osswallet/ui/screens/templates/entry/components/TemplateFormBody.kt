package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryEvent
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryState

/**
 * Main content layout for template entry screen.
 * Optimized with shared components and inlined logic to reduce component bloat.
 */
@Composable
fun TemplateFormBody(
    state: TemplateEntryState,
    onEvent: (TemplateEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val allCurrencies = remember { CurrencyService.getAllCurrencies() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // IMPACT: Type selector + Amount input + Title
            AeonImpactHeaderCard(
                transactionType = state.transactionType,
                onTypeSelected = { onEvent(TemplateEntryEvent.SetTransactionType(it)) },
                amount = state.amount,
                currency = state.currency,
                onCurrencyClick = { onEvent(TemplateEntryEvent.ShowCurrencySheet(true)) },
                onAmountClick = { onEvent(TemplateEntryEvent.ShowNumpad(true)) },
                name = state.name,
                onNameChange = { onEvent(TemplateEntryEvent.SetName(it)) },
                nameLabel = "TEMPLATE NAME",
                isTransfer = state.transactionType == 2,
                targetAccountId = state.targetAccountId,
                targetCurrency = state.targetCurrency,
                onTargetCurrencyClick = { onEvent(TemplateEntryEvent.ShowTargetCurrencySheet(true)) },
                transferRate = state.transferRate,
                onExchangeRateClick = { onEvent(TemplateEntryEvent.ShowExchangeRateDialog(true)) },
                supportedCurrencies = allCurrencies
            )

            // CONTEXT: Action cards row: Account, Category
            AeonContextGrid {
                val accountIcon = getAccountIconByName(state.selectedAccount?.icon)
                EntryActionCard(
                    icon = accountIcon,
                    label = if (state.transactionType == 2) "FROM" else "ACCOUNT",
                    value = state.selectedAccount?.name ?: "Select",
                    onClick = { onEvent(TemplateEntryEvent.ShowAccountSheet(true)) },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )

                if (state.transactionType == 2) {
                    val targetDisplay = state.selectedTargetAccount?.name ?: "Select"
                    val targetIcon = getAccountIconByName(state.selectedTargetAccount?.icon)
                    EntryActionCard(
                        icon = targetIcon,
                        label = "TO",
                        value = targetDisplay,
                        onClick = { onEvent(TemplateEntryEvent.ShowTargetAccountSheet(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    val categoryIcon = getIconByName(state.selectedCategory?.iconName)
                    EntryActionCard(
                        icon = categoryIcon,
                        label = "CATEGORY",
                        value = state.selectedCategory?.name ?: "Unknown",
                        onClick = { onEvent(TemplateEntryEvent.ShowCategorySheet(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // DETAIL: Metadata section
            AeonDetailSection(
                note = state.note,
                onNoteChange = { onEvent(TemplateEntryEvent.SetNote(it)) },
                payee = state.payee,
                onPayeeChange = { onEvent(TemplateEntryEvent.SetPayee(it)) },
                selectedTags = state.tags,
                onTagClick = { onEvent(TemplateEntryEvent.ShowTagSheet(true)) },
                onRemoveTag = { tag -> onEvent(TemplateEntryEvent.SetTags(state.tags - tag)) },
                noteSuggestions = state.uniqueNotes,
                payeeSuggestions = state.uniquePayees,
                additionalContent = {
                    // Linked Transactions
                    EntryLinkedTransactionsSection(
                        linkedTransactions = state.displayLinkedTxs,
                        accounts = state.accounts,
                        categories = state.categories,
                        is24Hour = state.use24HourFormat,
                        onShowSheet = { onEvent(TemplateEntryEvent.ShowLinkedTransactionSheet(true)) },
                        onRemoveLink = { txId ->
                            onEvent(TemplateEntryEvent.RemoveLinkedTransaction(state.templateId ?: -1L, txId))
                        }
                    )

                    // Payment Method and Status row
                    val paymentIcon = AppIconMap[state.paymentMethod.iconName] ?: Icons.Rounded.Payments

                    val statusColor = Color(android.graphics.Color.parseColor(state.transactionStatus.colorHex))
                    val statusIcon = AppIconMap[state.transactionStatus.iconName] ?: Icons.Rounded.CheckCircle

                    EntryPaymentMethodStatusRow(
                        paymentMethod = state.paymentMethod.displayName,
                        onPaymentMethodClick = { onEvent(TemplateEntryEvent.ShowPaymentMethodSheet(true)) },
                        statusLabel = state.transactionStatus.displayName,
                        onStatusClick = { onEvent(TemplateEntryEvent.ShowStatusSheet(true)) },
                        paymentMethodIcon = paymentIcon,
                        statusIcon = statusIcon,
                        statusIconTint = statusColor,
                        statusContainerColor = statusColor.copy(alpha = 0.1f)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )

                    // Warranty
                    EntryWarrantySection(
                        enabled = state.isWarrantyEnabled,
                        onToggle = { onEvent(TemplateEntryEvent.SetWarrantyEnabled(it)) },
                        expirationMillis = state.warrantyExpirationMillis,
                        onDateClick = { onEvent(TemplateEntryEvent.ShowWarrantyDatePicker(true)) }
                    )

                    // Location
                    EntryLocationRow(
                        coords = if (state.latitude != null && state.longitude != null) {
                            state.latitude to state.longitude
                        } else null,
                        onShowLocationSheet = { onEvent(TemplateEntryEvent.ShowLocationSheet(true)) },
                        label = "Template Location"
                    )
                }
            )
        }
    }
}
