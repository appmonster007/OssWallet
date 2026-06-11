package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun AeonSettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
        Text(
            text = title,
            style = AppTextStyles.sectionOverline,
            color = AppColors.primary,
            modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding)
        )
        Surface(
            shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
            color = AppColors.surfaceContainerLow,
            border = BorderStroke(1.dp, AppColors.outlineVariant)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun AeonSettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    color: Color = AppColors.contentPrimary,
    showChevron: Boolean = onClick != null && trailingContent == null
) {
    val dims = AeonTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(dims.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.spacingMd)
        ) {
            Surface(
                modifier = Modifier.size(dims.iconSizeLg),
                shape = CircleShape,
                color = color.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = AppTextStyles.managementItemTitle, color = color)
                Text(subtitle, style = AppTextStyles.caption, color = AppColors.contentSecondary)
            }
        }

        if (trailingContent != null || showChevron) {
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = dims.spacingMd)
                    .width(0.5.dp),
                color = AppColors.divider.copy(alpha = 0.3f)
            )

            Box(
                modifier = Modifier
                    .padding(start = dims.spacingMd, end = dims.cardPadding)
                    .padding(vertical = dims.cardPadding),
                contentAlignment = Alignment.Center
            ) {
                if (trailingContent != null) {
                    trailingContent()
                } else if (showChevron) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = AppColors.contentSecondary,
                        modifier = Modifier.size(dims.iconSizeMd)
                    )
                }
            }
        }
    }
}
