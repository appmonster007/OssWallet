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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.BlurOff
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
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
import com.osscabinet.ossWallet.ui.components.AeonSettingsItem
import com.osscabinet.ossWallet.ui.components.AeonSettingsSection
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.ContourBackground
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedGlassSettings(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToGlassLab: () -> Unit
) {
    val glassmorphismEnabled by viewModel.glassmorphismEnabled.collectAsStateWithLifecycle()
    val contourBgVersion by viewModel.contourBgVersion.collectAsStateWithLifecycle()
    val contourLinesEnabled by viewModel.contourLinesEnabled.collectAsStateWithLifecycle()
    val randomizeContourEachLoad by viewModel.randomizeContourEachLoad.collectAsStateWithLifecycle()

    var showContourDialog by remember { mutableStateOf(false) }

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

    AeonHazeProvider { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.screenBackground)
                ) {
                    ContourBackground()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .hazeSource(hazeLayers.states[HazeLayer.Overlay])
                            .statusBarsPadding()
                            .padding(
                                top = AeonSize.D64.value + AeonTheme.dimensions.screenPadding,
                                bottom = innerPadding.calculateBottomPadding()
                            )
                            .padding(horizontal = AeonTheme.dimensions.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacing2xl)
                    ) {
                        AeonSettingsSection("GLASSMORPHISM") {
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
                                    title = "Glass Lab",
                                    subtitle = "Fine-tune blur and opacity for each layer",
                                    icon = Icons.Rounded.Tune,
                                    onClick = onNavigateToGlassLab
                                )
                            }
                        }

                        AeonSettingsSection("CONTOUR BACKGROUND") {
                            EntrySwitchRow(
                                label = "Contour Lines",
                                subtitle = if (contourLinesEnabled) "Draw topographic lines in background" else "Hide topographic lines",
                                icon = Icons.Rounded.AddCircleOutline,
                                checked = contourLinesEnabled,
                                onCheckedChange = { viewModel.setContourLinesEnabled(it) },
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
                                    onCheckedChange = { viewModel.setRandomizeContourEachLoad(it) },
                                    isFlush = false
                                )
                            }
                        }
                    }
                }
            }

            AeonTopAppBar(
                title = "Glass & Background",
                onBackClick = onBack
            )
        }
    }
}
