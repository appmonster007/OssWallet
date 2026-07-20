package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import kotlin.math.abs

private fun formatGpsNsew(gpsLocation: String): String? {
    val parts = gpsLocation.split(",")
    if (parts.size != 2) return null
    val lat = parts[0].toDoubleOrNull() ?: return null
    val lon = parts[1].toDoubleOrNull() ?: return null
    val ns = if (lat >= 0) "N" else "S"
    val ew = if (lon >= 0) "E" else "W"
    return "%.5f°%s  %.5f°%s".format(abs(lat), ns, abs(lon), ew)
}

@Composable
fun EntryLocationRow(
    gpsLocation: String?,
    locationName: String? = null,
    onShowLocationSheet: () -> Unit,
    label: String = "Location"
) {
    Surface(
        onClick = onShowLocationSheet,
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = AppTextStyles.transactionSwipeAction.copy(
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                )
                if (gpsLocation == null) {
                    Text(
                        text = "Add Location",
                        style = AppTextStyles.transactionAmount
                    )
                } else {
                    if (!locationName.isNullOrBlank()) {
                        Text(
                            text = locationName,
                            style = AppTextStyles.transactionAmount.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val gpsFormatted = formatGpsNsew(gpsLocation)
                    if (gpsFormatted != null) {
                        Text(
                            text = gpsFormatted,
                            style = AppTextStyles.transactionSwipeAction.copy(
                                color = AppColors.contentSecondary.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = AppColors.contentSecondary.copy(alpha = 0.4f)
            )
        }
    }
}

@ThemePreviews
@Composable
fun EntryLocationRowPreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EntryLocationRow(
                gpsLocation = null,
                locationName = null,
                onShowLocationSheet = {}
            )
            EntryLocationRow(
                gpsLocation = "37.77490,-122.41940",
                locationName = null,
                onShowLocationSheet = {}
            )
            EntryLocationRow(
                gpsLocation = "48.85341,2.34880",
                locationName = "Rue de Rivoli, 1er Arrondissement, Paris, Île-de-France, France Métropolitaine, 75001, France",
                onShowLocationSheet = {}
            )
        }
    }
}
