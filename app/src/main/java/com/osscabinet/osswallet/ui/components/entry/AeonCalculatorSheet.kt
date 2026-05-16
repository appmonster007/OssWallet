package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad
import com.osscabinet.ossWallet.ui.theme.AppColors

/**
 * Calculator bottom sheet using the full CalculatorNumpad component.
 * Following the "Architect Slate" design specification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonCalculatorSheet(
    currentAmount: String,
    transactionType: TransactionType = TransactionType.INCOME,
    decimalPrecision: Int = 2,
    onAmountChanged: (String) -> Unit,
    onTypeChanged: (TransactionType) -> Unit = {},
    onDismiss: () -> Unit,
    title: String? = null
) {
    val dims = AeonTheme.dimensions

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = dims.dialogCornerRadius, topEnd = dims.dialogCornerRadius),
        containerColor = AppColors.cardSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.divider.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = dims.spacingXl)
                .padding(bottom = dims.spacing2xl)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = dims.spacingLg, start = dims.spacingSm)
                )
            }

            CalculatorNumpad(
                amount = currentAmount,
                onAmountChange = { amount, type ->
                    onAmountChanged(amount)
                    onTypeChanged(type)
                },
                transactionType = transactionType,
                decimalPrecision = decimalPrecision,
                onDone = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
