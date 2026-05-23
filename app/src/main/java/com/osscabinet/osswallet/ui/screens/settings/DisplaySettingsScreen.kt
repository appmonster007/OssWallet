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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppFont
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val selectedFont by viewModel.selectedFont.collectAsStateWithLifecycle()
    val cardAnimationEnabled by viewModel.cardAnimationEnabled.collectAsStateWithLifecycle()
    val expandableFabEnabled by viewModel.expandableFabEnabled.collectAsStateWithLifecycle()
    val recentActivitiesBreakdown by viewModel.recentActivitiesBreakdown.collectAsStateWithLifecycle()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Display & Style", style = AppTextStyles.calculatorKey) },
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
            AeonSettingsSection("APPEARANCE") {
                AeonSettingsItem(
                    title = "Theme",
                    subtitle = theme,
                    icon = Icons.Rounded.Palette,
                    onClick = { showThemeDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                AeonSettingsItem(
                    title = "Font",
                    subtitle = AppFont.fromName(selectedFont).displayName,
                    icon = Icons.Rounded.FontDownload,
                    onClick = { showFontDialog = true }
                )
            }

            AeonSettingsSection("ANIMATIONS & INTERFACE") {
                AeonSettingsItem(
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
                            )
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                AeonSettingsItem(
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
                            )
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.5f))
                AeonSettingsItem(
                    title = "Recent Activities Breakdown",
                    subtitle = if (recentActivitiesBreakdown) "Currency breakdown shown in group headers" else "Currency breakdown hidden in group headers",
                    icon = Icons.Rounded.CurrencyExchange,
                    trailingContent = {
                        Switch(
                            checked = recentActivitiesBreakdown,
                            onCheckedChange = { viewModel.setRecentActivitiesBreakdown(it) },
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
