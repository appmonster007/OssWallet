package com.osscabinet.ossWallet.ui.components.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AppColors

/**
 * Generic list item container for management screens.
 */
@Composable
fun ManagementListItem(
    onClick: () -> Unit,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingContent: @Composable (ColumnScope.() -> Unit)? = null,
    menuItems: @Composable (ColumnScope.(onDismiss: () -> Unit) -> Unit)? = null,
    isWarning: Boolean = false,
    additionalContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isWarning) AppColors.errorContainer.copy(alpha = 0.15f)
                else AppColors.cardSurface,
        border = BorderStroke(
            0.5.dp,
            if (isWarning) AppColors.error.copy(alpha = 0.2f)
            else AppColors.divider.copy(alpha = 0.15f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = iconTint
                        )
                    }
                }

                // Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.contentSecondary.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Trailing
                if (trailingContent != null) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        trailingContent()
                    }
                }

                // Menu
                if (menuItems != null) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
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
            
            if (additionalContent != null) {
                additionalContent()
            }
        }
    }
}
