package com.osscabinet.gleemanager.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.domain.model.Transaction
import com.osscabinet.gleemanager.ui.util.formatCurrencyRight
import com.osscabinet.gleemanager.ui.util.formatAmount
import com.osscabinet.gleemanager.ui.util.isDark
import com.osscabinet.gleemanager.ui.util.toContainerColor
import com.osscabinet.gleemanager.ui.util.toOnContainerColor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun Numpad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onOperatorClick: (String) -> Unit,
    onEqualClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        "C", "÷", "×", "DEL",
        "7", "8", "9", "−",
        "4", "5", "6", "+",
        "1", "2", "3", "=",
        ".", "0", "00", ""
    )

    val rows = keys.chunked(4)

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    if (key.isNotEmpty()) {
                        Surface(
                            onClick = {
                                when (key) {
                                    "DEL" -> onDeleteClick()
                                    "C" -> onClearClick()
                                    "=" -> onEqualClick()
                                    "+", "−", "×", "÷" -> onOperatorClick(key)
                                    else -> onNumberClick(key)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(20.dp),
                            color = when (key) {
                                "=", "+", "−", "×", "÷" -> MaterialTheme.colorScheme.primaryContainer
                                "C", "DEL" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "DEL") {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.Backspace,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (key == "=" || key == "+" || key == "−" || key == "×" || key == "÷") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
fun RecordFieldCard(
    icon: ImageVector,
    label: String,
    value: String,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    isSlim: Boolean = false
) {
    val verticalPadding = if (isSlim) 10.dp else 16.dp
    val iconSize = if (isSlim) 36.dp else 48.dp
    val innerIconSize = if (isSlim) 18.dp else 24.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(if (isSlim) 20.dp else 28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(iconSize),
                shape = CircleShape,
                color = iconContainerColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(innerIconSize)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = if (isSlim) 9.sp else 11.sp,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = if (isSlim) 14.sp else 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailingIcon != null) {
                Icon(
                    trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EditorialInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    readOnly: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = readOnly, onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.outline,
                        letterSpacing = 0.5.sp
                    )
                )
                if (readOnly) {
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (value.isEmpty()) Text("Enter $label", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                            innerTextField()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerSheet(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    val sortedAccounts = remember(accounts) { accounts.sortedBy { it.name } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            AeonSectionHeader(title = "Select Account")
            Spacer(modifier = Modifier.height(24.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sortedAccounts) { account ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAccountSelected(account) },
                        shape = RoundedCornerShape(24.dp),
                        color = if (selectedAccount?.id == account.id) Color(android.graphics.Color.parseColor(account.colorHex)).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        border = if (selectedAccount?.id == account.id) BorderStroke(2.dp, Color(android.graphics.Color.parseColor(account.colorHex))) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(account.colorHex))))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(account.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(formatCurrencyRight(account.balance, account.currencyCode), style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencySelector(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    supportedCurrencies: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    selectedCurrency.ifEmpty { "USD" },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (supportedCurrencies.isEmpty()) {
                DropdownMenuItem(text = { Text("USD") }, onClick = { onCurrencySelected("USD"); expanded = false })
            } else {
                supportedCurrencies.sorted().forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            onCurrencySelected(currency)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExchangeRateDialog(
    initialRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var rate by remember { mutableStateOf(initialRate.toString()) }
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
                onClick = { onConfirm(rate.toDoubleOrNull() ?: 1.0) },
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

@Composable
fun CategoryItem(name: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = if (isSelected) Color(0xFF004D56) else Color(0xFFF1F3F4)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = name,
                    tint = if (isSelected) Color.White else Color(0xFF546E7A),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color(0xFF004D56) else Color(0xFF546E7A)
        )
    }
}

@Composable
fun DetailDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    onSwipeOpen: () -> Unit,
    content: @Composable () -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val offset by animateDpAsState(if (isOpen) 0.dp else screenWidth)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Tab indicator on the right edge when closed
        if (!isOpen) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 8.dp) // Hide slightly
                    .width(48.dp)
                    .height(80.dp)
                    .clickable { onSwipeOpen() }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (dragAmount < -20) {
                                change.consume()
                                onSwipeOpen()
                            }
                        }
                    },
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft, 
                        contentDescription = "Open Details",
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }
        }

        // Drawer content overlay
        if (isOpen || offset < screenWidth) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(screenWidth)
                    .offset(x = offset)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (dragAmount > 20) {
                                change.consume()
                                onClose()
                            }
                        }
                    },
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 24.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Back")
                        }
                        Text(
                            "Additional Details", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        content()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            AeonSectionHeader(title = "Classification")
            Spacer(modifier = Modifier.height(32.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(categoryIcons.toList().sortedBy { it.first }) { (name, icon) ->
                    CategoryItem(
                        name = name,
                        icon = icon,
                        isSelected = selectedCategory == name,
                        onClick = {
                            onCategorySelected(name)
                        }
                    )
                }
            }
        }
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
    "Income" to Icons.Rounded.Payments,
    "Coffee" to Icons.Rounded.CoffeeMaker,
    "Grocery" to Icons.Rounded.ShoppingCart,
    "Other" to Icons.Rounded.MoreHoriz
)

@Composable
fun AeonFlowBreakdownItem(curr: String, amount: Double, inBase: Double, baseCurrency: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                curr,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.outline
            )
            if (curr != baseCurrency) {
                Text(
                    "≈ ${formatCurrencyRight(inBase, baseCurrency)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Text(
            formatCurrencyRight(amount, curr),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black)
        )
    }
}

@Composable
fun AeonSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            letterSpacing = 1.sp
        )
        trailing?.invoke()
    }
}

@Composable
fun AeonAccountHeroCard(
    account: Account,
    balance: Double,
    netBalance: Double,
    baseCurrency: String,
    exchangeRates: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val accountColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val isDark = accountColor.isDark()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = accountColor,
        contentColor = if (isDark) Color.White else Color.Black,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        account.structureType.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        ),
                        color = LocalContentColor.current.copy(alpha = 0.7f)
                    )
                    Text(
                        formatCurrencyRight(balance, account.currencyCode),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
                    )
                    if (account.currencyCode != baseCurrency) {
                        val rate = exchangeRates[baseCurrency] ?: 1.0
                        val convertedBalance = balance * rate
                        Text(
                            "≈ ${formatCurrencyRight(convertedBalance, baseCurrency)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LocalContentColor.current.copy(alpha = 0.7f)
                        )
                    }
                }
                Icon(
                    AccountIconMap[account.icon] ?: Icons.Rounded.AccountBalance,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = LocalContentColor.current.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "MONTHLY NET",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = LocalContentColor.current.copy(alpha = 0.7f)
                    )
                    Text(
                        formatCurrencyRight(netBalance, account.currencyCode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                }
                Text(
                    account.id.takeLast(4).let { "**** $it" },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = LocalContentColor.current.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun AeonCurrencyBreakdown(
    balances: Map<String, Double>,
    baseCurrency: String,
    accountCurrency: String,
    exchangeRates: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "CURRENCY BREAKDOWN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            balances.toList().sortedBy { it.first }.forEach { (curr, bal) ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            curr,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatCurrencyRight(bal, curr),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                            )
                            if (accountCurrency != baseCurrency) {
                                val rate = exchangeRates[baseCurrency] ?: 1.0
                                val convertedBal = bal * rate
                                Text(
                                    "≈ ${formatCurrencyRight(convertedBal, baseCurrency)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AeonCashFlowSection(
    transactions: List<Transaction>,
    baseCurrency: String,
    exchangeRates: Map<String, Double>,
    currentTheme: String,
    showTitle: Boolean = true
) {
    val allSupportedCurrencies = remember(transactions) {
        transactions.map { it.currency }.distinct().sorted()
    }

    val isDarkTheme = when (currentTheme) {
        "Light" -> false
        "Dark", "Amoled" -> true
        else -> isSystemInDarkTheme()
    }
    val isAmoled = currentTheme == "Amoled"

    val incomeColor = Color(0xFF2E7D32)
    val expenseColor = Color(0xFFC62828)

    val incomeContainer = if (isAmoled) Color.Black else incomeColor.toContainerColor(isDarkTheme, false).copy(alpha = 0.1f)
    val expenseContainer = if (isAmoled) Color.Black else expenseColor.toContainerColor(isDarkTheme, false).copy(alpha = 0.1f)
    
    val incomeBorder = if (isAmoled) BorderStroke(1.dp, incomeColor.copy(alpha = 0.3f)) else BorderStroke(1.dp, incomeColor.copy(alpha = 0.1f))
    val expenseBorder = if (isAmoled) BorderStroke(1.dp, expenseColor.copy(alpha = 0.3f)) else BorderStroke(1.dp, expenseColor.copy(alpha = 0.1f))

    val flowData by remember(allSupportedCurrencies, transactions, exchangeRates, baseCurrency) {
        derivedStateOf {
            allSupportedCurrencies.sorted().mapNotNull { curr ->
                val txsForCurr = transactions.filter { it.currency == curr }
                val income = txsForCurr.filter { it.isIncome }.sumOf { it.amount }
                val expense = txsForCurr.filter { !it.isIncome }.sumOf { it.amount }

                if (income == 0.0 && expense == 0.0) null
                else {
                    val rate = if (curr == baseCurrency) 1.0 else (exchangeRates[curr] ?: 1.0)
                    val incomeInBase = if (rate != 0.0) income / rate else income
                    val expenseInBase = if (rate != 0.0) expense / rate else expense
                    Triple(curr, Pair(income, incomeInBase), Pair(expense, expenseInBase))
                }
            }
        }
    }

    val totalIncomeInBase = flowData.sumOf { it.second.second }
    val totalExpenseInBase = flowData.sumOf { it.third.second }

    val density = LocalDensity.current
    var incomeSize by remember { mutableStateOf(IntSize.Zero) }
    var expenseSize by remember { mutableStateOf(IntSize.Zero) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (showTitle) {
            Text("CASH FLOW SUMMARY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Income Section
            Surface(
                modifier = Modifier.weight(1f).onSizeChanged { incomeSize = it },
                shape = RoundedCornerShape(24.dp),
                color = incomeContainer,
                border = incomeBorder
            ) {
                Box {
                    val iconHeight = with(density) { (incomeSize.height * 0.75f).toDp() }
                    Icon(
                        imageVector = Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset { IntOffset(x = (incomeSize.width * 0.25f).toInt(), y = 0) }
                            .size(iconHeight)
                            .alpha(0.2f),
                        tint = Color(0xFF2E7D32)
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("INCOME", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            formatCurrencyRight(totalIncomeInBase, baseCurrency),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            flowData.forEach { (curr, income, _) ->
                                AeonFlowBreakdownItem(curr, income.first, income.second, baseCurrency)
                            }
                        }
                    }
                }
            }

            // Expense Section
            Surface(
                modifier = Modifier.weight(1f).onSizeChanged { expenseSize = it },
                shape = RoundedCornerShape(24.dp),
                color = expenseContainer,
                border = expenseBorder
            ) {
                Box {
                    val iconHeight = with(density) { (expenseSize.height * 0.75f).toDp() }
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset { IntOffset(x = (expenseSize.width * 0.25f).toInt(), y = 0) }
                            .size(iconHeight)
                            .alpha(0.2f),
                        tint = Color(0xFFC62828)
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("EXPENSE", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            formatCurrencyRight(totalExpenseInBase, baseCurrency),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            flowData.forEach { (curr, _, expense) ->
                                AeonFlowBreakdownItem(curr, expense.first, expense.second, baseCurrency)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AeonTransactionItem(
    title: String,
    sub: String,
    amount: String,
    isIncome: Boolean,
    account: String? = null,
    icon: ImageVector? = null,
    iconBg: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = iconBg ?: (if (isIncome) Color(0xFF2E7D32).copy(alpha = 0.1f) else Color(0xFFC62828).copy(alpha = 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon ?: (if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Text(sub, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    amount,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = if (isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                )
                if (account != null) {
                    Text(account, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
fun AeonDashedButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    val contentColor = if (isError) {
        if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
    } else {
        if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    }
    
    val borderColor = if (isError) {
        if (enabled) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
    } else {
        if (enabled) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .drawBehind {
                val strokeWidth = 1.5.dp.toPx()
                val dashLength = 12f
                val dashGap = 8f
                val stroke = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, dashGap), 0f)
                )
                drawRoundRect(
                    color = borderColor,
                    style = stroke,
                    cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = contentColor
            )
        }
    }
}

@Composable
fun AeonBudgetProgress(label: String, current: Float, total: Float, color: Color, baseCurrency: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text("${formatCurrencyRight(current.toDouble(), baseCurrency)} / ${formatCurrencyRight(total.toDouble(), baseCurrency)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        LinearProgressIndicator(
            progress = { current / total },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}
