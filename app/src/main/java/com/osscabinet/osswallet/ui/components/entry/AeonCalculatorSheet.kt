package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad

/**
 * Calculator bottom sheet using the full CalculatorNumpad component.
 * Following the "Architect Slate" design specification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonCalculatorSheet(
    currentAmount: String,
    transactionType: TransactionType,
    decimalPrecision: Int = 2,
    onAmountChanged: (String) -> Unit,
    onTypeChanged: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val transactionTypeStr = transactionType.value

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            CalculatorNumpad(
                amount = currentAmount,
                onAmountChange = { amount, type ->
                    onAmountChanged(amount)
                    onTypeChanged(type)
                },
                transactionType = transactionTypeStr,
                decimalPrecision = decimalPrecision,
                onDone = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
