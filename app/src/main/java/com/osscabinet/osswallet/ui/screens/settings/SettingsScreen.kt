package com.osscabinet.ossWallet.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection
import com.osscabinet.ossWallet.ui.components.ContourBackground
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onDisplayClick: () -> Unit,
    onCurrencyClick: () -> Unit,
    onAccountsClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onTagsClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onSmartSuggestionsClick: () -> Unit,
    onFiltersClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onDataManagementClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val currencyCode by viewModel.currency.collectAsStateWithLifecycle()
    val currencyProviderId by viewModel.currencyProvider.collectAsStateWithLifecycle()
    val availableProviders = viewModel.availableProviders

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = AppTextStyles.screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AppColors.screenBackground)
        ) {
            ContourBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(AeonTheme.dimensions.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacing2xl)
            ) {
                AeonSettingsSection("PERSONALIZATION") {
                    SettingsNavigationItem(
                        title = "Display & Style",
                        subtitle = "Theme, fonts, and animations",
                        icon = Icons.Rounded.Palette,
                        onClick = onDisplayClick
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Currency & Localization",
                        subtitle = "$currencyCode • ${availableProviders.find { it.id == currencyProviderId }?.name ?: "Provider"}",
                        icon = Icons.Rounded.CurrencyExchange,
                        onClick = onCurrencyClick
                    )
                }

                AeonSettingsSection("FINANCIAL SETUP") {
                    SettingsNavigationItem(
                        title = "Accounts",
                        subtitle = "Manage balances and visibility",
                        icon = Icons.Rounded.AccountBalance,
                        onClick = onAccountsClick
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Categories",
                        subtitle = "Income and expense classification",
                        icon = Icons.Rounded.Category,
                        onClick = onCategoriesClick
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Tags",
                        subtitle = "Custom labeling for transactions",
                        icon = Icons.AutoMirrored.Rounded.Label,
                        onClick = onTagsClick
                    )
                }

                AeonSettingsSection("AUTOMATION & TOOLS") {
                    SettingsNavigationItem(
                        title = "Transaction Templates",
                        subtitle = "Quick shortcuts for frequent entries",
                        icon = Icons.Rounded.AutoAwesome,
                        onClick = onTemplatesClick
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Smart Suggestions",
                        subtitle = "Discover recurring transactions",
                        icon = Icons.Rounded.AutoAwesome,
                        onClick = onSmartSuggestionsClick
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Saved Filters",
                        subtitle = "Custom search presets",
                        icon = Icons.Rounded.FilterList,
                        onClick = onFiltersClick
                    )
                }

                AeonSettingsSection("PRIVACY & SAFETY") {
                    SettingsNavigationItem(
                        title = "Security & Privacy",
                        subtitle = "App lock, location, and web access",
                        icon = Icons.Rounded.Lock,
                        onClick = onSecurityClick
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = "Data Management",
                        subtitle = "Import, export, and backups",
                        icon = Icons.Rounded.CloudSync,
                        onClick = onDataManagementClick
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AeonTheme.dimensions.spacingLg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                ) {
                    Text(
                        text = "OssWallet v1.1.0",
                        style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.contentPrimary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Built for Modern Accounts",
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))

                    Text(
                        "Currency rates provided by",
                        style = AppTextStyles.transactionSwipeAction,
                        color = AppColors.contentSecondary.copy(alpha = 0.5f)
                    )
                    Text(
                        availableProviders.find { it.id == currencyProviderId }?.attribution
                            ?: "Exchangerate.host",
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = AppColors.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color = AppColors.contentPrimary
) {
    AeonSettingsItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = onClick,
        color = color
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
        thickness = 0.5.dp,
        color = AppColors.divider.copy(alpha = 0.5f)
    )
}
