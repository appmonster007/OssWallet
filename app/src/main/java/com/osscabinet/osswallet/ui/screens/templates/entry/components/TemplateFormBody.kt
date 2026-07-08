package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.TransactionSide
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
import com.osscabinet.ossWallet.ui.components.entry.EntrySheetOrchestrator
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
    val sourceCurrencies = remember(state.selectedAccountId, state.accounts, state.shortlistedCurrencies) {
        state.accounts.find { it.id == state.selectedAccountId }
            ?.allowedCurrencies?.toList()?.ifEmpty { null }
            ?: state.shortlistedCurrencies.ifEmpty { CurrencyService.getAllCurrencies() }
    }

    val targetCurrencies =
        remember(state.targetAccountId, state.accounts, state.shortlistedCurrencies) {
            state.accounts.find { it.id == state.targetAccountId }
                ?.allowedCurrencies?.toList()?.ifEmpty { null }
                ?: state.shortlistedCurrencies.ifEmpty { CurrencyService.getAllCurrencies() }
        }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
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
                transactionType = state.entryMode,
                onTypeSelected = { onEvent(TemplateEntryEvent.SetTransactionType(it)) },
                source = AeonAmountData(
                    value = state.amount,
                    currency = state.currency,
                    onValueClick = {
                        onEvent(
                            TemplateEntryEvent.ShowNumpad(
                                true,
                                AmountEditingSide.SOURCE
                            )
                        )
                    },
                    onCurrencyClick = { onEvent(TemplateEntryEvent.ShowCurrencySheet(true)) },
                    label = "SOURCE AMOUNT",
                    supportedCurrencies = sourceCurrencies
                ),
                target = AeonAmountData(
                    value = state.targetAmount ?: "",
                    currency = state.targetCurrency ?: state.currency,
                    onValueClick = {
                        onEvent(
                            TemplateEntryEvent.ShowNumpad(
                                true,
                                AmountEditingSide.TARGET
                            )
                        )
                    },
                    onCurrencyClick = { onEvent(TemplateEntryEvent.ShowTargetCurrencySheet(true)) },
                    label = "DESTINATION AMOUNT",
                    supportedCurrencies = targetCurrencies
                ),
                categoryId = state.selectedCategoryId,
                nameState = AeonTextFieldState(
                    value = state.name,
                    onValueChange = { onEvent(TemplateEntryEvent.SetName(it)) },
                    label = "TEMPLATE NAME",
                    isError = state.errors.contains(EntryField.NAME)
                ),
                targetAccountId = state.targetAccountId,
                transferRate = state.exchangeRate,
                isRateFixed = state.isRateFixed,
                onToggleRateMode = { onEvent(TemplateEntryEvent.ToggleRateMode) },
                onExchangeRateClick = { onEvent(TemplateEntryEvent.ShowExchangeRateDialog(true)) },
                onSwapClick = if (state.isTransfer &&
                    (state.selectedAccountId != null || state.targetAccountId != null)
                )
                    ({ onEvent(TemplateEntryEvent.SwapAccounts) }) else null,
                accountId = state.selectedAccountId,
                errors = state.errors
            )

            if (state.isTransfer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AeonTheme.dimensions.spacingSm),
                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = AeonTheme.dimensions.spacingXs)
                    )
                    Text(
                        text = "Exchange rate resets to market rate when template is reopened",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // CONTEXT: Action cards row: Account, Category
            AeonContextGrid {
                AeonEntryContextRow(
                    isTransfer = state.entryMode == EntryMode.TRANSFER,
                    accountSelector = AeonSelectorState(
                        selection = state.selectedAccount,
                        onClick = { onEvent(TemplateEntryEvent.ShowAccountSheet(true)) },
                        isError = state.errors.contains(EntryField.ACCOUNT)
                    ),
                    categorySelector = if (state.entryMode != EntryMode.TRANSFER) {
                        AeonSelectorState(
                            selection = state.selectedCategory,
                            onClick = { onEvent(TemplateEntryEvent.ShowCategorySheet(true)) },
                            isError = state.errors.contains(EntryField.CATEGORY)
                        )
                    } else null,
                    targetAccountSelector = if (state.entryMode == EntryMode.TRANSFER) {
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
                selectedNames = state.selectedTags,
                allAvailableTags = state.allTags,
                onTagClick = { onEvent(TemplateEntryEvent.ShowTagSheet(true)) },
                onRemoveTag = { tag -> onEvent(TemplateEntryEvent.SetTags(state.selectedTags - tag)) },
                onTagsSelected = { onEvent(TemplateEntryEvent.SetTags(it)) },
                onAddTag = { name, color -> onEvent(TemplateEntryEvent.AddTag(name, color)) },
                noteSuggestions = state.allNotes,
                payeeSuggestions = state.allPayees,
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

        TemplateSheetOrchestrator(
            state = state,
            onSearch = onSearch,
            onEvent = onEvent
        )
    }
}

@Composable
private fun TemplateSheetOrchestrator(
    state: TemplateEntryState,
    onSearch: (String) -> kotlinx.coroutines.flow.Flow<List<com.osscabinet.ossWallet.domain.model.Transaction>>,
    onEvent: (TemplateEntryEvent) -> Unit
) {
    EntrySheetOrchestrator(
        sheetState = state.sheetState,
        accounts = state.accounts,
        categories = state.categories,
        allTags = state.allTags,
        selectedTags = state.selectedTags,
        currentAmount = if (state.activeAmountSide == AmountEditingSide.SOURCE) state.amount else state.targetAmount,
        currentExchangeRate = state.exchangeRate,
        shortlistedCurrencies = state.shortlistedCurrencies,
        fullCurrencies = state.fullProviderCurrencies,
        isTransfer = state.isTransfer,
        onAmountChange = { amt ->
            if (state.activeAmountSide == AmountEditingSide.SOURCE) {
                onEvent(TemplateEntryEvent.SetAmount(amt))
            } else {
                onEvent(TemplateEntryEvent.SetTargetAmount(amt))
            }
        },
        onAccountChange = { accId -> onEvent(TemplateEntryEvent.SetAccountId(accId)) },
        onTargetAccountChange = { accId -> onEvent(TemplateEntryEvent.SetTargetAccountId(accId)) },
        onCategoryChange = { catId -> onEvent(TemplateEntryEvent.SetCategoryId(catId)) },
        onCurrencyChange = { curr -> onEvent(TemplateEntryEvent.SetCurrency(curr)) },
        onTargetCurrencyChange = { curr -> onEvent(TemplateEntryEvent.SetTargetCurrency(curr)) },
        onExchangeRateChange = { rate -> onEvent(TemplateEntryEvent.SetExchangeRate(rate)) },
        onTagsChange = { tags -> onEvent(TemplateEntryEvent.SetTags(tags)) },
        onAddTag = { name, color -> onEvent(TemplateEntryEvent.AddTag(name, color)) },
        onDismissSheet = { field ->
            when (field) {
                EntryField.AMOUNT -> onEvent(TemplateEntryEvent.ShowNumpad(false))
                EntryField.ACCOUNT -> onEvent(TemplateEntryEvent.ShowAccountSheet(false))
                EntryField.TARGET_ACCOUNT -> onEvent(TemplateEntryEvent.ShowTargetAccountSheet(false))
                EntryField.CATEGORY -> onEvent(TemplateEntryEvent.ShowCategorySheet(false))
                else -> onEvent(TemplateEntryEvent.DismissAllSheets)
            }
        },
        onSearchTransactions = onSearch,
        linkedIds = state.linkedTransactionIdSet,
        onLinkedTransactionsChanged = { ids ->
            onEvent(
                TemplateEntryEvent.SetLinkedTransactionIds(
                    ids.toList()
                )
            )
        },
        selectedAccountId = state.selectedAccountId,
        selectedTargetAccountId = state.targetAccountId,
        selectedCategoryId = state.selectedCategoryId,
        selectedCurrency = state.currency,
        selectedTargetCurrency = state.targetCurrency,
        warrantyDateMillis = state.warrantyExpirationMillis,
        onWarrantyDateChange = { date -> onEvent(TemplateEntryEvent.SetWarrantyExpiration(date)) },
        gpsLocation = state.gpsLocation,
        onLocationChange = { gps, loc -> onEvent(TemplateEntryEvent.SetLocation(gps, loc)) },
        paymentMethod = state.paymentMethod,
        onPaymentMethodChange = { method -> onEvent(TemplateEntryEvent.SetPaymentMethod(method)) },
        transactionStatus = state.transactionStatus,
        onTransactionStatusChange = { status ->
            onEvent(
                TemplateEntryEvent.SetTransactionStatus(
                    status
                )
            )
        }
    )
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
