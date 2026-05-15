package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.AeonDateRangePicker
import com.osscabinet.ossWallet.ui.theme.AeonTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonTimelineSheet(
    selectedRange: TimelineRange,
    onRangeSelected: (TimelineRange) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Period"
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AeonTheme.dimensions.spacingXl)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)
            )

            // Mode 1: All Time
            ListItem(
                headlineContent = { Text("All Time") },
                leadingContent = { Icon(Icons.Rounded.AllInclusive, null) },
                trailingContent = {
                    RadioButton(
                        selected = selectedRange is TimelineRange.AllTime,
                        onClick = { onRangeSelected(TimelineRange.AllTime); onDismiss() }
                    )
                },
                modifier = Modifier.clickable { onRangeSelected(TimelineRange.AllTime); onDismiss() }
            )

            // Mode 2: This Period
            var showThisPeriodOptions by remember { mutableStateOf(selectedRange is TimelineRange.ThisPeriod) }
            ListItem(
                headlineContent = { Text("This Period") },
                supportingContent = {
                    if (selectedRange is TimelineRange.ThisPeriod) {
                        Text(selectedRange.period.label)
                    }
                },
                leadingContent = { Icon(Icons.Rounded.Today, null) },
                trailingContent = {
                    RadioButton(
                        selected = selectedRange is TimelineRange.ThisPeriod,
                        onClick = { showThisPeriodOptions = !showThisPeriodOptions }
                    )
                },
                modifier = Modifier.clickable { showThisPeriodOptions = !showThisPeriodOptions }
            )
            
            if (showThisPeriodOptions) {
                Row(
                    modifier = Modifier.padding(horizontal = 56.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThisPeriodType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedRange is TimelineRange.ThisPeriod && selectedRange.period == type,
                            onClick = { onRangeSelected(TimelineRange.ThisPeriod(type)); onDismiss() },
                            label = { Text(type.label) }
                        )
                    }
                }
            }

            // Mode 3: Last Period
            var showLastPeriodOptions by remember { mutableStateOf(selectedRange is TimelineRange.LastPeriod) }
            ListItem(
                headlineContent = { Text("Last X Days") },
                supportingContent = {
                    if (selectedRange is TimelineRange.LastPeriod) {
                        Text(selectedRange.period.label)
                    }
                },
                leadingContent = { Icon(Icons.Rounded.History, null) },
                trailingContent = {
                    RadioButton(
                        selected = selectedRange is TimelineRange.LastPeriod,
                        onClick = { showLastPeriodOptions = !showLastPeriodOptions }
                    )
                },
                modifier = Modifier.clickable { showLastPeriodOptions = !showLastPeriodOptions }
            )

            if (showLastPeriodOptions) {
                Row(
                    modifier = Modifier.padding(horizontal = 56.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LastPeriodType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedRange is TimelineRange.LastPeriod && selectedRange.period == type,
                            onClick = { onRangeSelected(TimelineRange.LastPeriod(type)); onDismiss() },
                            label = { Text(type.label) }
                        )
                    }
                }
            }

            // Mode 4: Custom Range
            ListItem(
                headlineContent = { Text("Custom Range") },
                leadingContent = { Icon(Icons.Rounded.DateRange, null) },
                trailingContent = {
                    RadioButton(
                        selected = selectedRange is TimelineRange.CustomRange,
                        onClick = { showCustomPicker = true }
                    )
                },
                modifier = Modifier.clickable { showCustomPicker = true }
            )
        }
    }

    if (showCustomPicker) {
        AeonDateRangePicker(
            initialStart = (selectedRange as? TimelineRange.CustomRange)?.start,
            initialEnd = (selectedRange as? TimelineRange.CustomRange)?.end,
            onDismiss = { showCustomPicker = false },
            onConfirmed = { start, end ->
                onRangeSelected(TimelineRange.CustomRange(start, end))
                showCustomPicker = false
                onDismiss()
            }
        )
    }
}
