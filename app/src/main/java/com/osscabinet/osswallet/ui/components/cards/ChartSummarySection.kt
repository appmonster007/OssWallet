package com.osscabinet.ossWallet.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective
import com.osscabinet.ossWallet.domain.model.settings.ChartMetric
import com.osscabinet.ossWallet.ui.components.AeonDropdownSelector
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyPartStyles
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun ChartSummarySection(
    total: BigDecimal,
    previousTotal: BigDecimal?,
    currency: String,
    showComparison: Boolean,
    isAllTime: Boolean,
    positiveDeltaIsGood: Boolean,
    perspective: AccountingPerspective,
    selectedMetric: ChartMetric,
    onMetricChange: (ChartMetric) -> Unit,
    modifier: Modifier = Modifier,
    splitTransfers: Boolean = false
) {
    val labels = AccountingPerspective.labelsFor(perspective, splitTransfers)
    val options = ChartMetric.entries.filter { it in labels }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOTAL",
                style = AppTextStyles.overlineLabel.copy(
                    letterSpacing = 1.25.sp
                ),
                color = AppColors.contentSecondary.copy(alpha = 0.6f)
            )

            AeonDropdownSelector(
                value = selectedMetric,
                options = options,
                labelProvider = { labels[it] ?: it.name },
                onValueChange = onMetricChange
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val contentColor = AppColors.contentPrimary
            Text(
                text = CurrencyReprUi.formatCurrencyAnnotated(
                    amount = total,
                    currencyCode = currency,
                    spec = CurrencyDisplaySpec(useSemanticColor = false),
                    partStyles = CurrencyPartStyles(
                        symbol = SpanStyle(
                            fontWeight = FontWeight.Light,
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 16.sp
                        ),
                        neutral = contentColor
                    )
                ),
                style = AppTextStyles.heroAmount.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = contentColor
            )

            if (showComparison && !isAllTime && previousTotal != null && previousTotal.signum() != 0) {
                val delta = total.abs().subtract(previousTotal.abs())
                    .divide(previousTotal.abs(), 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal("100"))

                val isIncrease = delta.signum() > 0
                val isDecrease = delta.signum() < 0

                val color = when {
                    isIncrease -> if (positiveDeltaIsGood) AppColors.positive else AppColors.negative
                    isDecrease -> if (positiveDeltaIsGood) AppColors.negative else AppColors.positive
                    else -> AppColors.contentSecondary
                }

                val icon = when {
                    isIncrease -> Icons.Default.ArrowUpward
                    isDecrease -> Icons.Default.ArrowDownward
                    else -> null
                }

                Surface(
                    color = color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = color
                            )
                        }
                        Text(
                            text = "${delta.abs().setScale(1, RoundingMode.HALF_UP)}%",
                            style = AppTextStyles.transactionBadge.copy(
                                fontSize = 11.sp
                            ),
                            color = color
                        )
                    }
                }
            }
        }
    }
}
