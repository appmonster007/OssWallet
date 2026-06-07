package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.components.AeonGlassHost
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Standard container for metadata/details in entry screens.
 * Following the "Architect Slate" design specification.
 */
@Composable
fun EntrySection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassEnabled = com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled.current

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        hazeStyle = GlassTokens.cardStyle(),
        glassVariant = com.osscabinet.ossWallet.ui.theme.GlassVariant.Card,
        fallbackColor = if (glassEnabled) AppColors.surfaceContainerLow.copy(alpha = 0.6f) else AppColors.surfaceContainerLow,
        borderColor = if (glassEnabled) AppColors.outlineVariant.copy(alpha = 0.2f) else AppColors.outlineVariant,
    ) {
        AeonGlassHost(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AeonTheme.dimensions.cardPadding),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
            ) {
                // Section Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AppColors.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                                tint = AppColors.primary
                            )
                        }
                    }
                    Text(
                        text = title.uppercase(),
                        style = AppTextStyles.sectionOverline,
                        color = AppColors.contentSecondary
                    )
                }

                content()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EntrySectionPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            EntrySection(
                title = "Information",
                icon = Icons.Rounded.Info
            ) {
                Text(
                    "This is the content of the entry section.",
                    style = AppTextStyles.listItemTitle
                )
                Text("It can contain multiple composables.", style = AppTextStyles.caption)
            }
        }
    }
}
