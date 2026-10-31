package com.osscabinet.ossWallet.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import com.osscabinet.ossWallet.R

const val MIN_BACKUP_PASSPHRASE_LENGTH = 8

@Composable
private fun PassphraseField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    isError: Boolean = false
) = OutlinedTextField(
    value = value,
    onValueChange = onChange,
    label = { Text(label) },
    singleLine = true,
    isError = isError,
    visualTransformation = PasswordVisualTransformation(),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    modifier = Modifier.fillMaxWidth()
)

/** Choose + confirm a backup passphrase, with the unrecoverable-loss warning up front. */
@Composable
fun PassphraseSetupDialog(
    title: String,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val tooShort = first.isNotEmpty() && first.length < MIN_BACKUP_PASSPHRASE_LENGTH
    val mismatch = second.isNotEmpty() && second != first
    val valid = first.length >= MIN_BACKUP_PASSPHRASE_LENGTH && first == second

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    stringResource(R.string.backup_passphrase_warning),
                    style = MaterialTheme.typography.bodySmall
                )
                PassphraseField(first, { first = it }, stringResource(R.string.backup_passphrase_label, MIN_BACKUP_PASSPHRASE_LENGTH), tooShort)
                PassphraseField(second, { second = it }, stringResource(R.string.backup_passphrase_confirm_label), mismatch)
                if (mismatch) Text(stringResource(R.string.backup_passphrase_mismatch), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(first.toCharArray()) }, enabled = valid) { Text(stringResource(R.string.backup_passphrase_continue)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/** Ask for the passphrase of an existing encrypted backup. */
@Composable
fun PassphraseEntryDialog(
    title: String,
    errorText: String? = null,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit
) {
    var pass by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                PassphraseField(pass, { pass = it }, stringResource(R.string.backup_passphrase_field_label), errorText != null)
                if (errorText != null) Text(errorText, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(pass.toCharArray()) }, enabled = pass.isNotEmpty()) { Text(stringResource(R.string.backup_passphrase_unlock)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
