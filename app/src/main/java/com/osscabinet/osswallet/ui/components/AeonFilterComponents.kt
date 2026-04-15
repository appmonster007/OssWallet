package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalDensity
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("timeline_type")
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
        // Redesigned Primary Selection (Segmented Control)
        AeonSegmentedControl(
            options = listOf(
                TimelineOption("All", Icons.Default.History),
                TimelineOption("This", Icons.Default.CalendarToday),
                TimelineOption("Last", Icons.Default.Schedule),
                TimelineOption("Custom", Icons.Default.DateRange)
            ),
            selectedOption = when (currentRange) {
                is TimelineRange.AllTime -> 0
                is TimelineRange.ThisPeriod -> 1
                is TimelineRange.LastPeriod -> 2
                is TimelineRange.CustomRange -> 3
            },
            onOptionSelected = { index ->
                when (index) {
                    0 -> onRangeSelected(TimelineRange.AllTime)
                    1 -> onRangeSelected(TimelineRange.ThisPeriod(ThisPeriodType.MONTH))
                    2 -> onRangeSelected(TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS))
                    3 -> showDatePicker = true
                }
            },
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Dropdown for This/Last
        AnimatedVisibility(
            visible = currentRange is TimelineRange.ThisPeriod || currentRange is TimelineRange.LastPeriod || currentRange is TimelineRange.CustomRange,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                when (currentRange) {
                    is TimelineRange.ThisPeriod -> {
                        AeonDropdown(
                            label = "Select Period",
                            options = ThisPeriodType.values().toList(),
                            selectedOption = currentRange.period,
                            onOptionSelected = { onRangeSelected(TimelineRange.ThisPeriod(it)) },
                            labelProvider = { it.label }
                        )
                    }
                    is TimelineRange.LastPeriod -> {
                        AeonDropdown(
                            label = "Select Range",
                            options = LastPeriodType.values().toList(),
                            selectedOption = currentRange.period,
                            onOptionSelected = { onRangeSelected(TimelineRange.LastPeriod(it)) },
                            labelProvider = { it.label }
                        )
                    }
                    is TimelineRange.CustomRange -> {
                        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        OutlinedCard(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "CUSTOM RANGE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "${formatter.format(Date(currentRange.start))} - ${formatter.format(Date(currentRange.end))}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    if (showDatePicker) {
        AeonDateRangePicker(
            initialStart = (currentRange as? TimelineRange.CustomRange)?.start,
            initialEnd = (currentRange as? TimelineRange.CustomRange)?.end,
            onDismiss = { showDatePicker = false },
            onConfirmed = { start, end ->
                onRangeSelected(TimelineRange.CustomRange(start, end))
                showDatePicker = false
            }
        )
    }
}

data class TimelineOption(val label: String, val icon: ImageVector)

@Composable
fun AeonSegmentedControl(
    options: List<TimelineOption>,
    selectedOption: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        shape = RoundedCornerShape(20.dp), // Increased for smoother Aeon look
        modifier = modifier.fillMaxWidth().height(60.dp) // Slightly taller
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedOption == index
                val backgroundColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    label = "bgColor"
                )
                val contentColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "contentColor"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp)) // Increased for smoother Aeon look
                        .background(backgroundColor)
                        .clickable { onOptionSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp), // Slightly larger
                            tint = contentColor
                        )
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold, // Bolder
                                fontSize = 11.sp // Slightly larger
                            ),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AeonDropdown(
    label: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = labelProvider(selectedOption),
            onValueChange = {},
            readOnly = true,
            label = { 
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                ) 
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp), // More rounded Aeon style
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .clip(RoundedCornerShape(16.dp))
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                DropdownMenuItem(
                    text = { 
                        Text(
                            labelProvider(option),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        ) 
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonDateRangePicker(
    initialStart: Long?,
    initialEnd: Long?,
    onDismiss: () -> Unit,
    onConfirmed: (Long, Long) -> Unit
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStart,
        initialSelectedEndDateMillis = initialEnd,
        yearRange = 1970..2100
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis
                    if (start != null && end != null) {
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = end
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        onConfirmed(start, calendar.timeInMillis)
                    }
                },
                enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Selection")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(28.dp),
        colors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
            // Quick Jump Header
            val currentMonth = remember(state.displayedMonthMillis) {
                Calendar.getInstance().apply {
                    timeInMillis = state.displayedMonthMillis
                }
            }
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val months = remember {
                        (0..11).map { m ->
                            Calendar.getInstance().apply { set(Calendar.MONTH, m) }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
                        }
                    }

                    // Month Selector
                    AeonJumpSelector(
                        currentValue = currentMonth.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "",
                        options = months,
                        selectedIndex = currentMonth.get(Calendar.MONTH),
                        onOptionSelected = { index ->
                            currentMonth.set(Calendar.MONTH, index)
                            state.displayedMonthMillis = currentMonth.timeInMillis
                        },
                        onPrevious = {
                            currentMonth.add(Calendar.MONTH, -1)
                            state.displayedMonthMillis = currentMonth.timeInMillis
                        },
                        onNext = {
                            currentMonth.add(Calendar.MONTH, 1)
                            state.displayedMonthMillis = currentMonth.timeInMillis
                        },
                        modifier = Modifier.weight(1.2f)
                    )

                    // Year Selector
                    val currentYear = currentMonth.get(Calendar.YEAR)
                    val years = remember { (1970..2100).map { it.toString() } }
                    AeonJumpSelector(
                        currentValue = currentYear.toString(),
                        options = years,
                        selectedIndex = (currentYear - 1970).coerceIn(0, years.size - 1),
                        onOptionSelected = { index ->
                            val year = 1970 + index
                            currentMonth.set(Calendar.YEAR, year)
                            state.displayedMonthMillis = currentMonth.timeInMillis
                        },
                        onPrevious = {
                            currentMonth.add(Calendar.YEAR, -1)
                            state.displayedMonthMillis = currentMonth.timeInMillis
                        },
                        onNext = {
                            currentMonth.add(Calendar.YEAR, 1)
                            state.displayedMonthMillis = currentMonth.timeInMillis
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Today Jump Button
                    IconButton(
                        onClick = {
                            state.displayedMonthMillis = System.currentTimeMillis()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Today,
                            contentDescription = "Jump to Today",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            DateRangePicker(
                state = state,
                modifier = Modifier.weight(1f),
                title = null,
                headline = {
                    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            Text(
                                text = if (start != null && end != null) {
                                    "${formatter.format(Date(start))} - ${formatter.format(Date(end))}"
                                } else if (start != null) {
                                    "${formatter.format(Date(start))} - Select End"
                                } else {
                                    "Select Range"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                },
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun AeonJumpSelector(
    currentValue: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    selectedIndex: Int = -1,
    onOptionSelected: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(4.dp)
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentValue,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .width(140.dp)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val scrollState = rememberScrollState()
                    val density = LocalDensity.current
                    
                    if (expanded) {
                        LaunchedEffect(selectedIndex) {
                            if (selectedIndex >= 0) {
                                // Approximate item height (48dp standard for DropdownMenuItem)
                                val itemHeightPx = with(density) { 48.dp.toPx() }
                                val scrollPx = ((selectedIndex - 2).coerceAtLeast(0) * itemHeightPx).toInt()
                                scrollState.scrollTo(scrollPx)
                            }
                        }
                    }

                    // Use a Box with fixed width and max height to:
                    // 1. Avoid intrinsic measurement crash (by providing fixed width intrinsics)
                    // 2. Avoid infinite height constraint crash for the inner scrollable Column
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .heightIn(max = 280.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            options.forEachIndexed { index, option ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            option, 
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    onClick = {
                                        onOptionSelected(index)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
