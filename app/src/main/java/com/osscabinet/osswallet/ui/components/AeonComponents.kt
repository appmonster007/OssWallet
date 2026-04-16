package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.model.TransactionWithBalance
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.ui.util.*
import com.osscabinet.ossWallet.ui.viewmodel.TagViewModel
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.draw.drawBehind
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange



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
                    Icon(icon, contentDescription = null, modifier = Modifier.size(innerIconSize), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (trailingIcon != null) {
                Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.outline)
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
    isMultiline: Boolean = false,
    onFocus: () -> Unit = {},
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = if (isMultiline) Alignment.Top else Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp).padding(top = if (isMultiline) 4.dp else 0.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth().then(if (onClick == null) Modifier.clickable { onFocus() } else Modifier),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium),
                    singleLine = !isMultiline,
                    minLines = if (isMultiline) 3 else 1,
                    readOnly = readOnly || onClick != null,
                    enabled = onClick == null,
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) Text("Tap to add...", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyLarge)
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
fun AccountPickerSheet(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    @OptIn(ExperimentalMaterial3Api::class)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            AeonSectionHeader(title = "Select Account")
            Spacer(modifier = Modifier.height(24.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(accounts) { account ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onAccountSelected(account) },
                        shape = RoundedCornerShape(24.dp),
                        color = if (selectedAccount?.id == account.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(account.currencyCode, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(account.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(account.structureType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
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
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = { showDialog = true },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                selectedCurrency,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Icon(
                Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column {
                    supportedCurrencies.forEach { currency ->
                        TextButton(
                            onClick = {
                                onCurrencySelected(currency)
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currency, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun ExchangeRateDialog(
    initialRate: BigDecimal,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal) -> Unit
) {
    var rateText by remember { mutableStateOf(initialRate.toPlainString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Exchange Rate") },
        text = {
            Column {
                Text("1 Source Currency = X Destination Currency", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                rateText.toBigDecimalOrNull()?.let { onConfirm(it) }
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CategoryItem(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
    val drawerWidth = 320.dp
    val density = LocalDensity.current
    val drawerWidthPx = with(density) { drawerWidth.toPx() }

    var offset by remember { mutableFloatStateOf(if (isOpen) 0f else drawerWidthPx) }

    LaunchedEffect(isOpen) {
        offset = if (isOpen) 0f else drawerWidthPx
    }

    val animatedOffset by animateFloatAsState(
        targetValue = offset,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "drawer_offset"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim
        if (isOpen || animatedOffset < drawerWidthPx) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1f - (animatedOffset / drawerWidthPx)) * 0.4f))
                    .clickable(enabled = isOpen) { onClose() }
            )
        }

        // Drawer Content
        Surface(
            modifier = Modifier
                .width(drawerWidth)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .offset { IntOffset(animatedOffset.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offset > drawerWidthPx * 0.4f) onClose()
                            else offset = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offset = (offset + dragAmount).coerceIn(0f, drawerWidthPx)
                        }
                    )
                },
            shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                content()
            }
        }

        // Floating Tab (Handle)
        if (!isOpen && animatedOffset >= drawerWidthPx) {
            Surface(
                onClick = onSwipeOpen,
                modifier = Modifier
                    .align(tabAlignment)
                    .offset(y = tabOffsetY)
                    .size(width = 48.dp, height = 80.dp)
                    .offset(x = 24.dp), // Peek a bit
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(start = 8.dp)) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Open Details",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabelPickerSheet(
    selectedTags: List<String>,
    onTagsChanged: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    tagViewModel: TagViewModel
) {
    val allTags by tagViewModel.tags.collectAsState()
    var newTagText by remember { mutableStateOf("") }

    @OptIn(ExperimentalMaterial3Api::class)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            AeonSectionHeader(title = "Labels")
            Spacer(modifier = Modifier.height(16.dp))

            // Add Tag
            OutlinedTextField(
                value = newTagText,
                onValueChange = { newTagText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("New Label...") },
                trailingIcon = {
                    IconButton(onClick = {
                        if (newTagText.isNotBlank()) {
                            tagViewModel.addTag(newTagText)
                            newTagText = ""
                        }
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allTags.forEach { tag ->
                    val isSelected = selectedTags.contains(tag.name)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newList = if (isSelected) selectedTags - tag.name else selectedTags + tag.name
                            onTagsChanged(newList)
                        },
                        label = { Text(tag.name) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentMethodPickerSheet(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val methods = listOf("Cash", "Credit Card", "Bank Transfer", "Debit Card", "E-Wallet", "Crypto")

    @OptIn(ExperimentalMaterial3Api::class)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            AeonSectionHeader(title = "Payment Method")
            Spacer(modifier = Modifier.height(24.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(methods) { method ->
                    Surface(
                        onClick = {
                            onMethodSelected(method)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selectedMethod == method) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            val icon = when (method) {
                                "Cash" -> Icons.Rounded.Payments
                                "Credit Card" -> Icons.Rounded.CreditCard
                                "Bank Transfer" -> Icons.Rounded.AccountBalance
                                "Debit Card" -> Icons.Rounded.Contactless
                                "E-Wallet" -> Icons.Rounded.AccountBalanceWallet
                                else -> Icons.Rounded.Token
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (selectedMethod == method) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(method, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryPickerSheet(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelected: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    @OptIn(ExperimentalMaterial3Api::class)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            AeonSectionHeader(title = "Classification")
            Spacer(modifier = Modifier.height(24.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(categories) { category ->
                    CategoryItem(
                        name = category.name,
                        icon = categoryIcons[category.iconName] ?: Icons.Rounded.Category,
                        isSelected = selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category) }
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
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        trailing?.invoke()
    }
}

@Composable
fun AeonCurrencyBreakdown(
    currencyTotals: Map<String, BigDecimal>,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal>,
    modifier: Modifier = Modifier,
    precision: Int = 2,
    layout: String = "GRID",
    showBaseConversion: Boolean = false,
    timelineRange: TimelineRange = TimelineRange.AllTime,
    onLayoutChange: (String) -> Unit = {},
    onShowBaseConversionChange: (Boolean) -> Unit = {},
    onTimelineRangeChange: (TimelineRange) -> Unit = {}
) {
    // Basic placeholder for the actual implementation in specific files
}

val categoryIcons = mapOf(
    "Shopping" to Icons.Rounded.ShoppingCart,
    "Food" to Icons.Rounded.Restaurant,
    "Transport" to Icons.Rounded.DirectionsCar,
    "Health" to Icons.Rounded.MedicalServices,
    "Education" to Icons.Rounded.School,
    "Bills" to Icons.AutoMirrored.Rounded.ReceiptLong,
    "Salary" to Icons.Rounded.Payments,
    "Savings" to Icons.Rounded.Savings,
    "Home" to Icons.Rounded.Home,
    "Coffee" to Icons.Rounded.CoffeeMaker,
    "Rent" to Icons.Rounded.Home,
    "Entertainment" to Icons.Rounded.Movie,
    "Other" to Icons.Rounded.Category
)

@Composable
fun AeonTransactionList(
    transactions: List<TransactionWithBalance>,
    accountMap: Map<String, String>,
    categoryMap: Map<Long, CategoryEntity>,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    isSelectionMode: Boolean = false,
    precision: Int = 2
) {
    // Basic placeholder
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
    note: String?,
    tags: List<String>,
    icon: ImageVector?,
    precision: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconBg: Color? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = iconBg ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon ?: Icons.Rounded.Category, contentDescription = null, tint = if (iconBg != null) Color.White else MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(account, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (type == CashFlowType.INCOME) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun AeonDashedButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isSlim: Boolean = false,
    isDashed: Boolean = true
) {
    val stroke = Stroke(width = 2.dp.value, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    val color = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isSlim) 48.dp else 56.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                if (isDashed) {
                    drawRoundRect(
                        color = color,
                        style = stroke,
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AeonFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
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
        shape = RoundedCornerShape(20.dp)
    ) {
        Icon(icon, contentDescription = contentDescription)
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
        icon = { Icon(icon, contentDescription = null) },
        text = { Text(text, fontWeight = FontWeight.Bold) },
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AeonShimmer(
    innerPadding: PaddingValues,
    showCashflow: Boolean = true,
    showNetWorth: Boolean = true,
    showAccounts: Boolean = true,
    showCurrencyBreakdown: Boolean = true,
    showTransactions: Boolean = true
) {
    // Basic Shimmer implementation placeholder
}

@Composable
fun AeonBudgetProgress(
    label: String,
    current: BigDecimal,
    total: BigDecimal,
    color: Color,
    baseCurrency: String,
    precision: Int,
    showLabels: Boolean
) {
    // Basic Budget implementation placeholder
}
