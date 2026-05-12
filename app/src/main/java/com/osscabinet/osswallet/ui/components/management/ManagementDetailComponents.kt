package com.osscabinet.ossWallet.ui.components.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared chip for displaying scope/context items (Accounts, Categories, Tags)
 */
@Composable
fun ScopeChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Shared section for displaying a list of scope items with an icon and label.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScopeSection(
    label: String,
    icon: ImageVector,
    allText: String,
    isEmpty: Boolean,
    items: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }

        if (isEmpty) {
            Text(
                allText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 56.dp)
            )
        } else {
            FlowRow(
                modifier = Modifier.padding(start = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val displayCount = if (isExpanded) items.size else 5
                items.take(displayCount).forEach { it() }

                if (items.size > 5 && !isExpanded) {
                    Surface(
                        onClick = { isExpanded = true },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = "+${items.size - 5} more",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
