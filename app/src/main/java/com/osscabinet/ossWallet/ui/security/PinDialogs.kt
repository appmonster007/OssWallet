package com.osscabinet.ossWallet.ui.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.osscabinet.ossWallet.domain.model.settings.AppLockConfig
import kotlinx.coroutines.launch

/** Choose + confirm a new PIN. [onConfirm] receives a fresh array the callee owns. */
@Composable
fun PinSetupDialog(
    title: String,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val tooShort = first.isNotEmpty() && first.length < AppLockConfig.MIN_PIN_LENGTH
    val mismatch = second.isNotEmpty() && second != first
    val valid = first.length >= AppLockConfig.MIN_PIN_LENGTH && first == second

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                PinField(first, { first = it }, "New PIN (${AppLockConfig.MIN_PIN_LENGTH}–${AppLockConfig.MAX_PIN_LENGTH} digits)",
                    Modifier.fillMaxWidth(), isError = tooShort, imeAction = ImeAction.Next)
                PinField(second, { second = it }, "Confirm PIN", Modifier.fillMaxWidth(), isError = mismatch,
                    onImeAction = { if (valid) onConfirm(first.toCharArray()) })
                if (mismatch) Text("PINs don't match", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                Text("If you forget this PIN the only way back in is restoring from a backup.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(first.toCharArray()) }, enabled = valid) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Re-authenticates with the current PIN before a sensitive change. */
@Composable
fun PinVerifyDialog(
    title: String,
    verify: suspend (CharArray) -> Boolean,
    onVerified: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        val attempt = pin.toCharArray()
        scope.launch {
            if (verify(attempt)) onVerified() else { wrong = true; pin = "" }
            attempt.fill('0')
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                PinField(pin, { pin = it; wrong = false }, "Current PIN", Modifier.fillMaxWidth(),
                    isError = wrong, onImeAction = ::submit)
                if (wrong) Text("Wrong PIN", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = ::submit, enabled = pin.length >= AppLockConfig.MIN_PIN_LENGTH) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
