package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.BarChart
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.osscabinet.ossWallet.ui.components.GenericAddCardDialog
import com.osscabinet.ossWallet.ui.components.CardTypeOptionData
import java.math.BigDecimal
import com.osscabinet.ossWallet.domain.model.settings.CategoryBreakdownCardConfig
import com.osscabinet.ossWallet.domain.model.settings.CategoryExpenseTreemapCardConfig
import com.osscabinet.ossWallet.domain.model.settings.TimelineTrajectoryCardConfig
import com.osscabinet.ossWallet.ui.analytics.components.CategoryBreakdownCard
import com.osscabinet.ossWallet.ui.analytics.components.CategoryExpenseTreemapCard
import com.osscabinet.ossWallet.ui.analytics.components.TimelineTrajectoryCard
import com.osscabinet.ossWallet.ui.components.AeonFlowSectionHeader
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.viewmodel.AnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val decimalPrecision by viewModel.decimalPrecision.collectAsStateWithLifecycle()
    val cardsConfig by viewModel.analyticsCardsConfig.collectAsStateWithLifecycle()
    val categoryBreakdownData by viewModel.categoryBreakdownData.collectAsStateWithLifecycle()
    val timelineTrajectoryData by viewModel.timelineTrajectoryData.collectAsStateWithLifecycle()
    val categoryExpenseTreemapData by viewModel.categoryExpenseTreemapData.collectAsStateWithLifecycle()

    var navTriggered by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Reset navTriggered when returning to this screen
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        navTriggered = false
    }

    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intelligence", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = { navigate { onBackClick() } }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddCardDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add card",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            item {
                AnalyticsHeroSection(uiState.currentBalance, uiState.selectedCurrency, decimalPrecision)
            }

            // Analytics Cards from config
            items(
                count = cardsConfig.cards.size,
                key = { index -> cardsConfig.cards.sortedBy { it.order }[index].cardId }
            ) { index ->
                val cardConfig = cardsConfig.cards.sortedBy { it.order }[index]
                when (cardConfig) {
                    is CategoryBreakdownCardConfig -> {
                        CategoryBreakdownCard(
                            config = cardConfig,
                            data = categoryBreakdownData,
                            onUpdate = { viewModel.updateCardConfig(it) },
                            onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                        )
                    }
                    is TimelineTrajectoryCardConfig -> {
                        TimelineTrajectoryCard(
                            config = cardConfig,
                            data = timelineTrajectoryData,
                            onUpdate = { viewModel.updateCardConfig(it) },
                            onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                        )
                    }
                    is CategoryExpenseTreemapCardConfig -> {
                        CategoryExpenseTreemapCard(
                            config = cardConfig,
                            data = categoryExpenseTreemapData,
                            onUpdate = { viewModel.updateCardConfig(it) },
                            onDelete = { coroutineScope.launch { viewModel.removeCard(cardConfig.cardId) } }
                        )
                    }
                    else -> {}
                }
            }

            item {
                InsightSection(uiState.selectedCurrency, decimalPrecision)
            }
        }
    }
    
    if (showAddCardDialog) {
        val analyticsCardOptions = listOf(
            CardTypeOptionData(
                title = "Category Breakdown",
                description = "View spending by category",
                icon = Icons.Default.PieChart,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            CategoryBreakdownCardConfig(
                                cardId = "analytics_category_breakdown_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "CATEGORY BREAKDOWN",
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS),
                                showLabels = true,
                                showLegend = true
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Timeline Trajectory",
                description = "Track balance over time",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            TimelineTrajectoryCardConfig(
                                cardId = "analytics_timeline_trajectory_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "TIMELINE TRAJECTORY",
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS),
                                showBalance = true,
                                showExpense = true,
                                showIncome = true
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            ),
            CardTypeOptionData(
                title = "Category Expenses",
                description = "Expense distribution treemap",
                icon = Icons.Default.BarChart,
                onClick = {
                    coroutineScope.launch {
                        viewModel.addCard(
                            CategoryExpenseTreemapCardConfig(
                                cardId = "analytics_category_expense_treemap_${System.currentTimeMillis()}",
                                order = cardsConfig.cards.size,
                                title = "CATEGORY EXPENSES",
                                timelineRange = com.osscabinet.ossWallet.domain.model.timeline.TimelineRange.LastPeriod(com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType.LAST_30_DAYS),
                                showLabels = true
                            )
                        )
                    }
                    showAddCardDialog = false
                }
            )
        )
        
        GenericAddCardDialog(
            onDismiss = { showAddCardDialog = false },
            cardOptions = analyticsCardOptions
        )
    }
}

@Composable
fun AnalyticsHeroSection(balance: BigDecimal, currency: String, precision: Int = 2) {
    Column {
        AeonFlowSectionHeader(title = "PROJECTED BALANCE")
        Text(
            formatCurrencyRight(balance, currency, precision),
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum", color = MaterialTheme.colorScheme.onSurface)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = Color(0xFF4CAF50).copy(alpha = 0.2f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("On track for goal", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InsightSection(currency: String, precision: Int = 2) {
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
                "Based on your current trajectory, your net worth will increase by 4.2% by the end of Q4. Consider increasing your 'Travel' budget by ${formatCurrencyRight(BigDecimal("400.0"), currency, precision)} for your upcoming trip.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
