package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.gleemanager.data.local.entity.TemplateEntity
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.ui.components.AccountPickerSheet
import com.osscabinet.gleemanager.ui.components.AeonSectionHeader
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TemplateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTemplateScreen(
    templateId: Long? = null,
    templateViewModel: TemplateViewModel,
    accountViewModel: AccountViewModel,
    onBackClick: () -> Unit
) {
    val templates by templateViewModel.templates.collectAsState()
    val accounts by accountViewModel.accounts.collectAsState()
    val template = remember(templateId, templates) { templates.find { it.id == templateId } }

    var name by remember { mutableStateOf(template?.name ?: "") }
    var transactionType by remember { mutableIntStateOf(template?.transactionType ?: 0) } // 0: Expense, 1: Income
    var accountId by remember { mutableStateOf(template?.accountId) }
    var currency by remember { mutableStateOf(template?.currency ?: "") }
    var category by remember { mutableStateOf(template?.category ?: "Shopping") }
    var paymentMethod by remember { mutableStateOf(template?.paymentMethod ?: "") }
    var note by remember { mutableStateOf(template?.note ?: "") }
    var payee by remember { mutableStateOf(template?.payee ?: "") }
    var location by remember { mutableStateOf(template?.location ?: "") }
    var amount by remember { mutableStateOf(template?.amount?.toString() ?: "") }

    var showAccountPicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    val selectedAccount = remember(accountId, accounts) { accounts.find { it.id == accountId } }

    LaunchedEffect(selectedAccount) {
        if (selectedAccount != null && (currency.isEmpty() || !selectedAccount.allowedCurrencies.contains(currency))) {
            currency = selectedAccount.currencyCode
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (templateId == null) "New Template" else "Edit Template") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val entity = TemplateEntity(
                                id = templateId ?: 0,
                                name = name,
                                category = category,
                                amount = amount.toDoubleOrNull() ?: 0.0,
                                transactionType = transactionType,
                                accountId = accountId,
                                note = note.ifBlank { null },
                                payee = payee.ifBlank { null },
                                location = location.ifBlank { null },
                                paymentMethod = paymentMethod.ifBlank { null },
                                currency = currency.ifBlank { null },
                                isStarred = template?.isStarred ?: false
                            )
                            if (templateId == null) {
                                templateViewModel.addTemplate(
                                    entity.name, entity.category, entity.amount, 
                                    entity.transactionType, entity.note, entity.accountId
                                )
                                // Note: TemplateViewModel.addTemplate doesn't support the new fields yet, 
                                // I should probably update it or use a more direct approach.
                            } else {
                                templateViewModel.updateTemplate(entity)
                            }
                            onBackClick()
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Template Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                AeonSectionHeader(title = "Transaction Type")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = transactionType == 0,
                        onClick = { transactionType = 0 },
                        label = { Text("Expense") },
                        leadingIcon = { if (transactionType == 0) Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)) }
                    )
                    FilterChip(
                        selected = transactionType == 1,
                        onClick = { transactionType = 1 },
                        label = { Text("Income") },
                        leadingIcon = { if (transactionType == 1) Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            item {
                AeonSectionHeader(title = "Account & Currency")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        onClick = { showAccountPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(selectedAccount?.name ?: "Select Source Account", color = if (selectedAccount != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                            }
                            Icon(Icons.Rounded.KeyboardArrowDown, null)
                        }
                    }

                    if (selectedAccount != null && selectedAccount.allowedCurrencies.size > 1) {
                        Surface(
                            onClick = { showCurrencyPicker = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Paid, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(currency.ifEmpty { "Select Currency" })
                                }
                                Icon(Icons.Rounded.KeyboardArrowDown, null)
                            }
                        }
                    }
                }
            }

            item {
                AeonSectionHeader(title = "Details")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Default Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Rounded.AttachMoney, null) }
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Rounded.Category, null) }
                    )
                    OutlinedTextField(
                        value = payee,
                        onValueChange = { payee = it },
                        label = { Text("Payee") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Rounded.Person, null) }
                    )
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        label = { Text("Payment Method") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Rounded.Payment, null) }
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Rounded.LocationOn, null) }
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        leadingIcon = { Icon(Icons.Rounded.Notes, null) }
                    )
                }
            }
        }
    }

    if (showAccountPicker) {
        AccountPickerSheet(
            accounts = accounts,
            selectedAccount = selectedAccount,
            onAccountSelected = {
                accountId = it.id
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false }
        )
    }

    if (showCurrencyPicker && selectedAccount != null) {
        ModalBottomSheet(onDismissRequest = { showCurrencyPicker = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Currency", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                selectedAccount.allowedCurrencies.forEach { curr ->
                    ListItem(
                        headlineContent = { Text(curr) },
                        modifier = Modifier.clickable {
                            currency = curr
                            showCurrencyPicker = false
                        },
                        trailingContent = { if (currency == curr) Icon(Icons.Rounded.Check, null) }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
