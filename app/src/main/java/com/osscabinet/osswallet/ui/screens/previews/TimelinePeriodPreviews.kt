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
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.TimelineFilterBar
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.components.transactions.AeonPeriodSelector
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Preview(showBackground = true, name = "Timeline Filter Bar - All Modes")
@Composable
fun TimelineFilterBarAllModesPreview() {
    OssWalletTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("All Time", style = MaterialTheme.typography.labelMedium)
                TimelineFilterBar(
                    currentRange = TimelineRange.AllTime,
                    onRangeSelected = {}
                )

                Text("This Month", style = MaterialTheme.typography.labelMedium)
                TimelineFilterBar(
                    currentRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
                    onRangeSelected = {}
                )

                Text("Last 30 Days", style = MaterialTheme.typography.labelMedium)
                TimelineFilterBar(
                    currentRange = TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS),
                    onRangeSelected = {}
                )

                Text("Custom Range", style = MaterialTheme.typography.labelMedium)
                TimelineFilterBar(
                    currentRange = TimelineRange.CustomRange(
                        start = 1715731200000L, // May 15, 2024
                        end = 1718409600000L   // June 15, 2024
                    ),
                    onRangeSelected = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Aeon Period Selector - All Modes")
@Composable
fun AeonPeriodSelectorAllModesPreview() {
    OssWalletTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("All Time (Page 0)", style = MaterialTheme.typography.labelMedium)
                AeonPeriodSelector(
                    filter = TransactionFilter(timelineRange = TimelineRange.AllTime),
                    onSetDateRange = {},
                    onClick = {},
                    showArcTab = false
                )

                Text("This Month (Page 1)", style = MaterialTheme.typography.labelMedium)
                AeonPeriodSelector(
                    filter = TransactionFilter(timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH)),
                    onSetDateRange = {},
                    onClick = {},
                    showArcTab = false
                )

                Text("Last 30 Days (Page 2)", style = MaterialTheme.typography.labelMedium)
                AeonPeriodSelector(
                    filter = TransactionFilter(timelineRange = TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS)),
                    onSetDateRange = {},
                    onClick = {},
                    showArcTab = false
                )

                Text("Custom Range (Page 3)", style = MaterialTheme.typography.labelMedium)
                AeonPeriodSelector(
                    filter = TransactionFilter(timelineRange = TimelineRange.CustomRange(
                        start = 1715731200000L, // May 15, 2024
                        end = 1718409600000L   // June 15, 2024
                    )),
                    onSetDateRange = {},
                    onClick = {},
                    showArcTab = false
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Aeon Timeline Period Selector - All Modes")
@Composable
fun AeonTimelinePeriodSelectorAllModesPreview() {
    OssWalletTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Component Swiping (Default)", style = MaterialTheme.typography.labelLarge)
                AeonTimelinePeriodSelector(
                    currentRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
                    onRangeSelected = {},
                    swipeEntireComponent = true
                )

                Spacer(Modifier.height(16.dp))

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
            }
        }
    }
}
