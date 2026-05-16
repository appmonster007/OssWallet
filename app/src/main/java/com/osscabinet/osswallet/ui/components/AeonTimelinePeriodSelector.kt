package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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

@Composable
fun AeonTimelinePeriodSelector(
    currentRange: TimelineRange,
    onRangeSelected: (TimelineRange) -> Unit,
    modifier: Modifier = Modifier
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

    val icons = listOf(
        Icons.Default.History,
        Icons.Default.CalendarToday,
        Icons.Default.Schedule,
        Icons.Default.DateRange
    )
    val labels = listOf("ALL", "THIS", "LAST", "CUSTOM")

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Button
            IconButton(
                onClick = { onRangeSelected(TimelineService.shiftRange(currentRange, -1)) },
                enabled = pagerState.currentPage > 0,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.ChevronLeft, 
                    contentDescription = "Previous",
                    modifier = Modifier.size(18.dp),
                    tint = if (pagerState.currentPage > 0) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
            }

            // Integrated Content Block
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 1. Integrated Header Indicators
                Row(
                    modifier = Modifier.animateContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    icons.forEachIndexed { index, icon ->
                        val isSelected = pagerState.currentPage == index
                        
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary 
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                Text(
                                    text = labels[index],
                                    modifier = Modifier.padding(start = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp,
                                        fontSize = 8.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // 2. Date Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                when (page) {
                                    1, 2 -> showDropdown = true
                                    3 -> showDatePicker = true
                                    else -> {}
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = getRangeSubtitleText(currentRange),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (page in 1..2) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp).padding(start = 2.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (showDropdown && pagerState.currentPage == page) {
                            DropdownMenu(
                                expanded = showDropdown,
                                onDismissRequest = { showDropdown = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
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
                }
            }

            // Next Button
            IconButton(
                onClick = { onRangeSelected(TimelineService.shiftRange(currentRange, 1)) },
                enabled = pagerState.currentPage > 0,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.ChevronRight, 
                    contentDescription = "Next",
                    modifier = Modifier.size(18.dp),
                    tint = if (pagerState.currentPage > 0) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
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

private fun getRangeSubtitleText(range: TimelineRange): String {
    val (start, end) = TimelineService.getDateRange(range)
    if (start == null || end == null) return "All History"

    val startCal = Calendar.getInstance().apply { timeInMillis = start }
    val endCal = Calendar.getInstance().apply { timeInMillis = end }
    
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val yearFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    
    // Check if it's a full month
    if (startCal.get(Calendar.DAY_OF_MONTH) == 1 &&
        endCal.get(Calendar.DAY_OF_MONTH) == endCal.getActualMaximum(Calendar.DAY_OF_MONTH) &&
        startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH) &&
        startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)
    ) {
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(start))
    }

    val displayFormat = if (startCal.get(Calendar.YEAR) == currentYear && endCal.get(Calendar.YEAR) == currentYear) {
        dateFormat
    } else {
        yearFormat
    }

    return "${displayFormat.format(Date(start))} - ${displayFormat.format(Date(end))}"
}
