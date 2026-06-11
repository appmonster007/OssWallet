package com.osscabinet.ossWallet.ui.components.management

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Unified list item for all Group-2 management and action lists in the Aeon design system.
 * Uses GlassSurface(Item variant), AeonTheme.dimensions throughout, and animates
 * selection / warning / drag states. Replaces the old plain-Surface ManagementListItem.
 *
 * Slots:
 *  - [leadingContent]   — drag handle or checkbox, placed before the icon
 *  - [iconOverlay]      — badge drawn on top of the icon circle (e.g. transaction-type badge)
 *  - [trailingContent]  — amount, switch, icon-buttons, etc., placed after the title column
 *  - [menuItems]        — MoreVert dropdown; rendered after trailingContent
 *  - [additionalContent]— extends below the main row (e.g. a progress bar)
 */
@Composable
fun AeonListItem(
    title: String,
    icon: ImageVector,
    iconTint: Color = AppColors.primary,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    isWarning: Boolean = false,
    isDragging: Boolean = false,
    iconContainerColor: Color? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    iconOverlay: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable ColumnScope.() -> Unit)? = null,
    menuItems: (@Composable ColumnScope.(onDismiss: () -> Unit) -> Unit)? = null,
    additionalContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val dims = AeonTheme.dimensions
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var showMenu by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        label = "scale"
    )

    val fallbackColor by animateColorAsState(
        targetValue = when {
            isDragging -> AppColors.selectionContainer.copy(alpha = 0.9f)
            isWarning -> AppColors.errorContainer.copy(alpha = 0.15f)
            isSelected -> iconTint.copy(alpha = 0.15f)
            else -> AppColors.surfaceContainerLow
        },
        label = "bgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isDragging -> AppColors.primary
            isWarning -> AppColors.error.copy(alpha = 0.4f)
            isSelected -> iconTint
            else -> AppColors.outlineVariant.copy(alpha = 0.6f)
        },
        label = "borderColor"
    )

    val borderWidth by animateDpAsState(
        targetValue = when {
            isDragging || isSelected -> 1.5.dp
            isWarning -> 0.5.dp
            else -> 1.dp
        },
        label = "borderWidth"
    )

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    } else Modifier

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(clickModifier),
        shape = RoundedCornerShape(dims.componentCornerRadius),
        hazeStyle = GlassTokens.itemStyle(),
        glassVariant = GlassVariant.Item,
        fallbackColor = fallbackColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
        shadowElevation = if (isDragging) 4.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = dims.cardPadding,
                vertical = dims.spacingMd
            ),
            verticalArrangement = Arrangement.spacedBy(dims.spacingSm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dims.spacingMd)
            ) {
                if (leadingContent != null) {
                    leadingContent()
                }

                // Icon with optional overlay badge
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        shape = CircleShape,
                        color = iconContainerColor ?: iconTint.copy(alpha = 0.18f),
                        modifier = Modifier.size(dims.iconSizeLg)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(dims.iconSizeMd),
                                tint = iconTint
                            )
                        }
                    }
                    iconOverlay?.invoke(this)
                }

                Spacer(modifier = Modifier.width(dims.spacingSm))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        title,
                        style = AppTextStyles.listItemTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = AppTextStyles.listItemSubtitle,
                            color = AppColors.contentSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (trailingContent != null) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        trailingContent()
                    }
                }

                if (menuItems != null) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(dims.iconSizeLg)
                        ) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = null,
                                modifier = Modifier.size(dims.iconSizeSm),
                                tint = AppColors.contentSecondary.copy(alpha = 0.6f)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            menuItems { showMenu = false }
                        }
                    }
                }
            }

            additionalContent?.invoke(this)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AeonListItemPreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AeonListItem(
                title = "Checking Account",
                subtitle = "Bank of America · **** 1234",
                icon = Icons.Rounded.AccountBalance,
                iconTint = AppColors.primary,
                onClick = {},
                trailingContent = {
                    Text("\$2,500.00", style = AppTextStyles.listItemTitle, color = AppColors.positive)
                }
            )
            AeonListItem(
                title = "Savings Account",
                subtitle = "Selected",
                icon = Icons.Rounded.AccountBalance,
                iconTint = AppColors.primary,
                onClick = {},
                isSelected = true,
                trailingContent = {
                    Text("\$12,000.00", style = AppTextStyles.listItemTitle, color = AppColors.positive)
                }
            )
            AeonListItem(
                title = "Overdue Payment",
                subtitle = "Warning state",
                icon = Icons.Rounded.AccountBalance,
                iconTint = AppColors.error,
                onClick = {},
                isWarning = true
            )
        }
    }
}
