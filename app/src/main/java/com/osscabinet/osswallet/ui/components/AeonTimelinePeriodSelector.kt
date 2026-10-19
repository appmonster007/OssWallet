package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.timeline.DefaultTimeline
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.timeline.PickedDays
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The "up to now" flag to keep when switching between period types. All Time's flag is a
 * sentinel default, not a user choice, so leaving it uses [defaultExcludeFuture] instead.
 */
private fun TimelineRange.carriedExcludeFuture(defaultExcludeFuture: Boolean): Boolean =
    if (this is TimelineRange.AllTime) defaultExcludeFuture else excludeFuture

/**
 * Optimized Timeline Period Selector.
 * 
 * @param swipeEntireComponent If true, the entire navigation row (buttons + icons + dates) swipes.
 *                            If false, buttons and icons are fixed and only the date value swipes.
 * @param defaultThisPeriod Period used when the user switches to the "This" tab from another tab.
 *                          The up-to-now (exclude future) flag is carried over from the current range.
 * @param defaultExcludeFuture Up-to-now flag used when leaving the All Time tab. Defaults to the
 *                             app-wide initial timeline; forms that define periods (budgets,
 *                             loans, saved filters) pass false so their periods stay whole.
 */
@Composable
fun AeonTimelinePeriodSelector(
    currentRange: TimelineRange,
    onRangeSelected: (TimelineRange) -> Unit,
    modifier: Modifier = Modifier,
    swipeEntireComponent: Boolean = false,
    defaultThisPeriod: ThisPeriodType = ThisPeriodType.YEAR,
    defaultExcludeFuture: Boolean = DefaultTimeline.INITIAL.excludeFuture,
    isFlush: Boolean = false,
    isActive: Boolean = false,
    containerColor: Color = AppColors.cardSurface,
    contentColor: Color = AppColors.primary,
    inactiveContentColor: Color = AppColors.contentSecondary.copy(alpha = 0.3f)
) {
    val glassEnabled = LocalGlassmorphismEnabled.current
    val accentColor = AppColors.accent

    val effectiveContainerColor = remember(containerColor, isActive, glassEnabled, accentColor) {
        when {
            glassEnabled && isActive && containerColor == accentColor -> containerColor.copy(alpha = 0.6f)
            glassEnabled && containerColor != Color.Transparent -> containerColor.copy(alpha = 0.8f)
            else -> containerColor
        }
    }

    val initialPage = remember(currentRange) {
        when (currentRange) {
            is TimelineRange.AllTime -> 0
            is TimelineRange.ThisPeriod -> 1
            is TimelineRange.LastPeriod -> 2
            is TimelineRange.CustomRange -> 3
            is TimelineRange.Periodic -> 1
        }
    }
    val pagerState = rememberPagerState(initialPage = initialPage) { 4 }
    val scope = rememberCoroutineScope()

    // Sync Pager -> Range
    LaunchedEffect(pagerState.settledPage) {
        val newRange = when (pagerState.settledPage) {
            0 -> TimelineRange.AllTime()
            1 -> if (currentRange is TimelineRange.ThisPeriod) currentRange else TimelineRange.ThisPeriod(
                defaultThisPeriod,
                excludeFuture = currentRange.carriedExcludeFuture(defaultExcludeFuture)
            )

            2 -> if (currentRange is TimelineRange.LastPeriod) currentRange else TimelineRange.LastPeriod(
                LastPeriodType.LAST_30_DAYS,
                excludeFuture = currentRange.carriedExcludeFuture(defaultExcludeFuture)
            )

            3 -> if (currentRange is TimelineRange.CustomRange) currentRange else {
                val now = System.currentTimeMillis()
                TimelineRange.CustomRange(now - 86400000L * 7, now)
            }

            else -> TimelineRange.AllTime()
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
        modifier = modifier
            .fillMaxWidth()
            .then(if (isFlush) Modifier else Modifier.padding(horizontal = AeonTheme.dimensions.cardPadding)),
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        color = effectiveContainerColor,
        tonalElevation = 1.dp,
        border = if (containerColor == Color.Transparent || isFlush) null else BorderStroke(
            1.dp,
            AppColors.outlineVariant
        )
    ) {
        Column {
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
                        onIconClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(it)
                            }
                        },
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
                    onIconClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(it)
                        }
                    },
                    showDropdown = showDropdown,
                    onDropdownShow = { showDropdown = it },
                    onDatePickerShow = { showDatePicker = it },
                    contentColor = contentColor,
                    inactiveContentColor = inactiveContentColor,
                    centerContent = {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                        ) { page ->
                            DateDisplayArea(
                                page = page,
                                activePage = pagerState.currentPage,
                                currentRange = currentRange,
                                showDropdown = showDropdown,
                                onDropdownShow = { showDropdown = it },
                                onDatePickerShow = { showDatePicker = it },
                                onRangeSelected = onRangeSelected,
                                contentColor = contentColor,
                                inactiveContentColor = inactiveContentColor,
                            )
                        }
                    }
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
    inactiveContentColor: Color = AppColors.contentSecondary,
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
    inactiveContentColor: Color = AppColors.contentSecondary
) {
    val icons = listOf(
        Icons.Default.History,
        Icons.Default.CalendarToday,
        Icons.Default.Schedule,
        Icons.Default.DateRange
    )
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
            .height(26.dp)
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = { onRangeSelected(currentRange.withExcludeFuture(!currentRange.excludeFuture)) },
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(22.dp)
            ) {
                Icon(
                    imageVector = if (currentRange.excludeFuture) Icons.Rounded.AccessTimeFilled else Icons.Rounded.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (currentRange.excludeFuture) contentColor else inactiveContentColor
                )
            }

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
                    modifier = Modifier
                        .size(14.dp)
                        .padding(start = 2.dp),
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
                    .background(AppColors.surfaceContainerHigh)
            ) {
                if (page == 1) {
                    ThisPeriodType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label, style = AppTextStyles.drawerItemLabel) },
                            onClick = {
                                onRangeSelected(TimelineRange.ThisPeriod(type, excludeFuture = currentRange.excludeFuture))
                                onDropdownShow(false)
                            }
                        )
                    }
                } else if (page == 2) {
                    LastPeriodType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label, style = AppTextStyles.drawerItemLabel) },
                            onClick = {
                                onRangeSelected(TimelineRange.LastPeriod(type, excludeFuture = currentRange.excludeFuture))
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
    val zone = java.time.ZoneId.systemDefault()
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStart?.let { PickedDays.toPickerMillis(it, zone) },
        initialSelectedEndDateMillis = initialEnd?.let { PickedDays.toPickerMillis(it, zone) }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis
                    if (start != null && end != null) {
                        // The picker reports UTC midnight; make it whole local days (end inclusive).
                        val (from, to) = PickedDays.toRange(start, end, zone)
                        onConfirmed(from, to)
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

    val displayFormat =
        if (startCal.get(Calendar.YEAR) == currentYear && endCal.get(Calendar.YEAR) == currentYear) dateFormat else yearFormat
    return "${displayFormat.format(Date(start))} - ${displayFormat.format(Date(end))}"
}

@Preview(showBackground = true)
@Composable
fun AeonTimelinePeriodSelectorPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            AeonTimelinePeriodSelector(
                currentRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
                onRangeSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonTimelinePeriodSelectorPillPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            AeonTimelinePeriodSelector(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                isFlush = true,
                containerColor = AppColors.primaryContainer,
                contentColor = AppColors.onSelectionContainer,
                inactiveContentColor = AppColors.onSelectionContainer,
                currentRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
                onRangeSelected = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonTimelinePeriodTogglePreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Exclude Future: TRUE (Filled Icon)")
            AeonTimelinePeriodSelector(
                currentRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH, excludeFuture = true),
                onRangeSelected = {}
            )

            Text("Exclude Future: FALSE (Lined Icon)")
            AeonTimelinePeriodSelector(
                currentRange = TimelineRange.ThisPeriod(
                    ThisPeriodType.MONTH,
                    excludeFuture = false
                ),
                onRangeSelected = {}
            )
        }
    }
}
