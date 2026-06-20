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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

/**
 * H editorial settings section — solid hairline panel with mono overline,
 * gradient hairline rule, and optional 2dp left accent rail per section.
 */
@Composable
fun AeonSettingsSection(
    title: String,
    accentBar: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.cardSurfaceProminent)
            .aeonHairlineBorder(shape)
            // 2dp left accent rail — clipped by shape so corners round naturally
            .then(
                if (accentBar != Color.Unspecified)
                    Modifier.drawBehind {
                        drawRect(color = accentBar, size = size.copy(width = 2.dp.toPx()))
                    }
                else Modifier
            )
    ) {
        Column {
            // Overline inside the panel with right-fading gradient hairline rule
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
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(AppColors.outlineVariant, Color.Transparent)
                            )
                        )
                )
            }
            content()
        }
    }
}

/**
 * H editorial settings row — circular icon with optional section-scoped tint,
 * Inter title, JetBrains Mono subtitle, champagne chevron.
 */
@Composable
fun AeonSettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    color: Color = AppColors.contentPrimary,
    iconTint: Color = AppColors.contentSecondary,
    showChevron: Boolean = onClick != null && trailingContent == null,
) {
    val dims = AeonTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = dims.cardPadding, vertical = dims.spacingMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.spacingMd)
    ) {
        Box(
            modifier = Modifier
                .size(dims.iconSizeLg)
                .clip(CircleShape)
                .background(AppColors.screenBackground)
                .border(0.5.dp, AppColors.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
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
