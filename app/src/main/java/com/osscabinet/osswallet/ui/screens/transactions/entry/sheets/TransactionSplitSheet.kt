package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionSplit
import com.osscabinet.ossWallet.ui.components.getIconByName
import java.math.BigDecimal

/**
 * Bottom sheet to manage transaction splitting into multiple records.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSplitSheet(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        TransactionSplitContent(
            state = state,
            onEvent = onEvent,
            onDismiss = onDismiss
        )
    }
}

/**
 * Internal content for transaction split management.
 */
@Composable
fun TransactionSplitContent(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val totalAmount = state.amountValue
    val currentSplitSum = state.splits.sumOf { it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO }
    val remainingAmount = totalAmount - currentSplitSum
    val isFullyAllocated = remainingAmount.compareTo(BigDecimal.ZERO) == 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SPLIT TRANSACTION",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Divide amount into multiple transactions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            Surface(
                onClick = { onEvent(TransactionEntryEvent.AddSplit) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Add Split",
                    modifier = Modifier.padding(8.dp).size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Summary Row - Refined to match Architect Slate design
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL AMOUNT", 
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                    Text(
                        text = "${state.selectedCurrency} ${state.amount}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (remainingAmount >= BigDecimal.ZERO) "REMAINING" else "OVER ALLOCATED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isFullyAllocated) Color(0xFF4CAF50) 
                                   else if (remainingAmount < BigDecimal.ZERO) MaterialTheme.colorScheme.error 
                                   else MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "${state.selectedCurrency} ${remainingAmount.abs().toPlainString()}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isFullyAllocated) Color(0xFF4CAF50) 
                                   else if (remainingAmount < BigDecimal.ZERO) MaterialTheme.colorScheme.error 
                                   else MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            itemsIndexed(state.splits) { index, split ->
                SplitItem(
                    index = index,
                    split = split,
                    state = state,
                    onEvent = onEvent
                )
            }
        }
        
        if (!isFullyAllocated && state.splits.isNotEmpty()) {
            Text(
                text = "Allocation must match total amount",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    onEvent(TransactionEntryEvent.ToggleSplitMode(false))
                    onDismiss()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "REVERT",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = isFullyAllocated && state.splits.size >= 2,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "CONFIRM SPLIT",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun SplitItem(
    index: Int,
    split: TransactionSplit,
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit
) {
    val category = state.categories.find { it.id == split.categoryId }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Selector
                Surface(
                    onClick = { onEvent(TransactionEntryEvent.ShowSplitCategorySheet(index)) },
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = (category?.colorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.1f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    getIconByName(category?.iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = category?.colorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = category?.name ?: "Select Category",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            color = if (category == null) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Amount Input
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "AMOUNT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                    TextField(
                        value = split.amount,
                        onValueChange = { 
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                onEvent(TransactionEntryEvent.UpdateSplit(index, amount = it))
                            }
                        },
                        modifier = Modifier.width(90.dp),
                        placeholder = { Text("0.00", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.End
                        )
                    )
                }

                IconButton(
                    onClick = { onEvent(TransactionEntryEvent.RemoveSplit(index)) },
                    modifier = Modifier.padding(start = 4.dp).size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline, 
                        contentDescription = "Remove", 
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Note Input
            TextField(
                value = split.note,
                onValueChange = { onEvent(TransactionEntryEvent.UpdateSplit(index, note = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add a note for this split...", style = MaterialTheme.typography.bodySmall) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.2f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Rounded.Notes,
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionSplitPreview() {
    MaterialTheme {
        val mockState = TransactionEntryState(
            amount = "150.00",
            selectedCurrency = "USD",
            categories = listOf(
                Category(id = 1, name = "Groceries", iconName = "shopping_cart", colorHex = 0xFF4CAF50.toInt(), isDefault = true, parentId = null),
                Category(id = 2, name = "Dining", iconName = "restaurant", colorHex = 0xFFFF9800.toInt(), isDefault = true, parentId = null)
            ),
            splits = listOf(
                TransactionSplit(categoryId = 1, amount = "100.00", note = "Weekly Groceries"),
                TransactionSplit(categoryId = 2, amount = "50.00", note = "Lunch")
            )
        )
        
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            TransactionSplitContent(
                state = mockState,
                onEvent = {},
                onDismiss = {}
            )
        }
    }
}
