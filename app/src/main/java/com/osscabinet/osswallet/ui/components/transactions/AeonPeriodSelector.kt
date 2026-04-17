package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AeonPeriodSelector(
    filter: TransactionFilter,
    onSetDateRange: (Long?, Long?) -> Unit,
    onClick: () -> Unit,
    isExpanded: Boolean = false,
    showArcTab: Boolean = true,
    backgroundColor: Color = Color(0xFF1E1E1E),
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // The "Arc" Tab
        if (showArcTab) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-12).dp)
                    .size(width = 64.dp, height = 32.dp)
                    .background(backgroundColor, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                contentAlignment = Alignment.TopCenter
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.padding(top = 4.dp).size(20.dp),
                    tint = Color.Gray.copy(alpha = 0.8f)
                )
            }
        }

        // Main Bar
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Period Navigation Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    Surface(
                        onClick = { navigateMonth(filter, -1, onSetDateRange) },
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(width = 48.dp, height = 36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = Color.Gray)
                        }
                    }

                    // Main Period Pill
                    Surface(
                        onClick = onClick,
                        color = primaryColor.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                getPeriodText(filter),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = Color.White
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }

                    // Next Button
                    Surface(
                        onClick = { navigateMonth(filter, 1, onSetDateRange) },
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(width = 48.dp, height = 36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.Gray)
                        }
                    }
                }
                
                // Page Indicators
                Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (index == 1) primaryColor else Color.Gray.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

private fun navigateMonth(filter: com.osscabinet.ossWallet.domain.model.TransactionFilter, delta: Int, onSetDateRange: (Long?, Long?) -> Unit) {
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

private fun getPeriodText(filter: com.osscabinet.ossWallet.domain.model.TransactionFilter): String {
    return if (filter.startDate != null && filter.endDate != null) {
        val startCal = Calendar.getInstance().apply { timeInMillis = filter.startDate }
        val endCal = Calendar.getInstance().apply { timeInMillis = filter.endDate }
        
        if (startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH) && 
            startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) &&
            startCal.get(Calendar.DAY_OF_MONTH) == 1 &&
            endCal.get(Calendar.DAY_OF_MONTH) == endCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(filter.startDate)).uppercase()
        } else {
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            "${dateFormat.format(Date(filter.startDate))} - ${dateFormat.format(Date(filter.endDate))}"
        }
    } else {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()).uppercase()
    }
}
