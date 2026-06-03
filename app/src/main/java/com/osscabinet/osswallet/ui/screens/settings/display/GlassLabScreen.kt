package com.osscabinet.ossWallet.ui.screens.settings.display

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.settings.GlassSettings
import com.osscabinet.ossWallet.domain.model.settings.GlassStyleConfig
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.ContourBackground
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.LocalGlassSettings
import com.osscabinet.ossWallet.ui.theme.LocalThemeMode
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import dev.chrisbanes.haze.hazeSource

@Composable
fun GlassLabScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val glassSettings by viewModel.glassSettings.collectAsStateWithLifecycle()
    
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
    val tokens = listOf("Standard", "Drawer", "Pill", "Card", "Sheet", "Popup")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Glass Lab", style = AppTextStyles.screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Preview Section
                CompositionLocalProvider(LocalGlassSettings provides glassSettings) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
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
                            AeonHazeProvider(
                                backgroundColor = if (isDarkPreview) Color(0xFF121212) else AppColors.surfaceContainerLow,
                                drawContours = true
                            ) { hazeLayers ->
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    // Background pattern to see the blur clearly
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .hazeSource(hazeLayers.states[0])
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

                                    // The Glass layer on top
                                    GlassSurface(
                                        modifier = Modifier.size(280.dp, 100.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        hazeState = hazeLayers.states[0],
                                        hazeStyle = when (selectedToken) {
                                            "Drawer" -> GlassTokens.drawerStyle(isDark = isDarkPreview)
                                            "Pill" -> GlassTokens.pillStyle(isDark = isDarkPreview)
                                            "Card" -> GlassTokens.cardStyle(isDark = isDarkPreview)
                                            "Sheet" -> GlassTokens.sheetStyle(isDark = isDarkPreview)
                                            "Popup" -> GlassTokens.popupStyle(isDark = isDarkPreview)
                                            else -> GlassTokens.style(isDark = isDarkPreview)
                                        },
                                        fallbackColor = (if (isDarkPreview) Color.Black else Color.White).copy(alpha = 0.2f),
                                        borderColor = (if (isDarkPreview) Color.White else AppColors.outlineVariant).copy(alpha = 0.5f)
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

                // Token Selection
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "SELECT STYLE TO TUNE",
                        style = AppTextStyles.sectionOverline,
                        color = AppColors.primary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            tokens.take(3).forEach { token ->
                                StyleChip(
                                    label = token,
                                    selected = selectedToken == token,
                                    onClick = { selectedToken = token }
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            tokens.drop(3).forEach { token ->
                                StyleChip(
                                    label = token,
                                    selected = selectedToken == token,
                                    onClick = { selectedToken = token }
                                )
                            }
                        }
                    }
                }

                // Tuning Controls
                val currentConfig = when (selectedToken) {
                    "Drawer" -> glassSettings.drawer
                    "Pill" -> glassSettings.pill
                    "Card" -> glassSettings.card
                    "Sheet" -> glassSettings.sheet
                    "Popup" -> glassSettings.popup
                    else -> glassSettings.standard
                }

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
                                value = currentConfig.blurRadius,
                                range = 0f..40f,
                                valueDisplay = "${currentConfig.blurRadius.toInt()}dp",
                                onValueChange = { v -> onUpdateStyle(selectedToken) { it.copy(blurRadius = v) } },
                                onReset = { onResetParameter(selectedToken, "blurRadius") }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.2f))
                            TuningSliderRow(
                                label = "Light Alpha",
                                value = currentConfig.lightAlpha,
                                range = 0f..1f,
                                valueDisplay = "${(currentConfig.lightAlpha * 100).toInt()}%",
                                onValueChange = { v -> onUpdateStyle(selectedToken) { it.copy(lightAlpha = v) } },
                                onReset = { onResetParameter(selectedToken, "lightAlpha") }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.2f))
                            TuningSliderRow(
                                label = "Dark Alpha",
                                value = currentConfig.darkAlpha,
                                range = 0f..1f,
                                valueDisplay = "${(currentConfig.darkAlpha * 100).toInt()}%",
                                onValueChange = { v -> onUpdateStyle(selectedToken) { it.copy(darkAlpha = v) } },
                                onReset = { onResetParameter(selectedToken, "darkAlpha") }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.2f))
                            TuningSliderRow(
                                label = "Noise Factor",
                                value = currentConfig.noiseFactor,
                                range = 0f..1f,
                                valueDisplay = "${(currentConfig.noiseFactor * 100).toInt()}%",
                                onValueChange = { v -> onUpdateStyle(selectedToken) { it.copy(noiseFactor = v) } },
                                onReset = { onResetParameter(selectedToken, "noiseFactor") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun GlassLabScreenPreviewLight() {
    val settings = GlassSettings()
    OssWalletTheme(theme = "Light", glassSettings = settings) {
        GlassLabContent(
            glassSettings = settings,
            onBack = {},
            onUpdateStyle = { _, _ -> },
            onResetStyle = {},
            onResetParameter = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun GlassLabScreenPreviewDark() {
    val settings = GlassSettings()
    OssWalletTheme(theme = "Dark", glassSettings = settings) {
        GlassLabContent(
            glassSettings = settings,
            onBack = {},
            onUpdateStyle = { _, _ -> },
            onResetStyle = {},
            onResetParameter = { _, _ -> }
        )
    }
}
