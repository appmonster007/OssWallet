package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun AeonBudgetProgress(
    label: String,
    current: BigDecimal,
    total: BigDecimal,
    color: Color,
    baseCurrency: String,
    showLabels: Boolean = true
) {
    val progress = if (total.signum() > 0) (current.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
    
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
                    "${formatCurrencyRight(current, baseCurrency, shorten = true)} / ${formatCurrencyRight(total, baseCurrency, shorten = true)}",
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
