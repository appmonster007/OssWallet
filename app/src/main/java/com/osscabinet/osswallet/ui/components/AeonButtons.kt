package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Composable
fun AeonDashedButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    val contentColor = if (isError) {
        if (enabled) AppColors.error else AppColors.error.copy(alpha = 0.3f)
    } else {
        if (enabled) AppColors.contentPrimary else AppColors.contentPrimary.copy(alpha = 0.3f)
    }

    val borderColor = if (isError) {
        if (enabled) AppColors.error.copy(alpha = 0.5f) else AppColors.error.copy(alpha = 0.1f)
    } else {
        if (enabled) AppColors.border.copy(alpha = 0.4f) else AppColors.border.copy(alpha = 0.1f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .drawBehind {
                val strokeWidth = 1.5.dp.toPx()
                val dashLength = 12f
                val dashGap = 8f
                val stroke = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, dashGap), 0f)
                )
                drawRoundRect(
                    color = borderColor,
                    style = stroke,
                    cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text,
                style = AppTextStyles.listItemTitle.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = contentColor
            )
        }
    }
}

@Composable
fun AeonFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Add,
    contentDescription: String? = null,
    containerColor: Color = AppColors.primary,
    contentColor: Color = AppColors.onPrimary
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun AeonExtendedFloatingActionButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = AppColors.primary,
    contentColor: Color = AppColors.onPrimary
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
        text = {
            Text(
                text,
                style = AppTextStyles.drawerItemLabel.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )
        }
    )
}

@Preview(showBackground = true, name = "Aeon Buttons Preview")
@Composable
fun AeonButtonsPreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Dashed Buttons", style = AppTextStyles.cardSectionTitle)
            AeonDashedButton(
                onClick = {},
                text = "Add Transaction",
                icon = Icons.Rounded.Add
            )
            AeonDashedButton(
                onClick = {},
                text = "Error Action",
                icon = Icons.Rounded.Add,
                isError = true
            )
            AeonDashedButton(
                onClick = {},
                text = "Disabled Button",
                icon = Icons.Rounded.Add,
                enabled = false
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Floating Action Buttons", style = AppTextStyles.cardSectionTitle)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AeonFloatingActionButton(onClick = {})
                AeonExtendedFloatingActionButton(
                    onClick = {},
                    text = "New Transaction",
                    icon = Icons.Rounded.Add
                )
            }
        }
    }
}
