package com.osscabinet.ossWallet.ui.screens.settings.display

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.BlurOff
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsSuggest
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.settings.GlassSettings
import com.osscabinet.ossWallet.domain.model.settings.GlassStyleConfig
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.LocalContourBgVersion
import com.osscabinet.ossWallet.ui.theme.LocalContourLinesEnabled
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.LocalGlassSettings
import com.osscabinet.ossWallet.ui.theme.LocalRandomizeContourEachLoad
import com.osscabinet.ossWallet.ui.theme.LocalThemeMode
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import dev.chrisbanes.haze.HazeState

@Composable
fun DesignLabScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val glassSettings by viewModel.glassSettings.collectAsState()
    val glassmorphismEnabled by viewModel.glassmorphismEnabled.collectAsStateWithLifecycle()
    val contourLinesEnabled by viewModel.contourLinesEnabled.collectAsStateWithLifecycle()
    val contourBgVersion by viewModel.contourBgVersion.collectAsStateWithLifecycle()
    val randomizeContourEachLoad by viewModel.randomizeContourEachLoad.collectAsStateWithLifecycle()

    DesignLabContent(
        glassSettings = glassSettings,
        glassmorphismEnabled = glassmorphismEnabled,
        contourLinesEnabled = contourLinesEnabled,
        contourBgVersion = contourBgVersion,
        randomizeContourEachLoad = randomizeContourEachLoad,
        onBack = onBack,
        onGlassmorphismToggle = { viewModel.setGlassmorphismEnabled(it) },
        onContourLinesToggle = { viewModel.setContourLinesEnabled(it) },
        onContourBgVersionChange = { viewModel.setContourBgVersion(it) },
        onRandomizeContourToggle = { viewModel.setRandomizeContourEachLoad(it) },
        onUpdateStyle = { token, update -> viewModel.updateGlassStyle(token, update) },
        onResetStyle = { token -> viewModel.resetGlassStyle(token) },
        onResetParameter = { token, param -> viewModel.resetGlassParameter(token, param) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignLabContent(
    glassSettings: GlassSettings,
    glassmorphismEnabled: Boolean,
    contourLinesEnabled: Boolean,
    contourBgVersion: Int,
    randomizeContourEachLoad: Boolean,
    onBack: () -> Unit,
    onGlassmorphismToggle: (Boolean) -> Unit,
    onContourLinesToggle: (Boolean) -> Unit,
    onContourBgVersionChange: (Int) -> Unit,
    onRandomizeContourToggle: (Boolean) -> Unit,
    onUpdateStyle: (String, (GlassStyleConfig) -> GlassStyleConfig) -> Unit,
    onResetStyle: (String) -> Unit,
    onResetParameter: (String, String) -> Unit
) {
    val themeMode = LocalThemeMode.current
    var selectedToken by remember { mutableStateOf("Standard") }
    var previewMode by remember {
        mutableStateOf(
            when {
                themeMode.isAmoled -> "Amoled"
                themeMode.isDarkTheme -> "Dark"
                else -> "Light"
            }
        )
    }
    val isDarkPreview = previewMode != "Light"
    var showContourDialog by remember { mutableStateOf(false) }
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

    val variantConstant = when (selectedToken) {
        "Drawer" -> GlassVariant.Drawer
        "Pill" -> GlassVariant.Pill
        "Card" -> GlassVariant.Card
        "Sheet" -> GlassVariant.Sheet
        "Popup" -> GlassVariant.Popup
        "Item" -> GlassVariant.Item
        else -> GlassVariant.Standard
    }

    // Contour style dialog
    if (showContourDialog) {
        val versions = listOf(1 to "Clean", 2 to "Bold", 3 to "Organic", 4 to "Geographical")
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
                Column(modifier = Modifier.padding(24.dp)) {
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
                                        onContourBgVersionChange(v)
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
                                Text(name, modifier = Modifier.weight(1f), style = AppTextStyles.listItemTitle)
                                if (v == contourBgVersion) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = AppColors.primary)
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(onClick = { showContourDialog = false }) {
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

    val baseConfig = LocalConfiguration.current
    val previewConfig = remember(isDarkPreview, baseConfig) {
        android.content.res.Configuration(baseConfig).apply {
            uiMode = (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (isDarkPreview)
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                else
                    android.content.res.Configuration.UI_MODE_NIGHT_NO
        }
    }

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(containerColor = Color.Transparent) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(
                            top = AeonSize.D64.value,
                            bottom = innerPadding.calculateBottomPadding()
                        )
                ) {
                    // ── STICKY LIVE PREVIEW ──────────────────────────────────────
                    CompositionLocalProvider(LocalGlassSettings provides glassSettings) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AeonTheme.dimensions.screenPadding),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    modifier = Modifier.clickable {
                                        previewMode = when (previewMode) {
                                            "Light" -> "Dark"
                                            "Dark"  -> "Amoled"
                                            else    -> "Light"
                                        }
                                    }
                                ) {
                                    Text(
                                        previewMode.uppercase(),
                                        style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold),
                                        color = AppColors.contentSecondary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        when (previewMode) {
                                            "Light"  -> Icons.Rounded.LightMode
                                            "Amoled" -> Icons.Rounded.Contrast
                                            else     -> Icons.Rounded.DarkMode
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = AppColors.primary
                                    )
                                }
                            }

                            // Preview box — token-contextual mock inside
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(260.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    // Evaluated outside OssWalletPreviewTheme scope — use explicit
                                    // values driven by isDarkPreview rather than AppColors.
                                    if (isDarkPreview) Color.White.copy(alpha = 0.10f)
                                    else Color.Black.copy(alpha = 0.10f)
                                )
                            ) {
                                // OssWalletPreviewTheme provides LocalAppColorScheme, LocalThemeMode,
                                // and MaterialTheme colors for the selected preview palette without
                                // triggering window/status-bar side effects.
                                com.osscabinet.ossWallet.ui.theme.OssWalletPreviewTheme(theme = previewMode) {
                                CompositionLocalProvider(
                                    LocalConfiguration provides previewConfig,
                                    LocalGlassmorphismEnabled provides glassmorphismEnabled,
                                    LocalContourLinesEnabled provides contourLinesEnabled,
                                    LocalContourBgVersion provides contourBgVersion,
                                    LocalRandomizeContourEachLoad provides randomizeContourEachLoad,
                                ) {
                                AeonHazeProvider(
                                    // AppColors.screenBackground now resolves to the M3 background
                                    // token from the preview palette (Light/Dark/Amoled).
                                    backgroundColor = AppColors.screenBackground,
                                    drawContours = contourLinesEnabled
                                ) { innerHaze ->
                                    // hazeSource is on the background Column; the mock is its
                                    // sibling — hazeEffect must never be a descendant of hazeSource
                                    // for the same state (Haze 1.7.2+ constraint).
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .aeonHazeSource(innerHaze.states[0])
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            repeat(10) {
                                                Text(
                                                    "DESIGN LAB PREVIEW  ".repeat(3),
                                                    style = AppTextStyles.caption.copy(
                                                        color = (if (isDarkPreview) Color.White else AppColors.contentSecondary).copy(alpha = 0.15f),
                                                        letterSpacing = 2.sp
                                                    ),
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        val previewHazeStyle = GlassTokens.createHazeStyle(
                                            config = currentConfig,
                                            bgColor = when (selectedToken) {
                                                "Pill" -> AppColors.componentOnCard
                                                "Card" -> AppColors.cardSurface
                                                "Sheet" -> AppColors.sheetSurface
                                                "Popup" -> AppColors.popupSurface
                                                "Item" -> AppColors.componentOnCard
                                                else -> AppColors.cardSurfaceProminent
                                            },
                                            isDark = isDarkPreview
                                        )
                                        val fallback = when (previewMode) {
                                            "Amoled" -> Color.Black.copy(alpha = 0.4f)
                                            "Dark"   -> Color.Black.copy(alpha = 0.2f)
                                            else     -> Color.White.copy(alpha = 0.2f)
                                        }
                                        val contentTint = if (isDarkPreview) Color.White else AppColors.contentPrimary

                                        // Token-contextual mock — sibling of hazeSource Column
                                        AnimatedContent(
                                            targetState = selectedToken,
                                            transitionSpec = {
                                                (fadeIn(spring()) + scaleIn(spring(), 0.92f))
                                                    .togetherWith(fadeOut(spring()))
                                            },
                                            label = "tokenPreview",
                                            modifier = Modifier.fillMaxSize()
                                        ) { token ->
                                            when (token) {
                                                "Standard" -> StandardMock(
                                                    innerHaze.states[0], previewHazeStyle,
                                                    variantConstant, fallback, contentTint
                                                )
                                                "Drawer" -> DrawerMock(
                                                    innerHaze.states[0], previewHazeStyle,
                                                    variantConstant, fallback, contentTint
                                                )
                                                "Pill" -> PillMock(
                                                    innerHaze.states[0], previewHazeStyle,
                                                    variantConstant, fallback, contentTint
                                                )
                                                "Card" -> CardMock(
                                                    innerHaze.states[0], previewHazeStyle,
                                                    variantConstant, fallback, contentTint
                                                )
                                                "Sheet" -> SheetMock(
                                                    innerHaze.states[0], previewHazeStyle,
                                                    variantConstant, fallback, contentTint
                                                )
                                                "Popup" -> PopupMock(
                                                    innerHaze.states[0], previewHazeStyle,
                                                    variantConstant, fallback, contentTint
                                                )
                                            }
                                        }
                                    }
                                }
                                } // CompositionLocalProvider
                                } // OssWalletPreviewTheme
                            }
                        }
                    }

                    // ── SCROLLABLE BODY ──────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = AeonTheme.dimensions.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Token selector
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = AeonTheme.dimensions.spacingSm),
                            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text(
                                    "SELECT COMPONENT STYLE",
                                    style = AppTextStyles.sectionOverline,
                                    color = AppColors.primary
                                )
                                Text(
                                    "Choose a surface profile to customize its properties",
                                    style = AppTextStyles.caption.copy(
                                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                                    )
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
                                                if (isSelected) 2.dp else 1.dp,
                                                if (isSelected) AppColors.primary else AppColors.outlineVariant
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
                                            token,
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

                        // VISUAL EFFECTS section
                        AeonSettingsSection("VISUAL EFFECTS") {
                            EntrySwitchRow(
                                label = "Glass Effect",
                                subtitle = if (glassmorphismEnabled) "Frosted blur on headers" else "Solid surface on headers",
                                icon = if (glassmorphismEnabled) Icons.Rounded.BlurOn else Icons.Rounded.BlurOff,
                                checked = glassmorphismEnabled,
                                onCheckedChange = onGlassmorphismToggle,
                                isFlush = false
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                thickness = 0.5.dp,
                                color = AppColors.divider.copy(alpha = 0.5f)
                            )
                            EntrySwitchRow(
                                label = "Contour Lines",
                                subtitle = if (contourLinesEnabled) "Draw topographic lines in background" else "Hide topographic lines",
                                icon = Icons.Rounded.AddCircleOutline,
                                checked = contourLinesEnabled,
                                onCheckedChange = onContourLinesToggle,
                                isFlush = false
                            )
                            if (contourLinesEnabled) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider.copy(alpha = 0.5f)
                                )
                                AeonSettingsItem(
                                    title = "Contour Style",
                                    subtitle = when (contourBgVersion) {
                                        1 -> "Clean"
                                        2 -> "Bold"
                                        3 -> "Organic"
                                        else -> "Geographical"
                                    },
                                    icon = Icons.Rounded.SettingsSuggest,
                                    onClick = { showContourDialog = true }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider.copy(alpha = 0.5f)
                                )
                                EntrySwitchRow(
                                    label = "Randomize Each Load",
                                    subtitle = if (randomizeContourEachLoad) "Generate new patterns on every load" else "Keep pattern consistent",
                                    icon = Icons.Rounded.Animation,
                                    checked = randomizeContourEachLoad,
                                    onCheckedChange = onRandomizeContourToggle,
                                    isFlush = false
                                )
                            }
                        }

                        // Reset header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AeonTheme.dimensions.screenPadding),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "PARAMETERS: ${selectedToken.uppercase()}",
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
                                    "RESET ALL",
                                    style = AppTextStyles.caption.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = AppColors.primary
                                    )
                                )
                            }
                        }

                        // GLASS EFFECT sliders — only visible when glass is on
                        AnimatedVisibility(
                            visible = glassmorphismEnabled,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
                                Text(
                                    "GLASS EFFECT",
                                    style = AppTextStyles.caption.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        color = AppColors.contentSecondary
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
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
                        }

                        // SURFACE DESIGN sliders — always visible
                        Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
                            Text(
                                "SURFACE DESIGN",
                                style = AppTextStyles.caption.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    color = AppColors.contentSecondary
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
                                color = AppColors.surfaceContainerLow,
                                border = BorderStroke(1.dp, AppColors.outlineVariant)
                            ) {
                                Column {
                                    TuningSliderRow(
                                        label = "Shadow Elevation",
                                        value = currentConfig.shadowElevation,
                                        range = 0f..32f,
                                        valueDisplay = "${currentConfig.shadowElevation.toInt()}dp",
                                        onValueChange = { v -> onUpdateStyle(selectedToken) { it.copy(shadowElevation = v) } },
                                        onReset = { onResetParameter(selectedToken, "shadowElevation") }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.2f))
                                    TuningSliderRow(
                                        label = "Ambient Shadow Alpha",
                                        value = currentConfig.ambientShadowAlpha,
                                        range = 0f..0.4f,
                                        valueDisplay = "${(currentConfig.ambientShadowAlpha * 100).toInt()}%",
                                        onValueChange = { v -> onUpdateStyle(selectedToken) { it.copy(ambientShadowAlpha = v) } },
                                        onReset = { onResetParameter(selectedToken, "ambientShadowAlpha") }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.2f))
                                    TuningSliderRow(
                                        label = "Spot Shadow Alpha",
                                        value = currentConfig.spotShadowAlpha,
                                        range = 0f..0.4f,
                                        valueDisplay = "${(currentConfig.spotShadowAlpha * 100).toInt()}%",
                                        onValueChange = { v -> onUpdateStyle(selectedToken) { it.copy(spotShadowAlpha = v) } },
                                        onReset = { onResetParameter(selectedToken, "spotShadowAlpha") }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.2f))
                                    TuningSliderRow(
                                        label = "Border Alpha",
                                        value = currentConfig.borderAlpha,
                                        range = 0f..1f,
                                        valueDisplay = "${(currentConfig.borderAlpha * 100).toInt()}%",
                                        onValueChange = { v -> onUpdateStyle(selectedToken) { it.copy(borderAlpha = v) } },
                                        onReset = { onResetParameter(selectedToken, "borderAlpha") }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = AppColors.divider.copy(alpha = 0.2f))
                                    TuningSliderRow(
                                        label = "Border Width",
                                        value = currentConfig.borderWidth,
                                        range = 0f..3f,
                                        valueDisplay = "${"%.1f".format(currentConfig.borderWidth)}dp",
                                        onValueChange = { v -> onUpdateStyle(selectedToken) { it.copy(borderWidth = v) } },
                                        onReset = { onResetParameter(selectedToken, "borderWidth") }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Design Lab",
                            onBackClick = onBack
                        )
                    }
                )
            )
        }
    }
}

// ── Token-contextual preview mocks ─────────────────────────────────────────

@Composable
private fun StandardMock(
    hazeState: HazeState,
    hazeStyle: dev.chrisbanes.haze.HazeStyle,
    variant: String,
    fallback: Color,
    tint: Color
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            hazeState = hazeState,
            hazeStyle = hazeStyle,
            glassVariant = variant,
            fallbackColor = fallback
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.size(24.dp, 4.dp).background(tint.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                Text("STANDARD", style = AppTextStyles.caption.copy(color = tint.copy(alpha = 0.6f), letterSpacing = 1.sp))
                Box(modifier = Modifier.size(24.dp, 4.dp).background(tint.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
private fun DrawerMock(
    hazeState: HazeState,
    hazeStyle: dev.chrisbanes.haze.HazeStyle,
    variant: String,
    fallback: Color,
    tint: Color
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
        GlassSurface(
            modifier = Modifier
                .width(160.dp)
                .fillMaxHeight()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
            hazeState = hazeState,
            hazeStyle = hazeStyle,
            glassVariant = variant,
            fallbackColor = fallback
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(4) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(16.dp).background(tint.copy(alpha = 0.25f), CircleShape))
                        Box(modifier = Modifier.height(4.dp).width(80.dp).background(tint.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
    }
}

@Composable
private fun PillMock(
    hazeState: HazeState,
    hazeStyle: dev.chrisbanes.haze.HazeStyle,
    variant: String,
    fallback: Color,
    tint: Color
) {
    Box(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp), contentAlignment = Alignment.BottomCenter) {
        GlassSurface(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth(0.85f)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            hazeState = hazeState,
            hazeStyle = hazeStyle,
            glassVariant = variant,
            fallbackColor = fallback
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(4) {
                    Box(modifier = Modifier.size(20.dp).background(tint.copy(alpha = 0.25f), CircleShape))
                }
            }
        }
    }
}

@Composable
private fun CardMock(
    hazeState: HazeState,
    hazeStyle: dev.chrisbanes.haze.HazeStyle,
    variant: String,
    fallback: Color,
    tint: Color
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .fillMaxHeight(0.65f),
            shape = RoundedCornerShape(20.dp),
            hazeState = hazeState,
            hazeStyle = hazeStyle,
            glassVariant = variant,
            fallbackColor = fallback
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.height(4.dp).width(80.dp).background(tint.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.height(3.dp).width(48.dp).background(tint.copy(alpha = 0.12f), RoundedCornerShape(2.dp)))
                }
                Box(modifier = Modifier.height(6.dp).width(120.dp).background(tint.copy(alpha = 0.3f), RoundedCornerShape(3.dp)))
            }
        }
    }
}

@Composable
private fun SheetMock(
    hazeState: HazeState,
    hazeStyle: dev.chrisbanes.haze.HazeStyle,
    variant: String,
    fallback: Color,
    tint: Color
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            hazeState = hazeState,
            hazeStyle = hazeStyle,
            glassVariant = variant,
            fallbackColor = fallback
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp).background(tint.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
                repeat(3) {
                    Box(modifier = Modifier.height(3.dp).fillMaxWidth(0.7f).background(tint.copy(alpha = 0.15f), RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}

@Composable
private fun PopupMock(
    hazeState: HazeState,
    hazeStyle: dev.chrisbanes.haze.HazeStyle,
    variant: String,
    fallback: Color,
    tint: Color
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassSurface(
            modifier = Modifier.width(160.dp),
            shape = RoundedCornerShape(16.dp),
            hazeState = hazeState,
            hazeStyle = hazeStyle,
            glassVariant = variant,
            fallbackColor = fallback
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(3) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(14.dp).background(tint.copy(alpha = 0.2f), CircleShape))
                        Box(modifier = Modifier.height(3.dp).width(90.dp).background(tint.copy(alpha = 0.15f), RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
    }
}

// ── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun DesignLabPreview() {
    OssWalletTheme {
        DesignLabContent(
            glassSettings = GlassSettings(),
            glassmorphismEnabled = true,
            contourLinesEnabled = false,
            contourBgVersion = 1,
            randomizeContourEachLoad = false,
            onBack = {},
            onGlassmorphismToggle = {},
            onContourLinesToggle = {},
            onContourBgVersionChange = {},
            onRandomizeContourToggle = {},
            onUpdateStyle = { _, _ -> },
            onResetStyle = {},
            onResetParameter = { _, _ -> }
        )
    }
}
