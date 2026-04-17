package com.osscabinet.ossWallet.ui.components.transactions

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "tab_rotation"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Main Bar
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Navigation Row (Integrated Segmented Pill)
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
                            .clickable { navigateMonth(filter, -1, onSetDateRange) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Previous",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Center Period (Blue Block)
                    Box(
                        modifier = Modifier
                            .weight(0.64f)
                            .fillMaxHeight()
                            .background(accentColor)
                            .clickable { onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                getPeriodText(filter),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Right Arrow
                    Box(
                        modifier = Modifier
                            .weight(0.18f)
                            .fillMaxHeight()
                            .clickable { navigateMonth(filter, 1, onSetDateRange) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                // Indicators (Dots)
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape))
                    Box(modifier = Modifier.size(6.dp).background(accentColor, CircleShape))
                    Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape))
                }
            }
        }

        // The Pull Tab "Hump" - Moved here to draw on top of Surface
        if (showArcTab) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-12.dp))
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
