package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.labelExtraSmall
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import java.math.BigDecimal
import kotlin.math.roundToInt

@Composable
fun AeonChartInteractionPopup(
    name: String,
    value: BigDecimal,
    color: Color,
    percentageOfTotal: Float,
    hasSubcategories: Boolean,
    subcategoryCount: Int,
    touchPoint: Offset,
    chartWidthPx: Float,
    chartHeightPx: Float,
    density: Float,
    currency: String = "USD",
    precision: Int = 2,
    onDrillInClick: () -> Unit = {},
    onNavigateToRecords: () -> Unit = {},
) {
    // Boundary-Aware Tooltip Positioning
    val tooltipWidth = if (hasSubcategories) 240.dp else 180.dp
    // Height is dynamic based on content but we need an estimate for positioning
    val tooltipHeightPxEstimate = 140.dp.value * density
    
    val tooltipWidthPx = tooltipWidth.value * density

    val xPos = (touchPoint.x - (tooltipWidthPx / 2))
        .coerceIn(0f, chartWidthPx - tooltipWidthPx)
    val yPos = (touchPoint.y - tooltipHeightPxEstimate - 10f)
        .coerceIn(0f, chartHeightPx - tooltipHeightPxEstimate)

    Surface(
        modifier = Modifier
            .offset { IntOffset(xPos.roundToInt(), yPos.roundToInt()) }
            .width(tooltipWidth)
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Color + Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Content: Value + Global %
            Column {
                Text(
                    text = formatCurrencyRight(value, currency, precision),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "GLOBAL SHARE",
                        style = MaterialTheme.typography.labelExtraSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                    Text(
                        text = "${"%.1f".format(percentageOfTotal * 100)}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons - Compact Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasSubcategories && subcategoryCount > 1) {
                    Button(
                        onClick = onDrillInClick,
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = color.copy(alpha = 0.15f),
                            contentColor = color
                        )
                    ) {
                        Icon(Icons.Default.ZoomIn, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "DIVE IN ($subcategoryCount)", 
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
                
                Button(
                    onClick = onNavigateToRecords,
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "RECORDS", 
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AeonChartInteractionPopupPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.size(300.dp).padding(20.dp), contentAlignment = Alignment.Center) {
            AeonChartInteractionPopup(
                name = "Food & Dining",
                value = BigDecimal("450.75"),
                color = Color(0xFFFF5722),
                percentageOfTotal = 0.225f,
                hasSubcategories = true,
                subcategoryCount = 5,
                touchPoint = Offset(150f, 150f),
                chartWidthPx = 1000f,
                chartHeightPx = 1000f,
                density = 2.5f
            )
        }
    }
}
