package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.ui.util.calculateTotalInCurrency
import com.osscabinet.ossWallet.ui.util.convertAmount
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.formatAmount
import com.osscabinet.ossWallet.ui.util.isDark
import com.osscabinet.ossWallet.ui.util.toContainerColor
import com.osscabinet.ossWallet.ui.util.toOnContainerColor
import com.osscabinet.ossWallet.ui.viewmodel.TagViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import java.math.BigDecimal
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
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val operators = listOf("+", "−", "×", "÷")

        @Composable
        fun NumpadKey(
            key: String,
            onClick: () -> Unit,
            modifier: Modifier = Modifier,
            color: Color? = null,
            contentColor: Color? = null
        ) {
            val backgroundColor = color ?: when (key) {
                in operators -> MaterialTheme.colorScheme.primaryContainer
                "C", "DEL" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
            val textColor = contentColor ?: if (key in operators) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Surface(
                onClick = onClick,
                modifier = modifier.fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                color = backgroundColor
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (key == "DEL") {
                        Icon(
                            Icons.AutoMirrored.Rounded.Backspace,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = textColor
                        )
                    } else {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        )
                    }
                }
            }
        }

        // Row 1: C, ÷, ×, DEL
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumpadKey("C", onClearClick, Modifier.weight(1f))
            NumpadKey("÷", { onOperatorClick("÷") }, Modifier.weight(1f))
            NumpadKey("×", { onOperatorClick("×") }, Modifier.weight(1f))
            NumpadKey("DEL", onDeleteClick, Modifier.weight(1f))
        }
        // Row 2: 7, 8, 9, −
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumpadKey("7", { onNumberClick("7") }, Modifier.weight(1f))
            NumpadKey("8", { onNumberClick("8") }, Modifier.weight(1f))
            NumpadKey("9", { onNumberClick("9") }, Modifier.weight(1f))
            NumpadKey("−", { onOperatorClick("−") }, Modifier.weight(1f))
        }
        // Row 3: 4, 5, 6, +
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumpadKey("4", { onNumberClick("4") }, Modifier.weight(1f))
            NumpadKey("5", { onNumberClick("5") }, Modifier.weight(1f))
            NumpadKey("6", { onNumberClick("6") }, Modifier.weight(1f))
            NumpadKey("+", { onOperatorClick("+") }, Modifier.weight(1f))
        }
        // Rows 4 & 5
        Row(modifier = Modifier.weight(2f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumpadKey("1", { onNumberClick("1") }, Modifier.weight(1f))
                    NumpadKey("2", { onNumberClick("2") }, Modifier.weight(1f))
                    NumpadKey("3", { onNumberClick("3") }, Modifier.weight(1f))
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumpadKey(".", { onNumberClick(".") }, Modifier.weight(1f))
                    NumpadKey("0", { onNumberClick("0") }, Modifier.weight(1f))
                    NumpadKey("00", { onNumberClick("00") }, Modifier.weight(1f))
                }
            }
            // Equal button spanning two rows
            NumpadKey(
                key = "=",
                onClick = onEqualClick,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
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
    com.osscabinet.ossWallet.ui.components.accounts.AccountPickerSheet(
        accounts = accounts,
        selectedAccount = selectedAccount,
        onAccountSelected = onAccountSelected,
        onDismiss = onDismiss
    )
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
    tabAlignment: Alignment = Alignment.CenterEnd,
    tabOffsetY: Dp = 0.dp,
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
                    .align(tabAlignment)
                    .offset(x = 8.dp, y = tabOffsetY) // Hide slightly
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LabelPickerSheet(
    selectedTags: List<String>,
    onTagsChanged: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    tagViewModel: TagViewModel
) {
    var tagInput by remember { mutableStateOf("") }
    val dbTags by tagViewModel.tags.collectAsState()
    val sortedTags = remember(dbTags) { dbTags.sortedBy { it.name.lowercase() } }

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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                if (newTag !in dbTags.map { it.name }) {
                                    tagViewModel.addTag(newTag)
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(methods) { (name, icon) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onMethodSelected(name)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(24.dp),
                        color = if (selectedMethod == name) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, tint = if (selectedMethod == name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedMethod == name) FontWeight.Bold else FontWeight.Medium
                            )
                            if (selectedMethod == name) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    categories: List<com.osscabinet.ossWallet.data.local.entity.CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelected: (com.osscabinet.ossWallet.data.local.entity.CategoryEntity) -> Unit,
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
                items(categories.sortedBy { it.name }) { category ->
                    CategoryItem(
                        name = category.name,
                        icon = categoryIcons[category.iconName] ?: Icons.Rounded.Category,
                        isSelected = selectedCategoryId == category.id,
                        onClick = {
                            onCategorySelected(category)
                        }
                    )
                }
            }
        }
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
            title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        trailing?.invoke()
    }
}

@Composable
fun AeonAccountHeroCard(
    account: Account,
    balance: BigDecimal,
    netBalance: BigDecimal,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    modifier: Modifier = Modifier,
    precision: Int = 2
) {
    val accountColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val isDark = accountColor.isDark()
    val contentColor = if (isDark) Color.White else Color.Black
    val icon = AccountIconMap[account.icon] ?: Icons.Rounded.AccountBalance
    
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { cardSize = it },
        shape = RoundedCornerShape(32.dp),
        color = accountColor,
        contentColor = contentColor,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Icon - Standardized with Large Card
            val iconHeight = with(density) { (cardSize.height * 0.75f).toDp() }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(iconHeight)
                    .alpha(0.12f),
                tint = contentColor
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            account.structureType.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
                            color = contentColor.copy(alpha = 0.7f)
                        )
                        Text(
                            formatCurrencyRight(balance, account.currencyCode, precision),
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black)
                        )
                        if (account.currencyCode != baseCurrency) {
                            val convertedBalance = convertAmount(balance, account.currencyCode, baseCurrency, exchangeRates, precision)
                            Text(
                                "≈ ${formatCurrencyRight(convertedBalance, baseCurrency, precision)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    // Top corner icon
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = contentColor.copy(alpha = 0.7f)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            "MONTHLY NET",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = contentColor.copy(alpha = 0.7f)
                        )
                        Text(
                            formatCurrencyRight(netBalance, account.currencyCode, precision),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                    }
                    
                    Text(
                        account.id.takeLast(4).let { "**** $it" },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = contentColor.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}





@Composable
fun AeonCurrencyBreakdown(
    balances: Map<String, BigDecimal>,
    baseCurrency: String,
    accountCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    precision: Int = 2
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (showTitle) {
                Text(
                    "CURRENCY BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
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
                                formatCurrencyRight(bal, curr, precision),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                            )
                            if (accountCurrency != baseCurrency) {
                                val convertedBal = convertAmount(bal, curr, baseCurrency, exchangeRates, precision)
                                Text(
                                    "≈ ${formatCurrencyRight(convertedBal, baseCurrency, precision)}",
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AeonTransactionList(
    transactions: List<TransactionWithBalance>,
    accountMap: Map<String, String>,
    categoryMap: Map<Long, CategoryEntity>,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showDailyHeader: Boolean = true,
    precision: Int = 2
) {
    com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionList(
        transactions = transactions,
        accountNames = accountMap,
        categories = categoryMap,
        onTransactionClick = onTransactionClick,
        modifier = modifier,
        contentPadding = contentPadding,
        showAccountName = true, // default
        precision = precision
    )
}

@Composable
fun AeonTransactionItem(
    category: String,
    account: String,
    amount: String,
    balanceBefore: String,
    balanceAfter: String,
    type: CashFlowType,
    timestamp: String,
    note: String? = null,
    tags: List<String> = emptyList(),
    icon: ImageVector? = null,
    iconBg: Color? = null,
    precision: Int = 2,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem(
        category = category,
        account = account,
        amount = amount,
        balanceBefore = balanceBefore,
        balanceAfter = balanceAfter,
        type = type,
        timestamp = timestamp,
        note = note,
        tags = tags,
        icon = icon,
        iconBg = iconBg,
        precision = precision,
        onClick = onClick,
        modifier = modifier
    )
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
fun AeonFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector = Icons.Rounded.Add,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(20.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun AeonExtendedFloatingActionButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(20.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
        text = { 
            Text(
                text, 
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            ) 
        }
    )
}

@Composable
fun AeonShimmer(
    innerPadding: PaddingValues,
    showNetWorth: Boolean = true,
    showCurrency: Boolean = true,
    showCashflow: Boolean = true,
    showAccounts: Boolean = true,
    showBudgets: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        if (showNetWorth) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(150.dp, 20.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                    Box(modifier = Modifier.size(250.dp, 48.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(8.dp)))
                    Box(modifier = Modifier.size(100.dp, 24.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(16.dp)))
                }
            }
        }
        if (showCurrency) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) {
                        Box(modifier = Modifier.size(120.dp, 60.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(20.dp)))
                    }
                }
            }
        }
        if (showCashflow) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(32.dp)))
            }
        }
        if (showAccounts) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier = Modifier.size(100.dp, 20.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                        Box(modifier = Modifier.size(60.dp, 20.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f).height(120.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(24.dp)))
                        Box(modifier = Modifier.weight(1f).height(120.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(24.dp)))
                    }
                }
            }
        }
        if (showBudgets) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(120.dp, 20.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(4.dp)))
                    repeat(2) {
                        Box(modifier = Modifier.fillMaxWidth().height(60.dp).background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(16.dp)))
                    }
                }
            }
        }
    }
}

@Composable
fun AeonBudgetProgress(label: String, current: BigDecimal, total: BigDecimal, color: Color, baseCurrency: String, precision: Int = 2) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text("${formatCurrencyRight(current, baseCurrency, precision)} / ${formatCurrencyRight(total, baseCurrency, precision)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        LinearProgressIndicator(
            progress = { if (total.signum() > 0) (current.toDouble() / total.toDouble()).toFloat() else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}
