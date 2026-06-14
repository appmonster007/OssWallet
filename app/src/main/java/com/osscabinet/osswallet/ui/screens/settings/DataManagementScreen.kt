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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig
import com.osscabinet.ossWallet.domain.repository.BackupScope
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.domain.repository.RestorableEntity
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.AeonDashedButton
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection
import com.osscabinet.ossWallet.ui.components.AeonStepperField
import com.osscabinet.ossWallet.ui.components.AeonTextField
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

    AeonConfirmDeleteDialog(
        show = showDeleteConfirmDialog,
        title = "Delete All Data?",
        message = "This action is permanent and cannot be undone. All your transactions, accounts, and settings will be wiped.",
        onConfirm = {
            showDeleteConfirmDialog = false
            viewModel.deleteAllData()
        },
        onDismiss = { showDeleteConfirmDialog = false },
        confirmButtonText = "Delete Everything"
    )

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
                                        style = AppTextStyles.formSectionLabel,
                                        color = AppColors.contentPrimary
                                    )
                                    Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AeonTextField(
                                            value = autoBackupConfig.intervalHours.toString(),
                                            onValueChange = { v ->
                                                v.toIntOrNull()?.let { hours ->
                                                    if (hours > 0) viewModel.updateAutoBackupConfig(autoBackupConfig.copy(intervalHours = hours))
                                                }
                                            },
                                            placeholder = "24",
                                            modifier = Modifier.width(100.dp)
                                        )
                                        Spacer(Modifier.width(AeonTheme.dimensions.spacingMd))
                                        Text("hours", color = AppColors.contentSecondary, style = AppTextStyles.caption)
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider.copy(alpha = 0.3f)
                                )

                                // Retention
                                Column(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
                                    Text(
                                        text = "Keep Last N Backups",
                                        style = AppTextStyles.formSectionLabel,
                                        color = AppColors.contentPrimary
                                    )
                                    Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
                                    AeonStepperField(
                                        value = autoBackupConfig.maxRetained,
                                        onDecrement = { if (autoBackupConfig.maxRetained > 1) viewModel.updateAutoBackupConfig(autoBackupConfig.copy(maxRetained = autoBackupConfig.maxRetained - 1)) },
                                        onIncrement = { viewModel.updateAutoBackupConfig(autoBackupConfig.copy(maxRetained = autoBackupConfig.maxRetained + 1)) },
                                        min = 1,
                                        modifier = Modifier.width(140.dp)
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider.copy(alpha = 0.3f)
                                )

                                // Storage mode
                                Column(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
                                    Text(
                                        text = "Storage Location",
                                        style = AppTextStyles.formSectionLabel,
                                        color = AppColors.contentPrimary
                                    )
                                    Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
                                    AutoBackupConfig.StorageMode.entries.forEach { mode ->
                                        val isSelected = autoBackupConfig.storageMode == mode
                                        Surface(
                                            onClick = { viewModel.updateAutoBackupConfig(autoBackupConfig.copy(storageMode = mode)) },
                                            color = if (isSelected) AppColors.accentContainer.copy(alpha = 0.1f) else Color.Transparent,
                                            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
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
                                                    color = if (isSelected) AppColors.primary else AppColors.contentPrimary,
                                                    style = AppTextStyles.listItemTitle
                                                )
                                            }
                                        }
                                    }

                                    if (autoBackupConfig.storageMode != AutoBackupConfig.StorageMode.PRIVATE) {
                                        Spacer(Modifier.height(AeonTheme.dimensions.spacingMd))
                                        AeonDashedButton(
                                            onClick = { customFolderLauncher.launch(null) },
                                            text = if (autoBackupConfig.customFolderUri != null) "Change Folder" else "Choose Folder",
                                            icon = Icons.Rounded.Folder
                                        )
                                        if (autoBackupConfig.customFolderUri != null) {
                                            Text(
                                                text = autoBackupConfig.customFolderUri!!,
                                                style = AppTextStyles.caption,
                                                color = AppColors.contentSecondary,
                                                modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider.copy(alpha = 0.3f)
                                )

                                // Notification preference
                                Column(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
                                    Text(
                                        text = "Notifications",
                                        style = AppTextStyles.formSectionLabel,
                                        color = AppColors.contentPrimary
                                    )
                                    Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
                                    AutoBackupConfig.NotifyMode.entries.forEach { mode ->
                                        val isSelected = autoBackupConfig.notifyMode == mode
                                        Surface(
                                            onClick = { viewModel.updateAutoBackupConfig(autoBackupConfig.copy(notifyMode = mode)) },
                                            color = if (isSelected) AppColors.accentContainer.copy(alpha = 0.1f) else Color.Transparent,
                                            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
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
                                                    color = if (isSelected) AppColors.primary else AppColors.contentPrimary,
                                                    style = AppTextStyles.listItemTitle
                                                )
                                            }
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
                                        style = AppTextStyles.formSectionLabel,
                                        color = AppColors.contentPrimary
                                    )
                                    Spacer(Modifier.height(AeonTheme.dimensions.spacingMd))
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
                                            Spacer(Modifier.width(AeonTheme.dimensions.spacingMd))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = backup.id,
                                                    style = AppTextStyles.caption,
                                                    color = AppColors.contentPrimary
                                                )
                                                Text(
                                                    text = SimpleDateFormat("MMM d, yyyy HH:mm", LocalConfiguration.current.locales[0])
                                                        .format(Date(backup.createdAt)) +
                                                            " · ${backup.sizeBytes / 1024} KB",
                                                    style = AppTextStyles.metaLabel,
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
                                        Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
                                    }
                                }
                            }
                        }

                        // Section 2: Manual Backup
                        AeonSettingsSection("MANUAL BACKUP") {
                            AeonSettingsItem(
                                title = "Import / Migrate Data",
                                subtitle = "Restore from backup or import from another app",
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
        title = { Text("Export Data", style = AppTextStyles.cardTitle) },
        text = { Text("Select the format you want to export your data in.", style = AppTextStyles.caption) },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
            ) {
                AeonDashedButton(onClick = onExportZipAll, text = "ZIP - All Profiles", icon = Icons.Rounded.Folder)
                AeonDashedButton(onClick = onExportZipProfile, text = "ZIP - Current Profile", icon = Icons.Rounded.Folder)
                AeonDashedButton(onClick = onExportCsv, text = "Flat CSV", icon = Icons.Rounded.FileDownload)
                AeonDashedButton(onClick = onExportSqlite, text = "SQLite Database", icon = Icons.Rounded.Backup)
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel", style = AppTextStyles.buttonLabel, color = AppColors.contentSecondary)
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
        title = { Text("Import / Migrate Data", style = AppTextStyles.cardTitle) },
        text = { Text("Restore an OssWallet backup or import transactions from another app.", style = AppTextStyles.caption) },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
            ) {
                AeonDashedButton(onClick = onImportZip, text = "OssWallet Backup (ZIP)", icon = Icons.Rounded.CloudSync)
                AeonDashedButton(onClick = onImportSqlite, text = "SQLite Database", icon = Icons.Rounded.Backup)
                AeonDashedButton(onClick = onImportCsv, text = "Another App (CSV)", icon = Icons.Rounded.AutoAwesome)
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel", style = AppTextStyles.buttonLabel, color = AppColors.contentSecondary)
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
        title = { Text("Select Data to Restore", style = AppTextStyles.cardTitle) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                TextButton(
                    onClick = { onSelectionChanged(RestorableEntity.entries.toSet()) }
                ) {
                    Text("Select All", style = AppTextStyles.buttonLabel, color = AppColors.primary)
                }
                TextButton(
                    onClick = { onSelectionChanged(emptySet()) }
                ) {
                    Text("Deselect All", style = AppTextStyles.buttonLabel, color = AppColors.contentSecondary)
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
                            style = AppTextStyles.listItemTitle,
                            color = AppColors.contentPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            AeonDashedButton(
                onClick = onConfirm,
                enabled = selectedEntities.isNotEmpty(),
                text = "Restore Selected",
                icon = Icons.Rounded.CloudSync
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = AppTextStyles.buttonLabel, color = AppColors.contentSecondary)
            }
        }
    )
}
