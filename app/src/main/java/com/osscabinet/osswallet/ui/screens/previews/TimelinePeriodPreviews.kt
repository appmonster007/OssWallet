package com.osscabinet.ossWallet.ui.screens.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.AppColors

@Preview(showBackground = true, name = "Aeon Timeline Period Selector - All Modes")
@Composable
fun AeonTimelinePeriodSelectorAllModesPreview() {
    OssWalletTheme {
        Surface(color = AppColors.screenBackground) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Value Swiping (Fixed Controls)", style = MaterialTheme.typography.labelLarge)
                AeonTimelinePeriodSelector(
                    currentRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
                    onRangeSelected = {},
                    swipeEntireComponent = false
                )
                
                Text("Custom Range (Value Swipe)", style = MaterialTheme.typography.labelSmall)
                AeonTimelinePeriodSelector(
                    currentRange = TimelineRange.CustomRange(
                        start = 1715731200000L, // May 15, 2024
                        end = 1718409600000L   // June 15, 2024
                    ),
                    onRangeSelected = {},
                    swipeEntireComponent = false
                )

                Spacer(Modifier.height(16.dp))

                Text("Component Swiping (Optional)", style = MaterialTheme.typography.labelLarge)
                AeonTimelinePeriodSelector(
                    currentRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
                    onRangeSelected = {},
                    swipeEntireComponent = true
                )
            }
        }
    }
}
