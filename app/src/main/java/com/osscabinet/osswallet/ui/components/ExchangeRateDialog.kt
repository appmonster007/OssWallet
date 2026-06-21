package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import java.math.BigDecimal

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
                style = AppTextStyles.cardTitle
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Enter the exchange rate for this transfer",
                    style = AppTextStyles.caption,
                    color = AppColors.contentSecondary
                )

                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Rate", style = AppTextStyles.formFieldHint) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                    textStyle = AppTextStyles.formFieldValue
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    rateText.toBigDecimalOrNull()?.let { onConfirm(it) }
                },
                shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
                enabled = rateText.toBigDecimalOrNull() != null,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = AppColors.primary,
                    contentColor = AppColors.onPrimary
                )
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                )
                Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingSm))
                Text("Confirm", style = AppTextStyles.buttonLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = AppTextStyles.buttonLabel, color = AppColors.primary)
            }
        },
        shape = RoundedCornerShape(AeonTheme.dimensions.dialogCornerRadius),
        containerColor = AppColors.panelSurface
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
