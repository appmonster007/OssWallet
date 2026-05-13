package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components.PlannedPaymentEntrySheets
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components.PlannedPaymentFormBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentEntryScreen(
    plannedPaymentId: Long? = null,
    prefill: com.osscabinet.ossWallet.domain.model.dto.PlannedPaymentPrefillDTO? = null,
    viewModel: PlannedPaymentEntryViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(plannedPaymentId, prefill) {
        viewModel.initialize(plannedPaymentId, prefill)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            onBackClick()
        }
    }

    val allCurrencies = remember { CurrencyService.getAllCurrencies() }

    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Payment" else "New Payment",
        onBackClick = onBackClick,
        onSaveClick = {
            viewModel.savePlannedPayment {
                viewModel.onEvent(PlannedPaymentEntryEvent.TriggerNavigation)
            }
        },
        canSave = state.canSave && !state.navTriggered,
        isEditMode = state.isEditMode,
        onDeleteClick = if (state.isEditMode) {
            { viewModel.onEvent(PlannedPaymentEntryEvent.ShowDeleteDialog(true)) }
        } else null
    ) { innerPadding ->
        PlannedPaymentFormBody(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(innerPadding)
        )

        PlannedPaymentEntrySheets(
            showScheduleBuilder = state.showScheduleBuilder,
            onDismissScheduleBuilder = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowScheduleBuilder(false)) },
            startDateMillis = state.startDateMillis,
            recurrenceType = state.recurrenceType,
            recurrenceInterval = state.recurrenceInterval,
            dayOfWeek = state.dayOfWeek,
            dayOfMonth = state.dayOfMonth,
            cronExpression = state.cronExpression,
            onScheduleConfirm = { start, type, interval, dow, dom, cron ->
                viewModel.onEvent(PlannedPaymentEntryEvent.SetRecurrence(start, type, interval, dow, dom, cron))
                viewModel.onEvent(PlannedPaymentEntryEvent.ShowScheduleBuilder(false))
            },
            showEndDatePicker = state.showEndDatePicker,
            onDismissEndDatePicker = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowEndDatePicker(false)) },
            endDateMillis = state.endDateMillis,
            onEndDateSelected = { 
                viewModel.onEvent(PlannedPaymentEntryEvent.SetEndDate(it))
                viewModel.onEvent(PlannedPaymentEntryEvent.ShowEndDatePicker(false))
            },
            showCategorySheet = state.showCategorySheet,
            onDismissCategorySheet = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowCategorySheet(false)) },
            categories = state.categories,
            selectedCategoryId = state.selectedCategoryId,
            onCategorySelected = { category ->
                viewModel.onEvent(PlannedPaymentEntryEvent.SetCategoryId(category.id))
                viewModel.onEvent(PlannedPaymentEntryEvent.ShowCategorySheet(false))
            },
            showAccountSheet = state.showAccountSheet,
            onDismissAccountSheet = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowAccountSheet(false)) },
            showTargetAccountSheet = state.showTargetAccountSheet,
            onDismissTargetAccountSheet = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowTargetAccountSheet(false)) },
            accounts = state.accounts,
            selectedAccountId = state.selectedAccountId,
            onAccountSelected = { account ->
                viewModel.onEvent(PlannedPaymentEntryEvent.SetAccountId(account?.id))
                viewModel.onEvent(PlannedPaymentEntryEvent.ShowAccountSheet(false))
            },
            targetAccountId = state.targetAccountId,
            onTargetAccountSelected = { account ->
                viewModel.onEvent(PlannedPaymentEntryEvent.SetTargetAccountId(account?.id))
                viewModel.onEvent(PlannedPaymentEntryEvent.ShowTargetAccountSheet(false))
            },
            showPaymentMethodSheet = state.showPaymentMethodSheet,
            onDismissPaymentMethodSheet = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowPaymentMethodSheet(false)) },
            paymentMethod = state.paymentMethod,
            onPaymentMethodSelected = { 
                viewModel.onEvent(PlannedPaymentEntryEvent.SetPaymentMethod(it))
                viewModel.onEvent(PlannedPaymentEntryEvent.ShowPaymentMethodSheet(false))
            },
            showTagSheet = state.showTagSheet,
            onDismissTagSheet = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowTagSheet(false)) },
            availableTags = state.availableTags,
            selectedTagNames = state.selectedTagNames,
            onTagsSelected = { 
                viewModel.onEvent(PlannedPaymentEntryEvent.SetTags(it))
                viewModel.onEvent(PlannedPaymentEntryEvent.ShowTagSheet(false))
            },
            showStatusSheet = state.showStatusSheet,
            onDismissStatusSheet = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowStatusSheet(false)) },
            status = state.status,
            onStatusSelected = { 
                viewModel.onEvent(PlannedPaymentEntryEvent.SetStatus(it))
                viewModel.onEvent(PlannedPaymentEntryEvent.ShowStatusSheet(false))
            },
            showNumpad = state.showNumpad,
            onDismissNumpad = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowNumpad(false)) },
            amountText = state.amount,
            transactionType = state.transactionType,
            onAmountChanged = { newAmount ->
                viewModel.onEvent(PlannedPaymentEntryEvent.SetAmountText(newAmount))
            },
            onTypeChangedFromNumpad = { newType ->
                val type = when (newType) {
                    "income" -> 1
                    "transfer" -> 2
                    else -> 0
                }
                viewModel.onEvent(PlannedPaymentEntryEvent.SetTransactionType(type))
            },
            showCurrencySheet = state.showCurrencySheet,
            onDismissCurrencySheet = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowCurrencySheet(false)) },
            currency = state.currency,
            onCurrencySelected = { 
                viewModel.onEvent(PlannedPaymentEntryEvent.SetCurrency(it))
                viewModel.onEvent(PlannedPaymentEntryEvent.ShowCurrencySheet(false))
            },
            showTargetCurrencySheet = state.showTargetCurrencySheet,
            onDismissTargetCurrencySheet = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowTargetCurrencySheet(false)) },
            targetCurrency = state.targetCurrency ?: "USD",
            onTargetCurrencySelected = { 
                viewModel.onEvent(PlannedPaymentEntryEvent.SetTargetCurrency(it))
                viewModel.onEvent(PlannedPaymentEntryEvent.ShowTargetCurrencySheet(false))
            },
            allCurrencies = allCurrencies,
            showTemplateSheet = state.showTemplateSheet,
            onDismissTemplateSheet = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowTemplateSheet(false)) },
            templates = state.templates,
            onTemplateSelected = { template ->
                viewModel.onEvent(PlannedPaymentEntryEvent.SetLinkedTemplate(template))
                viewModel.onEvent(PlannedPaymentEntryEvent.ShowTemplateSheet(false))
            },
            showExchangeRateDialog = state.showExchangeRateDialog,
            onDismissExchangeRateDialog = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowExchangeRateDialog(false)) },
            exchangeRate = state.transferRate,
            onExchangeRateChanged = { viewModel.onEvent(PlannedPaymentEntryEvent.SetTransferRate(it)) }
        )

        AeonConfirmDeleteDialog(
            show = state.showDeleteDialog,
            title = "Delete Planned Payment",
            message = "Are you sure you want to delete this planned payment?",
            onConfirm = {
                viewModel.deletePlannedPayment {
                    onBackClick()
                }
            },
            onDismiss = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowDeleteDialog(false)) },
            extraContent = {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.deleteTemplateWithPayment,
                            onCheckedChange = { viewModel.onEvent(PlannedPaymentEntryEvent.SetDeleteTemplateWithPayment(it)) }
                        )
                        Text("Also delete linked template", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    if (state.deleteTemplateWithPayment && state.templateUsageCount > 1) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Warning: This template is shared with ${state.templateUsageCount - 1} other payment(s). Deleting it will also delete those payments.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
