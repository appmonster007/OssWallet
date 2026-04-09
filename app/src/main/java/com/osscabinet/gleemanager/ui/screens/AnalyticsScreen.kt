package com.osscabinet.gleemanager.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.ui.util.getCurrencySymbol
import com.osscabinet.gleemanager.ui.viewmodel.AnalyticsViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intelligence", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Share, contentDescription = "Share") }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            AnalyticsHeroSection(uiState.currentBalance, uiState.selectedCurrency)

            // Cash Flow Chart (Using custom Compose implementation for stability)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("CASH FLOW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, letterSpacing = 1.sp)
                EditorialChartCard(Color(0xFF006064)) {
                    SimpleBarChart(uiState.dailyNetFlow, Color.White)
                }
            }

            // Balance Outlook Chart (Using custom Compose implementation for stability)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("BALANCE OUTLOOK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, letterSpacing = 1.sp)
                EditorialChartCard(Color.White) {
                    SimpleLineChart(uiState.forecast, Color(0xFF006064))
                }
            }

            InsightSection()
        }
    }
}

@Composable
fun SimpleBarChart(data: Map<Long, Double>, barColor: Color) {
    val maxValue = data.values.maxOfOrNull { Math.abs(it) }?.coerceAtLeast(1.0) ?: 1.0
    Row(
        modifier = Modifier.height(180.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.values.forEach { value ->
            val heightPercent = (Math.abs(value) / maxValue).toFloat().coerceIn(0.1f, 1f)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightPercent),
                color = barColor,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            ) {}
        }
    }
}

@Composable
fun SimpleLineChart(data: Map<Long, Double>, lineColor: Color) {
    val values = data.values.toList()
    if (values.isEmpty()) return
    
    val maxValue = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val minValue = values.minOrNull()?.coerceAtMost(0.0) ?: 0.0
    val range = (maxValue - minValue).coerceAtLeast(1.0)

    Row(
        modifier = Modifier.height(180.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEach { value ->
            val heightPercent = ((value - minValue) / range).toFloat().coerceIn(0.05f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightPercent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(lineColor, CircleShape)
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
fun AnalyticsHeroSection(balance: Double, currency: String) {
    Column {
        Text("PROJECTED BALANCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${getCurrencySymbol(currency)}${String.format(Locale.getDefault(), "%,.0f", balance)}",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
            )
            Text(
                ".${String.format(Locale.getDefault(), "%02d", ((balance % 1) * 100).toInt())}",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), fontFeatureSettings = "tnum"),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = Color(0xFFE0F2F1),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF00695C), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("On track for $142K goal", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00695C), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EditorialChartCard(containerColor: Color, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = containerColor,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(24.dp)) {
            content()
        }
    }
}

@Composable
fun InsightSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Color(0xFF006064)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoGraph, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("INTELLIGENCE INSIGHT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF006064)))
            }
            Text(
                "Based on your current trajectory, your net worth will increase by 4.2% by the end of Q4. Consider increasing your 'Travel' budget by $400 for your upcoming trip.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
