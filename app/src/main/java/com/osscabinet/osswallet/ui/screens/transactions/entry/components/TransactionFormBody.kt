package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

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
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.domain.model.isPicked
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonAmountData
import com.osscabinet.ossWallet.ui.components.entry.AeonCalculatorSheet
import com.osscabinet.ossWallet.ui.components.entry.AeonCategorySelection
import com.osscabinet.ossWallet.ui.components.entry.AeonContextGrid
import com.osscabinet.ossWallet.ui.components.entry.AeonDetailSection
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryContextRow
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryMetadataSection
import com.osscabinet.ossWallet.ui.components.entry.AeonImpactHeaderCard
import com.osscabinet.ossWallet.ui.components.entry.AeonMetadataState
import com.osscabinet.ossWallet.ui.components.entry.AeonSelectorState
import com.osscabinet.ossWallet.ui.components.entry.AeonTemplateSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonTransactionSplitSheet
import com.osscabinet.ossWallet.ui.components.entry.EntryActionCard
import com.osscabinet.ossWallet.ui.components.entry.EntryActionRow
import com.osscabinet.ossWallet.ui.components.entry.EntryField
import com.osscabinet.ossWallet.ui.components.entry.EntrySheetOrchestrator
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main content layout for transaction entry screen.
 * Optimized with shared components and unified metadata section.
 */
@Composable
fun TransactionFormBody(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit,
    onSearch: (String) -> Flow<List<Transaction>>,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    extraContent: @Composable () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AeonTheme.dimensions.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
        ) {
            Spacer(Modifier.height(contentPadding.calculateTopPadding()))
            if (state.prefillDto != null) MlKitPrefillBanner()
            ImpactHeader(state, onEvent)
            ContextGrid(state, onEvent)
            DetailSection(state, onEvent)
            extraContent()
            Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
        }

        ReplaceOpeningBalanceDialog(state.initialBalanceConflict, onEvent)
        FormSheets(state, onEvent, onSearch)
        SplitSheets(state, onEvent)
        if (state.showTemplateSheet) TemplateSheet(state, onEvent)
    }
}

@Composable
private fun MlKitPrefillBanner() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "✨ Auto-decoded with ML Kit. Review and save.",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/** Transaction type selector and the source/destination amount inputs. */
@Composable
private fun ImpactHeader(state: TransactionEntryState, onEvent: (TransactionEntryEvent) -> Unit) {
    val sourceCurrencies = remember(state.selectedAccount, state.shortlistedCurrencies) {
        state.selectedAccount?.allowedCurrencies?.ifEmpty { null }
            ?: state.shortlistedCurrencies.ifEmpty { listOf("USD") }
    }
    val targetCurrencies = remember(state.targetAccount, state.shortlistedCurrencies) {
        state.targetAccount?.allowedCurrencies?.ifEmpty { null }
            ?: state.shortlistedCurrencies.ifEmpty { CurrencyService.getAllCurrencies() }
    }

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
            value = state.targetAmount,
            currency = state.targetCurrency,
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
            if (!state.isSplitMode) onEvent(TransactionEntryEvent.ToggleSplitMode(true))
            onEvent(TransactionEntryEvent.ShowSplitSheet(true))
        },
        onSwapClick = if (state.isTransfer && (state.source.account.isPicked || state.destination.account.isPicked)) {
            { onEvent(TransactionEntryEvent.SwapAccounts) }
        } else null,
        accountId = state.selectedAccount?.id,
        errors = state.errors
    )
}

/** Template and date cards, then the account and category (or target account) selectors. */
@Composable
private fun ContextGrid(state: TransactionEntryState, onEvent: (TransactionEntryEvent) -> Unit) {
    AeonContextGrid {
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

        AeonEntryContextRow(
            isTransfer = state.isTransfer,
            accountSelector = AeonSelectorState(
                selection = state.selectedAccount,
                onClick = { onEvent(TransactionEntryEvent.ShowAccountSheet(true)) },
                isError = state.hasError(EntryField.ACCOUNT)
            ),
            categorySelector = if (!state.isTransfer) {
                AeonSelectorState(
                    selection = state.selectedCategory,
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
}

/** Note, payee and tags, plus the payment/status/warranty/location/links metadata. */
@Composable
private fun DetailSection(state: TransactionEntryState, onEvent: (TransactionEntryEvent) -> Unit) {
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
                    status = state.transactionStatus,
                    isWarrantyEnabled = state.isWarrantyEnabled,
                    warrantyExpiration = state.warrantyExpirationMillis,
                    gpsLocation = state.gpsLocation,
                    locationName = state.location,
                    linkedTransactions = state.displayLinkedTxs
                ),
                accounts = state.accounts,
                categories = state.categories,
                allTags = state.allTags,
                onShowLinkedTransactions = { onEvent(TransactionEntryEvent.ShowLinkedTransactionSheet(true)) },
                onRemoveLink = { txId ->
                    onEvent(TransactionEntryEvent.RemoveLinkedTransaction(state.transactionId ?: -1L, txId))
                },
                onPaymentMethodClick = { onEvent(TransactionEntryEvent.ShowPaymentMethodSheet(true)) },
                onStatusClick = { onEvent(TransactionEntryEvent.SetTransactionStatus(it)) },
                onWarrantyToggle = { onEvent(TransactionEntryEvent.SetWarrantyEnabled(it)) },
                onWarrantyDateClick = { onEvent(TransactionEntryEvent.ShowWarrantyDatePicker(true)) },
                onLocationClick = { onEvent(TransactionEntryEvent.ShowLocationSheet(true)) },
                locationLabel = "Transaction Location"
            )
        }
    )
}

/** Asked when this row would become a second opening balance for the same account and currency. */
@Composable
private fun ReplaceOpeningBalanceDialog(
    conflict: Transaction?,
    onEvent: (TransactionEntryEvent) -> Unit
) {
    AeonConfirmDeleteDialog(
        show = conflict != null,
        title = "Replace opening balance?",
        message = conflict?.let {
            val amount = it.amount.abs().stripTrailingZeros().toPlainString()
            val date = DateFormat.getDateInstance().format(Date(it.timestamp))
            "This account already has an opening balance of $amount ${it.currency} dated $date. " +
                "Replacing it uses this row's amount instead and removes the old one. " +
                "An opening balance is always dated one day before the account's earliest transaction. " +
                "Keep the old one to save this as a plain transfer."
        } ?: "",
        onConfirm = { onEvent(TransactionEntryEvent.ConfirmReplaceInitialBalance) },
        onDismiss = { onEvent(TransactionEntryEvent.DeclineReplaceInitialBalance) },
        confirmButtonText = "Replace",
        dismissButtonText = "Keep old"
    )
}

/** The shared pickers (account, category, currency, tags, ...) driven by [TransactionEntryState.sheetState]. */
@Composable
private fun FormSheets(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit,
    onSearch: (String) -> Flow<List<Transaction>>
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
        // Picking a transfer category here switches the form to a single-leg transfer.
        transferCategoriesSelectable = true,
        onAmountChange = { amount ->
            onEvent(
                if (state.activeAmountSide == AmountEditingSide.SOURCE) TransactionEntryEvent.SetAmount(amount)
                else TransactionEntryEvent.SetTargetAmount(amount)
            )
        },
        onAccountChange = { onEvent(TransactionEntryEvent.SetAccountId(it)) },
        onTargetAccountChange = { onEvent(TransactionEntryEvent.SetTargetAccountId(it)) },
        onCategoryChange = { onEvent(TransactionEntryEvent.SetCategoryId(it)) },
        onCurrencyChange = { onEvent(TransactionEntryEvent.SetCurrency(it)) },
        onTargetCurrencyChange = { onEvent(TransactionEntryEvent.SetTargetCurrency(it)) },
        onExchangeRateChange = { onEvent(TransactionEntryEvent.SetExchangeRate(it)) },
        onTagsChange = { onEvent(TransactionEntryEvent.SetTags(it)) },
        onAddTag = { name: String, color: Int? -> onEvent(TransactionEntryEvent.AddTag(name, color)) },
        onDismissSheet = { field: EntryField? ->
            onEvent(
                when (field) {
                    EntryField.AMOUNT -> TransactionEntryEvent.ShowNumpad(false)
                    EntryField.ACCOUNT -> TransactionEntryEvent.ShowAccountSheet(false)
                    EntryField.TARGET_ACCOUNT -> TransactionEntryEvent.ShowTargetAccountSheet(false)
                    EntryField.CATEGORY -> TransactionEntryEvent.ShowCategorySheet(false)
                    else -> TransactionEntryEvent.DismissAllSheets
                }
            )
        },
        onSearchTransactions = onSearch,
        linkedIds = state.linkedTransactionIdSet,
        onLinkedTransactionsChanged = { onEvent(TransactionEntryEvent.SetLinkedTransactionIds(it.toList())) },
        selectedAccountId = state.selectedAccount?.id,
        selectedTargetAccountId = state.targetAccount?.id,
        selectedCategoryId = state.selectedCategoryId,
        selectedCurrency = state.selectedCurrency,
        selectedTargetCurrency = state.targetCurrency,
        selectedDateMillis = state.selectedDateMillis,
        onDateChange = { onEvent(TransactionEntryEvent.SetDate(it)) },
        warrantyDateMillis = state.warrantyExpirationMillis,
        onWarrantyDateChange = { onEvent(TransactionEntryEvent.SetWarrantyExpiration(it)) },
        gpsLocation = state.gpsLocation,
        onLocationChange = { gps: String?, location: String? -> onEvent(TransactionEntryEvent.SetLocation(gps, location)) },
        paymentMethod = state.paymentMethod,
        onPaymentMethodChange = { onEvent(TransactionEntryEvent.SetPaymentMethod(it)) },
        transactionStatus = state.transactionStatus,
        onTransactionStatusChange = { onEvent(TransactionEntryEvent.SetTransactionStatus(it)) },
        currentTransactionId = state.transactionId
    )
}

// Split sheets are handled here rather than by the orchestrator since they are transaction-specific.
@Composable
private fun SplitSheets(state: TransactionEntryState, onEvent: (TransactionEntryEvent) -> Unit) {
    if (state.showSplitSheet) {
        AeonTransactionSplitSheet(
            totalAmount = state.amountValue,
            currency = state.selectedCurrency,
            splits = state.splits,
            categories = state.categories,
            isIncome = state.isIncome,
            onAddSplit = { onEvent(TransactionEntryEvent.AddSplit) },
            onRemoveSplit = { index -> onEvent(TransactionEntryEvent.RemoveSplit(index)) },
            onUpdateSplit = { index, amount, note, categoryId ->
                onEvent(TransactionEntryEvent.UpdateSplit(index, categoryId, amount, note))
            },
            onShowCategorySheet = { index -> onEvent(TransactionEntryEvent.ShowSplitCategorySheet(index)) },
            onShowCalculator = { index -> onEvent(TransactionEntryEvent.ShowSplitNumpad(index)) },
            onToggleSplitMode = { enabled -> onEvent(TransactionEntryEvent.ToggleSplitMode(enabled)) },
            onDismiss = { onEvent(TransactionEntryEvent.ShowSplitSheet(false)) }
        )
    }

    state.showSplitCategorySheet?.let { splitIndex ->
        // A split is real income/expense, so the transfer categories are not offered.
        val splittableCategories = remember(state.categories) {
            state.categories.filterNot { Category.isTransfer(it.id) }
        }
        AeonCategorySelection(
            categories = splittableCategories,
            selectedCategoryIds = state.splits.getOrNull(splitIndex)?.categoryId?.let { setOf(it) } ?: emptySet(),
            onSelectionChange = { ids ->
                onEvent(TransactionEntryEvent.UpdateSplit(splitIndex, ids.firstOrNull(), null, null))
                onEvent(TransactionEntryEvent.ShowSplitCategorySheet(null))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowSplitCategorySheet(null)) }
        )
    }

    state.showSplitNumpad?.let { splitIndex ->
        AeonCalculatorSheet(
            currentAmount = state.splits.getOrNull(splitIndex)?.amount ?: "",
            onAmountChanged = { amount ->
                onEvent(TransactionEntryEvent.UpdateSplit(splitIndex, null, amount, null))
            },
            onDismiss = { onEvent(TransactionEntryEvent.ShowSplitNumpad(null)) }
        )
    }
}

@Composable
private fun TemplateSheet(state: TransactionEntryState, onEvent: (TransactionEntryEvent) -> Unit) {
    AeonTemplateSelection(
        templates = state.templates,
        categories = state.categories,
        onTemplateSelected = { template ->
            if (template != null) onEvent(TransactionEntryEvent.ApplyTemplate(template))
            onEvent(TransactionEntryEvent.ShowTemplateSheet(false))
        },
        onDismiss = { onEvent(TransactionEntryEvent.ShowTemplateSheet(false)) },
        showCreateNew = false
    )
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(millis))

@Preview(showBackground = true)
@Composable
fun TransactionFormBodyPreview() {
    OssWalletTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TransactionFormBody(
                state = TransactionEntryState(
                    source = TransactionSide(amount = "125.00", currency = "USD"),
                    note = "Dinner with friends"
                ),
                onEvent = {},
                onSearch = { flowOf(emptyList()) }
            )
        }
    }
}
