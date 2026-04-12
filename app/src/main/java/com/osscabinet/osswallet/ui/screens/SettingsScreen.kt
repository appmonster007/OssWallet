package com.osscabinet.ossWallet.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.viewmodel.SettingsViewModel
import com.osscabinet.ossWallet.ui.util.getCurrencySymbol
import com.osscabinet.ossWallet.ui.util.getCurrencyName
import com.osscabinet.ossWallet.ui.util.getAllCurrencies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onManageAccountsClick: () -> Unit,
    onManageLabelsClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val currencyCode by viewModel.currency.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val dashboardGridColumns by viewModel.dashboardGridColumns.collectAsState()
    
    val scrollState = rememberScrollState()
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showGridDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
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

    if (showGridDialog) {
        AlertDialog(
            onDismissRequest = { showGridDialog = false },
            title = { Text("Dashboard Grid") },
            text = {
                Column {
                    (1..4).forEach { cols ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDashboardGridColumns(cols)
                                    showGridDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when(cols) {
                                    1 -> Icons.Rounded.ViewStream
                                    2 -> Icons.Rounded.GridView
                                    3 -> Icons.Rounded.TableChart
                                    else -> Icons.Rounded.CalendarViewMonth
                                },
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("$cols ${if (cols == 1) "Column" else "Columns"}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            if (cols == dashboardGridColumns) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGridDialog = false }) { Text("Close") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineSmall) },
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
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            SettingsSection("GENERAL") {
                SettingsItem(
                    title = "Base Currency",
                    subtitle = "${getCurrencySymbol(currencyCode)} ${getCurrencyName(currencyCode)}",
                    icon = Icons.Rounded.CurrencyExchange,
                    onClick = { showCurrencyDialog = true }
                )
                SettingsItem(
                    title = "App Appearance",
                    subtitle = theme,
                    icon = Icons.Rounded.Palette,
                    onClick = { showThemeDialog = true }
                )
                SettingsItem(
                    title = "Dashboard Layout",
                    subtitle = "$dashboardGridColumns Columns",
                    icon = Icons.Rounded.DashboardCustomize,
                    onClick = { showGridDialog = true }
                )
                SettingsItem(
                    title = "Manage Accounts",
                    subtitle = "Reorder, hide, or edit your accounts",
                    icon = Icons.Rounded.AccountBalance,
                    onClick = onManageAccountsClick
                )
                SettingsItem(
                    title = "Manage Labels",
                    subtitle = "Create, edit, or delete tags",
                    icon = Icons.Rounded.Label,
                    onClick = onManageLabelsClick
                )
                SettingsItem(
                    title = "Transaction Templates",
                    subtitle = "Quick shortcuts for frequent entries",
                    icon = Icons.Rounded.AutoAwesome,
                    onClick = onTemplatesClick
                )
            }

            SettingsSection("DATA MANAGEMENT") {
                SettingsItem(
                    title = "Import Backup",
                    subtitle = "Restore your history from a JSON file",
                    icon = Icons.Rounded.CloudSync,
                    onClick = { importLauncher.launch("application/json") }
                )
                SettingsItem(
                    title = "Export Data",
                    subtitle = "Download JSON of all records",
                    icon = Icons.Rounded.FileDownload,
                    onClick = { exportLauncher.launch("osswallet_backup_${System.currentTimeMillis()}.json") }
                )
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            letterSpacing = 1.sp
        )
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
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
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}
