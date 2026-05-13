package com.osscabinet.ossWallet.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.getCurrencyName
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.getCurrencySymbol
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService.getAllCurrencies
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.ui.util.*
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onManageAccountsClick: () -> Unit,
    onManageTagsClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onManageFiltersClick: () -> Unit,
    onSmartSuggestionsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val currencyCode by viewModel.currency.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val decimalPrecision by viewModel.decimalPrecision.collectAsStateWithLifecycle()
    val autoSaveExchangeRates by viewModel.autoSaveExchangeRates.collectAsStateWithLifecycle()
    val cardAnimationEnabled by viewModel.cardAnimationEnabled.collectAsStateWithLifecycle()
    val expandableFabEnabled by viewModel.expandableFabEnabled.collectAsStateWithLifecycle()
    val use24HourFormat by viewModel.use24HourFormat.collectAsStateWithLifecycle()
    val mapsAccessEnabled by viewModel.mapsAccessEnabled.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var navTriggered by remember { mutableStateOf(false) }
    
    // Reset navTriggered when returning to this screen
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        navTriggered = false
    }

    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

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
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            importLauncher.launch("*/*") // Some file managers don't recognize sqlite mime well
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

    if (showCurrencyDialog) {
        val currencies = remember { getAllCurrencies() }
        var searchQuery by remember { mutableStateOf("") }
        val filteredCurrencies = remember(searchQuery) {
            currencies.filter { 
                it.contains(searchQuery, ignoreCase = true) || 
                getCurrencyName(it).contains(searchQuery, ignoreCase = true)
            }
        }

        BasicAlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "Select Base Currency",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search currency...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredCurrencies) { code ->
                                val symbol = getCurrencySymbol(code)
                                val name = getCurrencyName(code)
                                Surface(
                                    onClick = {
                                        viewModel.setCurrency(code)
                                        showCurrencyDialog = false
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (code == currencyCode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = CircleShape,
                                            color = if (code == currencyCode) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    symbol,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (code == currencyCode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                name, 
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                                color = if (code == currencyCode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                code, 
                                                style = MaterialTheme.typography.labelSmall, 
                                                color = if (code == currencyCode) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        if (code == currencyCode) {
                                            Icon(
                                                Icons.Rounded.Check, 
                                                contentDescription = null, 
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCurrencyDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        val themes = listOf("System", "Light", "Dark", "Amoled")
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("App Appearance") },
            text = {
                Column {
                    themes.forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTheme(t)
                                    showThemeDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = when(t) {
                                "Light" -> Icons.Rounded.LightMode
                                "Dark" -> Icons.Rounded.DarkMode
                                "Amoled" -> Icons.Rounded.Contrast
                                else -> Icons.Rounded.SettingsSuggest
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(t, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            if (t == theme) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = { navigate { onBackClick() } }) {
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
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            SettingsSection("PREFERENCE") {
                SettingsItem(
                    title = "Base Currency",
                    subtitle = "${getCurrencySymbol(currencyCode)} ${getCurrencyName(currencyCode)}",
                    icon = Icons.Rounded.CurrencyExchange,
                    onClick = { showCurrencyDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    title = "App Appearance",
                    subtitle = theme,
                    icon = Icons.Rounded.Palette,
                    onClick = { showThemeDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Auto-save Exchange Rates",
                    subtitle = if (autoSaveExchangeRates) "Automatically save fresh rates" else "Show preview before saving",
                    icon = if (autoSaveExchangeRates) Icons.Rounded.AutoFixHigh else Icons.Rounded.Quiz,
                    trailingContent = {
                        Switch(
                            checked = autoSaveExchangeRates,
                            onCheckedChange = { viewModel.setAutoSaveExchangeRates(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                SettingsItem(
                    title = "Card Animations",
                    subtitle = if (cardAnimationEnabled) "Animate cards on screen load" else "Disable card animations",
                    icon = if (cardAnimationEnabled) Icons.Rounded.Animation else Icons.Rounded.Block,
                    trailingContent = {
                        Switch(
                            checked = cardAnimationEnabled,
                            onCheckedChange = { viewModel.setCardAnimationEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                SettingsItem(
                    title = "Expandable Action Button",
                    subtitle = if (expandableFabEnabled) "Show quick templates and actions" else "Simple button for new entries",
                    icon = if (expandableFabEnabled) Icons.Rounded.GridView else Icons.Rounded.AddCircleOutline,
                    trailingContent = {
                        Switch(
                            checked = expandableFabEnabled,
                            onCheckedChange = { viewModel.setExpandableFabEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                SettingsItem(
                    title = "24-Hour Time Format",
                    subtitle = if (use24HourFormat) "Use 24-hour clock (14:30)" else "Use 12-hour clock (02:30 PM)",
                    icon = Icons.Rounded.Schedule,
                    trailingContent = {
                        Switch(
                            checked = use24HourFormat,
                            onCheckedChange = { viewModel.setUse24HourFormat(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Decimal Precision",
                    subtitle = "Toggle between 2 and 3 places",
                    icon = Icons.Rounded.PrecisionManufacturing,
                    trailingContent = {
                        Row(
                            modifier = Modifier
                                .height(32.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    CircleShape
                                )
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(2, 3).forEach { p ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(40.dp)
                                        .clickable {
                                            if (decimalPrecision != p) {
                                                viewModel.setDecimalPrecision(p)
                                                Toast.makeText(context, "Precision set to $p decimal places", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    shape = CircleShape,
                                    color = if (decimalPrecision == p) MaterialTheme.colorScheme.primary else Color.Transparent
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            p.toString(),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (decimalPrecision == p) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                SettingsItem(
                    title = "Location Auto-fill",
                    subtitle = if (mapsAccessEnabled) "Auto-fill current location for new transactions" else "Disabled location auto-fill",
                    icon = if (mapsAccessEnabled) Icons.Rounded.LocationOn else Icons.Rounded.LocationOff,
                    trailingContent = {
                        Switch(
                            checked = mapsAccessEnabled,
                            onCheckedChange = { viewModel.setMapsAccessEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )
                    }
                )
            }

            SettingsSection("ORGANIZATION") {
                SettingsItem(
                    title = "Manage Accounts",
                    subtitle = "Reorder, hide, or edit your accounts",
                    icon = Icons.Rounded.AccountBalance,
                    onClick = { navigate(onManageAccountsClick) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Manage Tags",
                    subtitle = "Create, edit, or delete tags",
                    icon = Icons.Rounded.Label,
                    onClick = { navigate(onManageTagsClick) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Manage Categories",
                    subtitle = "Organize income and expenses",
                    icon = Icons.Rounded.Category,
                    onClick = { navigate(onManageCategoriesClick) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Transaction Templates",
                    subtitle = "Quick shortcuts for frequent entries",
                    icon = Icons.Rounded.AutoAwesome,
                    onClick = { navigate(onTemplatesClick) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Saved Filters",
                    subtitle = "Templates for quick transaction searching",
                    icon = Icons.Rounded.FilterList,
                    onClick = { navigate(onManageFiltersClick) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Smart Suggestions",
                    subtitle = "Discover recurring transactions automatically",
                    icon = Icons.Rounded.AutoAwesome,
                    onClick = { navigate(onSmartSuggestionsClick) }
                )
            }

            SettingsSection("DATA & SECURITY") {
                SettingsItem(
                    title = "Import Backup",
                    subtitle = "Restore your history from a file",
                    icon = Icons.Rounded.CloudSync,
                    onClick = { showImportDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Export Data",
                    subtitle = "Download all records and database",
                    icon = Icons.Rounded.FileDownload,
                    onClick = { showExportDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Delete All Data",
                    subtitle = "Permanently clear your account",
                    icon = Icons.Rounded.DeleteForever,
                    color = MaterialTheme.colorScheme.error,
                    onClick = { /* Handle delete */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("OssWallet v1.1.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("Built for the Modern Account", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Currency rates provided by",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
                Text(
                    "Exchangerate.host",
                    modifier = Modifier.clickable {
                        // In a real app, use Context to open the URL
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.onSurface,
    showChevron: Boolean = onClick != null && trailingContent == null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = color)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        
        if (trailingContent != null) {
            trailingContent()
        } else if (showChevron) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
