package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AppColors

@Composable
fun EntryLocationRow(
    coords: Pair<Double, Double>?,
    onShowLocationSheet: () -> Unit,
    label: String = "Location"
) {
    Surface(
        onClick = onShowLocationSheet,
        shape = RoundedCornerShape(16.dp),
        color = AppColors.recessedSurface,
        border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = AppColors.tertiary.copy(alpha = 0.1f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AppColors.tertiary
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                )
                Text(
                    text = coords?.let { "Coordinates: ${it.first.toString().take(6)}, ${it.second.toString().take(6)}" }
                        ?: "Add Location",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = AppColors.contentSecondary.copy(alpha = 0.4f)
            )
        }
    }
}
