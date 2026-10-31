package com.osscabinet.ossWallet.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.settings.AppLockConfig
import com.osscabinet.ossWallet.ui.security.BiometricHelper.findFragmentActivity
import com.osscabinet.ossWallet.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen gate drawn over the whole UI. Opaque and non-interactive underneath — the
 * content below is never shown, only covered, so the caller must keep it from taking input.
 */
@Composable
fun LockScreen(
    manager: AppLockManager,
    biometricEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var lockoutEndsInMs by remember { mutableStateOf(0L) }
    val canUseBiometric = biometricEnabled && remember { BiometricHelper.isAvailable(context) }

    fun promptBiometric() {
        val activity = context.findFragmentActivity() ?: return
        BiometricHelper.prompt(
            activity = activity,
            title = "Unlock OssWallet",
            negativeText = "Use PIN",
            onSuccess = manager::unlock
        )
    }

    LaunchedEffect(canUseBiometric) { if (canUseBiometric) promptBiometric() }

    LaunchedEffect(lockoutEndsInMs) {
        var left = lockoutEndsInMs
        while (left > 0) {
            error = "Too many attempts. Try again in ${(left + 999) / 1000}s"
            delay(1000)
            left -= 1000
        }
        if (lockoutEndsInMs > 0) { error = null; lockoutEndsInMs = 0 }
    }

    fun submit() {
        if (pin.length < AppLockConfig.MIN_PIN_LENGTH || lockoutEndsInMs > 0) return
        val attempt = pin.toCharArray()
        scope.launch {
            when (val r = manager.tryPin(attempt)) {
                AppLockManager.PinResult.Success -> Unit
                is AppLockManager.PinResult.Wrong -> {
                    error = "Wrong PIN. ${r.attemptsLeft} attempts left"
                    pin = ""
                }
                is AppLockManager.PinResult.LockedOut -> { pin = ""; lockoutEndsInMs = r.remainingMs }
            }
            attempt.fill('0')
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.screenBackground)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = AppColors.contentSecondary)
        Spacer(Modifier.height(16.dp))
        Text("OssWallet is locked", style = MaterialTheme.typography.titleLarge, color = AppColors.contentPrimary)
        Spacer(Modifier.height(24.dp))
        PinField(
            value = pin,
            onValueChange = { pin = it; if (lockoutEndsInMs == 0L) error = null },
            label = "PIN",
            isError = error != null,
            onImeAction = ::submit,
            modifier = Modifier.fillMaxWidth()
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = ::submit,
            enabled = pin.length >= AppLockConfig.MIN_PIN_LENGTH && lockoutEndsInMs == 0L,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Unlock") }
        if (canUseBiometric) {
            TextButton(onClick = ::promptBiometric) {
                Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                Text("  Use biometrics")
            }
        }
    }
}
