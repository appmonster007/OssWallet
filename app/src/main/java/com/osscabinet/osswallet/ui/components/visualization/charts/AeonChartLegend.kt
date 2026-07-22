package com.osscabinet.ossWallet.ui.components.visualization.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AeonChartLegend(
    items: List<AeonLegendItemData>,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        items.forEach { item ->
            AeonLegendItem(item)
        }
    }
}

@Composable
fun AeonLegendItem(
    item: AeonLegendItemData,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clickable(onClick = item.onClick)
            .alpha(if (item.isEnabled) 1f else 0.4f)
    ) {
        if (item.isDashed) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(width = 16.dp, height = 3.dp)) {
                drawLine(
                    color = item.color.copy(alpha = item.alpha),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f)),
                    cap = StrokeCap.Round
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 3.dp)
                    .background(
                        color = if (item.isEnabled) item.color.copy(alpha = item.alpha) else item.color.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
        Text(
            text = item.label,
            style = AppTextStyles.caption.copy(fontSize = 10.sp),
            color = if (item.isEnabled) AppColors.contentPrimary else AppColors.contentSecondary
        )
    }
}
