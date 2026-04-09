package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.ui.components.CalculatorDialog
import com.osscabinet.gleemanager.ui.components.OCRScanner
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel,
    accountViewModel: AccountViewModel,
    onBackClick: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Electronics") }
    var tagsInput by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    
    val accounts by accountViewModel.accounts.collectAsState()
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    
    var showCalculator by remember { mutableStateOf(false) }
    var showOCR by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        if (selectedAccount == null && accounts.isNotEmpty()) {
            selectedAccount = accounts.first()
        }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "NEW RECORD", 
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showOCR = true }) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = "Scan Receipt")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Amount Input
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "TRANSACTION AMOUNT", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 1.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        "$", 
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Light
                        )
                    )
                    TextField(
                        value = amount,
                        onValueChange = { 
                            if (it.isEmpty() || it.toDoubleOrNull() != null || it == ".") {
                                amount = it
                            }
                        },
                        placeholder = { Text("0.00", style = MaterialTheme.typography.displayMedium.copy(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))) },
                        textStyle = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.width(IntrinsicSize.Min)
                    )
                    IconButton(
                        onClick = { showCalculator = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Rounded.Calculate, contentDescription = "Calculator", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Transaction Type
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (!isIncome) Color(0xFFC62828).copy(alpha = 0.9f) else Color.Transparent)
                        .clickable { isIncome = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "EXPENSE", 
                        style = MaterialTheme.typography.labelMedium, 
                        color = if (!isIncome) Color.White else MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (isIncome) Color(0xFF006064).copy(alpha = 0.9f) else Color.Transparent)
                        .clickable { isIncome = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "INCOME", 
                        style = MaterialTheme.typography.labelMedium, 
                        color = if (isIncome) Color.White else MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                EditorialInput(
                    value = title,
                    onValueChange = { title = it },
                    label = "RECORD TITLE",
                    placeholder = "e.g. Weekly Groceries",
                    icon = Icons.Rounded.Edit
                )

                EditorialInput(
                    value = selectedAccount?.name ?: "Select Ledger",
                    onValueChange = { },
                    label = "SOURCE ACCOUNT",
                    icon = Icons.Rounded.AccountBalanceWallet,
                    readOnly = true,
                    onClick = { showAccountSheet = true }
                )

                EditorialInput(
                    value = category,
                    onValueChange = { },
                    label = "CLASSIFICATION",
                    icon = categoryIcons[category] ?: Icons.Rounded.Category,
                    readOnly = true,
                    onClick = { showCategorySheet = true }
                )

                EditorialInput(
                    value = formatter.format(Date(selectedDateMillis)),
                    onValueChange = { },
                    label = "ENTRY DATE",
                    icon = Icons.Rounded.CalendarToday,
                    readOnly = true,
                    onClick = { showDatePicker = true }
                )

                EditorialInput(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = "TAGS",
                    placeholder = "food, essentials, etc.",
                    icon = Icons.AutoMirrored.Rounded.Label
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    selectedAccount?.id?.let { accountId ->
                        val tagsList = tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        viewModel.addTransaction(
                            title = title,
                            amount = amountDouble,
                            category = category,
                            isIncome = isIncome,
                            accountId = accountId,
                            tags = tagsList,
                            timestamp = selectedDateMillis
                        )
                        // Update account balance
                        accountViewModel.addAccount(
                            selectedAccount!!.name,
                            if (isIncome) selectedAccount!!.balance + amountDouble else selectedAccount!!.balance - amountDouble,
                            selectedAccount!!.currencyCode,
                            selectedAccount!!.accountType,
                            selectedAccount!!.colorHex
                        )
                        onBackClick()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = title.isNotBlank() && amount.isNotBlank() && selectedAccount != null
            ) {
                Text(
                    "COMMIT TO LEDGER", 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }

    if (showAccountSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Text("SELECT LEDGER", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(24.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(accounts) { account ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAccount = account
                                    showAccountSheet = false
                                },
                            shape = RoundedCornerShape(24.dp),
                            color = if (selectedAccount?.id == account.id) Color(account.colorHex).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            border = if (selectedAccount?.id == account.id) BorderStroke(2.dp, Color(account.colorHex)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(account.colorHex)))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(account.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.weight(1f))
                                Text("$${String.format("%.2f", account.balance)}", style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text("CONFIRM") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("CANCEL") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Text("CLASSIFICATION", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(32.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(categoryIcons.toList()) { (name, icon) ->
                        CategoryItem(
                            name = name,
                            icon = icon,
                            isSelected = category == name,
                            onClick = {
                                category = name
                                showCategorySheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCalculator) {
        CalculatorDialog(
            initialValue = amount,
            onDismiss = { showCalculator = false },
            onConfirm = {
                amount = it
                showCalculator = false
            }
        )
    }

    if (showOCR) {
        OCRScanner(
            onResult = { result ->
                if (result != null) {
                    amount = result.toString()
                }
                showOCR = false
            },
            onDismiss = { showOCR = false }
        )
    }
}

@Composable
fun CategoryItem(name: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = if (isSelected) Color(0xFF006064) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = name,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(name, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
    }
}

val categoryIcons = mapOf(
    "Food" to Icons.Rounded.Restaurant,
    "Transport" to Icons.Rounded.DirectionsCar,
    "Shopping" to Icons.Rounded.ShoppingBag,
    "Electronics" to Icons.Rounded.Smartphone,
    "Entertainment" to Icons.Rounded.SportsEsports,
    "Health" to Icons.Rounded.MedicalServices,
    "Bills" to Icons.Rounded.Receipt,
    "Investment" to Icons.AutoMirrored.Rounded.TrendingUp,
    "Housing" to Icons.Rounded.Home,
    "Education" to Icons.Rounded.School,
    "Other" to Icons.Rounded.MoreHoriz
)

@Composable
fun EditorialInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    icon: ImageVector,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            label, 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.outline, 
            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable(enabled = onClick != null) { onClick?.invoke() },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                if (readOnly) {
                    Text(
                        text = if (value.isEmpty()) placeholder else value,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = if (value.isEmpty()) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    TextField(
                        value = value,
                        onValueChange = onValueChange,
                        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
