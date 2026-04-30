package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad

/**
 * Calculator bottom sheet using the full CalculatorNumpad component
 * with expression evaluation, history, and full arithmetic operations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorSheet(
    currentAmount: String,
    transactionType: Int, // 0=expense, 1=income, 2=transfer
    decimalPrecision: Int = 2,
    onAmountChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Map transaction type to string for CalculatorNumpad
    val transactionTypeStr = when (transactionType) {
        0 -> "expense"
        1 -> "income"
        else -> "expense" // transfers show as expense (negative)
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp)
                .padding(bottom = 48.dp)
        ) {
            CalculatorNumpad(
                amount = currentAmount,
                onAmountChange = { amount, _ ->
                    onAmountChanged(amount)
                },
                transactionType = transactionTypeStr,
                decimalPrecision = decimalPrecision,
                onDone = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
