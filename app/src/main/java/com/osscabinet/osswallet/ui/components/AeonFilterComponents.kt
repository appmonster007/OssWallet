package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import kotlinx.serialization.Serializable

@Serializable
sealed class TimelineRange {
    @Serializable
    object AllTime : TimelineRange()
    @Serializable
    data class ThisPeriod(val period: ThisPeriodType) : TimelineRange()
    @Serializable
    data class LastPeriod(val period: LastPeriodType) : TimelineRange()
    @Serializable
    data class CustomRange(val start: Long, val end: Long) : TimelineRange()
}

@Serializable
enum class ThisPeriodType(val label: String) {
    TODAY("Today"),
    WEEK("This Week"),
    MONTH("This Month"),
    QUARTER("This Quarter"),
    HALF_YEAR("Half Year"),
    YEAR("This Year")
}

@Serializable
enum class LastPeriodType(val label: String) {
    LAST_24H("24h"),
    LAST_7_DAYS("7 Days"),
    LAST_30_DAYS("30 Days"),
    LAST_12_WEEKS("12 Weeks"),
    LAST_6_MONTHS("6 Months"),
    LAST_1_YEAR("1 Year"),
    LAST_5_YEARS("5 Years")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineFilterBar(
    currentRange: TimelineRange,
    onRangeSelected: (TimelineRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // Primary Selection
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            val options = listOf(
                Triple("All", Icons.Default.History, 0),
                Triple("This", Icons.Default.CalendarToday, 1),
                Triple("Last", Icons.Default.Schedule, 2),
                Triple("Custom", Icons.Default.DateRange, 3)
            )

            options.forEachIndexed { index, (label, icon, _) ->
                val isSelected = when (currentRange) {
                    is TimelineRange.AllTime -> index == 0
                    is TimelineRange.ThisPeriod -> index == 1
                    is TimelineRange.LastPeriod -> index == 2
                    is TimelineRange.CustomRange -> index == 3
                }

                SegmentedButton(
                    selected = isSelected,
                    onClick = {
                        when (index) {
                            0 -> onRangeSelected(TimelineRange.AllTime)
                            1 -> onRangeSelected(TimelineRange.ThisPeriod(ThisPeriodType.MONTH))
                            2 -> onRangeSelected(TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS))
                            3 -> showDatePicker = true
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Secondary Row with expressive chips
        AnimatedVisibility(
            visible = currentRange !is TimelineRange.AllTime,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (currentRange) {
                    is TimelineRange.ThisPeriod -> {
                        items(ThisPeriodType.values()) { type ->
                            FilterChip(
                                selected = currentRange.period == type,
                                onClick = { onRangeSelected(TimelineRange.ThisPeriod(type)) },
                                label = { Text(type.label) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                    is TimelineRange.LastPeriod -> {
                        items(LastPeriodType.values()) { type ->
                            FilterChip(
                                selected = currentRange.period == type,
                                onClick = { onRangeSelected(TimelineRange.LastPeriod(type)) },
                                label = { Text(type.label) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                    is TimelineRange.CustomRange -> {
                        item {
                            val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
                            FilterChip(
                                selected = true,
                                onClick = { showDatePicker = true },
                                label = { 
                                    Text("${formatter.format(Date(currentRange.start))} - ${formatter.format(Date(currentRange.end))}") 
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(14.dp)) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    if (showDatePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        // Ensure end is at the very end of the day for inclusive filtering
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = end
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        onRangeSelected(TimelineRange.CustomRange(start, calendar.timeInMillis))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                title = { Text("Select date range", modifier = Modifier.padding(16.dp)) },
                headline = { 
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                        if (start != null) Text(formatter.format(Date(start)))
                        Text(" - ")
                        if (end != null) Text(formatter.format(Date(end)))
                    }
                }
            )
        }
    }
}
