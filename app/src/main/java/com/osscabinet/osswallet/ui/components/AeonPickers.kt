package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.ui.components.AeonHierarchicalHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerSheet(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    com.osscabinet.ossWallet.ui.components.accounts.AccountPickerSheet(
        accounts = accounts,
        selectedAccount = selectedAccount,
        onAccountSelected = onAccountSelected,
        onDismiss = onDismiss
    )
}

@Composable
fun ExchangeRateDialog(
    initialRate: BigDecimal,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal) -> Unit
) {
    var rate by remember { mutableStateOf(initialRate.toPlainString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exchange Rate", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = rate,
                onValueChange = { rate = it },
                label = { Text("Rate (Source to Target)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(rate.toBigDecimalOrNull() ?: BigDecimal.ONE) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LabelPickerSheet(
    selectedTags: List<String>,
    onTagsChanged: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    allTags: List<Tag>,
    onAddTag: (String) -> Unit
) {
    var tagInput by remember { mutableStateOf("") }
    val sortedTags = remember(allTags) { allTags.sortedBy { it.name.lowercase() } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth()
        ) {
            AeonSectionHeader(title = "Labels")
            Spacer(modifier = Modifier.height(16.dp))

            // Current Tags
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                selectedTags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { onTagsChanged(selectedTags - tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (selectedTags.isNotEmpty()) Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add custom label (multi-word)") },
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (tagInput.isNotBlank()) {
                                val newTag = tagInput.trim()
                                if (newTag !in allTags.map { it.name }) {
                                    onAddTag(newTag)
                                }
                                onTagsChanged((selectedTags + newTag).distinct())
                                tagInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add")
                    }
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "SUGGESTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                sortedTags.filter { it.name !in selectedTags }.forEach { tag ->
                    SuggestionChip(
                        onClick = { onTagsChanged((selectedTags + tag.name).distinct()) },
                        label = { Text(tag.name) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodPickerSheet(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val methods = listOf(
        "Cash" to Icons.Rounded.Payments,
        "Credit Card" to Icons.Rounded.CreditCard,
        "Debit Card" to Icons.Rounded.CreditCard,
        "Bank Transfer" to Icons.Rounded.AccountBalance,
        "Digital Wallet" to Icons.Rounded.Wallet,
        "Mobile Payment" to Icons.Rounded.Contactless,
        "Check" to Icons.Rounded.Description,
        "Cryptocurrency" to Icons.Rounded.CurrencyBitcoin,
        "Other" to Icons.Rounded.MoreHoriz
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            AeonSectionHeader(title = "Payment Method")
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.weight(1f, fill = false)) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AeonHybridSelector(
                        items = methods,
                        mode = AeonSelectMode.SINGLE,
                        selectedIds = setOf(selectedMethod),
                        onSelectionChange = { ids ->
                            ids.firstOrNull()?.let { 
                                onMethodSelected(it)
                                onDismiss()
                            }
                        },
                        idMapper = { it.first },
                        labelMapper = { it.first },
                        iconMapper = { it.second }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            AeonSectionHeader(title = "Classification")
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(modifier = Modifier.weight(1f, fill = false)) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AeonHierarchicalHybridSelector(
                        categories = categories,
                        mode = AeonSelectMode.SINGLE,
                        selectedIds = if (selectedCategoryId != null) setOf(selectedCategoryId) else emptySet(),
                        onSelectionChange = { ids ->
                            val selectedId = ids.firstOrNull()
                            if (selectedId != null) {
                                val category = categories.find { it.id == selectedId }
                                if (category != null) {
                                    onCategorySelected(category)
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
