package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

/**
 * H editorial settings section — solid hairline panel with mono overline
 * and extending hairline rule inside the panel top.
 */
@Composable
fun AeonSettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.cardSurfaceProminent)
            .aeonHairlineBorder(shape)
    ) {
        Column {
            // Overline inside the panel with right-extending hairline rule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AeonTheme.dimensions.cardPadding,
                        vertical = AeonTheme.dimensions.spacingMd
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
            ) {
                Text(
                    text = title,
                    style = AppTextStyles.sectionOverline,
                    color = AppColors.accent
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(0.5.dp)
                        .background(AppColors.outlineVariant)
                )
            }
            content()
        }
    }
}

/**
 * H editorial settings row — inset square icon (monochrome), Inter title,
 * JetBrains Mono subtitle, champagne chevron.
 */
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
    val iconShape = CircleShape
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = dims.cardPadding, vertical = dims.spacingMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.spacingMd)
    ) {
        // Inset square icon — monochrome, no saturated fill
        Box(
            modifier = Modifier
                .size(dims.iconSizeLg)
                .clip(iconShape)
                .background(AppColors.screenBackground)
                .border(0.5.dp, AppColors.outlineVariant, iconShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppColors.contentSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = AppTextStyles.managementItemTitle, color = color)
            Text(subtitle, style = AppTextStyles.metaLabel, color = AppColors.contentSecondary)
        }
        if (trailingContent != null) {
            trailingContent()
        } else if (showChevron) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = if (color == AppColors.contentPrimary) AppColors.accent else color,
                modifier = Modifier.size(dims.iconSizeMd)
            )
        }
    }
}
