package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

/**
 * Dialog for entering exchange rate between currencies.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeRateDialog(
    initialRate: BigDecimal?,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal) -> Unit
) {
    var rateText by remember(initialRate) { 
        mutableStateOf(initialRate?.toPlainString() ?: "1.0") 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Exchange Rate",
                style = AppTextStyles.heroAmount
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Enter the exchange rate for this transfer",
                    style = AppTextStyles.transactionTitle,
                    color = AppColors.contentSecondary
                )

                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Rate") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    rateText.toBigDecimalOrNull()?.let { onConfirm(it) }
                },
                shape = RoundedCornerShape(12.dp),
                enabled = rateText.toBigDecimalOrNull() != null
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ExchangeRateDialogPreview() {
    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        ExchangeRateDialog(
            initialRate = BigDecimal("1.25"),
            onDismiss = {},
            onConfirm = {}
        )
    }
}
