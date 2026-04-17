package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.AeonDateRangePicker
import com.osscabinet.ossWallet.ui.util.FilterUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AeonPeriodSelector(
    filter: TransactionFilter,
    onSetDateRange: (Long?, Long?) -> Unit,
    onClick: () -> Unit,
    isExpanded: Boolean = false,
    showArcTab: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "tab_rotation"
    )

    // Mode handling
    val initialTimelineRange = remember(filter.startDate, filter.endDate) { guessTimelineRange(filter) }
    
    val currentModePage = remember(initialTimelineRange) {
        when (initialTimelineRange) {
            is TimelineRange.AllTime -> 0
            is TimelineRange.ThisPeriod -> 1
            is TimelineRange.LastPeriod -> 2
            is TimelineRange.CustomRange -> 3
        }
    }

    val pagerState = rememberPagerState(
        initialPage = currentModePage,
        pageCount = { 4 }
    )

    // Sync Pager -> Filter (on swipe)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentModePage) {
            val newRange = when (pagerState.currentPage) {
                0 -> TimelineRange.AllTime
                1 -> TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
                2 -> TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS)
                3 -> {
                    if (initialTimelineRange is TimelineRange.CustomRange) initialTimelineRange
                    else {
                        val now = System.currentTimeMillis()
                        TimelineRange.CustomRange(now - 86400000 * 7, now)
                    }
                }
                else -> TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
            }
            applyTimelineRange(newRange, onSetDateRange)
        }
    }

    // Sync Filter -> Pager (on external change)
    LaunchedEffect(currentModePage) {
        if (pagerState.currentPage != currentModePage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(currentModePage)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Main Bar
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Swipeable Navigation Row (Integrated Segmented Pill)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        PillNavigationRow(
                            page = page,
                            filter = filter,
                            accentColor = accentColor,
                            initialTimelineRange = initialTimelineRange,
                            onSetDateRange = onSetDateRange,
                            onToggleExpand = onClick
                        )
                    }
                }
                
                // Indicators (Dots)
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { i ->
                        val isSelected = pagerState.currentPage == i
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 6.dp else 4.dp)
                                .background(
                                    if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }

        // The Pull Tab "Hump"
        if (showArcTab) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 90.dp, height = 24.dp)
                    .background(
                        backgroundColor, 
                        RoundedCornerShape(topStart = 45.dp, topEnd = 45.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(18.dp)
                        .graphicsLayer { rotationZ = rotation },
                    tint = accentColor
                )
            }
        }
    }
}

@Composable
private fun PillNavigationRow(
    page: Int,
    filter: TransactionFilter,
    accentColor: Color,
    initialTimelineRange: TimelineRange,
    onSetDateRange: (Long?, Long?) -> Unit,
    onToggleExpand: () -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .height(30.dp)
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Arrow
        Box(
            modifier = Modifier
                .weight(0.18f)
                .fillMaxHeight()
                .clickable(enabled = page == 1) { navigateMonth(filter, -1, onSetDateRange) },
            contentAlignment = Alignment.Center
        ) {
            if (page == 1) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Center Period (Blue Block)
        Box(
            modifier = Modifier
                .weight(0.64f)
                .fillMaxHeight()
                .background(accentColor)
                .clickable { 
                    when (page) {
                        0 -> onToggleExpand()
                        3 -> showDatePicker = true
                        else -> showDropdown = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (page == 0) "All Time" else getPeriodText(filter),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                if (page > 0) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (page == 3) Icons.Default.DateRange else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            if (showDropdown) {
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false }
                ) {
                    if (page == 1) {
                        ThisPeriodType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    applyTimelineRange(TimelineRange.ThisPeriod(type), onSetDateRange)
                                    showDropdown = false
                                }
                            )
                        }
                    } else if (page == 2) {
                        LastPeriodType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    applyTimelineRange(TimelineRange.LastPeriod(type), onSetDateRange)
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Right Arrow
        Box(
            modifier = Modifier
                .weight(0.18f)
                .fillMaxHeight()
                .clickable(enabled = page == 1) { navigateMonth(filter, 1, onSetDateRange) },
            contentAlignment = Alignment.Center
        ) {
            if (page == 1) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showDatePicker) {
        AeonDateRangePicker(
            initialStart = (initialTimelineRange as? TimelineRange.CustomRange)?.start,
            initialEnd = (initialTimelineRange as? TimelineRange.CustomRange)?.end,
            onDismiss = { showDatePicker = false },
            onConfirmed = { start, end ->
                onSetDateRange(start, end)
                showDatePicker = false
            }
        )
    }
}

private fun applyTimelineRange(range: TimelineRange, onSetDateRange: (Long?, Long?) -> Unit) {
    when (range) {
        is TimelineRange.AllTime -> onSetDateRange(null, null)
        is TimelineRange.ThisPeriod -> {
            val (start, end) = FilterUtils.getRangeForThis(range.period)
            onSetDateRange(start, end)
        }
        is TimelineRange.LastPeriod -> {
            val (start, end) = FilterUtils.getRangeForLast(range.period)
            onSetDateRange(start, end)
        }
        is TimelineRange.CustomRange -> onSetDateRange(range.start, range.end)
    }
}

private fun guessTimelineRange(filter: TransactionFilter): TimelineRange {
    if (filter.startDate == null && filter.endDate == null) return TimelineRange.AllTime
    
    val start = filter.startDate!!
    val end = filter.endDate!!
    val actualDuration = end - start

    // Check ThisPeriod
    for (type in ThisPeriodType.entries) {
        val range = FilterUtils.getRangeForThis(type)
        if (Math.abs(range.first - start) < 2000 && Math.abs(range.second - end) < 2000) return TimelineRange.ThisPeriod(type)
    }

    // Check LastPeriod
    for (type in LastPeriodType.entries) {
        val range = FilterUtils.getRangeForLast(type)
        val expectedDuration = range.second - range.first
        
        if (type == LastPeriodType.LAST_24H) {
            // For Last 24h, the duration is variable but always within a specific range since end is fixed at end-of-day
            if (Math.abs(expectedDuration - actualDuration) < 5000) return TimelineRange.LastPeriod(type)
        } else {
            if (Math.abs(expectedDuration - actualDuration) < 2000) return TimelineRange.LastPeriod(type)
        }
    }

    return TimelineRange.CustomRange(start, end)
}

private fun navigateMonth(filter: TransactionFilter, delta: Int, onSetDateRange: (Long?, Long?) -> Unit) {
    val calendar = Calendar.getInstance()
    filter.startDate?.let {
        calendar.timeInMillis = it
        calendar.add(Calendar.MONTH, delta)
    } ?: run {
        calendar.add(Calendar.MONTH, delta)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
    }
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val start = calendar.timeInMillis
    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    val end = calendar.timeInMillis
    onSetDateRange(start, end)
}

private fun getPeriodText(filter: TransactionFilter): String {
    return if (filter.startDate != null && filter.endDate != null) {
        val startCal = Calendar.getInstance().apply { timeInMillis = filter.startDate }
        val endCal = Calendar.getInstance().apply { timeInMillis = filter.endDate }
        
        if (startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH) && 
            startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) &&
            startCal.get(Calendar.DAY_OF_MONTH) == 1 &&
            endCal.get(Calendar.DAY_OF_MONTH) == endCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            
            val currentCal = Calendar.getInstance()
            if (startCal.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH) &&
                startCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR)) {
                "This month"
            } else {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(filter.startDate))
            }
        } else {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            "${dateFormat.format(Date(filter.startDate))} - ${dateFormat.format(Date(filter.endDate))}"
        }
    } else {
        "This month"
    }
}

@Preview(showBackground = false)
@Composable
fun AeonPeriodSelectorPreview() {
    val filter = TransactionFilter()
    Box(modifier = Modifier.padding(top = 16.dp)) {
        AeonPeriodSelector(
            filter = filter,
            onSetDateRange = { _, _ -> },
            onClick = { },
            isExpanded = false,
            showArcTab = true
        )
    }
}

@Preview(showBackground = false)
@Composable
fun AeonPeriodSelectorExpandedPreview() {
    val filter = TransactionFilter()
    Box(modifier = Modifier.padding(top = 16.dp)) {
        AeonPeriodSelector(
            filter = filter,
            onSetDateRange = { _, _ -> },
            onClick = { },
            isExpanded = true,
            showArcTab = true
        )
    }
}
