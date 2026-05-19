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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
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
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

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
    swipeEntireComponent: Boolean = false,
    containerColor: Color = AppColors.cardSurface,
    contentColor: Color = AppColors.primary,
    inactiveContentColor: Color = AppColors.contentSecondary.copy(alpha = 0.3f)
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
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        color = containerColor,
        tonalElevation = 1.dp,
        border = if (containerColor == Color.Transparent) null else androidx.compose.foundation.BorderStroke(
            (AeonTheme.dimensions.spacingXs / 8),
            AppColors.divider.copy(alpha = 0.15f)
        )
    ) {
        if (swipeEntireComponent) {
            // 1. Full Component Pager (Tactile Experience)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                TimelineSelectorLayout(
                    page = page,
                    activePage = pagerState.currentPage,
                    currentRange = currentRange,
                    onRangeSelected = onRangeSelected,
                    onIconClick = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                    showDropdown = showDropdown,
                    onDropdownShow = { showDropdown = it },
                    onDatePickerShow = { showDatePicker = it },
                    contentColor = contentColor,
                    inactiveContentColor = inactiveContentColor
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
                showDropdown = showDropdown,
                onDropdownShow = { showDropdown = it },
                onDatePickerShow = { showDatePicker = it },
                contentColor = contentColor,
                inactiveContentColor = inactiveContentColor,
                centerContent = {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().height(22.dp)
                    ) { page ->
                        DateDisplayArea(
                            page = page,
                            activePage = pagerState.currentPage,
                            currentRange = currentRange,
                            showDropdown = showDropdown,
                            onDropdownShow = { showDropdown = it },
                            onDatePickerShow = { showDatePicker = it },
                            onRangeSelected = onRangeSelected
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
}

@Composable
private fun TimelineSelectorLayout(
    page: Int,
    activePage: Int,
    currentRange: TimelineRange,
    onRangeSelected: (TimelineRange) -> Unit,
    onIconClick: (Int) -> Unit,
    showDropdown: Boolean,
    onDropdownShow: (Boolean) -> Unit,
    onDatePickerShow: (Boolean) -> Unit,
    contentColor: Color = AppColors.primary,
    inactiveContentColor: Color = AppColors.contentSecondary.copy(alpha = 0.3f),
    centerContent: @Composable ColumnScope.() -> Unit = {
        DateDisplayArea(
            page = page,
            activePage = activePage,
            currentRange = currentRange,
            showDropdown = showDropdown,
            onDropdownShow = onDropdownShow,
            onDatePickerShow = onDatePickerShow,
            onRangeSelected = onRangeSelected,
            contentColor = contentColor,
            inactiveContentColor = inactiveContentColor
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
                tint = if (currentRange !is TimelineRange.AllTime) contentColor else inactiveContentColor
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconIndicators(
                activePage = activePage, 
                onIconClick = onIconClick,
                contentColor = contentColor,
                inactiveContentColor = inactiveContentColor
            )
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
                tint = if (currentRange !is TimelineRange.AllTime) contentColor else inactiveContentColor
            )
        }
    }
}

@Composable
private fun IconIndicators(
    activePage: Int,
    onIconClick: (Int) -> Unit,
    contentColor: Color = AppColors.primary,
    inactiveContentColor: Color = AppColors.contentSecondary.copy(alpha = 0.3f)
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
                    tint = if (isSelected) contentColor else inactiveContentColor
                )
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Text(
                        text = labels[index],
                        modifier = Modifier.padding(start = 4.dp),
                        style = AppTextStyles.timelineTabLabel,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun DateDisplayArea(
    page: Int,
    activePage: Int,
    currentRange: TimelineRange,
    showDropdown: Boolean,
    onDropdownShow: (Boolean) -> Unit,
    onDatePickerShow: (Boolean) -> Unit,
    onRangeSelected: (TimelineRange) -> Unit,
    contentColor: Color = AppColors.contentPrimary,
    inactiveContentColor: Color = AppColors.contentSecondary
) {
    var width by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(8.dp))
            .onGloballyPositioned { width = it.size.width }
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
                style = AppTextStyles.timelinePeriodValue,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (page in 1..2) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    null,
                    modifier = Modifier.size(14.dp).padding(start = 2.dp),
                    tint = inactiveContentColor
                )
            }
        }

        if (showDropdown && page == activePage && (page == 1 || page == 2)) {
            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { onDropdownShow(false) },
                offset = DpOffset(0.dp, 4.dp),
                modifier = Modifier
                    .width(with(density) { width.toDp() })
                    .background(AppColors.panelSurface)
            ) {
                if (page == 1) {
                    ThisPeriodType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label, style = AppTextStyles.drawerItemLabel) },
                            onClick = {
                                onRangeSelected(TimelineRange.ThisPeriod(type))
                                onDropdownShow(false)
                            }
                        )
                    }
                } else if (page == 2) {
                    LastPeriodType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label, style = AppTextStyles.drawerItemLabel) },
                            onClick = {
                                onRangeSelected(TimelineRange.LastPeriod(type))
                                onDropdownShow(false)
                            }
                        )
                    }
                }
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
        initialSelectedEndDateMillis = initialEnd
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis
                    if (start != null && end != null) {
                        onConfirmed(start, end)
                    }
                },
                enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(
            state = state,
            title = { Text("Select Date Range", modifier = Modifier.padding(16.dp)) },
            modifier = Modifier.height(450.dp)
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
