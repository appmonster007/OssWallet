package com.osscabinet.ossWallet.ui.screens.settings.display

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.settings.GlassSettings
import com.osscabinet.ossWallet.domain.model.settings.GlassStyleConfig
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.LocalGlassSettings
import com.osscabinet.ossWallet.ui.theme.LocalThemeMode
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.OssWalletPreviewTheme

@Composable
fun GlassLabScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val glassSettings by viewModel.glassSettings.collectAsState()

    GlassLabContent(
        glassSettings = glassSettings,
        onBack = onBack,
        onUpdateStyle = { token, update -> viewModel.updateGlassStyle(token, update) },
        onResetStyle = { token -> viewModel.resetGlassStyle(token) },
        onResetParameter = { token, param -> viewModel.resetGlassParameter(token, param) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassLabContent(
    glassSettings: GlassSettings,
    onBack: () -> Unit,
    onUpdateStyle: (String, (GlassStyleConfig) -> GlassStyleConfig) -> Unit,
    onResetStyle: (String) -> Unit,
    onResetParameter: (String, String) -> Unit
) {
    val themeMode = LocalThemeMode.current
    var selectedToken by remember { mutableStateOf("Standard") }
    var isDarkPreview by remember(themeMode.isDarkTheme) { mutableStateOf(themeMode.isDarkTheme) }
    val tokens = listOf("Standard", "Drawer", "Pill", "Card", "Sheet", "Popup", "Item")

    val currentConfig = when (selectedToken) {
        "Drawer" -> glassSettings.drawer
        "Pill" -> glassSettings.pill
        "Card" -> glassSettings.card
        "Sheet" -> glassSettings.sheet
        "Popup" -> glassSettings.popup
        "Item" -> glassSettings.item
        else -> glassSettings.standard
    }

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(
                                top = AeonSize.D64.value,
                                bottom = innerPadding.calculateBottomPadding()
                            )
                    ) {
                        // Sticky Preview Section
                        CompositionLocalProvider(LocalGlassSettings provides glassSettings) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AeonTheme.dimensions.screenPadding),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "LIVE PREVIEW",
                                        style = AppTextStyles.sectionOverline,
                                        color = AppColors.primary
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { isDarkPreview = !isDarkPreview }
                                    ) {
                                        Text(
                                            if (isDarkPreview) "DARK MODE" else "LIGHT MODE",
                                            style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold),
                                            color = AppColors.contentSecondary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            if (isDarkPreview) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = AppColors.primary
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color.Transparent,
                                    border = BorderStroke(1.dp, if (isDarkPreview) Color.White.copy(alpha = 0.1f) else AppColors.outlineVariant)
                                ) {
                                    OssWalletPreviewTheme(theme = if (isDarkPreview) "Dark" else "Light") {
                                        AeonHazeProvider(
                                            backgroundColor = AppColors.screenBackground,
                                            drawContours = true
                                        ) { innerHaze ->
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                // Background pattern to see the blur clearly
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .aeonHazeSource(innerHaze.states[0])
                                                        .padding(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    repeat(8) {
                                                        Text(
                                                            "GLASSMORPHISM PREVIEW TEXT ".repeat(2),
                                                            style = AppTextStyles.caption.copy(
                                                                color = (if (isDarkPreview) Color.White else AppColors.contentSecondary).copy(alpha = 0.2f),
                                                                letterSpacing = 2.sp
                                                            )
                                                        )
                                                    }
                                                }

                                                val previewBgColor = when (selectedToken) {
                                                    "Pill" -> AppColors.componentOnCard
                                                    "Card" -> AppColors.cardSurface
                                                    "Sheet" -> AppColors.sheetSurface
                                                    "Popup" -> AppColors.popupSurface
                                                    "Item" -> AppColors.componentOnCard
                                                    else -> AppColors.cardSurfaceProminent
                                                }

                                                // The Glass layer on top
                                                GlassSurface(
                                                    modifier = Modifier.size(280.dp, 100.dp),
                                                    shape = RoundedCornerShape(20.dp),
                                                    hazeState = innerHaze.states[0],
                                                    hazeStyle = GlassTokens.createHazeStyle(
                                                        config = currentConfig,
                                                        bgColor = previewBgColor,
                                                        isDark = isDarkPreview
                                                    ),
                                                    glassVariant = selectedToken.lowercase(),
                                                    shadowElevation = currentConfig.surface.shadowElevation.dp,
                                                    borderAlpha = currentConfig.surface.borderAlpha,
                                                    fallbackColor = previewBgColor
                                                ) {
                                                    Text(
                                                        selectedToken.uppercase(),
                                                        style = AppTextStyles.managementItemTitle.copy(
                                                            letterSpacing = 2.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isDarkPreview) Color.White else AppColors.contentPrimary
                                                        ),
                                                        modifier = Modifier.align(Alignment.Center)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Scrollable Tuning Section
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = AeonTheme.dimensions.screenPadding),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Token Selection (Revamped)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AeonTheme.dimensions.spacingSm),
                                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                                    Text(
                                        "SELECT COMPONENT STYLE",
                                        style = AppTextStyles.sectionOverline,
                                        color = AppColors.primary
                                    )
                                    Text(
                                        "Choose a glass profile to customize its specific properties",
                                        style = AppTextStyles.caption.copy(color = AppColors.contentSecondary.copy(alpha = 0.6f))
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                                ) {
                                    tokens.forEach { token ->
                                        val isSelected = selectedToken == token
                                        val icon = when (token) {
                                            "Drawer" -> Icons.AutoMirrored.Rounded.MenuOpen
                                            "Pill" -> Icons.Rounded.Circle
                                            "Card" -> Icons.Rounded.Dashboard
                                            "Sheet" -> Icons.Rounded.KeyboardArrowUp
                                            "Popup" -> Icons.Rounded.PictureInPicture
                                            "Item" -> Icons.Rounded.GridView
                                            else -> Icons.Rounded.Layers
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(72.dp)
                                                .clickable { selectedToken = token }
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
                                                color = if (isSelected) AppColors.primary.copy(alpha = 0.15f) else AppColors.surfaceContainerLow,
                                                border = BorderStroke(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) AppColors.primary else AppColors.outlineVariant
                                                ),
                                                modifier = Modifier.size(64.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = if (isSelected) AppColors.primary else AppColors.contentSecondary.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))
                                            Text(
                                                text = token,
                                                style = AppTextStyles.caption.copy(
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = if (isSelected) AppColors.primary else AppColors.contentSecondary
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Tuning Controls
                            Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "PARAMETERS: ${selectedToken.uppercase()}",
                                        style = AppTextStyles.sectionOverline,
                                        color = AppColors.primary
                                    )
                                    
                                    TextButton(
                                        onClick = { onResetStyle(selectedToken) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = AppColors.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "RESET STYLE",
                                            style = AppTextStyles.caption.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                                color = AppColors.primary
                                            )
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
                                    color = AppColors.surfaceContainerLow,
                                    border = BorderStroke(1.dp, AppColors.outlineVariant)
                                ) {
                                    Column {
                                        TuningSliderRow(
                                            label = "Blur Radius",
                                            value = currentConfig.glass.blurRadius,
                                            range = 0f..40f,
                                            valueDisplay = "${currentConfig.glass.blurRadius.toInt()}dp",
                                            onValueChange = { v -> onUpdateStyle(selectedToken) { it.copy(glass = it.glass.copy(blurRadius = v)) } },
                                            onReset = { onResetParameter(selectedToken, "blurRadius") }
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.2f))
                                        TuningSliderRow(
                                            label = "Light Alpha",
                                            value = currentConfig.glass.lightAlpha,
                                            range = 0f..1f,
                                            valueDisplay = "${(currentConfig.glass.lightAlpha * 100).toInt()}%",
                                            onValueChange = { v -> onUpdateStyle(selectedToken) { it.copy(glass = it.glass.copy(lightAlpha = v)) } },
                                            onReset = { onResetParameter(selectedToken, "lightAlpha") }
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.2f))
                                        TuningSliderRow(
                                            label = "Dark Alpha",
                                            value = currentConfig.glass.darkAlpha,
                                            range = 0f..1f,
                                            valueDisplay = "${(currentConfig.glass.darkAlpha * 100).toInt()}%",
                                            onValueChange = { v -> onUpdateStyle(selectedToken) { it.copy(glass = it.glass.copy(darkAlpha = v)) } },
                                            onReset = { onResetParameter(selectedToken, "darkAlpha") }
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.2f))
                                        TuningSliderRow(
                                            label = "Noise Factor",
                                            value = currentConfig.glass.noiseFactor,
                                            range = 0f..1f,
                                            valueDisplay = "${(currentConfig.glass.noiseFactor * 100).toInt()}%",
                                            onValueChange = { v -> onUpdateStyle(selectedToken) { it.copy(glass = it.glass.copy(noiseFactor = v)) } },
                                            onReset = { onResetParameter(selectedToken, "noiseFactor") }
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.2f))
                                        TuningSliderRow(
                                            label = "Shadow Elevation",
                                            value = currentConfig.surface.shadowElevation,
                                            range = 0f..32f,
                                            valueDisplay = "${currentConfig.surface.shadowElevation.toInt()}dp",
                                            onValueChange = { v -> onUpdateStyle(selectedToken) { it.copy(surface = it.surface.copy(shadowElevation = v)) } },
                                            onReset = { onResetParameter(selectedToken, "shadowElevation") }
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.2f))
                                        TuningSliderRow(
                                            label = "Border Alpha",
                                            value = currentConfig.surface.borderAlpha,
                                            range = 0f..1f,
                                            valueDisplay = "${(currentConfig.surface.borderAlpha * 100).toInt()}%",
                                            onValueChange = { v -> onUpdateStyle(selectedToken) { it.copy(surface = it.surface.copy(borderAlpha = v)) } },
                                            onReset = { onResetParameter(selectedToken, "borderAlpha") }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Glass Lab",
                            onBackClick = onBack
                        )
                    }
                )
            )
        }
    }
}
