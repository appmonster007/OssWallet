package com.osscabinet.ossWallet.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Api
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.getCurrencyName
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.getCurrencySymbol
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySettingsScreen(
    viewModel: SettingsViewModel,
    onShortlistedCurrenciesClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val currencyCode by viewModel.currency.collectAsStateWithLifecycle()
    val decimalPrecision by viewModel.decimalPrecision.collectAsStateWithLifecycle()
    val numberingSystem by viewModel.numberingSystem.collectAsStateWithLifecycle()
    val autoSaveExchangeRates by viewModel.autoSaveExchangeRates.collectAsStateWithLifecycle()
    val use24HourFormat by viewModel.use24HourFormat.collectAsStateWithLifecycle()
    val currencyProviderId by viewModel.currencyProvider.collectAsStateWithLifecycle()
    val availableProviders = viewModel.availableProviders
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()
    val shortlistedCurrencies by viewModel.shortlistedCurrencies.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showProviderDialog by remember { mutableStateOf(false) }
    var showSystemDialog by remember { mutableStateOf(false) }

    if (showSystemDialog) {
        AlertDialog(
            onDismissRequest = { showSystemDialog = false },
            title = { Text("Number Formatting System") },
            text = {
                Column {
                    val previewAmount = java.math.BigDecimal("1234.5678")
                    listOf(
                        "WESTERN",
                        "EUROPEAN",
                        "INDIAN",
                        "SWISS",
                        "FRENCH",
                        "ISO",
                        "SOUTH_AMERICAN"
                    ).forEach { systemKey ->
                        val system = CurrencyFormatter.FormattingSystem.fromString(systemKey)
                        val previewText = CurrencyFormatter.formatForSystem(
                            amount = previewAmount,
                            system = system,
                            currencyCode = currencyCode,
                            precision = decimalPrecision
                        )

                        val label = when (systemKey) {
                            "WESTERN" -> "Western"
                            "EUROPEAN" -> "European"
                            "INDIAN" -> "Indian"
                            "SWISS" -> "Swiss"
                            "FRENCH" -> "French"
                            "ISO" -> "ISO/Standard"
                            "SOUTH_AMERICAN" -> "South American"
                            else -> systemKey
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setNumberingSystem(systemKey)
                                    showSystemDialog = false
                                }
                                .padding(AeonTheme.dimensions.cardPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    label,
                                    style = AppTextStyles.listItemTitle
                                )
                                Text(
                                    previewText,
                                    style = AppTextStyles.caption,
                                    color = AppColors.primary
                                )
                            }
                            if (systemKey == numberingSystem) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = AppColors.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSystemDialog = false }) { Text("Close") }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeonTheme.dimensions.spacingXl)
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
                                            color = if (code == currencyCode) AppColors.onPrimaryContainer.copy(
                                                alpha = 0.1f
                                            ) else AppColors.secondaryContainer
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
                                                    style = AppTextStyles.listItemTitle.copy(
                                                        fontWeight = FontWeight.Medium
                                                    ),
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
                                                color = if (code == currencyCode) AppColors.onPrimaryContainer.copy(
                                                    alpha = 0.7f
                                                ) else AppColors.border
                                            )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AeonTheme.dimensions.spacingLg),
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
                            Icon(
                                Icons.Rounded.Api,
                                contentDescription = null,
                                modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd)
                            )
                            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(provider.name, style = AppTextStyles.listItemTitle)
                                Text(
                                    provider.attribution,
                                    style = AppTextStyles.caption,
                                    color = AppColors.border
                                )
                            }
                            if (provider.id == currencyProviderId) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = AppColors.primary
                                )
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Currency & Localization", style = AppTextStyles.calculatorKey) },
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
            AeonSettingsSection("CURRENCY") {
                AeonSettingsItem(
                    title = "Base Currency",
                    subtitle = "${getCurrencySymbol(currencyCode)} ${getCurrencyName(currencyCode)}",
                    icon = Icons.Rounded.CurrencyExchange,
                    onClick = { showCurrencyDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                    thickness = 0.5.dp,
                    color = AppColors.divider.copy(alpha = 0.5f)
                )
                AeonSettingsItem(
                    title = "Active Currencies",
                    subtitle = shortlistedCurrencies?.let { "${it.size} currencies selected" }
                        ?: "All currencies",
                    icon = Icons.Rounded.Bookmarks,
                    onClick = onShortlistedCurrenciesClick
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                    thickness = 0.5.dp,
                    color = AppColors.divider.copy(alpha = 0.5f)
                )
                AeonSettingsItem(
                    title = "Exchange Rate Provider",
                    subtitle = availableProviders.find { it.id == currencyProviderId }?.name
                        ?: "Select Provider",
                    icon = Icons.Rounded.Api,
                    onClick = { showProviderDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                    thickness = 0.5.dp,
                    color = AppColors.divider.copy(alpha = 0.5f)
                )
                AeonSettingsItem(
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
                            )
                        )
                    }
                )
            }

            AeonSettingsSection("FORMATTING") {
                AeonSettingsItem(
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
                                                Toast.makeText(
                                                    context,
                                                    "Precision set to $p decimal places",
                                                    Toast.LENGTH_SHORT
                                                ).show()
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
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                    thickness = 0.5.dp,
                    color = AppColors.divider.copy(alpha = 0.5f)
                )
                AeonSettingsItem(
                    title = "Formatting System",
                    subtitle = CurrencyFormatter.formatAuto(
                        amount = java.math.BigDecimal("1234.5678"),
                        currencyCode = currencyCode,
                        precision = decimalPrecision
                    ),
                    icon = Icons.Rounded.AutoFixHigh,
                    onClick = { showSystemDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                    thickness = 0.5.dp,
                    color = AppColors.divider.copy(alpha = 0.5f)
                )
                AeonSettingsItem(
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
                            )
                        )
                    }
                )
            }
        }
    }
}
