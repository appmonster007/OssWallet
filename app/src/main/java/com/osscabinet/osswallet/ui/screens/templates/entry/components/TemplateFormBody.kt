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

import com.osscabinet.ossWallet.ui.theme.AeonTheme

/**
 * Main content layout for template entry screen.
 * Optimized with shared components and unified metadata section.
 */
@Composable
fun TemplateFormBody(
    state: TemplateEntryState,
    onSearch: (String) -> kotlinx.coroutines.flow.Flow<List<com.osscabinet.ossWallet.domain.model.Transaction>>,
    onEvent: (TemplateEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val allCurrencies = remember { CurrencyService.getAllCurrencies() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = AeonTheme.dimensions.screenPadding,
                    vertical = AeonTheme.dimensions.spacingLg
                ),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
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
            AeonContextGrid {
                AeonEntryContextRow(
                    isTransfer = state.transactionType == TransactionType.TRANSFER,
                    selectedAccount = state.selectedAccount,
                    onAccountClick = { onEvent(TemplateEntryEvent.ShowAccountSheet(true)) },
                    selectedCategory = state.selectedCategory,
                    onCategoryClick = { onEvent(TemplateEntryEvent.ShowCategorySheet(true)) },
                    targetAccount = state.selectedTargetAccount,
                    onTargetAccountClick = { onEvent(TemplateEntryEvent.ShowTargetAccountSheet(true)) }
                )
            }

            // DETAIL: Metadata section
            AeonDetailSection(
                note = state.note,
                onNoteChange = { onEvent(TemplateEntryEvent.SetNote(it)) },
                payee = state.payee,
                onPayeeChange = { onEvent(TemplateEntryEvent.SetPayee(it)) },
                selectedNames = state.tags,
                allAvailableTags = state.allTags,
                onTagClick = { onEvent(TemplateEntryEvent.ShowTagSheet(true)) },
                onRemoveTag = { tag -> onEvent(TemplateEntryEvent.SetTags(state.tags - tag)) },
                noteSuggestions = state.uniqueNotes,
                payeeSuggestions = state.uniquePayees,
                additionalContent = {
                    AeonEntryMetadataSection(
                        state = AeonMetadataState(
                            paymentMethod = state.paymentMethod,
                            status = state.transactionStatus,
                            isWarrantyEnabled = state.isWarrantyEnabled,
                            warrantyExpiration = state.warrantyExpirationMillis,
                            location = if (state.latitude != null && state.longitude != null) state.latitude!! to state.longitude!! else null,
                            linkedTransactions = state.displayLinkedTxs
                        ),
                        accounts = state.accounts,
                        categories = state.categories,
                        onShowLinkedTransactions = { onEvent(TemplateEntryEvent.ShowLinkedTransactionSheet(true)) },
                        onRemoveLink = { txId -> onEvent(TemplateEntryEvent.RemoveLinkedTransaction(state.templateId ?: -1L, txId)) },
                        onPaymentMethodClick = { onEvent(TemplateEntryEvent.ShowPaymentMethodSheet(true)) },
                        onStatusClick = { onEvent(TemplateEntryEvent.SetTransactionStatus(it)) },
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
