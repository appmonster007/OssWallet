package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import com.osscabinet.ossWallet.ui.theme.AppColors

/**
 * Compact date selection card for entry screens.
 * Following the "Architect Slate" design specification.
 */
@Composable
fun EntryDateCard(
    icon: ImageVector,
    label: String,
    dateMillis: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select Date"
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = AppColors.recessedSurface,
        border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.2f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = AppColors.secondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val dateLabel = remember(dateMillis) {
                    if (dateMillis != null) {
                        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(dateMillis))
                    } else {
                        placeholder
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = AppColors.contentSecondary.copy(alpha = 0.6f)
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (dateMillis != null) AppColors.contentPrimary else AppColors.contentSecondary.copy(alpha = 0.5f)
                    )
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = AppColors.contentSecondary.copy(alpha = 0.4f)
            )
        }
    }
}
