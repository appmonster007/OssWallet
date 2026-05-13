package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.ui.components.accounts.AccountSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionAccountSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionCategorySelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.PaymentMethodSelection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.TagSelection

@Composable
fun TemplateEntrySheets(
    isNumpadVisible: Boolean,
    onDismissNumpad: () -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    onTypeChangedFromNumpad: (String) -> Unit = {},
    transactionType: Int,
    decimalPrecision: Int,
    showAccountSheet: Boolean,
    onDismissAccountSheet: () -> Unit,
    showTargetAccountSheet: Boolean = false,
    onDismissTargetAccountSheet: () -> Unit = {},
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit,
    targetAccount: Account? = null,
    onTargetAccountSelected: (Account?) -> Unit = {},
    showCategorySheet: Boolean,
    onDismissCategorySheet: () -> Unit,
    categories: List<Category>,
    selectedCategoryId: Long,
    onCategorySelected: (String, Long) -> Unit,
    showPaymentMethodSheet: Boolean,
    onDismissPaymentMethodSheet: () -> Unit,
    paymentMethod: String,
    onPaymentMethodSelected: (String) -> Unit,
    showTagSheet: Boolean,
    onDismissTagSheet: () -> Unit,
    allTags: List<Tag>,
    selectedTags: List<String>,
    onTagsSelected: (List<String>) -> Unit,
    onAddTag: (String, Int) -> Unit,
    onEditTag: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    showCurrencySheet: Boolean = false,
    onDismissCurrencySheet: () -> Unit = {},
    currency: String = "USD",
    onCurrencySelected: (String) -> Unit = {},
    showTargetCurrencySheet: Boolean = false,
    onDismissTargetCurrencySheet: () -> Unit = {},
    targetCurrency: String = "USD",
    onTargetCurrencySelected: (String) -> Unit = {},
    showExchangeRateDialog: Boolean = false,
    onDismissExchangeRateDialog: () -> Unit = {},
    exchangeRate: java.math.BigDecimal = java.math.BigDecimal.ONE,
    onExchangeRateChanged: (java.math.BigDecimal) -> Unit = {}
) {
    if (isNumpadVisible) {
        com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.CalculatorSheet(
            currentAmount = amount,
            transactionType = transactionType,
            decimalPrecision = decimalPrecision,
            onAmountChanged = onAmountChange,
            onTypeChanged = onTypeChangedFromNumpad,
            onDismiss = onDismissNumpad
        )
    }

    if (showAccountSheet) {
        TransactionAccountSelection(
            accounts = accounts,
            selectedAccountId = selectedAccount?.id ?: "",
            onAccountSelected = { it?.let { onAccountSelected(it) } },
            onDismiss = onDismissAccountSheet,
            title = "Select Account"
        )
    }

    if (showTargetAccountSheet) {
        TransactionAccountSelection(
            accounts = accounts,
            selectedAccountId = targetAccount?.id ?: "",
            onAccountSelected = onTargetAccountSelected,
            onDismiss = onDismissTargetAccountSheet,
            title = "Select Target Account",
            allowOutsideWallet = true
        )
    }

    if (showCategorySheet) {
        TransactionCategorySelection(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = onCategorySelected,
            onDismiss = onDismissCategorySheet
        )
    }

    if (showPaymentMethodSheet) {
        PaymentMethodSelection(
            selectedMethod = paymentMethod,
            onMethodSelected = onPaymentMethodSelected,
            onDismiss = onDismissPaymentMethodSheet
        )
    }

    if (showTagSheet) {
        TagSelection(
            tags = allTags,
            selectedTags = selectedTags,
            onTagsSelected = onTagsSelected,
            onAddTag = onAddTag,
            onEditTag = onEditTag,
            onDeleteTag = onDeleteTag,
            onDismiss = onDismissTagSheet
        )
    }

    if (showCurrencySheet) {
        com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog(onDismissRequest = onDismissCurrencySheet) {
            com.osscabinet.ossWallet.ui.components.AeonSelectionScreen(
                title = "Select Currency",
                onBack = onDismissCurrencySheet
            ) { padding ->
                val allCurrencies = com.osscabinet.ossWallet.domain.service.currency.CurrencyService.getAllCurrencies()
                var query by remember { mutableStateOf("") }
                val filtered = remember(query) {
                    if (query.isBlank()) allCurrencies
                    else allCurrencies.filter { it.contains(query, ignoreCase = true) }
                }

                Column(modifier = Modifier.padding(padding).padding(horizontal = 24.dp)) {
                    com.osscabinet.ossWallet.ui.components.AeonSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = "Search currency..."
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            com.osscabinet.ossWallet.ui.components.AeonHybridSelector(
                                items = filtered,
                                mode = com.osscabinet.ossWallet.ui.components.AeonSelectMode.SINGLE,
                                selectedIds = setOf(currency),
                                onSelectionChange = { ids ->
                                    ids.firstOrNull()?.let { onCurrencySelected(it) }
                                    onDismissCurrencySheet()
                                },
                                idMapper = { it },
                                labelMapper = { it },
                                iconMapper = { Icons.Rounded.CurrencyExchange }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTargetCurrencySheet) {
        com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog(onDismissRequest = onDismissTargetCurrencySheet) {
            com.osscabinet.ossWallet.ui.components.AeonSelectionScreen(
                title = "Select Target Currency",
                onBack = onDismissTargetCurrencySheet
            ) { padding ->
                val allCurrencies = com.osscabinet.ossWallet.domain.service.currency.CurrencyService.getAllCurrencies()
                var query by remember { mutableStateOf("") }
                val filtered = remember(query) {
                    if (query.isBlank()) allCurrencies
                    else allCurrencies.filter { it.contains(query, ignoreCase = true) }
                }

                Column(modifier = Modifier.padding(padding).padding(horizontal = 24.dp)) {
                    com.osscabinet.ossWallet.ui.components.AeonSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = "Search currency..."
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            com.osscabinet.ossWallet.ui.components.AeonHybridSelector(
                                items = filtered,
                                mode = com.osscabinet.ossWallet.ui.components.AeonSelectMode.SINGLE,
                                selectedIds = setOf(targetCurrency),
                                onSelectionChange = { ids ->
                                    ids.firstOrNull()?.let { onTargetCurrencySelected(it) }
                                    onDismissTargetCurrencySheet()
                                },
                                idMapper = { it },
                                labelMapper = { it },
                                iconMapper = { Icons.Rounded.CurrencyExchange }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExchangeRateDialog) {
        com.osscabinet.ossWallet.ui.components.ExchangeRateDialog(
            initialRate = exchangeRate,
            onConfirm = onExchangeRateChanged,
            onDismiss = onDismissExchangeRateDialog
        )
    }
}
