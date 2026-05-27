package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.entry.AeonContextGrid
import com.osscabinet.ossWallet.ui.components.entry.AeonDetailSection
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryContextRow
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryMetadataSection
import com.osscabinet.ossWallet.ui.components.entry.AeonImpactHeaderCard
import com.osscabinet.ossWallet.ui.components.entry.AeonMetadataState
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryEvent
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.flow.flowOf

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
    val sourceCurrencies = remember(state.accountId, state.accounts, state.shortlistedCurrencies) {
        state.accounts.find { it.id == state.accountId }
            ?.allowedCurrencies?.toList()?.ifEmpty { null }
            ?: state.shortlistedCurrencies.ifEmpty { CurrencyService.getAllCurrencies() }
    }

    val targetCurrencies = remember(state.selectedTargetAccount) {
        state.selectedTargetAccount?.allowedCurrencies?.toList()?.ifEmpty { null }
            ?: CurrencyService.getAllCurrencies()
    }

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
                onCurrencyClick = { currency -> onEvent(TemplateEntryEvent.SetCurrency(currency)) },
                onAmountClick = { side -> onEvent(TemplateEntryEvent.ShowNumpad(true, side)) },
                amountLabel = if (state.isTransfer) "SOURCE AMOUNT" else "AMOUNT",
                targetAmount = if (state.isTransfer) state.targetAmount else null,
                name = state.name,
                onNameChange = { onEvent(TemplateEntryEvent.SetName(it)) },
                nameLabel = "TEMPLATE NAME",
                isTransfer = state.transactionType == TransactionType.TRANSFER,
                targetAccountId = state.targetAccountId,
                targetCurrency = state.targetCurrency,
                onTargetCurrencyClick = { currency ->
                    onEvent(
                        TemplateEntryEvent.SetTargetCurrency(
                            currency
                        )
                    )
                },
                transferRate = state.transferRate,
                onExchangeRateClick = { onEvent(TemplateEntryEvent.ShowExchangeRateDialog(true)) },
                supportedCurrencies = sourceCurrencies,
                targetSupportedCurrencies = targetCurrencies,
                accountId = state.accountId,
                isAmountPositive = state.isTransferInflow
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
                            gpsLocation = state.gpsLocation,
                            locationName = state.location.ifBlank { null },
                            linkedTransactions = state.displayLinkedTxs
                        ),
                        accounts = state.accounts,
                        categories = state.categories,
                        onShowLinkedTransactions = {
                            onEvent(
                                TemplateEntryEvent.ShowLinkedTransactionSheet(
                                    true
                                )
                            )
                        },
                        onRemoveLink = { txId ->
                            onEvent(
                                TemplateEntryEvent.RemoveLinkedTransaction(
                                    state.templateId ?: -1L,
                                    txId
                                )
                            )
                        },
                        onPaymentMethodClick = {
                            onEvent(
                                TemplateEntryEvent.ShowPaymentMethodSheet(
                                    true
                                )
                            )
                        },
                        onStatusClick = { onEvent(TemplateEntryEvent.SetTransactionStatus(it)) },
                        onWarrantyToggle = { onEvent(TemplateEntryEvent.SetWarrantyEnabled(it)) },
                        onWarrantyDateClick = {
                            onEvent(
                                TemplateEntryEvent.ShowWarrantyDatePicker(
                                    true
                                )
                            )
                        },
                        onLocationClick = { onEvent(TemplateEntryEvent.ShowLocationSheet(true)) },
                        locationLabel = "Template Location"
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TemplateFormBodyPreview() {
    OssWalletTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TemplateFormBody(
                state = TemplateEntryState(
                    name = "Grocery Template",
                    amount = "50.00",
                    currency = "USD"
                ),
                onSearch = { flowOf(emptyList()) },
                onEvent = {}
            )
        }
    }
}
