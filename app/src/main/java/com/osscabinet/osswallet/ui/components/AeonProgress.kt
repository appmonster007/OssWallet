package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAuto
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

@Composable
fun AeonBudgetProgress(
    label: String,
    current: BigDecimal,
    total: BigDecimal,
    color: Color,
    baseCurrency: String,
    showLabels: Boolean = true
) {
    val progress = if (total.signum() > 0) (current.toDouble() / total.toDouble()).toFloat()
        .coerceIn(0f, 1f) else 0f

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showLabels) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    label,
                    style = AppTextStyles.managementItemTitle,
                    color = AppColors.contentPrimary
                )
                Text(
                    "${
                        formatAuto(
                            current,
                            baseCurrency
                        )
                    } / ${formatAuto(total, baseCurrency)}",
                    style = AppTextStyles.transactionSwipeAction.copy(
                        color = AppColors.border
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.insetSurface.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.7f),
                                color
                            )
                        )
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonBudgetProgressPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AeonBudgetProgress(
                    label = "Groceries",
                    current = BigDecimal("350.00"),
                    total = BigDecimal("500.00"),
                    color = Color.Blue,
                    baseCurrency = "USD"
                )
                AeonBudgetProgress(
                    label = "Dining Out",
                    current = BigDecimal("120.00"),
                    total = BigDecimal("100.00"),
                    color = Color.Red,
                    baseCurrency = "USD"
                )
            }
        }
    }
}
