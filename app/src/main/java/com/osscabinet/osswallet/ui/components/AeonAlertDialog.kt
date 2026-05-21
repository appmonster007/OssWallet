package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Standardized Delete Confirmation Dialog.
 */
@Composable
fun AeonConfirmDeleteDialog(
    show: Boolean,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = "Delete",
    dismissButtonText: String = "Cancel",
    extraContent: @Composable (() -> Unit)? = null
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title, style = AppTextStyles.heroAmount) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(message)
                    extraContent?.invoke()
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(confirmButtonText, color = AppColors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(dismissButtonText)
                }
            }
        )
    }
}

/**
 * Standardized Info Dialog.
 */
@Composable
fun AeonInfoDialog(
    show: Boolean,
    title: String,
    message: String,
    onDismiss: () -> Unit,
    buttonText: String = "OK"
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title, style = AppTextStyles.heroAmount) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(buttonText)
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AeonConfirmDeleteDialogPreview() {
    OssWalletTheme {
        Surface {
            AeonConfirmDeleteDialog(
                show = true,
                title = "Delete Transaction?",
                message = "Are you sure you want to delete this transaction? This action cannot be undone.",
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonInfoDialogPreview() {
    OssWalletTheme {
        Surface {
            AeonInfoDialog(
                show = true,
                title = "Account Info",
                message = "This account is used for your monthly savings. You can manage your budget from the settings menu.",
                onDismiss = {}
            )
        }
    }
}
