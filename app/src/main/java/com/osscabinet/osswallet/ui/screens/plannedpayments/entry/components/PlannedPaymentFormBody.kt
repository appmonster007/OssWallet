package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.TransactionSide
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.ui.components.entry.AeonAmountData
import com.osscabinet.ossWallet.ui.components.entry.AeonContextGrid
import com.osscabinet.ossWallet.ui.components.entry.AeonDetailSection
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryContextRow
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryMetadataSection
import com.osscabinet.ossWallet.ui.components.entry.AeonImpactHeaderCard
import com.osscabinet.ossWallet.ui.components.entry.AeonMetadataState
import com.osscabinet.ossWallet.ui.components.entry.AeonPlannedPaymentStatusSelection
import com.osscabinet.ossWallet.ui.components.entry.AeonSelectorState
import com.osscabinet.ossWallet.ui.components.entry.AeonTextFieldState
import com.osscabinet.ossWallet.ui.components.entry.EntryActionCard
import com.osscabinet.ossWallet.ui.components.entry.EntryActionChip
import com.osscabinet.ossWallet.ui.components.entry.EntryActionRow
import com.osscabinet.ossWallet.ui.components.entry.EntryDateCard
import com.osscabinet.ossWallet.ui.components.entry.EntryField
import com.osscabinet.ossWallet.ui.components.entry.EntrySection
import com.osscabinet.ossWallet.ui.components.entry.EntrySheetOrchestrator
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryEvent
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.flow.flowOf

/**
 * Main content layout for planned payment entry screen.
 * Optimized with shared components and unified metadata section.
 */
@Composable
fun PlannedPaymentFormBody(
    state: PlannedPaymentEntryState,
    onSearch: (String) -> kotlinx.coroutines.flow.Flow<List<com.osscabinet.ossWallet.domain.model.Transaction>>,
    onEvent: (PlannedPaymentEntryEvent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val sourceCurrencies =
        remember(state.selectedAccountId, state.accounts, state.shortlistedCurrencies) {
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

    Box(modifier = modifier
        .fillMaxSize()
        .statusBarsPadding()
        .imePadding()) {
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
                onTypeSelected = { onType ->
                    onEvent(
                        PlannedPaymentEntryEvent.SetTransactionType(
                            onType
                        )
                    )
                },
                source = AeonAmountData(
                    value = state.amount,
                    currency = state.currency,
                    onValueClick = {
                        onEvent(
                            PlannedPaymentEntryEvent.ShowNumpad(
                                true,
                                AmountEditingSide.SOURCE
                            )
                        )
                    },
                    onCurrencyClick = { onEvent(PlannedPaymentEntryEvent.ShowCurrencySheet(true)) },
                    label = "SOURCE AMOUNT",
                    supportedCurrencies = sourceCurrencies,
                    isError = state.hasError(EntryField.AMOUNT)
                ),
                target = AeonAmountData(
                    value = state.targetAmount ?: "",
                    currency = state.targetCurrency ?: state.currency,
                    onValueClick = {
                        onEvent(
                            PlannedPaymentEntryEvent.ShowNumpad(
                                true,
                                AmountEditingSide.TARGET
                            )
                        )
                    },
                    onCurrencyClick = {
                        onEvent(
                            PlannedPaymentEntryEvent.ShowTargetCurrencySheet(
                                true
                            )
                        )
                    },
                    label = "DESTINATION AMOUNT",
                    supportedCurrencies = targetCurrencies,
                    isError = state.isTransfer && state.hasError(EntryField.AMOUNT)
                ),
                categoryId = state.selectedCategoryId,
                nameState = AeonTextFieldState(
                    value = state.name,
                    onValueChange = { onEvent(PlannedPaymentEntryEvent.SetName(it)) },
                    label = "PAYMENT NAME",
                    isError = state.hasError(EntryField.NAME)
                ),
                showAmount = true,
                targetAccountId = state.targetAccountId,
                transferRate = state.exchangeRate,
                isRateFixed = state.isRateFixed,
                onToggleRateMode = { onEvent(PlannedPaymentEntryEvent.ToggleRateMode) },
                onExchangeRateClick = { onEvent(PlannedPaymentEntryEvent.ShowExchangeRateDialog(true)) },
                onSwapClick = if (state.isTransfer &&
                    (state.selectedAccountId != null || state.targetAccountId != null)
                )
                    ({ onEvent(PlannedPaymentEntryEvent.SwapAccounts) }) else null,
                accountId = state.selectedAccountId,
                errors = state.errors
            )

            // CONTEXT: Action cards row: Template, Schedule, Account, Category
            AeonContextGrid {
                // Row 1: Template and Schedule
                EntryActionRow {
                    EntryActionCard(
                        icon = Icons.Rounded.Bookmark,
                        label = "TEMPLATE",
                        value = state.linkedTemplate?.name ?: "None",
                        onClick = { onEvent(PlannedPaymentEntryEvent.ShowTemplateSheet(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = AppColors.formIconContainerPrimary,
                        iconTint = AppColors.onPrimaryContainer
                    )

                    val scheduleSummary = PlannedPaymentService.buildScheduleSummary(
                        state.startDateMillis,
                        RecurrenceRule(
                            state.recurrenceType,
                            state.recurrenceInterval,
                            state.dayOfWeek,
                            state.dayOfMonth,
                            state.cronExpression
                        )
                    )

                    EntryActionCard(
                        icon = Icons.Rounded.CalendarToday,
                        label = "SCHEDULE",
                        value = scheduleSummary,
                        onClick = { onEvent(PlannedPaymentEntryEvent.ShowScheduleBuilder(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = AppColors.formIconContainerSecondary,
                        iconTint = AppColors.onSecondaryContainer
                    )
                }

                // Row 2: Account and Category/Target
                AeonEntryContextRow(
                    isTransfer = state.entryMode == EntryMode.TRANSFER,
                    accountSelector = AeonSelectorState(
                        selection = state.selectedAccount,
                        onClick = { onEvent(PlannedPaymentEntryEvent.ShowAccountSheet(true)) },
                        isError = state.hasError(EntryField.ACCOUNT)
                    ),
                    categorySelector = if (state.entryMode != EntryMode.TRANSFER) {
                        AeonSelectorState(
                            selection = state.categories.find { it.id == state.selectedCategoryId },
                            onClick = { onEvent(PlannedPaymentEntryEvent.ShowCategorySheet(true)) },
                            isError = state.hasError(EntryField.CATEGORY)
                        )
                    } else null,
                    targetAccountSelector = if (state.entryMode == EntryMode.TRANSFER) {
                        AeonSelectorState(
                            selection = state.targetAccount,
                            onClick = { onEvent(PlannedPaymentEntryEvent.ShowTargetAccountSheet(true)) },
                            isError = state.hasError(EntryField.TARGET_ACCOUNT)
                        )
                    } else null
                )
            }

            // DETAIL: Metadata section
            AeonDetailSection(
                note = state.note,
                onNoteChange = { onEvent(PlannedPaymentEntryEvent.SetNote(it)) },
                payee = state.payee,
                onPayeeChange = { onEvent(PlannedPaymentEntryEvent.SetPayee(it)) },
                selectedNames = state.selectedTags,
                allAvailableTags = state.allTags,
                onTagClick = { onEvent(PlannedPaymentEntryEvent.ShowTagSheet(true)) },
                onRemoveTag = { tag -> onEvent(PlannedPaymentEntryEvent.SetTags(state.selectedTags - tag)) },
                onTagsSelected = { onEvent(PlannedPaymentEntryEvent.SetTags(it)) },
                onAddTag = { name, _ -> onEvent(PlannedPaymentEntryEvent.SetTags(state.selectedTags + name)) },
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
                                PlannedPaymentEntryEvent.ShowLinkedTransactionSheet(
                                    true
                                )
                            )
                        },
                        onRemoveLink = { txId ->
                            onEvent(
                                PlannedPaymentEntryEvent.RemoveLinkedTransaction(
                                    state.plannedPaymentId ?: -1L,
                                    txId
                                )
                            )
                        },
                        onPaymentMethodClick = {
                            onEvent(
                                PlannedPaymentEntryEvent.ShowPaymentMethodSheet(
                                    true
                                )
                            )
                        },
                        onStatusClick = { onEvent(PlannedPaymentEntryEvent.SetTransactionStatus(it)) },
                        onWarrantyToggle = { onEvent(PlannedPaymentEntryEvent.SetWarrantyEnabled(it)) },
                        onWarrantyDateClick = {
                            onEvent(
                                PlannedPaymentEntryEvent.ShowWarrantyDatePicker(
                                    true
                                )
                            )
                        },
                        onLocationClick = { onEvent(PlannedPaymentEntryEvent.ShowLocationSheet(true)) },
                        locationLabel = "Payment Location"
                    )
                }
            )

            // MANAGEMENT: Plan specific settings
            EntrySection(
                title = "PLAN MANAGEMENT",
                icon = Icons.Rounded.Settings
            ) {
                // 1. Planned Payment Status Row
                val ppStatusColor =
                    Color(android.graphics.Color.parseColor(state.status.colorHex))
                val ppStatusIcon =
                    com.osscabinet.ossWallet.ui.components.AppIconMap[state.status.iconName]
                        ?: Icons.Rounded.CheckCircle

                EntryActionChip(
                    icon = ppStatusIcon,
                    label = "Payment Status: ${state.status.displayName}",
                    onClick = { onEvent(PlannedPaymentEntryEvent.ShowStatusSheet(true)) },
                    modifier = Modifier.fillMaxWidth(),
                    iconTint = ppStatusColor,
                    iconContainerColor = AppColors.surfaceContainerHigh
                )

                // 2. End Date and Auto Confirmation
                EntrySwitchRow(
                    label = "Has End Date",
                    checked = state.hasEndDate,
                    onCheckedChange = { onEvent(PlannedPaymentEntryEvent.SetHasEndDate(it)) },
                    icon = Icons.Rounded.EventBusy,
                    isFlush = true
                )

                if (state.hasEndDate) {
                    EntryDateCard(
                        icon = Icons.Rounded.CalendarToday,
                        label = "END DATE",
                        dateMillis = state.endDateMillis,
                        onClick = { onEvent(PlannedPaymentEntryEvent.ShowEndDatePicker(true)) }
                    )
                }

                EntrySwitchRow(
                    label = "Auto-confirm Transaction",
                    checked = state.autoConfirm,
                    onCheckedChange = { onEvent(PlannedPaymentEntryEvent.SetAutoConfirm(it)) },
                    icon = Icons.Rounded.AutoFixHigh,
                    isFlush = true
                )
            }
            Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
        }

        PlannedPaymentSheetOrchestrator(
            state = state,
            onSearch = onSearch,
            onEvent = onEvent
        )
    }
}

@Composable
private fun PlannedPaymentSheetOrchestrator(
    state: PlannedPaymentEntryState,
    onSearch: (String) -> kotlinx.coroutines.flow.Flow<List<com.osscabinet.ossWallet.domain.model.Transaction>>,
    onEvent: (PlannedPaymentEntryEvent) -> Unit
) {
    // Schedule builder is unique to planned payments, kept here
    if (state.showScheduleBuilder) {
        com.osscabinet.ossWallet.ui.components.entry.AeonScheduleBuilder(
            initialStartDate = state.startDateMillis,
            initialRecurrenceType = state.recurrenceType,
            initialInterval = state.recurrenceInterval,
            initialDayOfWeek = state.dayOfWeek,
            initialDayOfMonth = state.dayOfMonth,
            initialCronExpression = state.cronExpression,
            onConfirm = { start, type, interval, dow, dom, cron ->
                onEvent(
                    PlannedPaymentEntryEvent.SetRecurrence(
                        start,
                        type,
                        interval,
                        dow,
                        dom,
                        cron
                    )
                )
                onEvent(PlannedPaymentEntryEvent.ShowScheduleBuilder(false))
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowScheduleBuilder(false)) }
        )
    }

    if (state.showEndDatePicker) {
        com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog(
            selectedDateMillis = state.endDateMillis,
            onDateSelected = {
                onEvent(PlannedPaymentEntryEvent.SetEndDate(it))
                onEvent(PlannedPaymentEntryEvent.ShowEndDatePicker(false))
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowEndDatePicker(false)) }
        )
    }

    if (state.showTemplateSheet) {
        com.osscabinet.ossWallet.ui.components.entry.AeonTemplateSelection(
            templates = state.templates,
            categories = state.categories,
            onTemplateSelected = { template ->
                onEvent(PlannedPaymentEntryEvent.SetLinkedTemplate(template))
                onEvent(PlannedPaymentEntryEvent.ShowTemplateSheet(false))
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowTemplateSheet(false)) }
        )
    }

    if (state.showStatusSheet) {
        AeonPlannedPaymentStatusSelection(
            selectedStatus = state.status,
            onStatusSelected = { status ->
                onEvent(PlannedPaymentEntryEvent.SetStatus(status))
                onEvent(PlannedPaymentEntryEvent.ShowStatusSheet(false))
            },
            onDismiss = { onEvent(PlannedPaymentEntryEvent.ShowStatusSheet(false)) }
        )
    }

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
                onEvent(PlannedPaymentEntryEvent.SetAmount(amt))
            } else {
                onEvent(PlannedPaymentEntryEvent.SetTargetAmount(amt))
            }
        },
        onAccountChange = { accId -> onEvent(PlannedPaymentEntryEvent.SetAccountId(accId)) },
        onTargetAccountChange = { accId -> onEvent(PlannedPaymentEntryEvent.SetTargetAccountId(accId)) },
        onCategoryChange = { catId -> onEvent(PlannedPaymentEntryEvent.SetCategoryId(catId)) },
        onCurrencyChange = { curr -> onEvent(PlannedPaymentEntryEvent.SetCurrency(curr)) },
        onTargetCurrencyChange = { curr -> onEvent(PlannedPaymentEntryEvent.SetTargetCurrency(curr)) },
        onExchangeRateChange = { rate -> onEvent(PlannedPaymentEntryEvent.SetExchangeRate(rate)) },
        onTagsChange = { tags -> onEvent(PlannedPaymentEntryEvent.SetTags(tags)) },
        onAddTag = { name, _ -> onEvent(PlannedPaymentEntryEvent.SetTags(state.selectedTags + name)) },
        onDismissSheet = { field ->
            when (field) {
                EntryField.AMOUNT -> onEvent(PlannedPaymentEntryEvent.ShowNumpad(false))
                EntryField.ACCOUNT -> onEvent(PlannedPaymentEntryEvent.ShowAccountSheet(false))
                EntryField.TARGET_ACCOUNT -> onEvent(
                    PlannedPaymentEntryEvent.ShowTargetAccountSheet(
                        false
                    )
                )

                EntryField.CATEGORY -> onEvent(PlannedPaymentEntryEvent.ShowCategorySheet(false))
                else -> onEvent(PlannedPaymentEntryEvent.DismissAllSheets)
            }
        },
        onSearchTransactions = onSearch,
        linkedIds = state.displayLinkedTxs.map { it.id }.toSet(),
        onLinkedTransactionsChanged = { ids ->
            onEvent(
                PlannedPaymentEntryEvent.SetLinkedTransactionIds(
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
        onWarrantyDateChange = { date -> onEvent(PlannedPaymentEntryEvent.SetWarrantyExpiration(date)) },
        gpsLocation = state.gpsLocation,
        onLocationChange = { gps, loc -> onEvent(PlannedPaymentEntryEvent.SetLocation(gps, loc)) },
        paymentMethod = state.paymentMethod,
        onPaymentMethodChange = { method -> onEvent(PlannedPaymentEntryEvent.SetPaymentMethod(method)) },
        transactionStatus = state.transactionStatus,
        onTransactionStatusChange = { status ->
            onEvent(
                PlannedPaymentEntryEvent.SetTransactionStatus(
                    status
                )
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PlannedPaymentFormBodyPreview() {
    OssWalletTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PlannedPaymentFormBody(
                state = PlannedPaymentEntryState(
                    name = "Netflix Subscription",
                    source = TransactionSide(
                        amount = "15.99",
                        currency = "USD"
                    )
                ),
                onSearch = { flowOf(emptyList()) },
                onEvent = {}
            )
        }
    }
}
