package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import com.osscabinet.ossWallet.ui.components.AeonDropdown
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

    // Sync Filter -> Pager (on external change like Reset)
    LaunchedEffect(currentModePage) {
        if (pagerState.currentPage != currentModePage) {
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
                    .padding(top = 16.dp, bottom = 8.dp)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Label
                Text(
                    "TIMELINE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Swipeable Modes Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (page) {
                            0 -> { // All Time
                                OutlinedCard(
                                    onClick = { /* No action needed, already selected */ },
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "FILTER MODE",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 1.sp
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                "All Time Transactions",
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        Icon(Icons.Default.History, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            1 -> { // This Mode
                                val current = initialTimelineRange as? TimelineRange.ThisPeriod ?: TimelineRange.ThisPeriod(ThisPeriodType.MONTH)
                                AeonDropdown(
                                    label = "This Period",
                                    options = ThisPeriodType.entries.toList(),
                                    selectedOption = current.period,
                                    onOptionSelected = { applyTimelineRange(TimelineRange.ThisPeriod(it), onSetDateRange) },
                                    labelProvider = { it.label }
                                )
                            }
                            2 -> { // Last Mode
                                val current = initialTimelineRange as? TimelineRange.LastPeriod ?: TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS)
                                AeonDropdown(
                                    label = "Last Duration",
                                    options = LastPeriodType.entries.toList(),
                                    selectedOption = current.period,
                                    onOptionSelected = { applyTimelineRange(TimelineRange.LastPeriod(it), onSetDateRange) },
                                    labelProvider = { it.label }
                                )
                            }
                            3 -> { // Custom Mode
                                var showDatePicker by remember { mutableStateOf(false) }
                                val current = initialTimelineRange as? TimelineRange.CustomRange
                                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                
                                OutlinedCard(
                                    onClick = { showDatePicker = true },
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "CUSTOM RANGE",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 1.sp
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                if (current != null) "${formatter.format(Date(current.start))} - ${formatter.format(Date(current.end))}" else "Select Dates",
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                if (showDatePicker) {
                                    AeonDateRangePicker(
                                        initialStart = current?.start,
                                        initialEnd = current?.end,
                                        onDismiss = { showDatePicker = false },
                                        onConfirmed = { start, end ->
                                            applyTimelineRange(TimelineRange.CustomRange(start, end), onSetDateRange)
                                            showDatePicker = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Indicators (Dots)
                Row(
                    modifier = Modifier.padding(top = 12.dp),
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

        // The Pull Tab "Hump" - Moved here to draw on top of Surface
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
    val start = filter.startDate ?: return TimelineRange.AllTime
    val end = filter.endDate ?: return TimelineRange.AllTime

    // Check ThisPeriod
    ThisPeriodType.entries.forEach { type ->
        val range = FilterUtils.getRangeForThis(type)
        if (range.first == start && range.second == end) return TimelineRange.ThisPeriod(type)
    }

    // Check LastPeriod
    LastPeriodType.entries.forEach { type ->
        val range = FilterUtils.getRangeForLast(type)
        if (range.first == start && range.second == end) return TimelineRange.LastPeriod(type)
    }

    return TimelineRange.CustomRange(start, end)
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
