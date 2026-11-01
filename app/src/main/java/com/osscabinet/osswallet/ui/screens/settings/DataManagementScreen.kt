package com.osscabinet.ossWallet.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.repository.AutoBackupConfig
import com.osscabinet.ossWallet.domain.repository.BackupLocation
import com.osscabinet.ossWallet.domain.repository.BackupMeta
import com.osscabinet.ossWallet.domain.repository.BackupScope
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.domain.repository.RestorableEntity
import com.osscabinet.ossWallet.domain.service.BackupRunState
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

private const val MIN_BACKUP_INTERVAL_HOURS = 1
private const val MAX_BACKUP_INTERVAL_HOURS = 720

private fun backupTypeOf(backup: BackupMeta): BackupType =
    if (backup.id.endsWith(".sqlite") || backup.id.endsWith(".sqlite.enc")) BackupType.SQLITE else BackupType.CSV_ZIP

private fun backupMimeType(backup: BackupMeta): String =
    if (backup.encrypted) "application/octet-stream"
    else if (backupTypeOf(backup) == BackupType.SQLITE) "application/x-sqlite3" else "application/zip"

/** Filename-safe timestamp, e.g. "20261101_143022_+0530" — numeric offset, no colon. */
private fun backupFilenameTimestamp(): String =
    java.time.ZonedDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_Z"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    viewModel: SettingsViewModel,
    actions: DataManagementActions
) {
    val autoBackupConfig by viewModel.autoBackupConfig.collectAsStateWithLifecycle()
    val recentBackups by viewModel.recentBackups.collectAsStateWithLifecycle()
    val backupOperation by viewModel.backupOperation.collectAsStateWithLifecycle()
    val backupRunStatus by viewModel.backupRunStatus.collectAsStateWithLifecycle()

    val backupKeyAvailable by viewModel.backupKeyAvailable.collectAsStateWithLifecycle()
    val uiScope = rememberCoroutineScope()
    val context = LocalContext.current
    val wrongPassphraseMessage = stringResource(R.string.backup_wrong_passphrase)

    var showExportDialog by remember { mutableStateOf(false) }

    // Recent-backups row actions (open/share/restore/delete)
    var menuBackup by remember { mutableStateOf<BackupMeta?>(null) }
    var pendingRestoreBackup by remember { mutableStateOf<BackupMeta?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showRestorePassphrase by remember { mutableStateOf(false) }
    var restorePassphraseError by remember { mutableStateOf<String?>(null) }

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
        viewModel.verifyBackup(uri, BackupType.CSV_ZIP, passphrase)
        resetImportFlow()
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
            // Without this, the SAF grant only lasts this app-process lifetime: the next time the
            // scheduled worker runs (likely a fresh process, possibly after a reboot) it would get
            // a SecurityException trying to write into the folder the user already picked.
            context.contentResolver.takePersistableUriPermission(
                folderUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            autoBackupConfig.customFolderUri?.let { old ->
                if (old != folderUri.toString()) {
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            old.toUri(),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                }
            }
            viewModel.updateAutoBackupConfig(autoBackupConfig.copy(customFolderUri = folderUri.toString()))
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.backupEvents.collect { event ->
            val (message, type) = when (event) {
                is BackupEvent.ExportSucceeded -> context.getString(
                    if (event.encrypted) R.string.backup_export_succeeded_encrypted else R.string.backup_export_succeeded
                ) to AeonSnackbarType.SUCCESS
                is BackupEvent.ExportFailed -> context.getString(
                    R.string.backup_export_failed, event.reason ?: context.getString(R.string.backup_unknown_error)
                ) to AeonSnackbarType.ERROR
                is BackupEvent.ImportSucceeded -> context.getString(
                    if (event.restarting) R.string.backup_import_succeeded_restarting else R.string.backup_import_succeeded
                ) to AeonSnackbarType.SUCCESS
                is BackupEvent.ImportFailed -> context.getString(
                    R.string.backup_import_failed, event.reason ?: context.getString(R.string.backup_unknown_error)
                ) to AeonSnackbarType.ERROR
                is BackupEvent.VerifySucceeded -> context.getString(
                    if (event.encrypted) R.string.backup_verify_succeeded_encrypted else R.string.backup_verify_succeeded,
                    event.entryCount, event.sizeKb
                ) to AeonSnackbarType.SUCCESS
                is BackupEvent.VerifyFailed -> context.getString(
                    R.string.backup_verify_failed, event.reason ?: context.getString(R.string.backup_unknown_error)
                ) to AeonSnackbarType.ERROR
                is BackupEvent.EncryptionChanged -> context.getString(
                    if (event.enabled) R.string.backup_encryption_enabled else R.string.backup_encryption_disabled
                ) to AeonSnackbarType.SUCCESS
                is BackupEvent.EncryptionFailed -> context.getString(
                    R.string.backup_encryption_failed, event.reason ?: context.getString(R.string.backup_unknown_error)
                ) to AeonSnackbarType.ERROR
                is BackupEvent.DeleteBackupFailed -> context.getString(
                    R.string.backup_delete_failed, event.reason ?: context.getString(R.string.backup_unknown_error)
                ) to AeonSnackbarType.ERROR
                BackupEvent.DeletingAllData -> context.getString(R.string.backup_deleting_all_data) to AeonSnackbarType.INFO
            }
            snackbarHostState.showAeonSnackbar(message = message, type = type)
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExportZipAll = {
                showExportDialog = false
                pendingExport = PendingExport(
                    BackupType.CSV_ZIP, BackupScope.ALL_PROFILES,
                    "osswallet_backup_${backupFilenameTimestamp()}.zip"
                ) { exportZipAllLauncher.launch(it) }
                showExportEncryptChoice = true
            },
            onExportZipProfile = {
                showExportDialog = false
                pendingExport = PendingExport(
                    BackupType.CSV_ZIP, BackupScope.SINGLE_PROFILE,
                    "osswallet_profile_${backupFilenameTimestamp()}.zip"
                ) { exportZipProfileLauncher.launch(it) }
                showExportEncryptChoice = true
            },
            onExportCsv = {
                showExportDialog = false
                exportCsvLauncher.launch("osswallet_transactions_${backupFilenameTimestamp()}.csv")
            },
            onExportSqlite = {
                showExportDialog = false
                pendingExport = PendingExport(
                    BackupType.SQLITE, BackupScope.ALL_PROFILES,
                    "osswallet_db_${backupFilenameTimestamp()}.sqlite"
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
            title = { Text(stringResource(R.string.backup_protect_export_title)) },
            text = { Text(stringResource(R.string.backup_protect_export_message)) },
            confirmButton = {
                TextButton(onClick = { showExportEncryptChoice = false; showExportPassphrase = true }) {
                    Text(stringResource(R.string.backup_protect_export_encrypt))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportEncryptChoice = false
                    pendingExport?.let { it.launchPlain(it.fileName) }
                    pendingExport = null
                }) { Text(stringResource(R.string.backup_protect_export_plain)) } }
        )
    }

    if (showExportPassphrase) {
        PassphraseSetupDialog(
            title = stringResource(R.string.backup_export_passphrase_title),
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
            title = stringResource(R.string.backup_encryption_title),
            onConfirm = { chars -> showEnableEncryption = false; viewModel.enableBackupEncryption(chars) },
            onDismiss = { showEnableEncryption = false }
        )
    }

    if (showDisableEncryption) {
        AlertDialog(
            onDismissRequest = { showDisableEncryption = false },
            title = { Text(stringResource(R.string.backup_stop_encrypting_title)) },
            text = { Text(stringResource(R.string.backup_stop_encrypting_message)) },
            confirmButton = {
                TextButton(onClick = { showDisableEncryption = false; viewModel.disableBackupEncryption() }) {
                    Text(stringResource(R.string.backup_turn_off))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableEncryption = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showImportPassphrase && pendingImportUri != null) {
        PassphraseEntryDialog(
            title = stringResource(
                if (verifyOnly) R.string.backup_verify_encrypted_title else R.string.backup_encrypted_backup_title
            ),
            errorText = importPassphraseError,
            onConfirm = { chars ->
                val uri = pendingImportUri ?: return@PassphraseEntryDialog
                uiScope.launch {
                    val check = viewModel.verifyBackupSuspend(uri, pendingImportType ?: BackupType.CSV_ZIP, chars)
                    if (check.isFailure) {
                        chars.fill('0')
                        importPassphraseError = wrongPassphraseMessage
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
        show = showRestoreConfirm,
        title = stringResource(R.string.backup_restore_confirm_title),
        message = stringResource(R.string.backup_restore_confirm_message),
        confirmButtonText = stringResource(R.string.backup_action_restore),
        onConfirm = {
            showRestoreConfirm = false
            pendingRestoreBackup?.let { viewModel.restoreBackup(it, null) }
            pendingRestoreBackup = null
        },
        onDismiss = { showRestoreConfirm = false; pendingRestoreBackup = null }
    )

    if (showRestorePassphrase && pendingRestoreBackup != null) {
        PassphraseEntryDialog(
            title = stringResource(R.string.backup_restore_confirm_title),
            errorText = restorePassphraseError,
            onConfirm = { chars ->
                val backup = pendingRestoreBackup ?: return@PassphraseEntryDialog
                uiScope.launch {
                    val check = viewModel.verifyBackupSuspend(backup.uriString.toUri(), backupTypeOf(backup), chars)
                    if (check.isFailure) {
                        chars.fill('0')
                        restorePassphraseError = wrongPassphraseMessage
                    } else {
                        showRestorePassphrase = false
                        restorePassphraseError = null
                        viewModel.restoreBackup(backup, chars)
                        pendingRestoreBackup = null
                    }
                }
            },
            onDismiss = {
                showRestorePassphrase = false
                restorePassphraseError = null
                pendingRestoreBackup = null
            }
        )
    }

    AeonConfirmDeleteDialog(
        show = showDeleteConfirmDialog,
        title = stringResource(R.string.backup_delete_all_confirm_title),
        message = stringResource(R.string.backup_delete_all_confirm_message),
        onConfirm = {
            showDeleteConfirmDialog = false
            viewModel.deleteAllData()
        },
        onDismiss = { showDeleteConfirmDialog = false },
        confirmButtonText = stringResource(R.string.backup_delete_all_confirm_button)
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
                        AeonSettingsSection(stringResource(R.string.backup_section_auto_title)) {
                            AeonSettingsItem(
                                title = stringResource(R.string.backup_enable_title),
                                subtitle = stringResource(R.string.backup_enable_subtitle),
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

                                // Interval — held as a local draft and committed on focus-loss/Done, not
                                // per keystroke, so typing "2" then "4" doesn't re-enqueue WorkManager twice.
                                var intervalDraft by remember(autoBackupConfig.intervalHours) {
                                    mutableStateOf(autoBackupConfig.intervalHours.toString())
                                }
                                var intervalError by remember { mutableStateOf(false) }
                                fun commitInterval() {
                                    val hours = intervalDraft.toIntOrNull()
                                    if (hours != null && hours in MIN_BACKUP_INTERVAL_HOURS..MAX_BACKUP_INTERVAL_HOURS) {
                                        intervalError = false
                                        if (hours != autoBackupConfig.intervalHours) {
                                            viewModel.updateAutoBackupConfig(autoBackupConfig.copy(intervalHours = hours))
                                        }
                                    } else {
                                        intervalError = true
                                    }
                                }
                                // onFocusChanged above only fires if the field actually loses focus before
                                // this composable leaves the tree. Pressing back (or otherwise navigating
                                // away) while the field is still focused disposes it directly, silently
                                // discarding whatever interval the user just typed. Commit on dispose too.
                                val latestCommitInterval = rememberUpdatedState(::commitInterval)
                                DisposableEffect(Unit) {
                                    onDispose { latestCommitInterval.value() }
                                }
                                Column(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
                                    Text(
                                        text = stringResource(R.string.backup_interval_label),
                                        style = AppTextStyles.formSectionLabel,
                                        color = AppColors.contentPrimary
                                    )
                                    Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AeonTextField(
                                            value = intervalDraft,
                                            onValueChange = { intervalDraft = it },
                                            placeholder = "24",
                                            modifier = Modifier
                                                .width(100.dp)
                                                .onFocusChanged { if (!it.isFocused) commitInterval() }
                                        )
                                        Spacer(Modifier.width(AeonTheme.dimensions.spacingMd))
                                        Text(
                                            stringResource(R.string.backup_interval_suffix),
                                            color = AppColors.contentSecondary,
                                            style = AppTextStyles.caption
                                        )
                                    }
                                    if (intervalError) {
                                        Text(
                                            stringResource(
                                                R.string.backup_interval_error,
                                                MIN_BACKUP_INTERVAL_HOURS, MAX_BACKUP_INTERVAL_HOURS
                                            ),
                                            style = AppTextStyles.metaLabel,
                                            color = AppColors.error
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider.copy(alpha = 0.3f)
                                )

                                // Scope
                                Column(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
                                    Text(
                                        text = stringResource(R.string.backup_scope_label),
                                        style = AppTextStyles.formSectionLabel,
                                        color = AppColors.contentPrimary
                                    )
                                    Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
                                    BackupScope.entries.forEach { scope ->
                                        val isSelected = autoBackupConfig.scope == scope
                                        Surface(
                                            onClick = { viewModel.updateAutoBackupConfig(autoBackupConfig.copy(scope = scope)) },
                                            color = if (isSelected) AppColors.accentContainer.copy(alpha = 0.1f) else Color.Transparent,
                                            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                                            ) {
                                                RadioButton(selected = isSelected, onClick = null)
                                                Text(
                                                    text = stringResource(
                                                        if (scope == BackupScope.ALL_PROFILES) R.string.backup_scope_all_profiles
                                                        else R.string.backup_scope_current_profile
                                                    ),
                                                    color = if (isSelected) AppColors.primary else AppColors.contentPrimary,
                                                    style = AppTextStyles.listItemTitle
                                                )
                                            }
                                        }
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
                                        text = stringResource(R.string.backup_retention_label),
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
                                        text = stringResource(R.string.backup_storage_location_label),
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
                                                // Click target lives on the parent Surface only — a second
                                                // onClick here would fire the same update twice per tap.
                                                RadioButton(selected = isSelected, onClick = null)
                                                Text(
                                                    text = stringResource(
                                                        when (mode) {
                                                            AutoBackupConfig.StorageMode.PRIVATE -> R.string.backup_storage_private
                                                            AutoBackupConfig.StorageMode.CUSTOM_FOLDER -> R.string.backup_storage_custom_folder
                                                            AutoBackupConfig.StorageMode.BOTH -> R.string.backup_storage_both
                                                        }
                                                    ),
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
                                            text = stringResource(
                                                if (autoBackupConfig.customFolderUri != null) R.string.backup_change_folder
                                                else R.string.backup_choose_folder
                                            ),
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
                                        } else {
                                            Text(
                                                text = stringResource(R.string.backup_no_folder_warning),
                                                style = AppTextStyles.caption,
                                                color = AppColors.error,
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
                                        text = stringResource(R.string.backup_notifications_label),
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
                                                RadioButton(selected = isSelected, onClick = null)
                                                Text(
                                                    text = stringResource(
                                                        when (mode) {
                                                            AutoBackupConfig.NotifyMode.ALWAYS -> R.string.backup_notify_always
                                                            AutoBackupConfig.NotifyMode.FAILURE_ONLY -> R.string.backup_notify_failure_only
                                                            AutoBackupConfig.NotifyMode.NEVER -> R.string.backup_notify_never
                                                        }
                                                    ),
                                                    color = if (isSelected) AppColors.primary else AppColors.contentPrimary,
                                                    style = AppTextStyles.listItemTitle
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider.copy(alpha = 0.3f)
                                )

                                // Last/next run status (from WorkManager — first use of WorkInfo observation in this app)
                                Column(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
                                    val statusText = when (backupRunStatus?.first) {
                                        BackupRunState.RUNNING -> stringResource(R.string.backup_last_run_running)
                                        BackupRunState.PENDING -> stringResource(R.string.backup_next_run_pending)
                                        BackupRunState.SUCCEEDED -> stringResource(R.string.backup_last_run_succeeded)
                                        BackupRunState.FAILED -> stringResource(R.string.backup_last_run_failed)
                                        else -> stringResource(R.string.backup_last_run_none)
                                    }
                                    val statusColor = when (backupRunStatus?.first) {
                                        BackupRunState.FAILED -> AppColors.error
                                        else -> AppColors.contentSecondary
                                    }
                                    Text(statusText, style = AppTextStyles.caption, color = statusColor)
                                }

                                // Battery optimization hint
                                val powerManager = remember { context.getSystemService(PowerManager::class.java) }
                                val ignoringBatteryOptimizations = remember(autoBackupConfig.enabled) {
                                    powerManager?.isIgnoringBatteryOptimizations(context.packageName) != false
                                }
                                if (!ignoringBatteryOptimizations) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                        thickness = 0.5.dp,
                                        color = AppColors.divider.copy(alpha = 0.3f)
                                    )
                                    AeonSettingsItem(
                                        title = stringResource(R.string.backup_battery_optimization_title),
                                        subtitle = stringResource(R.string.backup_battery_optimization_subtitle),
                                        icon = Icons.Rounded.BatteryAlert,
                                        color = AppColors.error,
                                        onClick = {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(
                                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                        "package:${context.packageName}".toUri()
                                                    )
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            // Progress banner for any in-flight backup/restore/verify/delete-all action
                            if (backupOperation != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider.copy(alpha = 0.5f)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(AeonTheme.dimensions.cardPadding),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(AeonTheme.dimensions.spacingMd))
                                    Text(
                                        text = stringResource(
                                            when (backupOperation) {
                                                BackupOperation.EXPORTING -> R.string.backup_progress_exporting
                                                BackupOperation.IMPORTING -> R.string.backup_progress_importing
                                                BackupOperation.VERIFYING -> R.string.backup_progress_verifying
                                                BackupOperation.RESTORING -> R.string.backup_progress_restoring
                                                BackupOperation.DELETING_ALL -> R.string.backup_progress_deleting_all
                                                null -> R.string.backup_progress_exporting
                                            }
                                        ),
                                        style = AppTextStyles.caption,
                                        color = AppColors.contentPrimary
                                    )
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
                                        text = stringResource(R.string.backup_recent_title),
                                        style = AppTextStyles.formSectionLabel,
                                        color = AppColors.contentPrimary
                                    )
                                    Spacer(Modifier.height(AeonTheme.dimensions.spacingMd))
                                    recentBackups.forEach { backup ->
                                        Box {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { menuBackup = backup },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Backup,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = AppColors.contentSecondary
                                                )
                                                Spacer(Modifier.width(AeonTheme.dimensions.spacingMd))
                                                if (backup.encrypted) {
                                                    Icon(
                                                        Icons.Rounded.Lock,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = AppColors.contentSecondary
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = backup.id,
                                                        style = AppTextStyles.caption,
                                                        color = AppColors.contentPrimary
                                                    )
                                                    Text(
                                                        text = SimpleDateFormat(
                                                            "MMM d, yyyy HH:mm",
                                                            LocalConfiguration.current.locales[0]
                                                        )
                                                            .format(Date(backup.createdAt)) +
                                                            " · ${backup.sizeBytes / 1024} KB · " +
                                                            stringResource(
                                                                if (backup.location == BackupLocation.PRIVATE)
                                                                    R.string.backup_item_location_private
                                                                else R.string.backup_item_location_custom
                                                            ),
                                                        style = AppTextStyles.metaLabel,
                                                        color = AppColors.contentSecondary
                                                    )
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = menuBackup == backup,
                                                onDismissRequest = { menuBackup = null }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.backup_action_restore)) },
                                                    leadingIcon = { Icon(Icons.Rounded.Restore, contentDescription = null) },
                                                    onClick = {
                                                        menuBackup = null
                                                        uiScope.launch {
                                                            pendingRestoreBackup = backup
                                                            if (viewModel.isEncryptedBackup(backup.uriString.toUri())) {
                                                                restorePassphraseError = null
                                                                showRestorePassphrase = true
                                                            } else {
                                                                showRestoreConfirm = true
                                                            }
                                                        }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.backup_action_open)) },
                                                    leadingIcon = { Icon(Icons.Rounded.OpenInNew, contentDescription = null) },
                                                    onClick = {
                                                        menuBackup = null
                                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                                            setDataAndType(backup.uriString.toUri(), backupMimeType(backup))
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        runCatching { context.startActivity(Intent.createChooser(intent, null)) }
                                                            .onFailure {
                                                                if (it is ActivityNotFoundException) {
                                                                    uiScope.launch {
                                                                        snackbarHostState.showAeonSnackbar(
                                                                            context.getString(R.string.backup_open_no_app),
                                                                            type = AeonSnackbarType.ERROR
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.backup_action_share)) },
                                                    leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                                                    onClick = {
                                                        menuBackup = null
                                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                                            type = backupMimeType(backup)
                                                            putExtra(Intent.EXTRA_STREAM, backup.uriString.toUri())
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        runCatching { context.startActivity(Intent.createChooser(intent, null)) }
                                                            .onFailure {
                                                                if (it is ActivityNotFoundException) {
                                                                    uiScope.launch {
                                                                        snackbarHostState.showAeonSnackbar(
                                                                            context.getString(R.string.backup_open_no_app),
                                                                            type = AeonSnackbarType.ERROR
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.backup_action_delete), color = AppColors.error) },
                                                    leadingIcon = {
                                                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = AppColors.error)
                                                    },
                                                    onClick = {
                                                        menuBackup = null
                                                        viewModel.deleteBackup(backup)
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
                                    }
                                }
                            }
                        }

                        // Section 1b: Backup encryption
                        AeonSettingsSection(stringResource(R.string.backup_encryption_section_title)) {
                            AeonSettingsItem(
                                title = stringResource(R.string.backup_encryption_title),
                                subtitle = stringResource(
                                    if (autoBackupConfig.encrypt && !backupKeyAvailable)
                                        R.string.backup_encryption_subtitle_key_unavailable
                                    else R.string.backup_encryption_subtitle_on
                                ),
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
                                    title = stringResource(R.string.backup_encryption_reenter_title),
                                    subtitle = stringResource(R.string.backup_encryption_reenter_subtitle),
                                    icon = Icons.Rounded.Lock,
                                    color = AppColors.error,
                                    onClick = { showEnableEncryption = true }
                                )
                            }
                        }

                        // Section 2: Manual Backup
                        AeonSettingsSection(stringResource(R.string.backup_manual_section_title)) {
                            AeonSettingsItem(
                                title = stringResource(R.string.backup_import_migrate_title),
                                subtitle = stringResource(R.string.backup_import_migrate_subtitle),
                                icon = Icons.Rounded.CloudSync,
                                onClick = { showImportDialog = true }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.5f)
                            )
                            AeonSettingsItem(
                                title = stringResource(R.string.backup_export_data_title),
                                subtitle = stringResource(R.string.backup_export_data_subtitle),
                                icon = Icons.Rounded.FileDownload,
                                onClick = { showExportDialog = true }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.5f)
                            )
                            AeonSettingsItem(
                                title = stringResource(R.string.backup_verify_file_title),
                                subtitle = stringResource(R.string.backup_verify_file_subtitle),
                                icon = Icons.Rounded.VerifiedUser,
                                onClick = {
                                    verifyOnly = true
                                    pendingImportType = BackupType.CSV_ZIP
                                    importLauncher.launch("*/*")
                                }
                            )
                        }

                        // Section 3: Danger Zone
                        AeonSettingsSection(stringResource(R.string.backup_danger_zone_title)) {
                            AeonSettingsItem(
                                title = stringResource(R.string.backup_delete_all_title),
                                subtitle = stringResource(R.string.backup_delete_all_subtitle),
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
                            title = stringResource(R.string.data_management_title),
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
        title = { Text(stringResource(R.string.backup_export_data_title), style = AppTextStyles.cardTitle) },
        text = {
            Text(
                stringResource(R.string.backup_export_dialog_message),
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
                    text = stringResource(R.string.backup_export_zip_all),
                    icon = Icons.Rounded.Folder
                )
                AeonDashedButton(
                    onClick = onExportZipProfile,
                    text = stringResource(R.string.backup_export_zip_profile),
                    icon = Icons.Rounded.Folder
                )
                AeonDashedButton(
                    onClick = onExportCsv,
                    text = stringResource(R.string.backup_export_flat_csv),
                    icon = Icons.Rounded.FileDownload
                )
                AeonDashedButton(
                    onClick = onExportSqlite,
                    text = stringResource(R.string.backup_export_sqlite),
                    icon = Icons.Rounded.Backup
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        stringResource(R.string.action_cancel),
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
        title = { Text(stringResource(R.string.backup_import_migrate_title), style = AppTextStyles.cardTitle) },
        text = {
            Text(
                stringResource(R.string.backup_import_dialog_message),
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
                    text = stringResource(R.string.backup_import_zip),
                    icon = Icons.Rounded.CloudSync
                )
                AeonDashedButton(
                    onClick = onImportSqlite,
                    text = stringResource(R.string.backup_export_sqlite),
                    icon = Icons.Rounded.Backup
                )
                AeonDashedButton(
                    onClick = onImportCsv,
                    text = stringResource(R.string.backup_import_csv),
                    icon = Icons.Rounded.AutoAwesome
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        stringResource(R.string.action_cancel),
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
    val groups = listOf(
        stringResource(R.string.backup_group_foundational) to listOf(
            RestorableEntity.PROFILES to stringResource(R.string.backup_entity_desc_profiles),
            RestorableEntity.ACCOUNTS to stringResource(R.string.backup_entity_desc_accounts),
            RestorableEntity.CATEGORIES to stringResource(R.string.backup_entity_desc_categories),
            RestorableEntity.TAGS to stringResource(R.string.backup_entity_desc_tags),
            RestorableEntity.CURRENCY_RATES to stringResource(R.string.backup_entity_desc_currency_rates)
        ),
        stringResource(R.string.backup_group_records) to listOf(
            RestorableEntity.TRANSACTIONS to stringResource(R.string.backup_entity_desc_transactions)
        ),
        stringResource(R.string.backup_group_planning) to listOf(
            RestorableEntity.BUDGETS to stringResource(R.string.backup_entity_desc_budgets),
            RestorableEntity.LOANS to stringResource(R.string.backup_entity_desc_loans),
            RestorableEntity.GOALS to stringResource(R.string.backup_entity_desc_goals),
            RestorableEntity.PLANNED_PAYMENTS to stringResource(R.string.backup_entity_desc_planned_payments),
            RestorableEntity.TEMPLATES to stringResource(R.string.backup_entity_desc_templates),
            RestorableEntity.FILTERS to stringResource(R.string.backup_entity_desc_filters)
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(stringResource(R.string.backup_restore_selection_title), style = AppTextStyles.cardTitle)
                Text(
                    stringResource(R.string.backup_restore_selection_subtitle),
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
                            stringResource(if (allSelected) R.string.action_deselect_all else R.string.action_select_all),
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
                text = stringResource(R.string.backup_restore_selected_button),
                icon = Icons.Rounded.CloudSync
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.action_cancel),
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
