package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.SplitStatus
import com.osscabinet.ossWallet.domain.model.TransactionSplit
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.visualization.AeonSegmentedProgressBar
import com.osscabinet.ossWallet.ui.components.visualization.ProgressBarSegment
import com.osscabinet.ossWallet.ui.theme.AeonDimensions
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.scaledSp
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.ui.util.formatCurrency
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Bottom sheet to manage transaction splitting.
 *
 * Visual grammar mirrors [com.osscabinet.ossWallet.ui.components.transactions.AeonTransactionItem]:
 *  - 32dp tinted category icon container + 18dp inner glyph, with bottom-left status dot.
 *  - Title (transactionTitle, 12sp Bold) + account-line + caption note.
 *  - Right-aligned transactionAmount (display family, 15sp Medium, colored by flow type)
 *    with transactionAmountMeta (8sp) share-% line beneath.
 *  - 0.5dp hairline dividers between rows.
 *
 * Keyboard handling: content has `imePadding()` so the focused note/amount field
 * lifts above the IME instead of being covered.
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
    onDismiss: () -> Unit,
    isIncome: Boolean = false,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.cardSurfaceProminent,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        AeonTransactionSplitContent(
            totalAmount = totalAmount,
            currency = currency,
            splits = splits,
            categories = categories,
            isIncome = isIncome,
            onAddSplit = onAddSplit,
            onRemoveSplit = onRemoveSplit,
            onUpdateSplit = onUpdateSplit,
            onShowCategorySheet = onShowCategorySheet,
            onToggleSplitMode = onToggleSplitMode,
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun AeonTransactionSplitContent(
    totalAmount: BigDecimal,
    currency: String,
    splits: List<TransactionSplit>,
    categories: List<Category>,
    onAddSplit: () -> Unit,
    onRemoveSplit: (Int) -> Unit,
    onUpdateSplit: (Int, String?, String?, Long?) -> Unit,
    onShowCategorySheet: (Int) -> Unit,
    onToggleSplitMode: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    isIncome: Boolean = false,
) {
    val dims = AeonTheme.dimensions
    val status = remember(totalAmount, splits) { SplitStatus.compute(totalAmount, splits) }
    val currentSum = remember(splits) { splits.sumOf { it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO } }
    val progress = remember(currentSum, totalAmount) {
        if (totalAmount.signum() == 0) 0f
        else currentSum.divide(totalAmount, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1.2f)
    }

    val amountColor = if (isIncome) AppColors.positive else AppColors.negative

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = dims.spacingXl),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dims.spacingXl, vertical = dims.spacingSm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SPLIT TRANSACTION",
                style = AppTextStyles.sectionOverline,
                color = AppColors.contentSecondary,
            )
        }


        SplitHeaderCard(
            totalAmount = totalAmount,
            status = status,
            splits = splits,
            categories = categories,
            currency = currency,
            progress = progress,
            dims = dims,
        )

        SectionLabel(dims = dims)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dims.spacingXl),
            color = AppColors.cardSurface,
            shape = RoundedCornerShape(dims.componentCornerRadius),
            border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.25f)),
        ) {
            Column {
                if (splits.isEmpty()) {
                    EmptyState(dims = dims, onAddSplit = onAddSplit)
                } else {
                    splits.forEachIndexed { index, split ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = dims.listPadding + dims.iconSizeLg + dims.spacingSm),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.5f),
                            )
                        }
                        SplitRow(
                            split = split,
                            totalAmount = totalAmount,
                            currency = currency,
                            categories = categories,
                            amountColor = amountColor,
                            isIncome = isIncome,
                            isOverRow = status is SplitStatus.OverAllocated && index == splits.lastIndex,
                            onRemove = { onRemoveSplit(index) },
                            onUpdate = { a, n, c -> onUpdateSplit(index, a, n, c) },
                            onShowCategory = { onShowCategorySheet(index) },
                        )
                    }
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = AppColors.divider.copy(alpha = 0.5f),
                    )
                    AddSplitRow(onAddSplit = onAddSplit, dims = dims)
                }
            }
        }

        Spacer(modifier = Modifier.height(dims.spacingLg))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dims.spacingXl),
            horizontalArrangement = Arrangement.spacedBy(dims.spacingMd),
        ) {
            OutlinedButton(
                onClick = {
                    onToggleSplitMode(false)
                    onDismiss()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(dims.buttonCornerRadius),
                border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.contentSecondary),
            ) {
                Text("REVERT", style = AppTextStyles.buttonLabel)
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1.4f)
                    .height(48.dp),
                enabled = status is SplitStatus.Balanced && splits.size >= 2,
                shape = RoundedCornerShape(dims.buttonCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primary,
                    contentColor = AppColors.onPrimary,
                ),
            ) {
                Text("CONFIRM SPLIT", style = AppTextStyles.buttonLabel)
            }
        }
    }
}

@Composable
private fun SplitStatusPill(
    status: SplitStatus,
    currency: String
) {
    val (label, color) = when (status) {
        is SplitStatus.OverAllocated -> "OVER BY ${formatCurrency(status.excess, currency)}" to AppColors.error
        is SplitStatus.Balanced -> "BALANCED" to AppColors.positive
        is SplitStatus.Splitting -> "SPLITTING (${formatCurrency(status.remaining, currency)} LEFT)" to AppColors.primary
        is SplitStatus.Drafting -> "REMAINING" to AppColors.contentSecondary
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape),
        )
        Text(text = label, style = AppTextStyles.overlineLabel, color = color)
    }
}

@Composable
private fun SplitHeaderCard(
    totalAmount: BigDecimal,
    status: SplitStatus,
    splits: List<TransactionSplit>,
    categories: List<Category>,
    currency: String,
    progress: Float,
    dims: AeonDimensions,
) {
    val accent = when (status) {
        is SplitStatus.OverAllocated -> AppColors.error
        is SplitStatus.Balanced -> AppColors.positive
        else -> AppColors.primary
    }
    val remainingAmount = when (status) {
        is SplitStatus.OverAllocated -> status.excess
        is SplitStatus.Splitting -> status.remaining
        else -> BigDecimal.ZERO
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacingXl, vertical = dims.spacingXs),
        color = AppColors.cardSurface,
        shape = RoundedCornerShape(dims.componentCornerRadius),
        border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.25f)),
    ) {
        Column(modifier = Modifier.padding(dims.cardPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(
                        text = "TOTAL",
                        style = AppTextStyles.overlineLabel,
                        color = AppColors.contentSecondary,
                    )
                    Text(
                        text = CurrencyReprUi.formatCurrencyAnnotated(
                            amount = totalAmount,
                            currencyCode = currency,
                            spec = CurrencyDisplaySpec(useSemanticColor = false),
                        ),
                        style = AppTextStyles.transactionAmount.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.contentPrimary,
                        ),
                        maxLines = 1,
                    )
                }
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.End
                ) {
                    SplitStatusPill(
                        status = status,
                        currency = currency
                    )
                    Text(
                        text = CurrencyReprUi.formatCurrencyAnnotated(
                            amount = remainingAmount,
                            currencyCode = currency,
                            spec = CurrencyDisplaySpec(useSemanticColor = false),
                        ),
                        style = AppTextStyles.transactionAmount.copy(
                            fontSize = 15.sp,
                            color = accent,
                        ),
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(dims.spacingMd))

            // Multi-colored progress bar
            val errorColor = AppColors.error
            val primaryColor = AppColors.primary
            val segments = remember(splits, status, totalAmount, errorColor, primaryColor) {
                if (status is SplitStatus.OverAllocated) {
                    listOf(ProgressBarSegment(1f, errorColor))
                } else {
                    val list = mutableListOf<ProgressBarSegment>()
                    var totalWeight = 0f
                    splits.forEach { split ->
                        val splitAmount = split.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        if (splitAmount.signum() > 0 && totalAmount.signum() > 0) {
                            val fraction = splitAmount.divide(totalAmount, 8, RoundingMode.HALF_UP).toFloat()
                            val catColor = categories.find { it.id == split.categoryId }?.colorHex?.let { Color(it) } ?: primaryColor
                            list.add(ProgressBarSegment(fraction, catColor))
                            totalWeight += fraction
                        }
                    }
                    if (totalWeight < 1f) {
                        list.add(ProgressBarSegment(1f - totalWeight, Color.Transparent))
                    }
                    list
                }
            }

            AeonSegmentedProgressBar(
                segments = segments,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${(progress * 100).toInt()}% ALLOCATED",
                    style = AppTextStyles.overlineLabel,
                    color = accent,
                )
                Text(
                    text = if (splits.isEmpty()) "NO PARTS YET" else "${splits.size} PART${if (splits.size == 1) "" else "S"}",
                    style = AppTextStyles.overlineLabel,
                    color = AppColors.contentSecondary,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(
    dims: AeonDimensions,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spacingXl, vertical = dims.spacingSm)
            .padding(top = dims.spacingSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Splits",
            style = AppTextStyles.transactionTitle.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            ),
            color = AppColors.contentPrimary,
        )
    }
}

@Composable
private fun EmptyState(
    dims: com.osscabinet.ossWallet.ui.theme.AeonDimensions,
    onAddSplit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddSplit)
            .padding(vertical = dims.spacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(AppColors.primary.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = AppColors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.height(dims.spacingSm))
        Text(
            text = "No splits yet",
            style = AppTextStyles.transactionTitle.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            ),
            color = AppColors.contentPrimary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Tap to allocate across categories",
            style = AppTextStyles.caption.copy(fontSize = 11.sp),
            color = AppColors.contentSecondary,
        )
    }
}

@Composable
private fun AddSplitRow(
    onAddSplit: () -> Unit,
    dims: AeonDimensions,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddSplit)
            .padding(horizontal = dims.listPadding, vertical = dims.spacingMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = null,
            tint = AppColors.primary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "ADD SPLIT",
            style = AppTextStyles.buttonLabel.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            color = AppColors.primary,
        )
    }
}

@Composable
private fun SplitRow(
    split: TransactionSplit,
    totalAmount: BigDecimal,
    currency: String,
    categories: List<Category>,
    amountColor: Color,
    isIncome: Boolean,
    isOverRow: Boolean,
    onRemove: () -> Unit,
    onUpdate: (String?, String?, Long?) -> Unit,
    onShowCategory: () -> Unit,
) {
    val dims = AeonTheme.dimensions
    val uiScale = AeonTheme.uiScale
    val category = categories.find { it.id == split.categoryId }
    val iconBg = category?.colorHex?.let { Color(it) } ?: AppColors.primary
    val splitAmountString = split.amount
    val splitAmount = splitAmountString.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val sharePct = remember(splitAmount, totalAmount) {
        if (totalAmount.signum() == 0) 0
        else splitAmount.divide(totalAmount, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .toInt()
    }
    val finalAmountColor = if (isOverRow) AppColors.error else amountColor
    val sign = if (isIncome) "+" else "-"
    val symbol = remember(currency) { CurrencyService.getCurrencySymbol(currency) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.listPadding, vertical = dims.spacingSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon — mirrors AeonTransactionItem.icon block
        Box(
            modifier = Modifier.size(dims.iconSizeLg)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onShowCategory),
                shape = CircleShape,
                color = iconBg.copy(alpha = 0.2f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        getIconByName(category?.iconName),
                        contentDescription = null,
                        tint = iconBg,
                        modifier = Modifier.size(dims.iconSizeSm),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(dims.spacingSm))

        Row(
            modifier = Modifier.weight(1f).height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(dims.spacingXs),
        ) {
            // Middle column — title (12sp Bold) + note (11sp)
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = category?.name ?: "Select category",
                    style = AppTextStyles.transactionTitle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = scaledSp(12f, uiScale),
                        color = if (category == null) AppColors.contentSecondary else AppColors.contentPrimary,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onShowCategory),
                )
                BasicTextField(
                    value = split.note,
                    onValueChange = { onUpdate(null, it, null) },
                    textStyle = AppTextStyles.caption.copy(
                        fontSize = scaledSp(11f, uiScale),
                        color = AppColors.contentSecondary,
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(AppColors.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    decorationBox = { inner ->
                        if (split.note.isEmpty()) {
                            Text(
                                text = "Add note",
                                style = AppTextStyles.caption.copy(
                                    fontSize = scaledSp(11f, uiScale),
                                    color = AppColors.contentSecondary.copy(alpha = 0.5f),
                                ),
                            )
                        }
                        inner()
                    },
                )
            }

            // Amount column — sign + symbol + amount cohesive unit
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = { /* Trigger calculator if needed */ })
                ) {
                    val amountDisplayText = buildString {
                        append(sign)
                        append(symbol)
                        if (splitAmountString.isEmpty()) append("0.00")
                        else append(splitAmountString)
                    }

                    Text(
                        text = amountDisplayText,
                        style = AppTextStyles.transactionAmount.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = scaledSp(15f, uiScale),
                            color = if (splitAmountString.isEmpty()) finalAmountColor.copy(alpha = 0.25f) else finalAmountColor,
                            textAlign = TextAlign.End,
                        ),
                        maxLines = 1,
                    )
                }
                if (totalAmount.signum() > 0) {
                    Text(
                        text = "$sharePct% of total",
                        style = AppTextStyles.transactionAmountMeta.copy(
                            fontSize = scaledSp(9f, uiScale)
                        ),
                        color = finalAmountColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(dims.spacingSm))

        Surface(
            onClick = onRemove,
            modifier = Modifier.size(dims.iconSizeMd),
            shape = CircleShape,
            color = AppColors.error.copy(alpha = 0.08f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Remove split",
                    tint = AppColors.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(dims.iconSizeSm),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonTransactionSplitSheetPreview() {
    OssWalletTheme {
        Surface(color = AppColors.cardSurfaceProminent) {
            AeonTransactionSplitContent(
                totalAmount = BigDecimal("100.00"),
                currency = "USD",
                splits = listOf(
                    TransactionSplit(amount = "60.00", categoryId = 1L, note = "Dinner with the team"),
                    TransactionSplit(amount = "40.00", categoryId = 2L, note = "Cabs split between us"),
                ),
                categories = listOf(
                    Category(1L, "Food", "Restaurant", android.graphics.Color.parseColor("#FF5722"), false, null),
                    Category(2L, "Transport", "DirectionsBus", android.graphics.Color.parseColor("#2196F3"), false, null),
                ),
                onAddSplit = {},
                onRemoveSplit = {},
                onUpdateSplit = { _, _, _, _ -> },
                onShowCategorySheet = {},
                onToggleSplitMode = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
fun AeonTransactionSplitSheetEmptyPreview() {
    OssWalletTheme {
        Surface(color = AppColors.cardSurfaceProminent) {
            AeonTransactionSplitContent(
                totalAmount = BigDecimal("100.00"),
                currency = "USD",
                splits = emptyList(),
                categories = emptyList(),
                onAddSplit = {},
                onRemoveSplit = {},
                onUpdateSplit = { _, _, _, _ -> },
                onShowCategorySheet = {},
                onToggleSplitMode = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(name = "Over Allocated", showBackground = true)
@Composable
fun AeonTransactionSplitSheetOverAllocatedPreview() {
    OssWalletTheme {
        Surface(color = AppColors.cardSurfaceProminent) {
            AeonTransactionSplitContent(
                totalAmount = BigDecimal("100.00"),
                currency = "USD",
                splits = listOf(
                    TransactionSplit(amount = "80.00", categoryId = 1L, note = "Dinner"),
                    TransactionSplit(amount = "40.00", categoryId = 2L, note = "Cabs"),
                ),
                categories = listOf(
                    Category(1L, "Food", "Restaurant", android.graphics.Color.parseColor("#FF5722"), false, null),
                    Category(2L, "Transport", "DirectionsBus", android.graphics.Color.parseColor("#2196F3"), false, null),
                ),
                onAddSplit = {},
                onRemoveSplit = {},
                onUpdateSplit = { _, _, _, _ -> },
                onShowCategorySheet = {},
                onToggleSplitMode = {},
                onDismiss = {},
            )
        }
    }
}
