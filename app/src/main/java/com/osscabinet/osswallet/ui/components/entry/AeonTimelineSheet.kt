package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector

/**
 * Dialog wrapping [AeonTimelinePeriodSelector] for selecting a [TimelineRange] in entry screens
 * (budget, loan, filter). The selection is buffered locally and only committed on Confirm.
 */
@Composable
fun AeonTimelineSheet(
    selectedRange: TimelineRange,
    onRangeSelected: (TimelineRange) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Period"
) {
    var currentRange by remember(selectedRange) { mutableStateOf(selectedRange) }
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = surfaceColor,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
                )

                Spacer(Modifier.height(4.dp))

                AeonTimelinePeriodSelector(
                    currentRange = currentRange,
                    onRangeSelected = { currentRange = it },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    swipeEntireComponent = false
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onRangeSelected(currentRange); onDismiss() }) { Text("Confirm") }
                }
            }
        }
    }
}
