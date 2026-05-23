package com.osscabinet.ossWallet.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    viewModel: SettingsViewModel,
    onOpenInWebClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val mapsAccessEnabled by viewModel.mapsAccessEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.exportBackup(it, BackupType.CSV_ZIP) } }

    val exportSqliteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri -> uri?.let { viewModel.exportBackup(it, BackupType.SQLITE) } }

    var pendingImportType by remember { mutableStateOf<BackupType?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { u ->
            pendingImportType?.let { type ->
                viewModel.importBackup(u, type)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.backupMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Data") },
            text = { Text("Select the format you want to export your data in.") },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showExportDialog = false
                            exportZipLauncher.launch("osswallet_backup_${System.currentTimeMillis()}.zip")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ZIP of CSV Files")
                    }
                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            exportSqliteLauncher.launch("osswallet_db_${System.currentTimeMillis()}.sqlite")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SQLite Database File")
                    }
                    TextButton(
                        onClick = { showExportDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Data") },
            text = { Text("Select the format of the backup file you want to import.") },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showImportDialog = false
                            pendingImportType = BackupType.CSV_ZIP
                            importLauncher.launch("application/zip")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ZIP of CSV Files")
                    }
                    OutlinedButton(
                        onClick = {
                            showImportDialog = false
                            pendingImportType = BackupType.SQLITE
                            importLauncher.launch("*/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SQLite Database File")
                    }
                    TextButton(
                        onClick = { showImportDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Security", style = AppTextStyles.calculatorKey) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AppColors.screenBackground)
                .verticalScroll(rememberScrollState())
                .padding(AeonTheme.dimensions.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacing2xl)
        ) {
            AeonSettingsSection("PRIVACY") {
                AeonSettingsItem(
                    title = "Location Auto-fill",
                    subtitle = if (mapsAccessEnabled) "Auto-fill current location for new transactions" else "Disabled location auto-fill",
                    icon = if (mapsAccessEnabled) Icons.Rounded.LocationOn else Icons.Rounded.LocationOff,
                    trailingContent = {
                        Switch(
                            checked = mapsAccessEnabled,
                            onCheckedChange = { viewModel.setMapsAccessEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppColors.primary,
                                checkedTrackColor = AppColors.switchTrackOn,
                            )
                        )
                    }
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
                    onClick = onOpenInWebClick
                )
            }

            AeonSettingsSection("BACKUP & RESTORE") {
                AeonSettingsItem(
                    title = "Import Backup",
                    subtitle = "Restore your history from a file",
                    icon = Icons.Rounded.CloudSync,
                    onClick = { showImportDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                    thickness = 0.5.dp,
                    color = AppColors.divider.copy(alpha = 0.5f)
                )
                AeonSettingsItem(
                    title = "Export Data",
                    subtitle = "Download all records and database",
                    icon = Icons.Rounded.FileDownload,
                    onClick = { showExportDialog = true }
                )
            }

            AeonSettingsSection("DANGER ZONE") {
                AeonSettingsItem(
                    title = "Delete All Data",
                    subtitle = "Permanently clear your account",
                    icon = Icons.Rounded.DeleteForever,
                    color = AppColors.error,
                    onClick = { /* Handle delete */ }
                )
            }
        }
    }
}
