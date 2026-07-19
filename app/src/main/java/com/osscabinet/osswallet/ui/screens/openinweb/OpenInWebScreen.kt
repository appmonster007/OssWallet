package com.osscabinet.ossWallet.ui.screens.openinweb

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.data.web.QRCodeGenerator
import com.osscabinet.ossWallet.domain.model.web.WebServerState
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenInWebScreen(
    viewModel: OpenInWebViewModel,
    actions: OpenInWebActions
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEnabled by viewModel.isEnabled.collectAsStateWithLifecycle()
    val stayAlive by viewModel.stayAlive.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0)
            ) { innerPadding ->
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                            .statusBarsPadding()
                            .padding(top = AeonSize.D64.value)
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!uiState.isWifiConnected) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = AppColors.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.WifiOff,
                                        contentDescription = null,
                                        tint = AppColors.onErrorContainer
                                    )
                                    Text(
                                        "Not connected to WiFi. Connect to a WiFi network to enable web access.",
                                        style = AppTextStyles.transactionSubtitle,
                                        color = AppColors.onErrorContainer
                                    )
                                }
                            }
                        }

                        Card {
                            Column {
                                EntrySwitchRow(
                                    modifier = Modifier.padding(16.dp),
                                    label = "Open in Web",
                                    subtitle = when (val s = uiState.serverState) {
                                        is WebServerState.Stopped -> "Disabled"
                                        is WebServerState.Starting -> "Starting..."
                                        is WebServerState.Running -> "Running on port ${s.port}"
                                        is WebServerState.Error -> "Error: ${s.message}"
                                    },
                                    checked = isEnabled,
                                    onCheckedChange = { viewModel.toggleServer(it) },
                                    isFlush = true
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = AppColors.divider
                                )
                                EntrySwitchRow(
                                    modifier = Modifier.padding(16.dp),
                                    label = "Stay Alive in Background",
                                    subtitle = "Keep server running when app is closed",
                                    checked = stayAlive,
                                    onCheckedChange = { viewModel.toggleStayAlive(it) },
                                    isFlush = true
                                )
                            }
                        }

                        val runningState = uiState.serverState as? WebServerState.Running
                        if (runningState != null) {
                            Card {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Access Code",
                                        style = AppTextStyles.formSectionLabel,
                                        color = AppColors.contentSecondary
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = runningState.accessCode,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 32.sp,
                                        letterSpacing = 6.sp,
                                        style = AppTextStyles.heroAmountDisplay,
                                        color = AppColors.primary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Type this code in your browser after opening the URL below.",
                                        style = AppTextStyles.caption,
                                        color = AppColors.contentSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // QR generation logic - Best practices:
                            // 1. Handled in UI layer to keep Domain layer pure (no Bitmap dependency)
                            // 2. Reacts to theme changes using remember with color keys
                            val color = AppColors.onSurface
                            val backgroundColor = AppColors.surfaceContainer

                            val qrBitmap = remember(runningState.accessUrl, color, backgroundColor) {
                                runCatching {
                                    QRCodeGenerator.generate(
                                        text = runningState.accessUrl,
                                        color = color.toArgb(),
                                        backgroundColor = backgroundColor.toArgb()
                                    )
                                }.getOrNull()
                            }

                            qrBitmap?.let { bitmap ->
                                Card {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Scan QR Code",
                                            style = AppTextStyles.formSectionLabel,
                                            color = AppColors.contentSecondary
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "QR code for ${runningState.accessUrl}",
                                            modifier = Modifier
                                                .size(240.dp)
                                                .border(
                                                    1.dp,
                                                    AppColors.divider,
                                                    RoundedCornerShape(8.dp)
                                                )
                                        )
                                    }
                                }
                            }

                            Card {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        "Access URL",
                                        style = AppTextStyles.formSectionLabel,
                                        color = AppColors.contentSecondary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = runningState.accessUrl,
                                            style = AppTextStyles.transactionTitle,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f),
                                            color = AppColors.primary
                                        )
                                        IconButton(
                                            onClick = {
                                                val clipboard =
                                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(
                                                    ClipData.newPlainText(
                                                        "OssWallet URL",
                                                        runningState.accessUrl
                                                    )
                                                )
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Copy URL"
                                            )
                                        }
                                    }
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = AppColors.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "How to connect",
                                        style = AppTextStyles.formSectionLabel,
                                        color = AppColors.onSecondaryContainer
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "1. Make sure your laptop/desktop is on the same WiFi network as this phone.\n" +
                                                "2. Open the URL above in a browser, or scan the QR code.\n" +
                                                "3. Enter the access code shown above.\n" +
                                                "4. The web view is read-only. The server stops when you turn off the toggle.",
                                        style = AppTextStyles.caption,
                                        color = AppColors.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        if (uiState.serverState is WebServerState.Starting) {
                            Card {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        val errorState = uiState.serverState as? WebServerState.Error
                        if (errorState != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = AppColors.errorContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Failed to start server",
                                        style = AppTextStyles.cardSectionTitle,
                                        color = AppColors.onErrorContainer
                                    )
                                    Text(
                                        errorState.message,
                                        style = AppTextStyles.caption,
                                        color = AppColors.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Open in Web",
                            onBackClick = actions.onNavigateBack
                        )
                    }
                )
            )
        }
    }
}

data class OpenInWebActions(
    val onNavigateBack: () -> Unit
)
