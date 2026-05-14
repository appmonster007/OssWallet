package com.osscabinet.ossWallet.ui.components.entry

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.TransactionSplit
import com.osscabinet.ossWallet.ui.components.getIconByName
import java.math.BigDecimal

/**
 * Generic bottom sheet to manage transaction splitting.
 * Consolidates splitting logic into a reusable UI block.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonTransactionSplitSheet(
    totalAmount: BigDecimal,
    currency: String,
    splits: List<TransactionSplit>,
    categories: List<Category>,
    onAddSplit: () -> Unit,
    onRemoveSplit: (Int) -> Unit,
    onUpdateSplit: (Int, String?, String?, Long?) -> Unit,
    onShowCategorySheet: (Int) -> Unit,
    onToggleSplitMode: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        val currentSplitSum = splits.sumOf { it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO }
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
                    onClick = onAddSplit,
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

            // Summary Row
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
                            text = "$currency ${totalAmount.toPlainString()}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val isOver = remainingAmount < BigDecimal.ZERO
                        Text(
                            text = if (isFullyAllocated) "ALLOCATED" else if (isOver) "OVER ALLOCATED" else "REMAINING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isFullyAllocated) Color(0xFF4CAF50)
                                else if (isOver) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "$currency ${remainingAmount.abs().toPlainString()}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isFullyAllocated) Color(0xFF4CAF50)
                                else if (isOver) MaterialTheme.colorScheme.error
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
                itemsIndexed(splits) { index, split ->
                    SplitItem(
                        split = split,
                        categories = categories,
                        onRemove = { onRemoveSplit(index) },
                        onUpdate = { a, n, c -> onUpdateSplit(index, a, n, c) },
                        onShowCategory = { onShowCategorySheet(index) }
                    )
                }
            }

            if (!isFullyAllocated && splits.isNotEmpty()) {
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
                        onToggleSplitMode(false)
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
                    enabled = isFullyAllocated && splits.size >= 2,
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
}

@Composable
private fun SplitItem(
    split: TransactionSplit,
    categories: List<Category>,
    onRemove: () -> Unit,
    onUpdate: (String?, String?, Long?) -> Unit,
    onShowCategory: () -> Unit
) {
    val category = categories.find { it.id == split.categoryId }

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
                    onClick = onShowCategory,
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
                                onUpdate(it, null, null)
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
                    onClick = onRemove,
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
                onValueChange = { onUpdate(null, it, null) },
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
