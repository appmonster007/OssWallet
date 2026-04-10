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
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            AnalyticsHeroSection(uiState.currentBalance, uiState.selectedCurrency)

            // Cash Flow Chart
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("CASH FLOW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, letterSpacing = 1.sp)
                EditorialChartCard(MaterialTheme.colorScheme.primaryContainer) {
                    SimpleBarChart(uiState.dailyNetFlow, MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            // Balance Outlook Chart
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("BALANCE OUTLOOK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, letterSpacing = 1.sp)
                EditorialChartCard(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                    SimpleLineChart(uiState.forecast, MaterialTheme.colorScheme.primary)
                }
            }

            InsightSection(uiState.selectedCurrency)
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
                color = barColor.copy(alpha = 0.8f),
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
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum", color = MaterialTheme.colorScheme.onSurface)
            )
            Text(
                ".${String.format(Locale.getDefault(), "%02d", ((balance % 1) * 100).toInt())}",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), fontFeatureSettings = "tnum"),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = Color(0xFF4CAF50).copy(alpha = 0.2f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("On track for goal", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
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
fun InsightSection(currency: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("INTELLIGENCE INSIGHT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
            }
            Text(
                "Based on your current trajectory, your net worth will increase by 4.2% by the end of Q4. Consider increasing your 'Travel' budget by ${getCurrencySymbol(currency)}400 for your upcoming trip.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
