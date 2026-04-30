package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorSheet(
    currentAmount: String,
    onAmountChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var displayValue by remember { mutableStateOf(currentAmount.ifEmpty { "0" }) }
    var previousValue by remember { mutableStateOf(BigDecimal.ZERO) }
    var operation by remember { mutableStateOf<String?>(null) }
    var shouldResetDisplay by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Numpad grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: 7 8 9 ÷
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumpadButton("7") { appendDigit("7", displayValue, shouldResetDisplay) { displayValue = it } }
                    NumpadButton("8") { appendDigit("8", displayValue, shouldResetDisplay) { displayValue = it } }
                    NumpadButton("9") { appendDigit("9", displayValue, shouldResetDisplay) { displayValue = it } }
                    OperationButton("÷") { /* Set operation */ }
                }

                // Row 2: 4 5 6 ×
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumpadButton("4") { appendDigit("4", displayValue, shouldResetDisplay) { displayValue = it } }
                    NumpadButton("5") { appendDigit("5", displayValue, shouldResetDisplay) { displayValue = it } }
                    NumpadButton("6") { appendDigit("6", displayValue, shouldResetDisplay) { displayValue = it } }
                    OperationButton("×") { /* Set operation */ }
                }

                // Row 3: 1 2 3 -
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumpadButton("1") { appendDigit("1", displayValue, shouldResetDisplay) { displayValue = it } }
                    NumpadButton("2") { appendDigit("2", displayValue, shouldResetDisplay) { displayValue = it } }
                    NumpadButton("3") { appendDigit("3", displayValue, shouldResetDisplay) { displayValue = it } }
                    OperationButton("-") { /* Set operation */ }
                }

                // Row 4: . 0 = +
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumpadButton(".") { appendDecimal(displayValue) { displayValue = it } }
                    NumpadButton("0") { appendDigit("0", displayValue, shouldResetDisplay) { displayValue = it } }
                    FilledIconButton(
                        onClick = {
                            onAmountChanged(displayValue)
                            onDismiss()
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Confirm")
                    }
                    OperationButton("+") { /* Set operation */ }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clear row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { displayValue = "0" }) { Text("C") }
                TextButton(onClick = {
                    displayValue = displayValue.dropLast(1).ifEmpty { "0" }
                }) { Text("⌫") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun RowScope.NumpadButton(
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun RowScope.OperationButton(
    label: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

private fun appendDigit(
    digit: String,
    current: String,
    shouldReset: Boolean,
    update: (String) -> Unit
) {
    if (shouldReset) {
        update(digit)
    } else {
        if (current == "0") {
            update(digit)
        } else {
            update(current + digit)
        }
    }
}

private fun appendDecimal(current: String, update: (String) -> Unit) {
    if (!current.contains(".")) {
        update(current + ".")
    }
}
