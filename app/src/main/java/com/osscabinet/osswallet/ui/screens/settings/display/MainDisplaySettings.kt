package com.osscabinet.ossWallet.ui.screens.settings.display

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.domain.model.settings.UiDensity
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppFont
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDisplaySettings(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onNavigateToDesignLab: () -> Unit
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val selectedFont by viewModel.selectedFont.collectAsStateWithLifecycle()
    val cardAnimationEnabled by viewModel.cardAnimationEnabled.collectAsStateWithLifecycle()
    val expandableFabEnabled by viewModel.expandableFabEnabled.collectAsStateWithLifecycle()
    val recentActivitiesBreakdown by viewModel.recentActivitiesBreakdown.collectAsStateWithLifecycle()
    val recordsSettings by viewModel.recordsSettings.collectAsStateWithLifecycle()

    val uiDensityName by viewModel.uiDensity.collectAsStateWithLifecycle()
    val currentDensity = UiDensity.fromName(uiDensityName)

    val dynamicAccent by viewModel.dynamicAccent.collectAsStateWithLifecycle()
    val dynamicAccentAvailable = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showDensityDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        val themes = listOf("System", "Light", "Dark", "Amoled")
        Dialog(
            onDismissRequest = { showThemeDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = screenHeight * 0.85f),
                shape = RoundedCornerShape(32.dp),
                color = AppColors.sheetSurface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "App Appearance",
                        style = AppTextStyles.heroAmount.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 28.sp,
                            color = AppColors.contentPrimary.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                val icon = when (t) {
                                    "Light" -> Icons.Rounded.LightMode
                                    "Dark" -> Icons.Rounded.DarkMode
                                    "Amoled" -> Icons.Rounded.Contrast
                                    else -> Icons.Rounded.SettingsSuggest
                                }
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd)
                                )
                                Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
                                Text(
                                    t,
                                    modifier = Modifier.weight(1f),
                                    style = AppTextStyles.listItemTitle
                                )
                                if (t == theme) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = AppColors.primary
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(
                            onClick = { showThemeDialog = false }
                        ) {
                            Text(
                                "Close",
                                style = AppTextStyles.listItemTitle.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFontDialog) {
        Dialog(
            onDismissRequest = { showFontDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = screenHeight * 0.85f),
                shape = RoundedCornerShape(32.dp),
                color = AppColors.sheetSurface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "App Font",
                        style = AppTextStyles.heroAmount.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 28.sp,
                            color = AppColors.contentPrimary.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                Icon(
                                    Icons.Rounded.FontDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd)
                                )
                                Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
                                Text(
                                    font.displayName,
                                    modifier = Modifier.weight(1f),
                                    style = AppTextStyles.listItemTitle
                                )
                                if (font.name == selectedFont) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = AppColors.primary
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(
                            onClick = { showFontDialog = false }
                        ) {
                            Text(
                                "Close",
                                style = AppTextStyles.listItemTitle.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDensityDialog) {
        Dialog(
            onDismissRequest = { showDensityDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = screenHeight * 0.85f),
                shape = RoundedCornerShape(32.dp),
                color = AppColors.sheetSurface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "UI Density",
                        style = AppTextStyles.heroAmount.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 28.sp,
                            color = AppColors.contentPrimary.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Controls the size of buttons, icons, spacing and text throughout the app.",
                        style = AppTextStyles.listItemSubtitle,
                        color = AppColors.contentSecondary,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UiDensity.entries.forEach { density ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setUiDensity(density)
                                        showDensityDialog = false
                                    }
                                    .padding(AeonTheme.dimensions.cardPadding),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.GridView,
                                    contentDescription = null,
                                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd)
                                )
                                Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(density.displayName, style = AppTextStyles.listItemTitle)
                                    Text(
                                        text = when (density) {
                                            UiDensity.SMALL -> "Compact — more content visible"
                                            UiDensity.MEDIUM -> "Slightly reduced sizing"
                                            UiDensity.HIGH -> "Default sizing"
                                            UiDensity.EXTRA_HIGH -> "Larger — easier to tap"
                                        },
                                        style = AppTextStyles.listItemSubtitle,
                                        color = AppColors.contentSecondary
                                    )
                                }
                                if (density == currentDensity) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = AppColors.primary
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(onClick = { showDensityDialog = false }) {
                            Text(
                                "Close",
                                style = AppTextStyles.listItemTitle.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0)
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                            .statusBarsPadding()
                            .padding(innerPadding)
                            .padding(
                                top = AeonSize.D64.value + AeonTheme.dimensions.screenPadding,
                                bottom = 40.dp
                            )
                            .padding(horizontal = AeonTheme.dimensions.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacing2xl)
                    ) {
                        AeonSettingsSection("APPEARANCE") {
                            AeonSettingsItem(
                                title = "Theme",
                                subtitle = theme,
                                icon = Icons.Rounded.Palette,
                                onClick = { showThemeDialog = true }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.5f)
                            )
                            AeonSettingsItem(
                                title = "Font",
                                subtitle = AppFont.fromName(selectedFont).displayName,
                                icon = Icons.Rounded.FontDownload,
                                onClick = { showFontDialog = true }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.5f)
                            )
                            AeonSettingsItem(
                                title = "UI Density",
                                subtitle = currentDensity.displayName,
                                icon = Icons.Rounded.GridView,
                                onClick = { showDensityDialog = true }
                            )
                            if (dynamicAccentAvailable) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider.copy(alpha = 0.5f)
                                )
                                EntrySwitchRow(
                                    label = "Dynamic Accent",
                                    subtitle = if (dynamicAccent) "Accent follows your wallpaper colors" else "Use the signature accent color",
                                    icon = Icons.Rounded.Palette,
                                    checked = dynamicAccent,
                                    onCheckedChange = { viewModel.setDynamicAccent(it) },
                                    isFlush = false
                                )
                            }
                        }

                        AeonSettingsSection("VISUAL EFFECTS") {
                            AeonSettingsItem(
                                title = "Design Lab",
                                subtitle = "Customize glass, surfaces, background and more",
                                icon = Icons.Rounded.Tune,
                                onClick = onNavigateToDesignLab
                            )
                        }

                        AeonSettingsSection("INTERFACE") {
                            EntrySwitchRow(
                                label = "Card Animations",
                                subtitle = if (cardAnimationEnabled) "Animate cards on screen load" else "Disable card animations",
                                icon = if (cardAnimationEnabled) Icons.Rounded.Animation else Icons.Rounded.Block,
                                checked = cardAnimationEnabled,
                                onCheckedChange = { viewModel.setCardAnimationEnabled(it) },
                                isFlush = false
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.5f)
                            )
                            EntrySwitchRow(
                                label = "Expandable Action Button",
                                subtitle = if (expandableFabEnabled) "Show quick templates and actions" else "Simple button for new entries",
                                icon = if (expandableFabEnabled) Icons.Rounded.GridView else Icons.Rounded.AddCircleOutline,
                                checked = expandableFabEnabled,
                                onCheckedChange = { viewModel.setExpandableFabEnabled(it) },
                                isFlush = false
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.5f)
                            )
                            EntrySwitchRow(
                                label = "Recent Activities Breakdown",
                                subtitle = if (recentActivitiesBreakdown) "Currency breakdown shown in group headers" else "Currency breakdown hidden in group headers",
                                icon = Icons.Rounded.CurrencyExchange,
                                checked = recentActivitiesBreakdown,
                                onCheckedChange = { viewModel.setRecentActivitiesBreakdown(it) },
                                isFlush = false
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.5f)
                            )
                            EntrySwitchRow(
                                label = "Sticky Headers",
                                subtitle = if (recordsSettings.stickyHeaders) "Transaction headers stick to top" else "Headers flow with list content",
                                icon = Icons.Rounded.GridView,
                                checked = recordsSettings.stickyHeaders,
                                onCheckedChange = { checked ->
                                    viewModel.updateRecordsSettings { it.copy(stickyHeaders = checked) }
                                },
                                isFlush = false
                            )
                        }
                    }
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Display & Style",
                            onBackClick = onBackClick
                        )
                    }
                )
            )
        }
    }
}
