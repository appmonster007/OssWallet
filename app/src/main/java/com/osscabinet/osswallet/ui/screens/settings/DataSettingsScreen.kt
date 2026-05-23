package com.osscabinet.ossWallet.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSettingsScreen(
    onManageAccountsClick: () -> Unit,
    onManageTagsClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onManageFiltersClick: () -> Unit,
    onSmartSuggestionsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data & Organization", style = AppTextStyles.calculatorKey) },
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
            AeonSettingsSection("MANAGEMENT") {
                AeonSettingsItem(
                    title = "Accounts",
                    subtitle = "Reorder, hide, or edit your accounts",
                    icon = Icons.Rounded.AccountBalance,
                    onClick = onManageAccountsClick
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                AeonSettingsItem(
                    title = "Tags",
                    subtitle = "Create, edit, or delete tags",
                    icon = Icons.AutoMirrored.Rounded.Label,
                    onClick = onManageTagsClick
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                AeonSettingsItem(
                    title = "Categories",
                    subtitle = "Organize income and expenses",
                    icon = Icons.Rounded.Category,
                    onClick = onManageCategoriesClick
                )
            }

            AeonSettingsSection("AUTOMATION & SEARCH") {
                AeonSettingsItem(
                    title = "Transaction Templates",
                    subtitle = "Quick shortcuts for frequent entries",
                    icon = Icons.Rounded.AutoAwesome,
                    onClick = onTemplatesClick
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                AeonSettingsItem(
                    title = "Saved Filters",
                    subtitle = "Templates for quick transaction searching",
                    icon = Icons.Rounded.FilterList,
                    onClick = onManageFiltersClick
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                AeonSettingsItem(
                    title = "Smart Suggestions",
                    subtitle = "Discover recurring transactions automatically",
                    icon = Icons.Rounded.AutoAwesome,
                    onClick = onSmartSuggestionsClick
                )
            }
        }
    }
}
