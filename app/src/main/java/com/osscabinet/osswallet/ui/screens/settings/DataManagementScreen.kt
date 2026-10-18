package com.osscabinet.ossWallet.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig
import com.osscabinet.ossWallet.domain.repository.BackupScope
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.domain.repository.RestorableEntity
import kotlinx.coroutines.launch
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.AeonDashedButton
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection
import com.osscabinet.ossWallet.ui.components.AeonSnackbarHost
import com.osscabinet.ossWallet.ui.components.AeonSnackbarType
import com.osscabinet.ossWallet.ui.components.AeonStepperField
import com.osscabinet.ossWallet.ui.components.AeonTextField
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.showAeonSnackbar
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    viewModel: SettingsViewModel,
    actions: DataManagementActions
) {
    val autoBackupConfig by viewModel.autoBackupConfig.collectAsStateWithLifecycle()
    val recentBackups by viewModel.recentBackups.collectAsStateWithLifecycle()

    val backupKeyAvailable by viewModel.backupKeyAvailable.collectAsStateWithLifecycle()
    val uiScope = rememberCoroutineScope()

    var showExportDialog by remember { mutableStateOf(false) }

    // Optional passphrase protection for a manual export.
    var pendingExport by remember { mutableStateOf<PendingExport?>(null) }
    var showExportEncryptChoice by remember { mutableStateOf(false) }
    var showExportPassphrase by remember { mutableStateOf(false) }
    var exportPassphrase by remember { mutableStateOf<CharArray?>(null) }

    // Scheduled-backup encryption setup/teardown.
    var showEnableEncryption by remember { mutableStateOf(false) }
    var showDisableEncryption by remember { mutableStateOf(false) }

    // Restore/verify of an encrypted file.
    var showImportPassphrase by remember { mutableStateOf(false) }
    var importPassphraseError by remember { mutableStateOf<String?>(null) }
    var importPassphrase by remember { mutableStateOf<CharArray?>(null) }
    var verifyOnly by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Entity selector state for selective import
    var showEntitySelector by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingImportType by remember { mutableStateOf<BackupType?>(null) }
    var selectedEntities by remember { mutableStateOf(RestorableEntity.entries.toSet()) }

    val exportZipAllLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            viewModel.exportBackup(
                it,
                BackupType.CSV_ZIP,
                BackupScope.ALL_PROFILES
            )
        }
    }

    val exportZipProfileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            viewModel.exportBackup(
                it,
                BackupType.CSV_ZIP,
                BackupScope.SINGLE_PROFILE
            )
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportBackup(it, BackupType.FLAT_CSV) } }

    val exportSqliteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri -> uri?.let { viewModel.exportBackup(it, BackupType.SQLITE) } }

    val exportEncryptedLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val export = pendingExport
        val passphrase = exportPassphrase
        exportPassphrase = null
        pendingExport = null
        if (uri != null && export != null && passphrase != null) {
            viewModel.exportBackup(uri, export.type, export.scope, passphrase)
        } else passphrase?.fill('0')
    }

    fun resetImportFlow() {
        pendingImportUri = null
        pendingImportType = null
        importPassphrase?.fill('0')
        importPassphrase = null
        importPassphraseError = null
        showImportPassphrase = false
        verifyOnly = false
    }

    fun verifyAndReport(uri: android.net.Uri, passphrase: CharArray?) {
        uiScope.launch {
            val result = viewModel.verifyBackup(uri, BackupType.CSV_ZIP, passphrase)
            passphrase?.fill('0')
            viewModel.reportBackupMessage(
                result.fold(
                    onSuccess = { "Backup is valid: ${it.entryCount} files, ${it.sizeBytes / 1024} KB" +
                        if (it.encrypted) " (encrypted)" else "" },
                    onFailure = { "Verification failed: ${it.message}" }
                )
            )
            resetImportFlow()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            resetImportFlow()
        } else {
            pendingImportUri = uri
            uiScope.launch {
                if (viewModel.isEncryptedBackup(uri)) {
                    importPassphraseError = null
                    showImportPassphrase = true
                } else if (verifyOnly) {
                    verifyAndReport(uri, null)
                } else {
                    val singleEntity = viewModel.detectBackupEntity(uri, null)
                    selectedEntities = if (singleEntity != null) setOf(singleEntity) else RestorableEntity.entries.toSet()
                    showEntitySelector = true
                }
            }
        }
    }

    val customFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { folderUri ->
            viewModel.updateAutoBackupConfig(autoBackupConfig.copy(customFolderUri = folderUri.toString()))
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.backupMessage.collect { message ->
            val type = if (message.contains("failed", ignoreCase = true) || message.contains(
                    "error",
                    ignoreCase = true
                )
            ) {
                AeonSnackbarType.ERROR
            } else {
                AeonSnackbarType.SUCCESS
            }
            snackbarHostState.showAeonSnackbar(
                message = message,
                type = type
            )
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExportZipAll = {
                showExportDialog = false
                pendingExport = PendingExport(
                    BackupType.CSV_ZIP, BackupScope.ALL_PROFILES,
                    "osswallet_backup_${System.currentTimeMillis()}.zip"
                ) { exportZipAllLauncher.launch(it) }
                showExportEncryptChoice = true
            },
            onExportZipProfile = {
                showExportDialog = false
                pendingExport = PendingExport(
                    BackupType.CSV_ZIP, BackupScope.SINGLE_PROFILE,
                    "osswallet_profile_${System.currentTimeMillis()}.zip"
                ) { exportZipProfileLauncher.launch(it) }
                showExportEncryptChoice = true
            },
            onExportCsv = {
                showExportDialog = false
                exportCsvLauncher.launch("osswallet_transactions_${System.currentTimeMillis()}.csv")
            },
            onExportSqlite = {
                showExportDialog = false
                pendingExport = PendingExport(
                    BackupType.SQLITE, BackupScope.ALL_PROFILES,
                    "osswallet_db_${System.currentTimeMillis()}.sqlite"
                ) { exportSqliteLauncher.launch(it) }
                showExportEncryptChoice = true
            }
        )
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onImportZip = {
                showImportDialog = false
                pendingImportType = BackupType.CSV_ZIP
                // Encrypted backups (.zip.enc) are octet-stream, so a zip-only filter would grey them out.
                importLauncher.launch("*/*")
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

    if (showExportEncryptChoice) {
        AlertDialog(
            onDismissRequest = { showExportEncryptChoice = false; pendingExport = null },
            title = { Text("Protect this export?") },
            text = { Text("Encrypt it with a passphrase so only you can open it. Plain exports can be read by anyone who gets the file.") },
            confirmButton = {
                TextButton(onClick = { showExportEncryptChoice = false; showExportPassphrase = true }) { Text("Encrypt") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportEncryptChoice = false
                    pendingExport?.let { it.launchPlain(it.fileName) }
                    pendingExport = null
                }) { Text("Export plain") }
            }
        )
    }

    if (showExportPassphrase) {
        PassphraseSetupDialog(
            title = "Export passphrase",
            onConfirm = { chars ->
                showExportPassphrase = false
                exportPassphrase = chars
                pendingExport?.let { exportEncryptedLauncher.launch(it.fileName + ".enc") }
            },
            onDismiss = { showExportPassphrase = false; pendingExport = null }
        )
    }

    if (showEnableEncryption) {
        PassphraseSetupDialog(
            title = "Encrypt scheduled backups",
            onConfirm = { chars -> showEnableEncryption = false; viewModel.enableBackupEncryption(chars) },
            onDismiss = { showEnableEncryption = false }
        )
    }

    if (showDisableEncryption) {
        AlertDialog(
            onDismissRequest = { showDisableEncryption = false },
            title = { Text("Stop encrypting backups?") },
            text = { Text("New scheduled backups will be plain files. Existing encrypted backups stay encrypted and still need the passphrase.") },
            confirmButton = {
                TextButton(onClick = { showDisableEncryption = false; viewModel.disableBackupEncryption() }) { Text("Turn off") }
            },
            dismissButton = { TextButton(onClick = { showDisableEncryption = false }) { Text("Cancel") } }
        )
    }

    if (showImportPassphrase && pendingImportUri != null) {
        PassphraseEntryDialog(
            title = if (verifyOnly) "Verify encrypted backup" else "Encrypted backup",
            errorText = importPassphraseError,
            onConfirm = { chars ->
                val uri = pendingImportUri ?: return@PassphraseEntryDialog
                uiScope.launch {
                    val check = viewModel.verifyBackup(uri, pendingImportType ?: BackupType.CSV_ZIP, chars)
                    if (check.isFailure) {
                        chars.fill('0')
                        importPassphraseError = "Wrong passphrase, or the backup is damaged"
                    } else if (verifyOnly) {
                        showImportPassphrase = false
                        verifyAndReport(uri, chars)
                    } else {
                        importPassphrase = chars
                        importPassphraseError = null
                        showImportPassphrase = false
                        val singleEntity = viewModel.detectBackupEntity(uri, chars)
                        selectedEntities = if (singleEntity != null) setOf(singleEntity) else RestorableEntity.entries.toSet()
                        showEntitySelector = true
                    }
                }
            },
            onDismiss = { resetImportFlow() }
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
                        viewModel.importBackup(uri, type, selectedEntities, importPassphrase)
                    }
                }
                // The view model owns (and wipes) the passphrase from here.
                importPassphrase = null
                resetImportFlow()
            },
            onDismiss = {
                showEntitySelector = false
                resetImportFlow()
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
                                            viewModel.updateAutoBackupConfig(
                                                autoBackupConfig.copy(
                                                    enabled = it
                                                )
                                            )
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
                                                    if (hours > 0) viewModel.updateAutoBackupConfig(
                                                        autoBackupConfig.copy(intervalHours = hours)
                                                    )
                                                }
                                            },
                                            placeholder = "24",
                                            modifier = Modifier.width(100.dp)
                                        )
                                        Spacer(Modifier.width(AeonTheme.dimensions.spacingMd))
                                        Text(
                                            "hours",
                                            color = AppColors.contentSecondary,
                                            style = AppTextStyles.caption
                                        )
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
                                        onDecrement = {
                                            if (autoBackupConfig.maxRetained > 1) viewModel.updateAutoBackupConfig(
                                                autoBackupConfig.copy(maxRetained = autoBackupConfig.maxRetained - 1)
                                            )
                                        },
                                        onIncrement = {
                                            viewModel.updateAutoBackupConfig(
                                                autoBackupConfig.copy(maxRetained = autoBackupConfig.maxRetained + 1)
                                            )
                                        },
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
                                            onClick = {
                                                viewModel.updateAutoBackupConfig(
                                                    autoBackupConfig.copy(storageMode = mode)
                                                )
                                            },
                                            color = if (isSelected) AppColors.accentContainer.copy(
                                                alpha = 0.1f
                                            ) else Color.Transparent,
                                            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(
                                                    vertical = 4.dp,
                                                    horizontal = 4.dp
                                                )
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = {
                                                        viewModel.updateAutoBackupConfig(
                                                            autoBackupConfig.copy(storageMode = mode)
                                                        )
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
                                                modifier = Modifier.padding(
                                                    top = 4.dp,
                                                    start = 8.dp
                                                )
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
                                            onClick = {
                                                viewModel.updateAutoBackupConfig(
                                                    autoBackupConfig.copy(notifyMode = mode)
                                                )
                                            },
                                            color = if (isSelected) AppColors.accentContainer.copy(
                                                alpha = 0.1f
                                            ) else Color.Transparent,
                                            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(
                                                    vertical = 4.dp,
                                                    horizontal = 4.dp
                                                )
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = {
                                                        viewModel.updateAutoBackupConfig(
                                                            autoBackupConfig.copy(notifyMode = mode)
                                                        )
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
                                                    text = (if (backup.encrypted) "🔒 " else "") + backup.id,
                                                    style = AppTextStyles.caption,
                                                    color = AppColors.contentPrimary
                                                )
                                                Text(
                                                    text = SimpleDateFormat(
                                                        "MMM d, yyyy HH:mm",
                                                        LocalConfiguration.current.locales[0]
                                                    )
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

                        // Section 1b: Backup encryption
                        AeonSettingsSection("BACKUP ENCRYPTION") {
                            AeonSettingsItem(
                                title = "Encrypt scheduled backups",
                                subtitle = if (autoBackupConfig.encrypt && !backupKeyAvailable)
                                    "Key unavailable on this device — set the passphrase again"
                                else "AES-256 with a passphrase only you know",
                                icon = Icons.Rounded.Lock,
                                trailingContent = {
                                    Switch(
                                        checked = autoBackupConfig.encrypt,
                                        onCheckedChange = { on ->
                                            if (on) showEnableEncryption = true else showDisableEncryption = true
                                        }
                                    )
                                }
                            )
                            if (autoBackupConfig.encrypt && !backupKeyAvailable) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider.copy(alpha = 0.5f)
                                )
                                AeonSettingsItem(
                                    title = "Re-enter passphrase",
                                    subtitle = "Scheduled backups fail until the key is restored",
                                    icon = Icons.Rounded.Lock,
                                    color = AppColors.error,
                                    onClick = { showEnableEncryption = true }
                                )
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
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.5f)
                            )
                            AeonSettingsItem(
                                title = "Verify a Backup File",
                                subtitle = "Check a backup is intact without restoring it",
                                icon = Icons.Rounded.VerifiedUser,
                                onClick = {
                                    verifyOnly = true
                                    pendingImportType = BackupType.CSV_ZIP
                                    importLauncher.launch("*/*")
                                }
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

            AeonSnackbarHost(hostState = snackbarHostState)

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
        text = {
            Text(
                "Select the format you want to export your data in.",
                style = AppTextStyles.caption
            )
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
            ) {
                AeonDashedButton(
                    onClick = onExportZipAll,
                    text = "ZIP - All Profiles",
                    icon = Icons.Rounded.Folder
                )
                AeonDashedButton(
                    onClick = onExportZipProfile,
                    text = "ZIP - Current Profile",
                    icon = Icons.Rounded.Folder
                )
                AeonDashedButton(
                    onClick = onExportCsv,
                    text = "Flat CSV",
                    icon = Icons.Rounded.FileDownload
                )
                AeonDashedButton(
                    onClick = onExportSqlite,
                    text = "SQLite Database",
                    icon = Icons.Rounded.Backup
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        "Cancel",
                        style = AppTextStyles.buttonLabel,
                        color = AppColors.contentSecondary
                    )
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
        text = {
            Text(
                "Restore an OssWallet backup (ZIP archive or single CSV table update) or import transactions from another app.",
                style = AppTextStyles.caption
            )
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
            ) {
                AeonDashedButton(
                    onClick = onImportZip,
                    text = "OssWallet Backup (ZIP or CSV)",
                    icon = Icons.Rounded.CloudSync
                )
                AeonDashedButton(
                    onClick = onImportSqlite,
                    text = "SQLite Database",
                    icon = Icons.Rounded.Backup
                )
                AeonDashedButton(
                    onClick = onImportCsv,
                    text = "Another App (CSV)",
                    icon = Icons.Rounded.AutoAwesome
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        "Cancel",
                        style = AppTextStyles.buttonLabel,
                        color = AppColors.contentSecondary
                    )
                }
            }
        }
    )
}

@Composable
internal fun RestorableEntitySelectorDialog(
    selectedEntities: Set<RestorableEntity>,
    onSelectionChanged: (Set<RestorableEntity>) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val groups = remember {
        listOf(
            "Foundational Data" to listOf(
                RestorableEntity.PROFILES to "User profiles and base currencies",
                RestorableEntity.ACCOUNTS to "Bank accounts, wallets, and cash",
                RestorableEntity.CATEGORIES to "Expense and income categories",
                RestorableEntity.TAGS to "Labels and custom tags",
                RestorableEntity.CURRENCY_RATES to "Downloaded exchange rates"
            ),
            "Records" to listOf(
                RestorableEntity.TRANSACTIONS to "All transaction history and transfers"
            ),
            "Financial Planning" to listOf(
                RestorableEntity.BUDGETS to "Monthly budgets and alerts",
                RestorableEntity.LOANS to "Debts, loans, and loan payments",
                RestorableEntity.GOALS to "Savings goals and progress",
                RestorableEntity.PLANNED_PAYMENTS to "Scheduled recurring transactions",
                RestorableEntity.TEMPLATES to "Transaction quick-entry templates",
                RestorableEntity.FILTERS to "Saved search and report filters"
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Restore Selection", style = AppTextStyles.cardTitle)
                Text(
                    "Choose which data components to restore from this backup.",
                    style = AppTextStyles.caption,
                    color = AppColors.contentSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allSelected = selectedEntities.size == RestorableEntity.entries.size
                    TextButton(
                        onClick = {
                            if (allSelected) onSelectionChanged(emptySet())
                            else onSelectionChanged(RestorableEntity.entries.toSet())
                        }
                    ) {
                        Text(
                            if (allSelected) "Deselect All" else "Select All",
                            style = AppTextStyles.buttonLabel,
                            color = if (allSelected) AppColors.contentSecondary else AppColors.primary
                        )
                    }
                }

                groups.forEach { (groupName, entities) ->
                    Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)) {
                        Text(
                            text = groupName.uppercase(),
                            style = AppTextStyles.overlineLabel,
                            color = AppColors.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )

                        entities.forEach { (entity, description) ->
                            val isSelected = entity in selectedEntities
                            Surface(
                                onClick = {
                                    onSelectionChanged(
                                        if (isSelected) selectedEntities - entity else selectedEntities + entity
                                    )
                                },
                                color = if (isSelected) AppColors.primaryContainer.copy(alpha = 0.1f) else Color.Transparent,
                                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                                border = BorderStroke(
                                    0.5.dp,
                                    if (isSelected) AppColors.primary.copy(alpha = 0.5f) else AppColors.outlineVariant.copy(
                                        alpha = 0.3f
                                    )
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            onSelectionChanged(
                                                if (checked) selectedEntities + entity else selectedEntities - entity
                                            )
                                        },
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entity.name.replace('_', ' ').lowercase()
                                                .replaceFirstChar { it.uppercase() },
                                            style = AppTextStyles.listItemTitle,
                                            color = if (isSelected) AppColors.contentPrimary else AppColors.contentSecondary
                                        )
                                        Text(
                                            text = description,
                                            style = AppTextStyles.metaLabel,
                                            color = AppColors.contentSecondary
                                        )
                                    }
                                }
                            }
                        }
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
                Text(
                    "Cancel",
                    style = AppTextStyles.buttonLabel,
                    color = AppColors.contentSecondary
                )
            }
        }
    )
}

@ThemePreviews
@Composable
private fun RestorableEntitySelectorDialogPreview() {
    OssWalletTheme {
        RestorableEntitySelectorDialog(
            selectedEntities = setOf(RestorableEntity.TRANSACTIONS, RestorableEntity.ACCOUNTS),
            onSelectionChanged = {},
            onConfirm = {},
            onDismiss = {}
        )
    }
}

/** A manual export waiting on the "encrypt it?" choice. */
private class PendingExport(
    val type: BackupType,
    val scope: BackupScope,
    val fileName: String,
    val launchPlain: (String) -> Unit
)
