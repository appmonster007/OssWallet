package com.osscabinet.ossWallet.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onDisplayClick: () -> Unit,
    onCurrencyClick: () -> Unit,
    onDataClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val currencyCode by viewModel.currency.collectAsStateWithLifecycle()
    val currencyProviderId by viewModel.currencyProvider.collectAsStateWithLifecycle()
    val availableProviders = viewModel.availableProviders

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = AppTextStyles.calculatorKey) },
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
                .verticalScroll(scrollState)
                .padding(AeonTheme.dimensions.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacing2xl)
        ) {
            AeonSettingsSection("GENERAL") {
                AeonSettingsItem(
                    title = "Display & Style",
                    subtitle = "Theme, fonts, and animations",
                    icon = Icons.Rounded.Palette,
                    onClick = onDisplayClick
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                AeonSettingsItem(
                    title = "Currency & Localization",
                    subtitle = "$currencyCode • ${availableProviders.find { it.id == currencyProviderId }?.name ?: "Provider"}",
                    icon = Icons.Rounded.CurrencyExchange,
                    onClick = onCurrencyClick
                )
            }

            AeonSettingsSection("ORGANIZATION") {
                AeonSettingsItem(
                    title = "Data & Organization",
                    subtitle = "Accounts, categories, and tags",
                    icon = Icons.Rounded.Category,
                    onClick = onDataClick
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                AeonSettingsItem(
                    title = "Privacy & Security",
                    subtitle = "Location, web access, and backups",
                    icon = Icons.Rounded.Lock,
                    onClick = onSecurityClick
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
                    style = AppTextStyles.transactionSwipeAction.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.primary
                    )
                )
            }
        }
    }
}
