package com.osscabinet.ossWallet.ui.screens.settings.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun StyleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) AppColors.primary.copy(alpha = 0.12f) else AppColors.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) AppColors.primary.copy(alpha = 0.5f) else AppColors.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = AppTextStyles.chipLabel.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (selected) AppColors.primary else AppColors.contentPrimary
                )
            )
            if (selected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = AppColors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TuningSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueDisplay: String,
    onValueChange: (Float) -> Unit,
    onReset: (() -> Unit)? = null,
    steps: Int = 0
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                style = AppTextStyles.sectionOverline,
                color = AppColors.contentSecondary.copy(alpha = 0.7f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = valueDisplay,
                    style = AppTextStyles.managementItemTitle.copy(
                        color = AppColors.primary,
                        fontWeight = FontWeight.Black
                    )
                )
                if (onReset != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Reset parameter",
                            modifier = Modifier.size(14.dp),
                            tint = AppColors.contentSecondary.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = AppColors.primary,
                activeTrackColor = AppColors.primary,
                inactiveTrackColor = AppColors.outlineVariant.copy(alpha = 0.4f)
            )
        )
    }
}
