package com.osscabinet.ossWallet.ui.screens.settings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BlurOff
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection
import com.osscabinet.ossWallet.ui.components.ContourBackground
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppFont
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

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
    val glassmorphismEnabled by viewModel.glassmorphismEnabled.collectAsStateWithLifecycle()
    val contourBgVersion by viewModel.contourBgVersion.collectAsStateWithLifecycle()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showContourDialog by remember { mutableStateOf(false) }

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

    if (showContourDialog) {
        val versions = listOf(
            1 to "Clean",
            2 to "Bold",
            3 to "Organic",
            4 to "Geographical"
        )
        Dialog(
            onDismissRequest = { showContourDialog = false },
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
                        text = "Contour Style",
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
                        versions.forEach { (v, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setContourBgVersion(v)
                                        showContourDialog = false
                                    }
                                    .padding(AeonTheme.dimensions.cardPadding),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.SettingsSuggest,
                                    contentDescription = null,
                                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd)
                                )
                                Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
                                Text(
                                    name,
                                    modifier = Modifier.weight(1f),
                                    style = AppTextStyles.listItemTitle
                                )
                                if (v == contourBgVersion) {
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
                            onClick = { showContourDialog = false }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Display & Style", style = AppTextStyles.screenTitle) },
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
                    EntrySwitchRow(
                        label = "Glass Effect",
                        subtitle = if (glassmorphismEnabled) "Frosted blur on headers" else "Solid surface on headers",
                        icon = if (glassmorphismEnabled) Icons.Rounded.BlurOn else Icons.Rounded.BlurOff,
                        checked = glassmorphismEnabled,
                        onCheckedChange = { viewModel.setGlassmorphismEnabled(it) },
                        isFlush = false
                    )
                    if (glassmorphismEnabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                            thickness = 0.5.dp,
                            color = AppColors.divider.copy(alpha = 0.5f)
                        )
                        AeonSettingsItem(
                            title = "Contour Background",
                            subtitle = when (contourBgVersion) {
                                1 -> "Clean"
                                2 -> "Bold"
                                3 -> "Organic"
                                else -> "Geographical"
                            },
                            icon = Icons.Rounded.SettingsSuggest,
                            onClick = { showContourDialog = true }
                        )
                    }
                }

                AeonSettingsSection("ANIMATIONS & INTERFACE") {
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
                }
            }
        }
    }
}
