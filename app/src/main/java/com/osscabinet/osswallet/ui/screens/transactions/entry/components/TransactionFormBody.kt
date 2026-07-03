package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.domain.model.isPicked
import com.osscabinet.ossWallet.ui.components.entry.AeonAmountData
import com.osscabinet.ossWallet.ui.components.entry.AeonContextGrid
import com.osscabinet.ossWallet.ui.components.entry.AeonDetailSection
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryContextRow
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryMetadataSection
import com.osscabinet.ossWallet.ui.components.entry.AeonImpactHeaderCard
import com.osscabinet.ossWallet.ui.components.entry.AeonMetadataState
import com.osscabinet.ossWallet.ui.components.entry.AeonSelectorState
import com.osscabinet.ossWallet.ui.components.entry.EntryActionCard
import com.osscabinet.ossWallet.ui.components.entry.EntryActionRow
import com.osscabinet.ossWallet.ui.components.entry.EntryDateCard
import com.osscabinet.ossWallet.ui.components.entry.EntryField
import com.osscabinet.ossWallet.ui.components.entry.EntrySheetOrchestrator
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.flow.flowOf

/**
 * Main content layout for transaction entry screen.
 * Optimized with shared components and unified metadata section.
 */
@Composable
fun TransactionFormBody(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit,
    onSearch: (String) -> kotlinx.coroutines.flow.Flow<List<com.osscabinet.ossWallet.domain.model.Transaction>>,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val sourceCurrencies = remember(state.selectedAccount, state.shortlistedCurrencies) {
        state.selectedAccount?.allowedCurrencies?.ifEmpty { null }
            ?: state.shortlistedCurrencies.ifEmpty { listOf("USD") }
    }

    val targetCurrencies = remember(state.targetAccount, state.shortlistedCurrencies) {
        state.targetAccount?.allowedCurrencies?.ifEmpty { null }
            ?: state.shortlistedCurrencies.ifEmpty { CurrencyService.getAllCurrencies() }
    }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AeonTheme.dimensions.screenPadding)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
        ) {
            Spacer(Modifier.height(contentPadding.calculateTopPadding()))
            // IMPACT: Header card containing transaction type selector and amount input
            AeonImpactHeaderCard(
                transactionType = state.entryMode,
                onTypeSelected = { onEvent(TransactionEntryEvent.SetTransactionType(it)) },
                source = AeonAmountData(
                    value = state.amount,
                    currency = state.selectedCurrency,
                    onValueClick = { onEvent(TransactionEntryEvent.ShowNumpad(true, AmountEditingSide.SOURCE)) },
                    onCurrencyClick = { onEvent(TransactionEntryEvent.SetCurrency(it)) },
                    label = "SOURCE AMOUNT",
                    supportedCurrencies = sourceCurrencies,
                    isError = state.hasError(EntryField.AMOUNT)
                ),
                target = AeonAmountData(
                    value = state.targetAmount ?: "",
                    currency = state.targetCurrency ?: state.selectedCurrency,
                    onValueClick = { onEvent(TransactionEntryEvent.ShowNumpad(true, AmountEditingSide.TARGET)) },
                    onCurrencyClick = { onEvent(TransactionEntryEvent.SetTargetCurrency(it)) },
                    label = "DESTINATION AMOUNT",
                    supportedCurrencies = targetCurrencies,
                    isError = state.isTransfer && state.hasError(EntryField.AMOUNT)
                ),
                categoryId = state.selectedCategoryId,
                showAmount = true,
                targetAccountId = state.targetAccount?.id,
                transferRate = state.exchangeRate,
                isRateFixed = state.isRateFixed,
                onExchangeRateClick = { onEvent(TransactionEntryEvent.ShowExchangeRateDialog(true)) },
                onToggleRateMode = { onEvent(TransactionEntryEvent.ToggleRateMode) },
                isSplitMode = state.isSplitMode,
                onSplitClick = {
                    if (!state.isSplitMode) {
                        onEvent(TransactionEntryEvent.ToggleSplitMode(true))
                    }
                    onEvent(TransactionEntryEvent.ShowSplitSheet(true))
                },
                onSwapClick = if (state.isTransfer &&
                    (state.source.account.isPicked || state.destination.account.isPicked))
                    ({ onEvent(TransactionEntryEvent.SwapAccounts) }) else null,
                accountId = state.selectedAccount?.id,
                errors = state.errors
            )

            // CONTEXT: Action cards row: Template, Date, Account, Category
            AeonContextGrid {
                // Row 1: Template and Date
                EntryActionRow {
                    if (!state.isEditMode) {
                        EntryActionCard(
                            icon = Icons.Rounded.Bookmark,
                            label = "TEMPLATE",
                            value = state.selectedTemplateName ?: "None",
                            onClick = { onEvent(TransactionEntryEvent.ShowTemplateSheet(true)) },
                            modifier = Modifier.weight(1f),
                            iconContainerColor = AppColors.formIconContainerPrimary,
                            iconTint = AppColors.onPrimaryContainer
                        )
                    }

                    EntryActionCard(
                        icon = Icons.Rounded.CalendarToday,
                        label = "DATE",
                        value = remember(state.selectedDateMillis) { formatDate(state.selectedDateMillis) },
                        onClick = { onEvent(TransactionEntryEvent.ShowDatePicker(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = AppColors.formIconContainerSecondary,
                        iconTint = AppColors.onSecondaryContainer
                    )
                }

                // Row 2: Account and Category/Target
                AeonEntryContextRow(
                    isTransfer = state.isTransfer,
                    accountSelector = AeonSelectorState(
                        selection = state.selectedAccount,
                        onClick = { onEvent(TransactionEntryEvent.ShowAccountSheet(true)) },
                        isError = state.hasError(EntryField.ACCOUNT)
                    ),
                    categorySelector = if (!state.isTransfer) {
                        AeonSelectorState(
                            selection = state.categories.find { it.id == state.selectedCategoryId },
                            onClick = { onEvent(TransactionEntryEvent.ShowCategorySheet(true)) },
                            isError = state.hasError(EntryField.CATEGORY)
                        )
                    } else null,
                    targetAccountSelector = if (state.isTransfer) {
                        AeonSelectorState(
                            selection = state.targetAccount,
                            onClick = { onEvent(TransactionEntryEvent.ShowTargetAccountSheet(true)) },
                            isError = state.hasError(EntryField.TARGET_ACCOUNT)
                        )
                    } else null,
                        isSplitMode = state.isSplitMode,
                    splitCount = state.splits.size,
                    onSplitClick = { onEvent(TransactionEntryEvent.ShowSplitSheet(true)) }
                )
            }

            // DETAIL: Metadata section
            AeonDetailSection(
                note = if (state.isSplitMode) "" else state.note,
                onNoteChange = { if (!state.isSplitMode) onEvent(TransactionEntryEvent.SetNote(it)) },
                notePlaceholder = if (state.isSplitMode) "Detail notes entered in splits" else "Tap to enter...",
                payee = state.payee,
                onPayeeChange = { onEvent(TransactionEntryEvent.SetPayee(it)) },
                selectedNames = state.selectedTags,
                allAvailableTags = state.allTags,
                onTagClick = { onEvent(TransactionEntryEvent.ShowTagSheet(true)) },
                onRemoveTag = { tag -> onEvent(TransactionEntryEvent.RemoveTag(tag)) },
                onTagsSelected = { tags -> onEvent(TransactionEntryEvent.SetTags(tags)) },
                onAddTag = { tag, color -> onEvent(TransactionEntryEvent.AddTag(tag, color)) },
                noteSuggestions = state.allNotes,
                payeeSuggestions = state.allPayees,
                additionalContent = {
                    AeonEntryMetadataSection(
                        state = AeonMetadataState(
                            paymentMethod = state.paymentMethod,
                            status = state.status,
                            isWarrantyEnabled = state.isWarrantyEnabled,
                            warrantyExpiration = state.warrantyExpirationMillis,
                            gpsLocation = state.gpsLocation,
                            locationName = state.location,
                            linkedTransactions = state.displayLinkedTxs
                        ),
                        accounts = state.accounts,
                        categories = state.categories,
                        allTags = state.allTags,
                        onShowLinkedTransactions = {
                            onEvent(
                                TransactionEntryEvent.ShowLinkedTransactionSheet(
                                    true
                                )
                            )
                        },
                        onRemoveLink = { txId ->
                            onEvent(
                                TransactionEntryEvent.RemoveLinkedTransaction(
                                    state.transactionId ?: -1L,
                                    txId
                                )
                            )
                        },
                        onPaymentMethodClick = {
                            onEvent(
                                TransactionEntryEvent.ShowPaymentMethodSheet(
                                    true
                                )
                            )
                        },
                        onStatusClick = { onEvent(TransactionEntryEvent.SetStatus(it)) },
                        onWarrantyToggle = { onEvent(TransactionEntryEvent.SetWarrantyEnabled(it)) },
                        onWarrantyDateClick = {
                            onEvent(
                                TransactionEntryEvent.ShowWarrantyDatePicker(
                                    true
                                )
                            )
                        },
                        onLocationClick = { onEvent(TransactionEntryEvent.ShowLocationSheet(true)) },
                        locationLabel = "Transaction Location"
                    )
                }
            )
            Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
        }

        // Bottom sheets and dialogs
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
                    onEvent(TransactionEntryEvent.SetAmount(amt))
                } else {
                    onEvent(TransactionEntryEvent.SetTargetAmount(amt))
                }
            },
            onAccountChange = { accId -> onEvent(TransactionEntryEvent.SetAccount(state.accounts.find { acc -> acc.id == accId })) },
            onTargetAccountChange = { accId -> onEvent(TransactionEntryEvent.SetTargetAccount(state.accounts.find { acc -> acc.id == accId })) },
            onCategoryChange = { catId -> onEvent(TransactionEntryEvent.SetCategory(catId)) },
            onCurrencyChange = { curr -> onEvent(TransactionEntryEvent.SetCurrency(curr)) },
            onTargetCurrencyChange = { curr -> onEvent(TransactionEntryEvent.SetTargetCurrency(curr)) },
            onExchangeRateChange = { rate -> onEvent(TransactionEntryEvent.SetExchangeRate(rate)) },
            onTagsChange = { tags -> onEvent(TransactionEntryEvent.SetTags(tags)) },
            onAddTag = { name: String, color: Int? -> onEvent(TransactionEntryEvent.AddTag(name, color)) },
            onDismissSheet = { field: EntryField? ->
                when (field) {
                    EntryField.AMOUNT -> onEvent(TransactionEntryEvent.ShowNumpad(false))
                    EntryField.ACCOUNT -> onEvent(TransactionEntryEvent.ShowAccountSheet(false))
                    EntryField.TARGET_ACCOUNT -> onEvent(TransactionEntryEvent.ShowTargetAccountSheet(false))
                    EntryField.CATEGORY -> onEvent(TransactionEntryEvent.ShowCategorySheet(false))
                    else -> {
                        // Close all for unspecified
                        onEvent(TransactionEntryEvent.ShowDatePicker(false))
                        onEvent(TransactionEntryEvent.ShowTagSheet(false))
                        onEvent(TransactionEntryEvent.ShowPaymentMethodSheet(false))
                        onEvent(TransactionEntryEvent.ShowStatusSheet(false))
                        onEvent(TransactionEntryEvent.ShowWarrantyDatePicker(false))
                        onEvent(TransactionEntryEvent.ShowLocationSheet(false))
                        onEvent(TransactionEntryEvent.ShowExchangeRateDialog(false))
                        onEvent(TransactionEntryEvent.ShowCurrencySheet(false))
                        onEvent(TransactionEntryEvent.ShowTargetCurrencySheet(false))
                        onEvent(TransactionEntryEvent.ShowLinkedTransactionSheet(false))
                    }
                }
            },
            onSearchTransactions = onSearch,
            linkedIds = state.linkedTransactionIds.toSet(),
            onLinkedTransactionsChanged = { ids -> onEvent(TransactionEntryEvent.SetLinkedTransactionIds(ids.toList())) },
            selectedAccountId = state.selectedAccount?.id,
            selectedTargetAccountId = state.targetAccount?.id,
            selectedCategoryId = state.selectedCategoryId,
            selectedCurrency = state.selectedCurrency,
            selectedTargetCurrency = state.targetCurrency,
            selectedDateMillis = state.selectedDateMillis,
            onDateChange = { date -> onEvent(TransactionEntryEvent.SetDate(date)) },
            warrantyDateMillis = state.warrantyExpirationMillis,
            onWarrantyDateChange = { date -> onEvent(TransactionEntryEvent.SetWarrantyExpiration(date)) },
            gpsLocation = state.gpsLocation,
            onLocationChange = { gps: String?, loc: String? -> onEvent(TransactionEntryEvent.SetLocation(gps, loc)) },
            paymentMethod = state.paymentMethod,
            onPaymentMethodChange = { method -> onEvent(TransactionEntryEvent.SetPaymentMethod(method)) },
            transactionStatus = state.status,
            onTransactionStatusChange = { status -> onEvent(TransactionEntryEvent.SetStatus(status)) }
        )
    }
}

private fun formatDate(millis: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(millis))
}

@Preview(showBackground = true)
@Composable
fun TransactionFormBodyPreview() {
    OssWalletTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TransactionFormBody(
                state = TransactionEntryState(
                    source = TransactionSide(
                        amount = "125.00",
                        currency = "USD"
                    ),
                    note = "Dinner with friends"
                ),
                onEvent = {},
                onSearch = { flowOf(emptyList()) }
            )
        }
    }
}
