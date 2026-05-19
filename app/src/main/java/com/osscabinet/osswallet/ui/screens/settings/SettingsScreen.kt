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
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.getCurrencyName
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.getCurrencySymbol
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService.getAllCurrencies
import com.osscabinet.ossWallet.domain.repository.BackupType
import com.osscabinet.ossWallet.ui.util.*
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppFont
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

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
    val selectedFont by viewModel.selectedFont.collectAsStateWithLifecycle()
    val decimalPrecision by viewModel.decimalPrecision.collectAsStateWithLifecycle()
    val autoSaveExchangeRates by viewModel.autoSaveExchangeRates.collectAsStateWithLifecycle()
    val cardAnimationEnabled by viewModel.cardAnimationEnabled.collectAsStateWithLifecycle()
    val expandableFabEnabled by viewModel.expandableFabEnabled.collectAsStateWithLifecycle()
    val use24HourFormat by viewModel.use24HourFormat.collectAsStateWithLifecycle()
    val mapsAccessEnabled by viewModel.mapsAccessEnabled.collectAsStateWithLifecycle()
    val recentActivitiesBreakdown by viewModel.recentActivitiesBreakdown.collectAsStateWithLifecycle()
    val currencyProviderId by viewModel.currencyProvider.collectAsStateWithLifecycle()
    val availableProviders = viewModel.availableProviders
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showProviderDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
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
        var searchQuery by remember { mutableStateOf("") }
        val filteredCurrencies = remember(searchQuery, availableCurrencies) {
            availableCurrencies.filter {
                it.code.contains(searchQuery, ignoreCase = true) || 
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }

        BasicAlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth().padding(AeonTheme.dimensions.spacingXl)
        ) {
            Surface(
                shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
                color = AppColors.panelSurface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(AeonTheme.dimensions.spacingXl)
                        .fillMaxWidth()
                ) {
                    Text(
                        "Select Base Currency",
                        style = AppTextStyles.calculatorKey,
                        modifier = Modifier.padding(bottom = AeonTheme.dimensions.spacingLg)
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
                        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingLg))
                    
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                        ) {
                            items(filteredCurrencies) { metadata ->
                                val code = metadata.code
                                val symbol = getCurrencySymbol(code)
                                val name = metadata.name
                                Surface(
                                    onClick = {
                                        viewModel.setCurrency(code)
                                        showCurrencyDialog = false
                                    },
                                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                                    color = if (code == currencyCode) AppColors.selectionContainer else AppColors.transparent
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(AeonTheme.dimensions.spacingMd),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl),
                                            shape = CircleShape,
                                            color = if (code == currencyCode) AppColors.onPrimaryContainer.copy(alpha = 0.1f) else AppColors.secondaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    symbol,
                                                    style = AppTextStyles.cardSectionTitle,
                                                    color = if (code == currencyCode) AppColors.onPrimaryContainer else AppColors.onSecondaryContainer
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    name, 
                                                    style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Medium),
                                                    color = if (code == currencyCode) AppColors.onPrimaryContainer else AppColors.contentPrimary
                                                )
                                                if (metadata.isVolatile) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        Icons.Rounded.Warning, 
                                                        contentDescription = "Volatile", 
                                                        tint = AppColors.error,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                "$code • ${metadata.country}", 
                                                style = AppTextStyles.transactionSwipeAction, 
                                                color = if (code == currencyCode) AppColors.onPrimaryContainer.copy(alpha = 0.7f) else AppColors.border
                                            )
                                            if (!metadata.note.isNullOrBlank()) {
                                                Text(
                                                    metadata.note,
                                                    style = AppTextStyles.caption.copy(fontSize = 10.sp),
                                                    color = AppColors.error.copy(alpha = 0.8f),
                                                    lineHeight = 12.sp
                                                )
                                            }
                                        }
                                        if (code == currencyCode) {
                                            Icon(
                                                Icons.Rounded.Check, 
                                                contentDescription = null, 
                                                tint = AppColors.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = AeonTheme.dimensions.spacingLg),
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

    if (showProviderDialog) {
        AlertDialog(
            onDismissRequest = { showProviderDialog = false },
            title = { Text("Exchange Rate Provider") },
            text = {
                Column {
                    availableProviders.forEach { provider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCurrencyProvider(provider.id)
                                    showProviderDialog = false
                                }
                                .padding(AeonTheme.dimensions.cardPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Api, contentDescription = null, modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd))
                            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(provider.name, style = AppTextStyles.listItemTitle)
                                Text(provider.attribution, style = AppTextStyles.caption, color = AppColors.border)
                            }
                            if (provider.id == currencyProviderId) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = AppColors.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProviderDialog = false }) { Text("Close") }
            }
        )
    }

    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("App Font") },
            text = {
                Column {
                    AppFont.entries.forEach { font ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSelectedFont(font.name)
                                    showFontDialog = false
                                }
                                .padding(AeonTheme.dimensions.cardPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.FontDownload, contentDescription = null, modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd))
                            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
                            Text(font.displayName, modifier = Modifier.weight(1f), style = AppTextStyles.listItemTitle)
                            if (font.name == selectedFont) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = AppColors.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) { Text("Close") }
            }
        )
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
                                .padding(AeonTheme.dimensions.cardPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = when(t) {
                                "Light" -> Icons.Rounded.LightMode
                                "Dark" -> Icons.Rounded.DarkMode
                                "Amoled" -> Icons.Rounded.Contrast
                                else -> Icons.Rounded.SettingsSuggest
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd))
                            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
                            Text(t, modifier = Modifier.weight(1f), style = AppTextStyles.listItemTitle)
                            if (t == theme) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = AppColors.primary)
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
                title = { Text("Settings", style = AppTextStyles.calculatorKey) },
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
                .background(AppColors.screenBackground)
                .verticalScroll(scrollState)
                .padding(AeonTheme.dimensions.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacing2xl)
        ) {
            SettingsSection("PREFERENCE") {
                SettingsItem(
                    title = "Base Currency",
                    subtitle = "${getCurrencySymbol(currencyCode)} ${getCurrencyName(currencyCode)}",
                    icon = Icons.Rounded.CurrencyExchange,
                    onClick = { showCurrencyDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                SettingsItem(
                    title = "App Appearance",
                    subtitle = theme,
                    icon = Icons.Rounded.Palette,
                    onClick = { showThemeDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                SettingsItem(
                    title = "App Font",
                    subtitle = AppFont.fromName(selectedFont).displayName,
                    icon = Icons.Rounded.FontDownload,
                    onClick = { showFontDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Exchange Rate Provider",
                    subtitle = availableProviders.find { it.id == currencyProviderId }?.name ?: "Select Provider",
                    icon = Icons.Rounded.Api,
                    onClick = { showProviderDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Auto-save Exchange Rates",
                    subtitle = if (autoSaveExchangeRates) "Automatically save fresh rates" else "Show preview before saving",
                    icon = if (autoSaveExchangeRates) Icons.Rounded.AutoFixHigh else Icons.Rounded.Quiz,
                    trailingContent = {
                        Switch(
                            checked = autoSaveExchangeRates,
                            onCheckedChange = { viewModel.setAutoSaveExchangeRates(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppColors.primary,
                                checkedTrackColor = AppColors.switchTrackOn,
                                uncheckedThumbColor = AppColors.border,
                                uncheckedTrackColor = AppColors.switchTrackOff,
                            )
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))

                SettingsItem(
                    title = "Card Animations",
                    subtitle = if (cardAnimationEnabled) "Animate cards on screen load" else "Disable card animations",
                    icon = if (cardAnimationEnabled) Icons.Rounded.Animation else Icons.Rounded.Block,
                    trailingContent = {
                        Switch(
                            checked = cardAnimationEnabled,
                            onCheckedChange = { viewModel.setCardAnimationEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppColors.primary,
                                checkedTrackColor = AppColors.switchTrackOn,
                                uncheckedThumbColor = AppColors.border,
                                uncheckedTrackColor = AppColors.switchTrackOff,
                            )
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))

                SettingsItem(
                    title = "Expandable Action Button",
                    subtitle = if (expandableFabEnabled) "Show quick templates and actions" else "Simple button for new entries",
                    icon = if (expandableFabEnabled) Icons.Rounded.GridView else Icons.Rounded.AddCircleOutline,
                    trailingContent = {
                        Switch(
                            checked = expandableFabEnabled,
                            onCheckedChange = { viewModel.setExpandableFabEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppColors.primary,
                                checkedTrackColor = AppColors.switchTrackOn,
                                uncheckedThumbColor = AppColors.border,
                                uncheckedTrackColor = AppColors.switchTrackOff,
                            )
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))

                SettingsItem(
                    title = "24-Hour Time Format",
                    subtitle = if (use24HourFormat) "Use 24-hour clock (14:30)" else "Use 12-hour clock (02:30 PM)",
                    icon = Icons.Rounded.Schedule,
                    trailingContent = {
                        Switch(
                            checked = use24HourFormat,
                            onCheckedChange = { viewModel.setUse24HourFormat(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppColors.primary,
                                checkedTrackColor = AppColors.switchTrackOn,
                                uncheckedThumbColor = AppColors.border,
                                uncheckedTrackColor = AppColors.switchTrackOff,
                            )
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Decimal Precision",
                    subtitle = "Toggle between 2 and 3 places",
                    icon = Icons.Rounded.PrecisionManufacturing,
                    trailingContent = {
                        Row(
                            modifier = Modifier
                                .height(AeonTheme.dimensions.spacing2xl)
                                .background(
                                    AppColors.insetSurface.copy(alpha = 0.5f),
                                    CircleShape
                                )
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(2, 3).forEach { p ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(AeonTheme.dimensions.iconSizeXl)
                                        .clickable {
                                            if (decimalPrecision != p) {
                                                viewModel.setDecimalPrecision(p)
                                                Toast.makeText(context, "Precision set to $p decimal places", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    shape = CircleShape,
                                    color = if (decimalPrecision == p) AppColors.primary else AppColors.transparent
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            p.toString(),
                                            style = AppTextStyles.drawerItemLabel,
                                            fontWeight = FontWeight.Bold,
                                            color = if (decimalPrecision == p) AppColors.onPrimary else AppColors.contentSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))

                SettingsItem(
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
                                uncheckedThumbColor = AppColors.border,
                                uncheckedTrackColor = AppColors.switchTrackOff,
                            )
                        )
                    }
                )
                SettingsItem(
                    title = "Recent Activities Breakdown",
                    subtitle = if (recentActivitiesBreakdown) "Currency breakdown shown in group headers" else "Currency breakdown hidden in group headers",
                    icon = if (recentActivitiesBreakdown) Icons.Rounded.CurrencyExchange else Icons.Rounded.Block,
                    trailingContent = {
                        Switch(
                            checked = recentActivitiesBreakdown,
                            onCheckedChange = { viewModel.setRecentActivitiesBreakdown(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppColors.primary,
                                checkedTrackColor = AppColors.switchTrackOn,
                                uncheckedThumbColor = AppColors.border,
                                uncheckedTrackColor = AppColors.switchTrackOff,
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
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Manage Tags",
                    subtitle = "Create, edit, or delete tags",
                    icon = Icons.Rounded.Label,
                    onClick = { navigate(onManageTagsClick) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Manage Categories",
                    subtitle = "Organize income and expenses",
                    icon = Icons.Rounded.Category,
                    onClick = { navigate(onManageCategoriesClick) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Transaction Templates",
                    subtitle = "Quick shortcuts for frequent entries",
                    icon = Icons.Rounded.AutoAwesome,
                    onClick = { navigate(onTemplatesClick) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Saved Filters",
                    subtitle = "Templates for quick transaction searching",
                    icon = Icons.Rounded.FilterList,
                    onClick = { navigate(onManageFiltersClick) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
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
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Export Data",
                    subtitle = "Download all records and database",
                    icon = Icons.Rounded.FileDownload,
                    onClick = { showExportDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                SettingsItem(
                    title = "Delete All Data",
                    subtitle = "Permanently clear your account",
                    icon = Icons.Rounded.DeleteForever,
                    color = AppColors.error,
                    onClick = { /* Handle delete */ }
                )
            }

            Spacer(modifier = Modifier.height(scrollBottomPadding()))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("OssWallet v1.1.0", style = AppTextStyles.transactionSwipeAction, color = AppColors.border)
                Text("Built for the Modern Account", style = AppTextStyles.transactionSwipeAction, color = AppColors.contentSecondary.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))
                Text(
                    "Currency rates provided by",
                    style = AppTextStyles.transactionSwipeAction,
                    color = AppColors.contentSecondary.copy(alpha = 0.5f)
                )
                Text(
                    availableProviders.find { it.id == currencyProviderId }?.attribution ?: "Exchangerate.host",
                    modifier = Modifier.clickable {
                        // In a real app, use Context to open the URL
                    },
                    style = AppTextStyles.transactionSwipeAction.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.primary
                    )
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
        Text(
            text = title,
            style = AppTextStyles.drawerItemLabel.copy(
                fontWeight = FontWeight.Bold,
                color = AppColors.primary,
                letterSpacing = 1.2.sp
            ),
            modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding)
        )
        Surface(
            shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
            color = AppColors.insetSurface.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.4f))
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
    color: Color = AppColors.contentPrimary,
    showChevron: Boolean = onClick != null && trailingContent == null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(AeonTheme.dimensions.cardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl),
            shape = CircleShape,
            color = color.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm))
            }
        }
        Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = AppTextStyles.managementItemTitle, color = color)
            Text(subtitle, style = AppTextStyles.caption, color = AppColors.border)
        }
        
        if (trailingContent != null) {
            trailingContent()
        } else if (showChevron) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = AppColors.border)
        }
    }
}
