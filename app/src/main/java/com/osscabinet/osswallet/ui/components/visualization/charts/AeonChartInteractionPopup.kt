package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ChartColorOrange
import com.osscabinet.ossWallet.ui.theme.ColorOnDark
import com.osscabinet.ossWallet.ui.theme.ColorOnLight
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
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
    precision: Int = com.osscabinet.ossWallet.domain.service.currency.CurrencyService.precision,
    onDrillInClick: () -> Unit = {},
    onNavigateToRecords: () -> Unit = {},
) {
    val dims = AeonTheme.dimensions

    // Boundary-Aware Tooltip Positioning
    val tooltipWidth = if (hasSubcategories) 240.dp else 180.dp
    // Height is dynamic based on content but we need an estimate for positioning
    val tooltipHeightPxEstimate = 140.dp.value * density

    val tooltipWidthPx = tooltipWidth.value * density

    val xPos = (touchPoint.x - (tooltipWidthPx / 2))
        .coerceIn(0f, (chartWidthPx - tooltipWidthPx).coerceAtLeast(0f))
    val yPos = (touchPoint.y - tooltipHeightPxEstimate - 10f)
        .coerceIn(0f, (chartHeightPx - tooltipHeightPxEstimate).coerceAtLeast(0f))

    Surface(
        modifier = Modifier
            .offset { IntOffset(xPos.roundToInt(), yPos.roundToInt()) }
            .width(tooltipWidth)
            .clip(RoundedCornerShape(dims.containerCornerRadius)),
        color = AppColors.cardSurfaceProminent,
        tonalElevation = dims.spacingSm,
        shadowElevation = dims.spacingMd,
        border = BorderStroke((dims.spacingXs / 8), AppColors.divider.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(dims.spacingMd),
            verticalArrangement = Arrangement.spacedBy(dims.spacingSm)
        ) {
            // Header: Color + Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dims.spacingSm)
            ) {
                Box(
                    modifier = Modifier
                        .size(dims.spacingSm)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = name,
                    style = AppTextStyles.drawerItemLabel.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = AppColors.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Content: Value + Global %
            Column {
                Text(
                    text = formatCurrency(
                        amount = value,
                        currencyCode = currency,
                        spec = CurrencyDisplaySpec(precision = precision)
                    ),
                    style = AppTextStyles.screenSubTitle.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    ),
                    color = AppColors.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dims.spacingXs)
                ) {
                    Text(
                        text = "GLOBAL SHARE",
                        style = AppTextStyles.metaLabel.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.contentSecondary.copy(alpha = 0.4f)
                        )
                    )
                    Text(
                        text = "${"%.1f".format(percentageOfTotal * 100)}%",
                        style = AppTextStyles.formSectionLabel,
                        color = AppColors.contentSecondary
                    )
                }
            }

            // Action Buttons - Compact Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dims.spacingSm)
            ) {
                if (hasSubcategories && subcategoryCount > 1) {
                    Button(
                        onClick = onDrillInClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(dims.spacing2xl),
                        contentPadding = PaddingValues(horizontal = dims.spacingSm),
                        shape = RoundedCornerShape(dims.spacingSm),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = color,
                            contentColor = if (ColorUtils.calculateLuminance(color.toArgb()) > 0.5) {
                                ColorOnLight
                            } else {
                                ColorOnDark
                            }
                        )
                    ) {
                        Icon(
                            Icons.Default.ZoomIn,
                            null,
                            modifier = Modifier.size(dims.iconSizeXs + 2.dp)
                        )
                        Spacer(modifier = Modifier.width(dims.spacingXs))
                        Text(
                            "DIVE IN ($subcategoryCount)",
                            style = AppTextStyles.transactionSwipeAction.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        )
                    }
                }

                Button(
                    onClick = onNavigateToRecords,
                    modifier = Modifier
                        .weight(1f)
                        .height(dims.spacing2xl),
                    contentPadding = PaddingValues(horizontal = dims.spacingSm),
                    shape = RoundedCornerShape(dims.spacingSm),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.primary.copy(alpha = 0.1f),
                        contentColor = AppColors.primary
                    )
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        modifier = Modifier.size(dims.iconSizeXs + 2.dp)
                    )
                    Spacer(modifier = Modifier.width(dims.spacingXs))
                    Text(
                        "RECORDS",
                        style = AppTextStyles.transactionSwipeAction.copy(
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
        Box(modifier = Modifier
            .size(300.dp)
            .padding(20.dp), contentAlignment = Alignment.Center) {
            AeonChartInteractionPopup(
                name = "Food & Dining",
                value = BigDecimal("450.75"),
                color = ChartColorOrange,
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
