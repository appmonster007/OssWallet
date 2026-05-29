package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        color = AppColors.surfaceContainerLow,
        border = BorderStroke(1.dp, AppColors.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = AppColors.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val dateLabel = remember(dateMillis) {
                    if (dateMillis != null) {
                        SimpleDateFormat(
                            "MMM d, yyyy",
                            Locale.getDefault()
                        ).format(Date(dateMillis))
                    } else {
                        placeholder
                    }
                }
                Text(
                    text = label,
                    style = AppTextStyles.overlineLabel,
                    color = AppColors.contentSecondary
                )
                Text(
                    text = dateLabel,
                    style = if (dateMillis != null) AppTextStyles.managementItemTitle else AppTextStyles.formFieldHint,
                    color = if (dateMillis != null) AppColors.contentPrimary else AppColors.contentSecondary
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = AppColors.contentSecondary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EntryDateCardPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            EntryDateCard(
                icon = Icons.Rounded.CalendarToday,
                label = "TRANSACTION DATE",
                dateMillis = System.currentTimeMillis(),
                onClick = {}
            )
        }
    }
}
