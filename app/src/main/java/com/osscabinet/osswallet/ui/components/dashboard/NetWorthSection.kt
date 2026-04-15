package com.osscabinet.ossWallet.ui.components.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.components.TimelineFilterBar
import com.osscabinet.ossWallet.ui.components.TimelineRange
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.viewmodel.NetWorthUiState
import com.osscabinet.ossWallet.ui.viewmodel.NetWorthViewModel
import kotlinx.serialization.Serializable

@Serializable
data class NetWorthSettings(
    val fontSize: String = "Medium", // "Small", "Medium", "Large"
    val timelineRange: TimelineRange = TimelineRange.AllTime
)

@Composable
fun NetWorthSection(
    viewModel: NetWorthViewModel,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is NetWorthUiState.Loading -> {
            // Shimmer or placeholder
            Box(modifier = modifier.height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is NetWorthUiState.Success -> {
            NetWorthContent(
                netWorth = state.netWorth,
                baseCurrency = state.baseCurrency,
                precision = state.precision,
                fontSize = state.fontSize,
                timelineRange = state.timelineRange,
                onFontSizeChange = { viewModel.updateNetWorthFontSize(it) },
                onTimelineRangeChange = { viewModel.updateTimelineRange(it) },
                modifier = modifier,
                headerModifier = headerModifier
            )
        }
        is NetWorthUiState.Error -> {
            Text("Error: ${state.message}")
        }
    }
}

@Composable
private fun NetWorthContent(
    netWorth: java.math.BigDecimal,
    baseCurrency: String,
    precision: Int,
    fontSize: String,
    timelineRange: TimelineRange,
    onFontSizeChange: (String) -> Unit,
    onTimelineRangeChange: (TimelineRange) -> Unit,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }

    val (textStyle, labelStyle) = when (fontSize) {
        "Small" -> MaterialTheme.typography.displaySmall to MaterialTheme.typography.labelSmall
        "Large" -> MaterialTheme.typography.displayLarge to MaterialTheme.typography.labelSmall
        else -> MaterialTheme.typography.displayMedium to MaterialTheme.typography.labelSmall
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AeonFlowSectionHeader(
                title = "NET WORTH",
                modifier = headerModifier,
                trailing = {
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatCurrencyRight(netWorth, baseCurrency, precision),
                    style = textStyle.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "TOTAL BALANCE ACROSS ALL ACCOUNTS",
                    style = labelStyle.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 9.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
    }

    if (showSettings) {
        NetWorthSettingsDialog(
            fontSize = fontSize,
            timelineRange = timelineRange,
            onFontSizeChange = onFontSizeChange,
            onTimelineRangeChange = onTimelineRangeChange,
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun NetWorthSettingsDialog(
    fontSize: String,
    timelineRange: TimelineRange,
    onFontSizeChange: (String) -> Unit,
    onTimelineRangeChange: (TimelineRange) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        title = {
            Text(
                "Net Worth Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Timeline Range Selection
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "TIMELINE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        TimelineFilterBar(
                            currentRange = timelineRange,
                            onRangeSelected = onTimelineRangeChange,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Font Size Selection
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "FONT SIZE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf("Small", "Medium", "Large").forEachIndexed { index, size ->
                                SegmentedButton(
                                    selected = fontSize == size,
                                    onClick = { onFontSizeChange(size) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = 3
                                    ),
                                    label = { 
                                        Text(
                                            when(size) {
                                                "Small" -> "S"
                                                "Medium" -> "M"
                                                else -> "L"
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
