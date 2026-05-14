package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryEvent
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryState

/**
 * Main content layout for template entry screen.
 * Optimized with shared components and unified metadata section.
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
                isTransfer = state.transactionType == TransactionType.TRANSFER,
                targetAccountId = state.targetAccountId,
                targetCurrency = state.targetCurrency,
                onTargetCurrencyClick = { onEvent(TemplateEntryEvent.ShowTargetCurrencySheet(true)) },
                transferRate = state.transferRate,
                onExchangeRateClick = { onEvent(TemplateEntryEvent.ShowExchangeRateDialog(true)) },
                supportedCurrencies = allCurrencies
            )

            // CONTEXT: Action cards row: Account, Category
            AeonEntryContextRow(
                isTransfer = state.transactionType == TransactionType.TRANSFER,
                selectedAccount = state.selectedAccount,
                onAccountClick = { onEvent(TemplateEntryEvent.ShowAccountSheet(true)) },
                selectedCategory = state.selectedCategory,
                onCategoryClick = { onEvent(TemplateEntryEvent.ShowCategorySheet(true)) },
                targetAccount = state.selectedTargetAccount,
                onTargetAccountClick = { onEvent(TemplateEntryEvent.ShowTargetAccountSheet(true)) }
            )

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
                    AeonEntryMetadataSection(
                        state = AeonMetadataState(
                            paymentMethod = state.paymentMethod,
                            statusLabel = state.transactionStatus.displayName,
                            statusIcon = state.transactionStatus.iconName,
                            statusColor = state.transactionStatus.colorHex,
                            isWarrantyEnabled = state.isWarrantyEnabled,
                            warrantyExpiration = state.warrantyExpirationMillis,
                            location = if (state.latitude != null && state.longitude != null) state.latitude!! to state.longitude!! else null,
                            linkedTransactions = state.displayLinkedTxs
                        ),
                        accounts = state.accounts,
                        categories = state.categories,
                        is24Hour = state.use24HourFormat,
                        onShowLinkedTransactions = { onEvent(TemplateEntryEvent.ShowLinkedTransactionSheet(true)) },
                        onRemoveLink = { txId -> onEvent(TemplateEntryEvent.RemoveLinkedTransaction(state.templateId ?: -1L, txId)) },
                        onPaymentMethodClick = { onEvent(TemplateEntryEvent.ShowPaymentMethodSheet(true)) },
                        onStatusClick = { onEvent(TemplateEntryEvent.ShowStatusSheet(true)) },
                        onWarrantyToggle = { onEvent(TemplateEntryEvent.SetWarrantyEnabled(it)) },
                        onWarrantyDateClick = { onEvent(TemplateEntryEvent.ShowWarrantyDatePicker(true)) },
                        onLocationClick = { onEvent(TemplateEntryEvent.ShowLocationSheet(true)) },
                        locationLabel = "Template Location"
                    )
                }
            )
        }
    }
}
