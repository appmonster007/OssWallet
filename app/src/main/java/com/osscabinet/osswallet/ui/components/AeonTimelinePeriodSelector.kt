package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Optimized Timeline Period Selector.
 * 
 * @param swipeEntireComponent If true, the entire navigation row (buttons + icons + dates) swipes.
 *                            If false, buttons and icons are fixed and only the date value swipes.
 */
@Composable
fun AeonTimelinePeriodSelector(
    currentRange: TimelineRange,
    onRangeSelected: (TimelineRange) -> Unit,
    modifier: Modifier = Modifier,
    swipeEntireComponent: Boolean = true
) {
    val initialPage = remember(currentRange) {
        when (currentRange) {
            is TimelineRange.AllTime -> 0
            is TimelineRange.ThisPeriod -> 1
            is TimelineRange.LastPeriod -> 2
            is TimelineRange.CustomRange -> 3
        }
    }
    val pagerState = rememberPagerState(initialPage = initialPage) { 4 }
    val scope = rememberCoroutineScope()

    // Sync Pager -> Range
    LaunchedEffect(pagerState.settledPage) {
        val newRange = when (pagerState.settledPage) {
            0 -> TimelineRange.AllTime
            1 -> if (currentRange is TimelineRange.ThisPeriod) currentRange else TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
            2 -> if (currentRange is TimelineRange.LastPeriod) currentRange else TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS)
            3 -> if (currentRange is TimelineRange.CustomRange) currentRange else {
                val now = System.currentTimeMillis()
                TimelineRange.CustomRange(now - 86400000L * 7, now)
            }
            else -> TimelineRange.AllTime
        }
        if (newRange != currentRange) {
            onRangeSelected(newRange)
        }
    }

    // Sync Range -> Pager
    LaunchedEffect(initialPage) {
        if (pagerState.currentPage != initialPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(initialPage)
        }
    }

    var showDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        if (swipeEntireComponent) {
            // 1. Full Component Pager (Tactile Experience)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                TimelineSelectorLayout(
                    page = page,
                    activePage = page,
                    currentRange = currentRange,
                    onRangeSelected = onRangeSelected,
                    onIconClick = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                    onDropdownShow = { showDropdown = it },
                    onDatePickerShow = { showDatePicker = it }
                )
            }
        } else {
            // 2. Value-Only Pager (Fixed Controls Experience)
            TimelineSelectorLayout(
                page = pagerState.currentPage,
                activePage = pagerState.currentPage,
                currentRange = currentRange,
                onRangeSelected = onRangeSelected,
                onIconClick = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                onDropdownShow = { showDropdown = it },
                onDatePickerShow = { showDatePicker = it },
                centerContent = {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().height(22.dp)
                    ) { page ->
                        DateDisplayArea(
                            page = page,
                            currentRange = currentRange,
                            onDropdownShow = { showDropdown = it },
                            onDatePickerShow = { showDatePicker = it }
                        )
                    }
                }
            )
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

    if (showDropdown) {
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            val page = pagerState.currentPage
            if (page == 1) {
                ThisPeriodType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label) },
                        onClick = {
                            onRangeSelected(TimelineRange.ThisPeriod(type))
                            showDropdown = false
                        }
                    )
                }
            } else if (page == 2) {
                LastPeriodType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label) },
                        onClick = {
                            onRangeSelected(TimelineRange.LastPeriod(type))
                            showDropdown = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineSelectorLayout(
    page: Int,
    activePage: Int,
    currentRange: TimelineRange,
    onRangeSelected: (TimelineRange) -> Unit,
    onIconClick: (Int) -> Unit,
    onDropdownShow: (Boolean) -> Unit,
    onDatePickerShow: (Boolean) -> Unit,
    centerContent: @Composable ColumnScope.() -> Unit = {
        DateDisplayArea(
            page = page,
            currentRange = currentRange,
            onDropdownShow = onDropdownShow,
            onDatePickerShow = onDatePickerShow
        )
    }
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onRangeSelected(TimelineService.shiftRange(currentRange, -1)) },
            enabled = currentRange !is TimelineRange.AllTime,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.ChevronLeft, 
                null, 
                modifier = Modifier.size(18.dp),
                tint = if (currentRange !is TimelineRange.AllTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconIndicators(activePage = activePage, onIconClick = onIconClick)
            centerContent()
        }

        IconButton(
            onClick = { onRangeSelected(TimelineService.shiftRange(currentRange, 1)) },
            enabled = currentRange !is TimelineRange.AllTime,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.ChevronRight, 
                null, 
                modifier = Modifier.size(18.dp),
                tint = if (currentRange !is TimelineRange.AllTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun IconIndicators(
    activePage: Int,
    onIconClick: (Int) -> Unit
) {
    val icons = listOf(Icons.Default.History, Icons.Default.CalendarToday, Icons.Default.Schedule, Icons.Default.DateRange)
    val labels = listOf("ALL", "THIS", "LAST", "CUSTOM")

    Row(
        modifier = Modifier.animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icons.forEachIndexed { index, icon ->
            val isSelected = activePage == index
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onIconClick(index) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Text(
                        text = labels[index],
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp, fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DateDisplayArea(
    page: Int,
    currentRange: TimelineRange,
    onDropdownShow: (Boolean) -> Unit,
    onDatePickerShow: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                when (page) {
                    1, 2 -> onDropdownShow(true)
                    3 -> onDatePickerShow(true)
                    else -> {}
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = getRangeSubtitleText(currentRange),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (page in 1..2) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    null,
                    modifier = Modifier.size(14.dp).padding(start = 2.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getRangeSubtitleText(range: TimelineRange): String {
    val (start, end) = TimelineService.getDateRange(range)
    if (start == null || end == null) return "All History"

    val startCal = Calendar.getInstance().apply { timeInMillis = start }
    val endCal = Calendar.getInstance().apply { timeInMillis = end }
    
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val yearFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    
    if (startCal.get(Calendar.DAY_OF_MONTH) == 1 &&
        endCal.get(Calendar.DAY_OF_MONTH) == endCal.getActualMaximum(Calendar.DAY_OF_MONTH) &&
        startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH) &&
        startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)
    ) {
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(start))
    }

    val displayFormat = if (startCal.get(Calendar.YEAR) == currentYear && endCal.get(Calendar.YEAR) == currentYear) dateFormat else yearFormat
    return "${displayFormat.format(Date(start))} - ${displayFormat.format(Date(end))}"
}
