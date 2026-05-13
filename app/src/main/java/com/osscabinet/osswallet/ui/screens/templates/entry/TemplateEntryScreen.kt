package com.osscabinet.ossWallet.ui.screens.templates.entry

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.screens.templates.entry.components.TemplateEntrySheets
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.templates.entry.components.TemplateFormBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEntryScreen(
    templateId: Long? = null,
    prefill: com.osscabinet.ossWallet.domain.model.dto.TemplatePrefillDTO? = null,
    viewModel: TemplateEntryViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(templateId, prefill) {
        viewModel.initialize(templateId, prefill)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            onBackClick()
        }
    }

    AeonEntryScaffold(
        title = if (state.templateId == null) "New Template" else "Edit Template",
        onBackClick = onBackClick,
        onSaveClick = {
            viewModel.saveTemplate { 
                viewModel.onEvent(TemplateEntryEvent.TriggerNavigation)
            }
        },
        canSave = state.canSave && !state.navTriggered,
        isEditMode = state.isEditMode,
        onDeleteClick = if (state.isEditMode) {
            {
                viewModel.deleteTemplate {
                    onBackClick()
                }
            }
        } else null
    ) { innerPadding ->
        TemplateFormBody(
            name = state.name,
            onNameChange = { viewModel.onEvent(TemplateEntryEvent.SetName(it)) },
            transactionType = state.transactionType,
            onTypeSelected = { viewModel.onEvent(TemplateEntryEvent.SetTransactionType(it)) },
            amount = state.amount,
            currency = state.currency,
            onAmountClick = { viewModel.onEvent(TemplateEntryEvent.ShowNumpad(true)) },
            selectedAccount = state.selectedAccount,
            onAccountClick = { viewModel.onEvent(TemplateEntryEvent.ShowAccountSheet(true)) },
            targetAccount = state.selectedTargetAccount,
            onTargetAccountClick = { viewModel.onEvent(TemplateEntryEvent.ShowTargetAccountSheet(true)) },
            categoryName = state.categoryName,
            categoryObj = state.selectedCategory,
            onCategoryClick = { viewModel.onEvent(TemplateEntryEvent.ShowCategorySheet(true)) },
            payee = state.payee,
            onPayeeChange = { viewModel.onEvent(TemplateEntryEvent.SetPayee(it)) },
            uniquePayees = state.uniquePayees,
            tags = state.tags,
            onShowTagSheet = { viewModel.onEvent(TemplateEntryEvent.ShowTagSheet(true)) },
            onRemoveTag = { tag -> viewModel.onEvent(TemplateEntryEvent.SetTags(state.tags - tag)) },
            paymentMethod = state.paymentMethod,
            onPaymentMethodClick = { viewModel.onEvent(TemplateEntryEvent.ShowPaymentMethodSheet(true)) },
            location = state.location,
            onLocationChange = { viewModel.onEvent(TemplateEntryEvent.SetLocation(it)) },
            note = state.note,
            onNoteChange = { viewModel.onEvent(TemplateEntryEvent.SetNote(it)) },
            uniqueNotes = state.uniqueNotes,
            transferRate = state.transferRate,
            targetCurrency = state.targetCurrency,
            onTargetCurrencyClick = { viewModel.onEvent(TemplateEntryEvent.ShowTargetCurrencySheet(true)) },
            onTransferRateClick = { viewModel.onEvent(TemplateEntryEvent.ShowExchangeRateDialog(true)) },
            onCurrencyClick = { viewModel.onEvent(TemplateEntryEvent.ShowCurrencySheet(true)) },
            modifier = Modifier.padding(innerPadding)
        )

        TemplateEntrySheets(
            isNumpadVisible = state.isNumpadVisible,
            onDismissNumpad = { viewModel.onEvent(TemplateEntryEvent.ShowNumpad(false)) },
            amount = state.amount,
            onAmountChange = { viewModel.onEvent(TemplateEntryEvent.SetAmount(it)) },
            onTypeChangedFromNumpad = { newType ->
                val type = when (newType) {
                    "income" -> 1
                    "transfer" -> 2
                    else -> 0
                }
                viewModel.onEvent(TemplateEntryEvent.SetTransactionType(type))
            },
            transactionType = state.transactionType,
            decimalPrecision = state.decimalPrecision,
            showAccountSheet = state.showAccountSheet,
            onDismissAccountSheet = { viewModel.onEvent(TemplateEntryEvent.ShowAccountSheet(false)) },
            showTargetAccountSheet = state.showTargetAccountSheet,
            onDismissTargetAccountSheet = { viewModel.onEvent(TemplateEntryEvent.ShowTargetAccountSheet(false)) },
            accounts = state.accounts,
            selectedAccount = state.selectedAccount,
            onAccountSelected = {
                viewModel.onEvent(TemplateEntryEvent.SetAccount(it.id))
                viewModel.onEvent(TemplateEntryEvent.ShowAccountSheet(false))
            },
            targetAccount = state.selectedTargetAccount,
            onTargetAccountSelected = {
                viewModel.onEvent(TemplateEntryEvent.SetTargetAccount(it.id))
                viewModel.onEvent(TemplateEntryEvent.ShowTargetAccountSheet(false))
            },
            showCategorySheet = state.showCategorySheet,
            onDismissCategorySheet = { viewModel.onEvent(TemplateEntryEvent.ShowCategorySheet(false)) },
            categories = state.categories,
            selectedCategoryId = state.categoryId,
            onCategorySelected = { _, id ->
                viewModel.onEvent(TemplateEntryEvent.SetCategory(id))
                viewModel.onEvent(TemplateEntryEvent.ShowCategorySheet(false))
            },
            showPaymentMethodSheet = state.showPaymentMethodSheet,
            onDismissPaymentMethodSheet = { viewModel.onEvent(TemplateEntryEvent.ShowPaymentMethodSheet(false)) },
            paymentMethod = state.paymentMethod,
            onPaymentMethodSelected = { 
                viewModel.onEvent(TemplateEntryEvent.SetPaymentMethod(it))
                viewModel.onEvent(TemplateEntryEvent.ShowPaymentMethodSheet(false))
            },
            showTagSheet = state.showTagSheet,
            onDismissTagSheet = { viewModel.onEvent(TemplateEntryEvent.ShowTagSheet(false)) },
            allTags = state.allTags,
            selectedTags = state.tags,
            onTagsSelected = { viewModel.onEvent(TemplateEntryEvent.SetTags(it)) },
            onAddTag = { name, color -> viewModel.onEvent(TemplateEntryEvent.AddTag(name, color)) },
            onEditTag = { viewModel.onEvent(TemplateEntryEvent.UpdateTag(it)) },
            onDeleteTag = { viewModel.onEvent(TemplateEntryEvent.DeleteTag(it)) },
            showCurrencySheet = state.showCurrencySheet,
            onDismissCurrencySheet = { viewModel.onEvent(TemplateEntryEvent.ShowCurrencySheet(false)) },
            currency = state.currency,
            onCurrencySelected = { 
                viewModel.onEvent(TemplateEntryEvent.SetCurrency(it))
                viewModel.onEvent(TemplateEntryEvent.ShowCurrencySheet(false))
            },
            showTargetCurrencySheet = state.showTargetCurrencySheet,
            onDismissTargetCurrencySheet = { viewModel.onEvent(TemplateEntryEvent.ShowTargetCurrencySheet(false)) },
            targetCurrency = state.targetCurrency ?: "USD",
            onTargetCurrencySelected = { 
                viewModel.onEvent(TemplateEntryEvent.SetTargetCurrency(it))
                viewModel.onEvent(TemplateEntryEvent.ShowTargetCurrencySheet(false))
            },
            showExchangeRateDialog = state.showExchangeRateDialog,
            onDismissExchangeRateDialog = { viewModel.onEvent(TemplateEntryEvent.ShowExchangeRateDialog(false)) },
            exchangeRate = state.transferRate,
            onExchangeRateChanged = { viewModel.onEvent(TemplateEntryEvent.SetTransferRate(it)) }
        )
    }
}
