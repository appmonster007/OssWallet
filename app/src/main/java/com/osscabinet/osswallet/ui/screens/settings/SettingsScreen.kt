package com.osscabinet.ossWallet.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.TableRows
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonDottedUnderline
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    actions: SettingsActions
) {
    val currencyCode by viewModel.currency.collectAsStateWithLifecycle()
    val currencyProviderId by viewModel.currencyProvider.collectAsStateWithLifecycle()
    val availableProviders = viewModel.availableProviders
    val providerName = availableProviders.find { it.id == currencyProviderId }?.name ?: "Provider"
    val providerAttribution = availableProviders.find { it.id == currencyProviderId }?.attribution ?: "Exchangerate.host"

    val scrollState = rememberScrollState()

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0)
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .statusBarsPadding()
                            .padding(innerPadding)
                            .padding(
                                top = AeonSize.D64.value + AeonTheme.dimensions.screenPadding,
                                bottom = 40.dp
                            )
                            .padding(horizontal = AeonTheme.dimensions.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacing2xl)
                    ) {
                        // ── PERSONALIZATION ─────────────────────────────────
                        AeonSettingsSection(
                            title = "PERSONALIZATION",
                            accentBar = AppColors.accent
                        ) {
                            SettingsNavigationItem(
                                title = "Display & Style",
                                subtitle = "THEME • FONTS • ANIMATIONS",
                                icon = Icons.Rounded.Palette,
                                iconTint = AppColors.accent,
                                onClick = actions.onDisplayClick
                            )
                            SettingsDivider()
                            SettingsNavigationItem(
                                title = "Currency & Localization",
                                subtitle = "$currencyCode  •  $providerName",
                                icon = Icons.Rounded.CurrencyExchange,
                                iconTint = AppColors.accent,
                                onClick = actions.onCurrencyClick
                            )
                        }

                        // ── FINANCIAL SETUP ─────────────────────────────────
                        AeonSettingsSection(
                            title = "FINANCIAL SETUP",
                            accentBar = AppColors.positive
                        ) {
                            SettingsNavigationItem(
                                title = "Accounts",
                                subtitle = "BALANCES • VISIBILITY",
                                icon = Icons.Rounded.AccountBalance,
                                iconTint = AppColors.positive,
                                onClick = actions.onAccountsClick
                            )
                            SettingsDivider()
                            SettingsNavigationItem(
                                title = "Categories",
                                subtitle = "INCOME • EXPENSE CLASSIFICATION",
                                icon = Icons.Rounded.Category,
                                iconTint = AppColors.positive,
                                onClick = actions.onCategoriesClick
                            )
                            SettingsDivider()
                            SettingsNavigationItem(
                                title = "Tags",
                                subtitle = "CUSTOM LABELS FOR TRANSACTIONS",
                                icon = Icons.AutoMirrored.Rounded.Label,
                                iconTint = AppColors.positive,
                                onClick = actions.onTagsClick
                            )
                        }

                        // ── AUTOMATION & TOOLS ──────────────────────────────
                        AeonSettingsSection(
                            title = "AUTOMATION & TOOLS",
                            accentBar = AppColors.accent.copy(alpha = 0.5f)
                        ) {
                            SettingsNavigationItem(
                                title = "Transaction Templates",
                                subtitle = "QUICK SHORTCUTS • FREQUENT ENTRIES",
                                icon = Icons.Rounded.TableRows,
                                iconTint = AppColors.accent.copy(alpha = 0.7f),
                                onClick = actions.onTemplatesClick
                            )
                            SettingsDivider()
                            SettingsNavigationItem(
                                title = "Smart Suggestions",
                                subtitle = "DISCOVER RECURRING TRANSACTIONS",
                                icon = Icons.Rounded.AutoAwesome,
                                iconTint = AppColors.accent.copy(alpha = 0.7f),
                                onClick = actions.onSmartSuggestionsClick
                            )
                            SettingsDivider()
                            SettingsNavigationItem(
                                title = "Saved Filters",
                                subtitle = "CUSTOM SEARCH PRESETS",
                                icon = Icons.Rounded.FilterList,
                                iconTint = AppColors.accent.copy(alpha = 0.7f),
                                onClick = actions.onFiltersClick
                            )
                        }

                        // ── PRIVACY & SAFETY ────────────────────────────────
                        AeonSettingsSection(
                            title = "PRIVACY & SAFETY",
                            accentBar = AppColors.negative
                        ) {
                            SettingsNavigationItem(
                                title = "Security & Privacy",
                                subtitle = "APP LOCK • LOCATION • WEB ACCESS",
                                icon = Icons.Rounded.Lock,
                                iconTint = AppColors.negative,
                                onClick = actions.onSecurityClick
                            )
                            SettingsDivider()
                            SettingsNavigationItem(
                                title = "Data Management",
                                subtitle = "IMPORT • EXPORT • BACKUPS",
                                icon = Icons.Rounded.CloudSync,
                                iconTint = AppColors.negative,
                                onClick = actions.onDataManagementClick
                            )
                        }

                        SettingsFooter(attribution = providerAttribution)
                    }
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Settings",
                            onBackClick = actions.onBackClick
                        )
                    }
                )
            )
        }
    }
}

@Composable
private fun SettingsFooter(attribution: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AeonTheme.dimensions.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Footer separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(AppColors.outlineVariant)
        )
        Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))
        Text(
            text = "v1.1.0",
            style = AppTextStyles.metaLabel,
            color = AppColors.contentSecondary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "BUILT FOR MODERN ACCOUNTS",
            style = AppTextStyles.sectionOverline,
            color = AppColors.contentSecondary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RATES BY",
                style = AppTextStyles.metaLabel,
                color = AppColors.contentSecondary.copy(alpha = 0.45f)
            )
            Text(
                text = attribution.uppercase(),
                style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.Bold),
                color = AppColors.accent.copy(alpha = 0.7f)
            )
        }
    }
}

data class SettingsActions(
    val onDisplayClick: () -> Unit,
    val onCurrencyClick: () -> Unit,
    val onAccountsClick: () -> Unit,
    val onCategoriesClick: () -> Unit,
    val onTagsClick: () -> Unit,
    val onTemplatesClick: () -> Unit,
    val onSmartSuggestionsClick: () -> Unit,
    val onFiltersClick: () -> Unit,
    val onSecurityClick: () -> Unit,
    val onDataManagementClick: () -> Unit,
    val onBackClick: () -> Unit
)

@Composable
private fun SettingsNavigationItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    color: Color = AppColors.contentPrimary,
    iconTint: Color = AppColors.contentSecondary,
) {
    AeonSettingsItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = onClick,
        color = color,
        iconTint = iconTint,
    )
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = AeonTheme.dimensions.cardPadding)
            .aeonDottedUnderline(AppColors.outlineVariant)
    )
}
