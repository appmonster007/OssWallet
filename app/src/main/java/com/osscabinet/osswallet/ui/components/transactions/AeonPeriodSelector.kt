package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.AeonDateRangePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AeonPeriodSelector(
    filter: TransactionFilter,
    onSetDateRange: (TimelineRange) -> Unit,
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
    val initialTimelineRange =
        remember(filter.timelineRange) { guessTimelineRange(filter) }

    val currentModePage = remember(initialTimelineRange) {
        when (initialTimelineRange) {
            is TimelineRange.AllTime -> 0
            is TimelineRange.ThisPeriod -> 1
            is TimelineRange.LastPeriod -> 2
            is TimelineRange.CustomRange -> 3
        }
    }

    val pagerState = remember {
        object : PagerState(currentPage = currentModePage) {
            override val pageCount: Int get() = 4
        }
    }

    // Sync Pager -> Filter (on swipe)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentModePage) {
            val newRange = when (pagerState.currentPage) {
                0 -> TimelineRange.AllTime
                1 -> TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
                2 -> TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS)
                3 -> {
                    if (filter.timelineRange is TimelineRange.CustomRange) filter.timelineRange
                    else {
                        val now = System.currentTimeMillis()
                        TimelineRange.CustomRange(now - 86400000 * 7, now)
                    }
                }

                else -> TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
            }
            onSetDateRange(newRange)
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
        // The Pull Tab "Hump"
        if (showArcTab) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 72.dp, height = 38.dp)
                    .background(
                        backgroundColor,
                        RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
            )
        }

        // Main Content Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (showArcTab) Modifier.padding(top = 16.dp) else Modifier)
                .background(backgroundColor)
                .padding(top = if (showArcTab) 16.dp else 8.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Swipeable Navigation Row
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PillNavigationRow(
                        page = page,
                        filter = filter,
                        accentColor = accentColor,
                        onSetDateRange = onSetDateRange,
                        onToggleExpand = onClick
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
        }

        // The Arrow Icon - Drawn last (on top)
        if (showArcTab) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 72.dp, height = 24.dp)
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
                        .padding(top = 8.dp)
                        .size(20.dp)
                        .graphicsLayer { rotationZ = rotation },
                    tint = accentColor.copy(alpha = 0.7f)
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
    onSetDateRange: (TimelineRange) -> Unit,
    onToggleExpand: () -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (page == 0) MaterialTheme.colorScheme.surfaceVariant
                else accentColor
            )
            .drawBehind {
                val strokeWidthPx = 1.dp.toPx()
                val dashPattern = floatArrayOf(10f, 10f)
                val cornerRadiusPx = 12.dp.toPx()

                // Inset the border so the 1dp stroke isn't cut off by the .clip()
                val inset = strokeWidthPx / 2
                val size = androidx.compose.ui.geometry.Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

                drawRoundRect(
                    color = accentColor.copy(alpha = 0.5f),
                    topLeft = Offset(inset, inset), // Start slightly inside
                    size = size,
                    style = Stroke(
                        width = strokeWidthPx,
                        pathEffect = PathEffect.dashPathEffect(dashPattern, 0f)
                    ),
                    cornerRadius = CornerRadius(cornerRadiusPx - inset)
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ){
        // Left Arrow (Shift Previous)
        Box(
            modifier = Modifier
                .weight(0.15f)
                .fillMaxHeight()
                .clickable(enabled = page > 0) {
                    val currentRange = when (page) {
                        1 -> if (filter.timelineRange is TimelineRange.ThisPeriod) filter.timelineRange else TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
                        2 -> if (filter.timelineRange is TimelineRange.LastPeriod) filter.timelineRange else TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS)
                        3 -> if (filter.timelineRange is TimelineRange.CustomRange) filter.timelineRange else {
                            val now = System.currentTimeMillis()
                            TimelineRange.CustomRange(now - 86400000 * 7, now)
                        }
                        else -> filter.timelineRange
                    }
                    onSetDateRange(TimelineService.shiftRange(currentRange, -1))
                },
            contentAlignment = Alignment.Center
        ) {
            if (page > 0) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Divider
        if (page > 0) {
            VerticalDivider(
                modifier = Modifier.height(16.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
            )
        }

        // Center Period
        Box(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
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
                    text = if (page == 0) "All Time" else getPeriodTextForPage(page, filter),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = if (page == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                )
                if (page > 0 && page < 3) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                } else if (page == 3) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
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
                                    onSetDateRange(TimelineRange.ThisPeriod(type))
                                    showDropdown = false
                                }
                            )
                        }
                    } else if (page == 2) {
                        LastPeriodType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    onSetDateRange(TimelineRange.LastPeriod(type))
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Divider
        if (page > 0) {
            VerticalDivider(
                modifier = Modifier.height(16.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
            )
        }

        // Right Arrow (Shift Next)
        Box(
            modifier = Modifier
                .weight(0.15f)
                .fillMaxHeight()
                .clickable(enabled = page > 0) {
                    val currentRange = when (page) {
                        1 -> if (filter.timelineRange is TimelineRange.ThisPeriod) filter.timelineRange else TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
                        2 -> if (filter.timelineRange is TimelineRange.LastPeriod) filter.timelineRange else TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS)
                        3 -> if (filter.timelineRange is TimelineRange.CustomRange) filter.timelineRange else {
                            val now = System.currentTimeMillis()
                            TimelineRange.CustomRange(now - 86400000 * 7, now)
                        }
                        else -> filter.timelineRange
                    }
                    onSetDateRange(TimelineService.shiftRange(currentRange, 1))
                },
            contentAlignment = Alignment.Center
        ) {
            if (page > 0) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDatePicker) {
        AeonDateRangePicker(
            initialStart = (filter.timelineRange as? TimelineRange.CustomRange)?.start,
            initialEnd = (filter.timelineRange as? TimelineRange.CustomRange)?.end,
            onDismiss = { showDatePicker = false },
            onConfirmed = { start, end ->
                onSetDateRange(TimelineRange.CustomRange(start ?: 0L, end ?: System.currentTimeMillis()))
                showDatePicker = false
            }
        )
    }
}

private fun guessTimelineRange(filter: TransactionFilter): TimelineRange {
    return filter.timelineRange
}

private fun getPeriodTextForPage(page: Int, filter: TransactionFilter): String {
    val currentRange = when (page) {
        1 -> if (filter.timelineRange is TimelineRange.ThisPeriod) filter.timelineRange else TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
        2 -> if (filter.timelineRange is TimelineRange.LastPeriod) filter.timelineRange else TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS)
        3 -> if (filter.timelineRange is TimelineRange.CustomRange) filter.timelineRange else return "Custom"
        else -> filter.timelineRange
    }
    
    val (resolvedStart, resolvedEnd) = TimelineService.getDateRange(currentRange)
    val start = resolvedStart
    val end = resolvedEnd

    if (start == null || end == null) return "All Time"
    
    // Check for "Today"
    val todayRange = TimelineService.getRangeForThis(ThisPeriodType.TODAY)
    if (Math.abs(todayRange.first - start) < 2000) return "Today"
    
    // Check for "This Month"
    val monthRange = TimelineService.getRangeForThis(ThisPeriodType.MONTH)
    if (Math.abs(monthRange.first - start) < 2000 && Math.abs(monthRange.second - end) < 2000) return "This month"

    val startCal = Calendar.getInstance().apply { timeInMillis = start }
    val endCal = Calendar.getInstance().apply { timeInMillis = end }

    return if (startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH) &&
        startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) &&
        startCal.get(Calendar.DAY_OF_MONTH) == 1 &&
        endCal.get(Calendar.DAY_OF_MONTH) == endCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    ) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(start))
    } else {
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val yearFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val displayFormat = if (startCal.get(Calendar.YEAR) == currentYear) dateFormat else yearFormat
        
        "${displayFormat.format(Date(start))} - ${displayFormat.format(Date(end))}"
    }
}

@Preview(showBackground = false)
@Composable
fun AeonPeriodSelectorPreview() {
    val filter = TransactionFilter()
    Box(modifier = Modifier.padding(top = 16.dp)) {
        AeonPeriodSelector(
            filter = filter,
            onSetDateRange = { },
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
            onSetDateRange = { },
            onClick = { },
            isExpanded = true,
            showArcTab = true
        )
    }
}
