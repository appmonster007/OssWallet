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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.domain.model.Transaction
import com.osscabinet.gleemanager.domain.model.TransactionWithBalance
import com.osscabinet.gleemanager.data.local.entity.CategoryEntity
import com.osscabinet.gleemanager.ui.util.formatCurrencyRight
import com.osscabinet.gleemanager.ui.util.formatAmount
import com.osscabinet.gleemanager.ui.util.isDark
import com.osscabinet.gleemanager.ui.util.toContainerColor
import com.osscabinet.gleemanager.ui.util.toOnContainerColor
import com.osscabinet.gleemanager.ui.viewmodel.TagViewModel
import com.osscabinet.gleemanager.ui.viewmodel.TransactionViewModel
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
    modifier: Modifier = Modifier,
    showTitle: Boolean = true
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AeonTransactionList(
    transactions: List<TransactionWithBalance>,
    accountMap: Map<String, String>,
    categoryMap: Map<String, CategoryEntity>,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showDailyHeader: Boolean = true
) {
    val sortedGroupedTransactions = remember(transactions) {
        transactions.sortedByDescending { it.transaction.timestamp }
            .groupBy {
                val cal = Calendar.getInstance().apply { timeInMillis = it.transaction.timestamp }
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        sortedGroupedTransactions.forEach { (dateMillis, dayTransactions) ->
            if (showDailyHeader) {
                stickyHeader {
                    val dayTotal = dayTransactions.sumOf {
                        if (it.transaction.isIncome) it.transaction.amount else -it.transaction.amount
                    }
                    val latestBalance = dayTransactions.first().balanceAfter
                    val currency = dayTransactions.first().transaction.currency

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                    ) {
                        Column {
                            // Date Row
                            Text(
                                text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(dateMillis)).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )

                            // Balance & Total Row (Matches screenshot)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Balance ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = formatCurrencyRight(latestBalance, currency),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Σ ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = formatCurrencyRight(dayTotal, currency),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (dayTotal >= 0) Color(0xFF2E7D32) else Color(0xFFEF5350)
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 8.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }

            items(dayTransactions, key = { it.transaction.id }) { item ->
                val tx = item.transaction
                val categoryEntity = categoryMap[tx.category.lowercase()]

                val displayTimestamp = remember(tx.timestamp) {
                    val now = Calendar.getInstance()
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val txDay = Calendar.getInstance().apply {
                        timeInMillis = tx.timestamp
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    when (txDay) {
                        today -> "Today, " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                        today - 86400000 -> "Yesterday, " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                        else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                    }
                }

                val balanceBeforeValue = item.balanceAfter - (if (tx.isIncome) tx.amount else -tx.amount)

                AeonTransactionItem(
                    category = tx.category,
                    account = accountMap[tx.accountId] ?: "Unknown",
                    amount = formatCurrencyRight(if (tx.isIncome) tx.amount else -tx.amount, tx.currency),
                    balanceBefore = formatCurrencyRight(balanceBeforeValue, tx.currency),
                    balanceAfter = formatCurrencyRight(item.balanceAfter, tx.currency),
                    isIncome = tx.isIncome,
                    timestamp = displayTimestamp,
                    note = tx.note,
                    tags = tx.tags,
                    icon = if (categoryEntity != null) categoryIcons[categoryEntity.name] else null,
                    iconBg = categoryEntity?.let { Color(it.colorHex) },
                    onClick = { onTransactionClick(tx) },
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
fun AeonTransactionItem(
    category: String,
    account: String,
    amount: String,
    balanceBefore: String,
    balanceAfter: String,
    isIncome: Boolean,
    timestamp: String,
    note: String? = null,
    tags: List<String> = emptyList(),
    icon: ImageVector? = null,
    iconBg: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Category Icon with Badge
            Box(modifier = Modifier.size(56.dp)) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = iconBg ?: (if (isIncome) Color(0xFF2E7D32).copy(alpha = 0.1f) else Color(0xFFC62828).copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon ?: Icons.Rounded.Category,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (iconBg != null) Color.White else (if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828))
                        )
                    }
                }
                // Small checkmark badge seen in screenshot
                Surface(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp),
                    shape = CircleShape,
                    color = Color(0xFF2E7D32),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(2.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Top Row: Category & Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        category,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        amount,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFEF5350)
                    )
                }

                // Middle Row: Account & Balance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        account,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "$balanceBefore → $balanceAfter",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                    )
                }

                // Bottom Row: Note & Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (!note.isNullOrBlank()) {
                        Text(
                            "\"$note\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f).padding(top = 2.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    Text(
                        timestamp,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Tags Row
                if (tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF3B4D7B), // Matching screenshot tag background
                            ) {
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
