package com.osscabinet.ossWallet.ui.screens.settings.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.settings.ReminderConfig
import com.osscabinet.ossWallet.reminder.ReminderNotifier
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

private enum class PickerDialog { LeadDays, Hour }

@Composable
fun NotificationSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: ReminderSettingsViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var canPost by remember { mutableStateOf(ReminderNotifier.canPost(context)) }
    var picker by remember { mutableStateOf<PickerDialog?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { canPost = ReminderNotifier.canPost(context) }

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(containerColor = Color.Transparent) { innerPadding ->
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
                    if (config.enabled && !canPost) {
                        AeonSettingsSection("PERMISSION") {
                            AeonSettingsItem(
                                title = "Notifications are blocked",
                                subtitle = "Allow notifications so reminders can reach you",
                                icon = Icons.Rounded.Notifications,
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }
                    }
                    AeonSettingsSection("REMINDERS") {
                        EntrySwitchRow(
                            label = "Reminders",
                            subtitle = "Due dates and budget alerts",
                            icon = Icons.Rounded.Notifications,
                            checked = config.enabled,
                            onCheckedChange = { on ->
                                viewModel.update { it.copy(enabled = on) }
                                if (on && !canPost && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            isFlush = false
                        )
                        if (config.enabled) {
                            Divider()
                            EntrySwitchRow(
                                label = "Planned payments",
                                subtitle = "Before a payment is due",
                                icon = Icons.Rounded.EventRepeat,
                                checked = config.plannedPayments,
                                onCheckedChange = { v -> viewModel.update { it.copy(plannedPayments = v) } },
                                isFlush = false
                            )
                            Divider()
                            EntrySwitchRow(
                                label = "Loans",
                                subtitle = "Before an expected settlement date",
                                icon = Icons.Rounded.Handshake,
                                checked = config.loans,
                                onCheckedChange = { v -> viewModel.update { it.copy(loans = v) } },
                                isFlush = false
                            )
                            Divider()
                            EntrySwitchRow(
                                label = "Budget alerts",
                                subtitle = "At each budget's alert threshold and when exceeded",
                                icon = Icons.Rounded.AccountBalanceWallet,
                                checked = config.budgetAlerts,
                                onCheckedChange = { v -> viewModel.update { it.copy(budgetAlerts = v) } },
                                isFlush = false
                            )
                            Divider()
                            EntrySwitchRow(
                                label = "Recurring payment suggestions",
                                subtitle = "A note when repeated payments could become planned payments",
                                icon = Icons.Rounded.AutoAwesome,
                                checked = config.suggestions,
                                onCheckedChange = { v -> viewModel.update { it.copy(suggestions = v) } },
                                isFlush = false
                            )
                        }
                    }
                    if (config.enabled) {
                        AeonSettingsSection("TIMING") {
                            AeonSettingsItem(
                                title = "Remind me",
                                subtitle = leadLabel(config.leadDays),
                                icon = Icons.Rounded.Today,
                                onClick = { picker = PickerDialog.LeadDays }
                            )
                            Divider()
                            AeonSettingsItem(
                                title = "Not before",
                                subtitle = hourLabel(config.hourOfDay),
                                icon = Icons.Rounded.Schedule,
                                onClick = { picker = PickerDialog.Hour }
                            )
                        }
                    }
                }
            }

            when (picker) {
                PickerDialog.LeadDays -> OptionDialog(
                    title = "Remind me",
                    options = ReminderConfig.LEAD_DAY_OPTIONS,
                    selected = config.leadDays,
                    label = ::leadLabel,
                    onPick = { v -> viewModel.update { it.copy(leadDays = v) }; picker = null },
                    onDismiss = { picker = null }
                )
                PickerDialog.Hour -> OptionDialog(
                    title = "Not before",
                    options = ReminderConfig.HOUR_OPTIONS,
                    selected = config.hourOfDay,
                    label = ::hourLabel,
                    onPick = { v -> viewModel.update { it.copy(hourOfDay = v) }; picker = null },
                    onDismiss = { picker = null }
                )
                null -> Unit
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = { AeonTopBarNavTitlePill(title = "Notifications", onBackClick = onBackClick) }
                )
            )
        }
    }
}

@Composable
private fun OptionDialog(
    title: String,
    options: List<Int>,
    selected: Int,
    label: (Int) -> String,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { v ->
                    TextButton(onClick = { onPick(v) }) {
                        Text(label(v) + if (v == selected) "  ✓" else "")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun Divider() = HorizontalDivider(
    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
    thickness = 0.5.dp,
    color = AppColors.divider.copy(alpha = 0.5f)
)

private fun leadLabel(days: Int) = when (days) {
    0 -> "On the day"
    1 -> "1 day before"
    else -> "$days days before"
}

private fun hourLabel(hour: Int) = "%02d:00".format(hour)
