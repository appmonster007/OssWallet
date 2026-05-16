package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Standardized date range card for filter screens.
 * Reuses EntryDateCard logic to handle "From" and "To" dates.
 */
@Composable
fun AeonDateRangeCard(
    startDateMillis: Long?,
    endDateMillis: Long?,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    modifier: Modifier = Modifier,
    startLabel: String = "FROM",
    endLabel: String = "TO"
) {
    EntryActionRow(modifier = modifier) {
        EntryDateCard(
            icon = Icons.Rounded.CalendarToday,
            label = startLabel,
            dateMillis = startDateMillis,
            onClick = onStartClick,
            modifier = Modifier.weight(1f),
            placeholder = "Start date..."
        )

        EntryDateCard(
            icon = Icons.Rounded.CalendarToday,
            label = endLabel,
            dateMillis = endDateMillis,
            onClick = onEndClick,
            modifier = Modifier.weight(1f),
            placeholder = "End date..."
        )
    }
}
