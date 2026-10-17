package com.osscabinet.ossWallet.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.domain.model.settings.AppLockConfig
import com.osscabinet.ossWallet.ui.security.AppLockViewModel
import com.osscabinet.ossWallet.ui.security.BiometricHelper
import com.osscabinet.ossWallet.ui.security.PinSetupDialog
import com.osscabinet.ossWallet.ui.security.PinVerifyDialog
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.HazeLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    viewModel: SettingsViewModel,
    actions: SecuritySettingsActions,
    lockViewModel: AppLockViewModel = hiltViewModel(),
    assistantViewModel: AssistantAccessViewModel = hiltViewModel()
) {
    val assistantEnabled by assistantViewModel.enabled.collectAsStateWithLifecycle()
    val assistantNotice by assistantViewModel.notice.collectAsStateWithLifecycle()
    val mapsAccessEnabled by viewModel.mapsAccessEnabled.collectAsStateWithLifecycle()
    val lock by lockViewModel.config.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val biometricAvailable = remember { BiometricHelper.isAvailable(context) }
    var dialog by remember { mutableStateOf<LockDialog?>(null) }

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                            .statusBarsPadding()
                            .padding(
                                top = AeonSize.D64.value + AeonTheme.dimensions.screenPadding,
                                bottom = innerPadding.calculateBottomPadding()
                            )
                            .padding(horizontal = AeonTheme.dimensions.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacing2xl)
                    ) {
                        AeonSettingsSection("APP LOCK") {
                            EntrySwitchRow(
                                label = "App Lock",
                                subtitle = if (lock.isActive) "PIN required to open the app" else "Require a PIN or biometrics to open the app",
                                icon = Icons.Rounded.Lock,
                                checked = lock.isActive,
                                onCheckedChange = { on ->
                                    dialog = if (on) LockDialog.SetupPin else LockDialog.VerifyToDisable
                                },
                                isFlush = false
                            )
                            if (lock.isActive) {
                                SettingsDivider()
                                if (biometricAvailable) {
                                    EntrySwitchRow(
                                        label = "Unlock with biometrics",
                                        subtitle = "Fingerprint or face; PIN stays as fallback",
                                        icon = Icons.Rounded.Fingerprint,
                                        checked = lock.biometricEnabled,
                                        onCheckedChange = { lockViewModel.setBiometric(it) },
                                        isFlush = false
                                    )
                                    SettingsDivider()
                                }
                                AeonSettingsItem(
                                    title = "Lock after",
                                    subtitle = timeoutLabel(lock.timeoutSeconds),
                                    icon = Icons.Rounded.LockClock,
                                    onClick = { dialog = LockDialog.PickTimeout }
                                )
                                SettingsDivider()
                                AeonSettingsItem(
                                    title = "Change PIN",
                                    subtitle = "Confirm your current PIN first",
                                    icon = Icons.Rounded.Password,
                                    onClick = { dialog = LockDialog.VerifyToChange }
                                )
                            }
                        }

                        AeonSettingsSection("PRIVACY") {
                            EntrySwitchRow(
                                label = "Privacy screen",
                                subtitle = "Block screenshots and hide the app in recent apps",
                                icon = Icons.Rounded.VisibilityOff,
                                checked = lock.privacyScreenEnabled,
                                onCheckedChange = { lockViewModel.setPrivacyScreen(it) },
                                isFlush = false
                            )
                            SettingsDivider()
                            EntrySwitchRow(
                                label = "Location Auto-fill",
                                subtitle = if (mapsAccessEnabled) "Auto-fill current location for new transactions" else "Disabled location auto-fill",
                                icon = if (mapsAccessEnabled) Icons.Rounded.LocationOn else Icons.Rounded.LocationOff,
                                checked = mapsAccessEnabled,
                                onCheckedChange = { viewModel.setMapsAccessEnabled(it) },
                                isFlush = false
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.5f)
                            )
                            AeonSettingsItem(
                                title = "Open in Web",
                                subtitle = "Browse your data on the same WiFi network",
                                icon = Icons.Rounded.Wifi,
                                onClick = actions.onOpenInWebClick
                            )
                        }

                        // AI assistants can only call apps on Android 16+, so older devices never see this.
                        if (assistantViewModel.isSupported) {
                            AeonSettingsSection("AI ASSISTANTS") {
                                EntrySwitchRow(
                                    label = "Allow AI assistants",
                                    subtitle = "Let assistants add records, search and analyse your transactions",
                                    icon = Icons.Rounded.AutoAwesome,
                                    checked = assistantEnabled,
                                    onCheckedChange = { assistantViewModel.setEnabled(it) },
                                    isFlush = false
                                )
                                Text(
                                    "What an assistant asks for is answered on this device, but the answer goes to the assistant, " +
                                        "which may process it off your device. Assistants can add new records, never edit or delete, " +
                                        "and see nothing while OssWallet is locked.",
                                    style = AppTextStyles.caption,
                                    color = AppColors.contentSecondary,
                                    modifier = Modifier.padding(
                                        horizontal = AeonTheme.dimensions.screenPadding,
                                        vertical = AeonTheme.dimensions.spacingSm
                                    )
                                )
                                assistantNotice?.let {
                                    Text(
                                        it,
                                        style = AppTextStyles.caption,
                                        color = AppColors.error,
                                        modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            when (dialog) {
                LockDialog.SetupPin -> PinSetupDialog(
                    title = "Set a PIN",
                    onConfirm = { pin ->
                        lockViewModel.enable(pin, biometric = biometricAvailable)
                        dialog = null
                    },
                    onDismiss = { dialog = null }
                )
                LockDialog.VerifyToDisable -> PinVerifyDialog(
                    title = "Turn off App Lock",
                    verify = lockViewModel::verifyPin,
                    onVerified = { lockViewModel.disable(); dialog = null },
                    onDismiss = { dialog = null }
                )
                LockDialog.VerifyToChange -> PinVerifyDialog(
                    title = "Change PIN",
                    verify = lockViewModel::verifyPin,
                    onVerified = { dialog = LockDialog.ChangePin },
                    onDismiss = { dialog = null }
                )
                LockDialog.ChangePin -> PinSetupDialog(
                    title = "New PIN",
                    onConfirm = { pin -> lockViewModel.changePin(pin); dialog = null },
                    onDismiss = { dialog = null }
                )
                LockDialog.PickTimeout -> AlertDialog(
                    onDismissRequest = { dialog = null },
                    title = { Text("Lock after") },
                    text = {
                        Column {
                            AppLockConfig.TIMEOUT_OPTIONS_SECONDS.forEach { seconds ->
                                TextButton(onClick = {
                                    lockViewModel.setTimeoutSeconds(seconds)
                                    dialog = null
                                }) {
                                    Text(timeoutLabel(seconds) + if (seconds == lock.timeoutSeconds) "  ✓" else "")
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } }
                )
                null -> Unit
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Security & Privacy",
                            onBackClick = actions.onBackClick
                        )
                    }
                )
            )
        }
    }
}

data class SecuritySettingsActions(
    val onOpenInWebClick: () -> Unit,
    val onBackClick: () -> Unit
)

private enum class LockDialog { SetupPin, VerifyToDisable, VerifyToChange, ChangePin, PickTimeout }

private fun timeoutLabel(seconds: Int) = when (seconds) {
    0 -> "Immediately"
    60 -> "After 1 minute"
    300 -> "After 5 minutes"
    else -> "After ${seconds}s"
}

@Composable
private fun SettingsDivider() = HorizontalDivider(
    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
    thickness = 0.5.dp,
    color = AppColors.divider.copy(alpha = 0.5f)
)
