package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.entry.AeonAmountData
import com.osscabinet.ossWallet.ui.components.entry.AeonContextGrid
import com.osscabinet.ossWallet.ui.components.entry.AeonDetailSection
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryContextRow
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryMetadataSection
import com.osscabinet.ossWallet.ui.components.entry.AeonImpactHeaderCard
import com.osscabinet.ossWallet.ui.components.entry.AeonMetadataState
import com.osscabinet.ossWallet.ui.components.entry.AeonSelectorState
import com.osscabinet.ossWallet.ui.components.entry.AeonTextFieldState
import com.osscabinet.ossWallet.ui.components.entry.EntryField
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
    contentPadding: PaddingValues = PaddingValues(),
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

    Box(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AeonTheme.dimensions.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
        ) {
            Spacer(Modifier.height(contentPadding.calculateTopPadding()))
            // IMPACT: Type selector + Amount input + Title
            AeonImpactHeaderCard(
                transactionType = state.transactionType,
                onTypeSelected = { onEvent(TemplateEntryEvent.SetTransactionType(it)) },
                source = AeonAmountData(
                    value = state.amount,
                    currency = state.currency,
                    onValueClick = { onEvent(TemplateEntryEvent.ShowNumpad(true, AmountEditingSide.SOURCE)) },
                    onCurrencyClick = { onEvent(TemplateEntryEvent.SetCurrency(it)) },
                    label = "SOURCE AMOUNT",
                    supportedCurrencies = sourceCurrencies
                ),
                target = AeonAmountData(
                    value = state.targetAmount ?: "",
                    currency = state.targetCurrency ?: state.currency,
                    onValueClick = { onEvent(TemplateEntryEvent.ShowNumpad(true, AmountEditingSide.TARGET)) },
                    onCurrencyClick = { onEvent(TemplateEntryEvent.SetTargetCurrency(it)) },
                    label = "DESTINATION AMOUNT",
                    supportedCurrencies = targetCurrencies
                ),
                categoryId = state.categoryId,
                nameState = AeonTextFieldState(
                    value = state.name,
                    onValueChange = { onEvent(TemplateEntryEvent.SetName(it)) },
                    label = "TEMPLATE NAME",
                    isError = state.errors.contains(EntryField.NAME)
                ),
                targetAccountId = state.targetAccountId,
                transferRate = state.transferRate,
                onExchangeRateClick = { onEvent(TemplateEntryEvent.ShowExchangeRateDialog(true)) },
                onSwapClick = if (state.isTransfer &&
                    (state.accountId != null || state.targetAccountId != null))
                    ({ onEvent(TemplateEntryEvent.SwapAccounts) }) else null,
                accountId = state.accountId,
                errors = state.errors
            )

            // CONTEXT: Action cards row: Account, Category
            AeonContextGrid {
                AeonEntryContextRow(
                    isTransfer = state.transactionType == TransactionType.TRANSFER,
                    accountSelector = AeonSelectorState(
                        selection = state.selectedAccount,
                        onClick = { onEvent(TemplateEntryEvent.ShowAccountSheet(true)) },
                        isError = state.errors.contains(EntryField.ACCOUNT)
                    ),
                    categorySelector = if (state.transactionType != TransactionType.TRANSFER) {
                        AeonSelectorState(
                            selection = state.selectedCategory,
                            onClick = { onEvent(TemplateEntryEvent.ShowCategorySheet(true)) },
                            isError = state.errors.contains(EntryField.CATEGORY)
                        )
                    } else null,
                    targetAccountSelector = if (state.transactionType == TransactionType.TRANSFER) {
                        AeonSelectorState(
                            selection = state.selectedTargetAccount,
                            onClick = { onEvent(TemplateEntryEvent.ShowTargetAccountSheet(true)) },
                            isError = state.errors.contains(EntryField.TARGET_ACCOUNT)
                        )
                    } else null
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
            Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
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
                    source = TransactionSide(
                        amount = "50.00",
                        currency = "USD"
                    )
                ),
                onSearch = { flowOf(emptyList()) },
                onEvent = {}
            )
        }
    }
}
