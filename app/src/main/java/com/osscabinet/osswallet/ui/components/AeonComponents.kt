package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.TagEntity
import com.osscabinet.ossWallet.ui.util.*
import com.osscabinet.ossWallet.ui.viewmodel.TagViewModel
import java.math.BigDecimal


@Composable
fun RecordFieldCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    isSlim: Boolean = false
) {
    val verticalPadding = if (isSlim) 8.dp else 16.dp
    val iconSize = if (isSlim) 32.dp else 48.dp
    val innerIconSize = if (isSlim) 16.dp else 24.dp

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
    var showDialog by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier.clickable { showDialog = true },
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

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Select Currency", fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        LazyColumn {
                            val list = if (supportedCurrencies.isEmpty()) listOf("USD") else supportedCurrencies.sorted()
                            items(list) { currency ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onCurrencySelected(currency)
                                            showDialog = false
                                        },
                                    color = if (selectedCurrency == currency) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        currency,
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (selectedCurrency == currency) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
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
                color = Color(0xFFC2D2FF),
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
    allTags: List<TagEntity>,
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

@Composable
fun CategoryListItem(
    category: CategoryEntity,
    isParent: Boolean,
    isSelected: Boolean = false,
    hasChildren: Boolean = false,
    isExpanded: Boolean = false,
    isManagementMode: Boolean = false,
    onToggleExpand: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayColor = if (category.colorHex == -1) 0xFF9E9E9E.toInt() else category.colorHex
        Surface(
            modifier = Modifier.size(if (isParent) 40.dp else 32.dp),
            shape = CircleShape,
            color = Color(displayColor).copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    AccountIconMap[category.iconName] ?: Icons.Rounded.Category,
                    contentDescription = null,
                    modifier = Modifier.size(if (isParent) 20.dp else 16.dp),
                    tint = Color(displayColor)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            category.name,
            style = if (isParent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isParent) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        if (isManagementMode) {
            IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
            }
        } else if (isSelected) {
            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        
        if (isParent && hasChildren) {
            IconButton(onClick = onToggleExpand, modifier = Modifier.size(24.dp)) {
                Icon(
                    if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun AeonCategoryList(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long? = null,
    onCategorySelected: (CategoryEntity) -> Unit,
    isManagementMode: Boolean = false,
    onEditClick: (CategoryEntity) -> Unit = {},
    onAddSubCategoryClick: (CategoryEntity) -> Unit = {}
) {
    val parentCategories = categories.filter { it.parentId == null }.sortedBy { it.name }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parentCategories.forEach { parent ->
            var expanded by remember { mutableStateOf(categories.any { it.parentId == parent.id && it.id == selectedCategoryId }) }
            val children = categories.filter { it.parentId == parent.id }.sortedBy { it.name }
            
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column {
                    CategoryListItem(
                        category = parent,
                        isParent = true,
                        isSelected = selectedCategoryId == parent.id,
                        hasChildren = children.isNotEmpty(),
                        isExpanded = expanded,
                        isManagementMode = isManagementMode,
                        onToggleExpand = { expanded = !expanded },
                        onEditClick = { onEditClick(parent) },
                        onClick = {
                            if (isManagementMode) onEditClick(parent) else onCategorySelected(parent)
                        }
                    )
                    
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 8.dp)) {
                            children.forEach { child ->
                                CategoryListItem(
                                    category = child,
                                    isParent = false,
                                    isSelected = selectedCategoryId == child.id,
                                    isManagementMode = isManagementMode,
                                    onEditClick = { onEditClick(child) },
                                    onClick = {
                                        if (isManagementMode) onEditClick(child) else onCategorySelected(child)
                                    }
                                )
                            }
                            
                            if (isManagementMode) {
                                // Add Sub-category button
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onAddSubCategoryClick(parent) }
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    color = Color.Transparent
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Add sub-category",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
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
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelected: (CategoryEntity) -> Unit,
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
                    AeonCategoryList(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelected = {
                            onCategorySelected(it)
                            onDismiss()
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


val categoryIcons = AccountIconMap

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
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Add,
    contentDescription: String? = null,
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
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
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
                    Box(modifier = Modifier.size(150.dp, 20.dp).background(brush, RoundedCornerShape(4.dp)))
                    Box(modifier = Modifier.size(250.dp, 48.dp).background(brush, RoundedCornerShape(8.dp)))
                    Box(modifier = Modifier.size(100.dp, 24.dp).background(brush, RoundedCornerShape(16.dp)))
                }
            }
        }
        if (showCurrency) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) {
                        Box(modifier = Modifier.size(120.dp, 60.dp).background(brush, RoundedCornerShape(20.dp)))
                    }
                }
            }
        }
        if (showCashflow) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(brush, RoundedCornerShape(32.dp)))
            }
        }
        if (showAccounts) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier = Modifier.size(100.dp, 20.dp).background(brush, RoundedCornerShape(4.dp)))
                        Box(modifier = Modifier.size(60.dp, 20.dp).background(brush, RoundedCornerShape(4.dp)))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f).height(120.dp).background(brush, RoundedCornerShape(24.dp)))
                        Box(modifier = Modifier.weight(1f).height(120.dp).background(brush, RoundedCornerShape(24.dp)))
                    }
                }
            }
        }
        if (showBudgets) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(120.dp, 20.dp).background(brush, RoundedCornerShape(4.dp)))
                    repeat(2) {
                        Box(modifier = Modifier.fillMaxWidth().height(60.dp).background(brush, RoundedCornerShape(16.dp)))
                    }
                }
            }
        }
    }
}

@Composable
fun AeonBudgetProgress(
    label: String,
    current: BigDecimal,
    total: BigDecimal,
    color: Color,
    baseCurrency: String,
    precision: Int = 2,
    showLabels: Boolean = true
) {
    val progress = if (total.signum() > 0) (current.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showLabels) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${formatCurrencyRight(current, baseCurrency, precision)} / ${formatCurrencyRight(total, baseCurrency, precision)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.7f),
                                color
                            )
                        )
                    )
            )
        }
    }
}
