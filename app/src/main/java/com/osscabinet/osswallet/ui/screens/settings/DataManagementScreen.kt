package com.osscabinet.ossWallet.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig
import com.osscabinet.ossWallet.domain.repository.BackupScope
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.domain.repository.RestorableEntity
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    viewModel: SettingsViewModel,
    actions: DataManagementActions
) {
    val context = LocalContext.current
    val autoBackupConfig by viewModel.autoBackupConfig.collectAsStateWithLifecycle()
    val recentBackups by viewModel.recentBackups.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var autoBackupExpanded by remember { mutableStateOf(true) }

    // Entity selector state for selective import
    var showEntitySelector by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingImportType by remember { mutableStateOf<BackupType?>(null) }
    var selectedEntities by remember { mutableStateOf(RestorableEntity.entries.toSet()) }

    val exportZipAllLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.exportBackup(it, BackupType.CSV_ZIP, BackupScope.ALL_PROFILES) } }

    val exportZipProfileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.exportBackup(it, BackupType.CSV_ZIP, BackupScope.SINGLE_PROFILE) } }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportBackup(it, BackupType.FLAT_CSV) } }

    val exportSqliteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri -> uri?.let { viewModel.exportBackup(it, BackupType.SQLITE) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showEntitySelector = true
        }
    }

    val customFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { folderUri ->
            viewModel.updateAutoBackupConfig(autoBackupConfig.copy(customFolderUri = folderUri.toString()))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.backupMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExportZipAll = {
                showExportDialog = false
                exportZipAllLauncher.launch("osswallet_backup_${System.currentTimeMillis()}.zip")
            },
            onExportZipProfile = {
                showExportDialog = false
                exportZipProfileLauncher.launch("osswallet_profile_${System.currentTimeMillis()}.zip")
            },
            onExportCsv = {
                showExportDialog = false
                exportCsvLauncher.launch("osswallet_transactions_${System.currentTimeMillis()}.csv")
            },
            onExportSqlite = {
                showExportDialog = false
                exportSqliteLauncher.launch("osswallet_db_${System.currentTimeMillis()}.sqlite")
            }
        )
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onImportZip = {
                showImportDialog = false
                pendingImportType = BackupType.CSV_ZIP
                importLauncher.launch("application/zip")
            },
            onImportSqlite = {
                showImportDialog = false
                pendingImportType = BackupType.SQLITE
                importLauncher.launch("*/*")
            },
            onImportCsv = {
                showImportDialog = false
                actions.onNavigateToCsvImport()
            }
        )
    }

    if (showEntitySelector && pendingImportUri != null) {
        RestorableEntitySelectorDialog(
            selectedEntities = selectedEntities,
            onSelectionChanged = { selectedEntities = it },
            onConfirm = {
                showEntitySelector = false
                pendingImportUri?.let { uri ->
                    pendingImportType?.let { type ->
                        viewModel.importBackup(uri, type, selectedEntities)
                    }
                }
                pendingImportUri = null
                pendingImportType = null
            },
            onDismiss = {
                showEntitySelector = false
                pendingImportUri = null
                pendingImportType = null
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete All Data?") },
            text = { Text("This action is permanent and cannot be undone. All your transactions, accounts, and settings will be wiped.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteAllData()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                        // Section 1: Auto Backup
                        AeonSettingsSection("AUTO BACKUP") {
                            AeonSettingsItem(
                                title = "Enable Auto Backup",
                                subtitle = "Automatically back up your data",
                                icon = Icons.Rounded.AutoAwesome,
                                trailingContent = {
                                    Switch(
                                        checked = autoBackupConfig.enabled,
                                        onCheckedChange = {
                                            viewModel.updateAutoBackupConfig(autoBackupConfig.copy(enabled = it))
                                        }
                                    )
                                }
                            )

                            if (autoBackupConfig.enabled) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider.copy(alpha = 0.5f)
                                )

                                // Interval
                                Column(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
                                    Text(
                                        text = "Backup Interval",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AppColors.contentPrimary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = autoBackupConfig.intervalHours.toString(),
                                            onValueChange = { v ->
                                                v.toIntOrNull()?.let { hours ->
                                                    if (hours > 0) viewModel.updateAutoBackupConfig(autoBackupConfig.copy(intervalHours = hours))
                                                }
                                            },
                                            modifier = Modifier.width(80.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            label = { Text("Hours") }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("hours", color = AppColors.contentSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider.copy(alpha = 0.5f)
                                )

                                // Retention
                                Column(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
                                    Text(
                                        text = "Keep Last N Backups",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AppColors.contentPrimary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = autoBackupConfig.maxRetained.toString(),
                                        onValueChange = { v ->
                                            v.toIntOrNull()?.let { n ->
                                                if (n > 0) viewModel.updateAutoBackupConfig(autoBackupConfig.copy(maxRetained = n))
                                            }
                                        },
                                        modifier = Modifier.width(80.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        label = { Text("Count") }
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider.copy(alpha = 0.5f)
                                )

                                // Storage mode
                                Column(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
                                    Text(
                                        text = "Storage Location",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AppColors.contentPrimary
                                    )
                                    AutoBackupConfig.StorageMode.entries.forEach { mode ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            RadioButton(
                                                selected = autoBackupConfig.storageMode == mode,
                                                onClick = {
                                                    viewModel.updateAutoBackupConfig(autoBackupConfig.copy(storageMode = mode))
                                                }
                                            )
                                            Text(
                                                text = when (mode) {
                                                    AutoBackupConfig.StorageMode.PRIVATE -> "Private (App Storage)"
                                                    AutoBackupConfig.StorageMode.CUSTOM_FOLDER -> "Custom Folder"
                                                    AutoBackupConfig.StorageMode.BOTH -> "Both"
                                                },
                                                color = AppColors.contentPrimary,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }

                                    if (autoBackupConfig.storageMode != AutoBackupConfig.StorageMode.PRIVATE) {
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { customFolderLauncher.launch(null) }
                                        ) {
                                            Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (autoBackupConfig.customFolderUri != null) "Change Folder" else "Choose Folder"
                                            )
                                        }
                                        if (autoBackupConfig.customFolderUri != null) {
                                            Text(
                                                text = autoBackupConfig.customFolderUri!!,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AppColors.contentSecondary,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider.copy(alpha = 0.5f)
                                )

                                // Notification preference
                                Column(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
                                    Text(
                                        text = "Notifications",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AppColors.contentPrimary
                                    )
                                    AutoBackupConfig.NotifyMode.entries.forEach { mode ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            RadioButton(
                                                selected = autoBackupConfig.notifyMode == mode,
                                                onClick = {
                                                    viewModel.updateAutoBackupConfig(autoBackupConfig.copy(notifyMode = mode))
                                                }
                                            )
                                            Text(
                                                text = when (mode) {
                                                    AutoBackupConfig.NotifyMode.ALWAYS -> "Always"
                                                    AutoBackupConfig.NotifyMode.FAILURE_ONLY -> "Failure Only"
                                                    AutoBackupConfig.NotifyMode.NEVER -> "Never"
                                                },
                                                color = AppColors.contentPrimary,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }

                            // Recent backups
                            if (recentBackups.isNotEmpty()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider.copy(alpha = 0.5f)
                                )
                                Column(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
                                    Text(
                                        text = "Recent Backups",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AppColors.contentPrimary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    recentBackups.forEach { backup ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Rounded.Backup,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = AppColors.contentSecondary
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = backup.id,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = AppColors.contentPrimary
                                                )
                                                Text(
                                                    text = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                                                        .format(Date(backup.createdAt)) +
                                                            " · ${backup.sizeBytes / 1024} KB",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AppColors.contentSecondary
                                                )
                                            }
                                            IconButton(onClick = { viewModel.deleteBackup(backup.id) }) {
                                                Icon(
                                                    Icons.Rounded.Delete,
                                                    contentDescription = "Delete backup",
                                                    tint = AppColors.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                        }

                        // Section 2: Manual Backup
                        AeonSettingsSection("MANUAL BACKUP") {
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

                        // Section 3: Danger Zone
                        AeonSettingsSection("DANGER ZONE") {
                            AeonSettingsItem(
                                title = "Delete All Data",
                                subtitle = "Permanently clear your account",
                                icon = Icons.Rounded.DeleteForever,
                                color = AppColors.error,
                                onClick = { showDeleteConfirmDialog = true }
                            )
                        }
                    }
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Data Management",
                            onBackClick = actions.onBackClick
                        )
                    }
                )
            )
        }
    }
}

data class DataManagementActions(
    val onBackClick: () -> Unit,
    val onNavigateToCsvImport: () -> Unit = {}
)

@Composable
private fun ExportDialog(
    onDismiss: () -> Unit,
    onExportZipAll: () -> Unit,
    onExportZipProfile: () -> Unit,
    onExportCsv: () -> Unit,
    onExportSqlite: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data") },
        text = { Text("Select the format you want to export your data in.") },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onExportZipAll, modifier = Modifier.fillMaxWidth()) {
                    Text("ZIP - All Profiles")
                }
                OutlinedButton(onClick = onExportZipProfile, modifier = Modifier.fillMaxWidth()) {
                    Text("ZIP - Current Profile")
                }
                OutlinedButton(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) {
                    Text("Flat CSV (Transactions Only)")
                }
                OutlinedButton(onClick = onExportSqlite, modifier = Modifier.fillMaxWidth()) {
                    Text("SQLite Database File")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun ImportDialog(
    onDismiss: () -> Unit,
    onImportZip: () -> Unit,
    onImportSqlite: () -> Unit,
    onImportCsv: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Data") },
        text = { Text("Select the format of the backup file you want to import.") },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onImportZip, modifier = Modifier.fillMaxWidth()) {
                    Text("ZIP of CSV Files (OssWallet)")
                }
                OutlinedButton(onClick = onImportSqlite, modifier = Modifier.fillMaxWidth()) {
                    Text("SQLite Database")
                }
                OutlinedButton(onClick = onImportCsv, modifier = Modifier.fillMaxWidth()) {
                    Text("Generic CSV")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun RestorableEntitySelectorDialog(
    selectedEntities: Set<RestorableEntity>,
    onSelectionChanged: (Set<RestorableEntity>) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Data to Restore") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { onSelectionChanged(RestorableEntity.entries.toSet()) }) {
                        Text("Select All")
                    }
                    TextButton(onClick = { onSelectionChanged(emptySet()) }) {
                        Text("Deselect All")
                    }
                }
                RestorableEntity.entries.forEach { entity ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = entity in selectedEntities,
                            onCheckedChange = { checked ->
                                onSelectionChanged(
                                    if (checked) selectedEntities + entity else selectedEntities - entity
                                )
                            }
                        )
                        Text(
                            text = entity.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.contentPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = selectedEntities.isNotEmpty()) {
                Text("Restore Selected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
