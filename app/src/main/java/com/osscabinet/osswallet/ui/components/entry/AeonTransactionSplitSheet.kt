package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.osscabinet.ossWallet.domain.model.TransactionSplit
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
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
    val dims = AeonTheme.dimensions
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.cardSurfaceProminent,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        val currentSplitSum = splits.sumOf { it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO }
        val remainingAmount = totalAmount - currentSplitSum
        val isFullyAllocated = remainingAmount.compareTo(BigDecimal.ZERO) == 0

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dims.spacing2xl)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.spacingXl, vertical = dims.spacingSm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SPLIT TRANSACTION",
                        style = AppTextStyles.drawerItemLabel.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = AppColors.primary
                    )
                    Text(
                        text = "Divide amount into multiple transactions",
                        style = AppTextStyles.transactionSwipeAction,
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                }

                Surface(
                    onClick = onAddSplit,
                    shape = CircleShape,
                    color = AppColors.primary.copy(alpha = 0.1f)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add Split",
                        modifier = Modifier
                            .padding(dims.spacingSm)
                            .size(dims.containerCornerRadius),
                        tint = AppColors.primary
                    )
                }
            }

            // Summary Row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.spacingXl, vertical = dims.spacingLg),
                color = AppColors.recessedSurface,
                shape = RoundedCornerShape(dims.cardCornerRadius),
                border = androidx.compose.foundation.BorderStroke(
                    (dims.spacingXs / 8),
                    AppColors.divider.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(dims.containerCornerRadius),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL AMOUNT",
                            style = AppTextStyles.transactionSwipeAction.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppColors.contentSecondary.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = "$currency ${totalAmount.toPlainString()}",
                            style = AppTextStyles.heroAmount
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val isOver = remainingAmount < BigDecimal.ZERO
                        Text(
                            text = if (isFullyAllocated) "ALLOCATED" else if (isOver) "OVER ALLOCATED" else "REMAINING",
                            style = AppTextStyles.transactionSwipeAction.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isFullyAllocated) PositiveAmountColor
                                else if (isOver) AppColors.error
                                else AppColors.primary
                            )
                        )
                        Text(
                            text = "$currency ${remainingAmount.abs().toPlainString()}",
                            style = AppTextStyles.calculatorKey.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isFullyAllocated) PositiveAmountColor
                                else if (isOver) AppColors.error
                                else AppColors.primary
                            )
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = dims.spacingXl),
                verticalArrangement = Arrangement.spacedBy(dims.spacingMd),
                contentPadding = PaddingValues(bottom = dims.spacingXl)
            ) {
                itemsIndexed(splits) { index, split ->
                    SplitItem(
                        split = split,
                        categories = categories,
                        onRemove = { onRemoveSplit(index) },
                        onUpdate = { a, n, c -> onUpdateSplit(index, a, n, c) },
                        onShowCategory = { onShowCategorySheet(index) },
                        dims = dims
                    )
                }
            }

            if (!isFullyAllocated && splits.isNotEmpty()) {
                Text(
                    text = "Allocation must match total amount",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = dims.spacingXl,
                            end = dims.spacingXl,
                            bottom = dims.spacingMd
                        ),
                    textAlign = TextAlign.Center,
                    style = AppTextStyles.chipLabel,
                    color = AppColors.error
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.spacingXl),
                horizontalArrangement = Arrangement.spacedBy(dims.spacingMd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        onToggleSplitMode(false)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(dims.spacing2xl + dims.spacingXl),
                    shape = RoundedCornerShape(dims.containerCornerRadius),
                    border = androidx.compose.foundation.BorderStroke(
                        (dims.spacingXs / 8),
                        AppColors.error.copy(alpha = 0.5f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.error
                    )
                ) {
                    Text(
                        text = "REVERT",
                        style = AppTextStyles.managementItemTitle
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(dims.spacing2xl + dims.spacingXl),
                    enabled = isFullyAllocated && splits.size >= 2,
                    shape = RoundedCornerShape(dims.containerCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.primary,
                        contentColor = AppColors.onPrimary
                    )
                ) {
                    Text(
                        text = "CONFIRM SPLIT",
                        style = AppTextStyles.managementItemTitle
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonTransactionSplitSheetPreview() {
    OssWalletTheme {
        AeonTransactionSplitSheet(
            totalAmount = BigDecimal("100.00"),
            currency = "USD",
            splits = listOf(
                TransactionSplit(amount = "60.00", categoryId = 1L, note = "Split 1"),
                TransactionSplit(amount = "40.00", categoryId = 2L, note = "Split 2")
            ),
            categories = listOf(
                Category(
                    id = 1L,
                    name = "Food",
                    iconName = "Restaurant",
                    colorHex = android.graphics.Color.parseColor("#FF5722"),
                    isDefault = false,
                    parentId = null
                ),
                Category(
                    id = 2L,
                    name = "Transport",
                    iconName = "DirectionsBus",
                    colorHex = android.graphics.Color.parseColor("#2196F3"),
                    isDefault = false,
                    parentId = null
                )
            ),
            onAddSplit = {},
            onRemoveSplit = {},
            onUpdateSplit = { _, _, _, _ -> },
            onShowCategorySheet = {},
            onToggleSplitMode = {},
            onDismiss = {}
        )
    }
}


@Composable
private fun SplitItem(
    split: TransactionSplit,
    categories: List<Category>,
    onRemove: () -> Unit,
    onUpdate: (String?, String?, Long?) -> Unit,
    onShowCategory: () -> Unit,
    dims: com.osscabinet.ossWallet.ui.theme.AeonDimensions
) {
    val category = categories.find { it.id == split.categoryId }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.recessedSurface,
        shape = RoundedCornerShape(dims.cardCornerRadius),
        border = androidx.compose.foundation.BorderStroke(
            (dims.spacingXs / 8),
            AppColors.divider.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(dims.spacingLg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Selector
                Surface(
                    onClick = onShowCategory,
                    modifier = Modifier.weight(1f),
                    color = AppColors.cardSurface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(dims.componentCornerRadius)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = dims.spacingMd,
                            vertical = dims.spacingSm
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = (category?.colorHex?.let { Color(it) }
                                ?: AppColors.primary).copy(alpha = 0.1f),
                            modifier = Modifier.size(dims.spacing2xl)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    getIconByName(category?.iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(dims.iconSizeSm),
                                    tint = category?.colorHex?.let { Color(it) }
                                        ?: AppColors.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(dims.spacingMd))
                        Text(
                            text = category?.name ?: "Select Category",
                            style = AppTextStyles.cardSecondaryAmount,
                            maxLines = 1,
                            color = if (category == null) AppColors.contentSecondary.copy(alpha = 0.6f) else AppColors.contentPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(dims.spacingMd))

                // Amount Input
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "AMOUNT",
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.contentSecondary.copy(alpha = 0.5f)
                        )
                    )
                    TextField(
                        value = split.amount,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                onUpdate(it, null, null)
                            }
                        },
                        modifier = Modifier.width(dims.iconSize3xl + dims.spacingXl),
                        placeholder = {
                            Text(
                                "0.00",
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AppColors.transparent,
                            unfocusedContainerColor = AppColors.transparent,
                            focusedIndicatorColor = AppColors.primary.copy(alpha = 0.5f),
                            unfocusedIndicatorColor = AppColors.transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = AppTextStyles.screenSubTitle.copy(
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.End
                        )
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .padding(start = dims.spacingXs)
                        .size(dims.spacingXl + dims.spacingMd)
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Remove",
                        tint = AppColors.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(dims.containerCornerRadius + 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(dims.spacingMd))

            // Note Input
            TextField(
                value = split.note,
                onValueChange = { onUpdate(null, it, null) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Add a note for this split...",
                        style = AppTextStyles.caption
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppColors.cardSurface.copy(alpha = 0.4f),
                    unfocusedContainerColor = AppColors.cardSurface.copy(alpha = 0.2f),
                    focusedIndicatorColor = AppColors.transparent,
                    unfocusedIndicatorColor = AppColors.transparent
                ),
                shape = RoundedCornerShape(dims.spacingMd),
                singleLine = true,
                textStyle = AppTextStyles.caption,
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Rounded.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(dims.spacingLg),
                        tint = AppColors.contentSecondary.copy(alpha = 0.5f)
                    )
                }
            )
        }
    }
}
