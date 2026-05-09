package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.service.discovery.DiscoveredPattern
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatShortCurrency
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import com.osscabinet.ossWallet.domain.model.Category
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSuggestionsScreen(
    viewModel: TransactionViewModel,
    onAcceptAsTemplate: () -> Unit,
    onAcceptAsPlannedPayment: () -> Unit,
    onBackClick: () -> Unit
) {
    val patterns by viewModel.discoveredPatterns.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var dismissedPatterns by remember { mutableStateOf(setOf<String>()) }
    val visiblePatterns = patterns.filter { "${it.payee}|${it.averageAmount}" !in dismissedPatterns }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Suggestions") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (visiblePatterns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No suggestions available yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "Continue tracking to see patterns",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visiblePatterns) { pattern ->
                    SuggestionListItem(
                        pattern = pattern,
                        categories = categories,
                        onAcceptAsTemplate = {
                            viewModel.saveTemplateFromPattern(it)
                            onAcceptAsTemplate()
                        },
                        onAcceptAsPlannedPayment = {
                            viewModel.savePlannedPaymentFromPattern(it)
                            onAcceptAsPlannedPayment()
                        },
                        onDismiss = {
                            dismissedPatterns = dismissedPatterns + "${pattern.payee}|${pattern.averageAmount}"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionListItem(
    pattern: DiscoveredPattern,
    categories: List<Category>,
    onAcceptAsTemplate: (DiscoveredPattern) -> Unit,
    onAcceptAsPlannedPayment: (DiscoveredPattern) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row: Type Indicator + Dismiss Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "Smart Suggestion",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Info Row: Payee + Abbreviated Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pattern.payee.ifEmpty { pattern.title },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatShortCurrency(pattern.averageAmount.abs(), pattern.currency),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = if (pattern.transactionType == "income") Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val recommendation = when {
                pattern.recurrenceRule != null -> "Recurring ${pattern.recurrenceRule.type.name.lowercase()} payment detected."
                else -> "This seems to be a common transaction."
            }

            Text(
                text = recommendation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Detailed Preview Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val categoryName = categories.find { it.id == pattern.categoryId }?.name ?: "Unknown"
                        DetailItem("Category", categoryName, Modifier.weight(1f))
                        DetailItem("Account", pattern.accountId, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailItem("Method", pattern.paymentMethod, Modifier.weight(1f))
                        if (pattern.note.isNotEmpty()) {
                            DetailItem("Note", pattern.note, Modifier.weight(1f))
                        }
                    }
                    if (pattern.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailItem("Tags", pattern.tags.joinToString(", "), Modifier.fillMaxWidth())
                    }
                }
            }
            
            Text(
                text = "Confidence: ${(pattern.confidence * 100).toInt()}% based on ${pattern.occurrences} occurrences.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pattern.recurrenceRule != null && !pattern.existsAsPlannedPayment) {
                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { onAcceptAsPlannedPayment(pattern) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Planned Payment", style = MaterialTheme.typography.labelSmall)
                        }
                        Text(
                            "Auto-creates records for future occurrences",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                if (!pattern.existsAsTemplate) {
                    Column(modifier = Modifier.weight(1f)) {
                        FilledTonalButton(
                            onClick = { onAcceptAsTemplate(pattern) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Template", style = MaterialTheme.typography.labelSmall)
                        }
                        Text(
                            "Shortcut for one-tap manual entry",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SmartSuggestionsScreenPreview() {
    val patterns = listOf(
        DiscoveredPattern(
            title = "Netflix Subscription",
            payee = "Netflix",
            categoryId = 1,
            accountId = "Savings",
            averageAmount = BigDecimal("-15.99"),
            currency = "USD",
            transactionType = "expense",
            paymentMethod = "Credit Card",
            note = "Monthly plan",
            tags = listOf("subscription", "streaming"),
            latitude = null,
            longitude = null,
            warrantyExpiration = null,
            recurrenceRule = com.osscabinet.ossWallet.domain.model.RecurrenceRule(com.osscabinet.ossWallet.domain.model.RecurrenceType.MONTHLY),
            confidence = 0.95f,
            occurrences = 5,
            lastDate = System.currentTimeMillis()
        ),
        DiscoveredPattern(
            title = "Weekly Groceries",
            payee = "Whole Foods",
            categoryId = 2,
            accountId = "Checking",
            averageAmount = BigDecimal("-85.40"),
            currency = "USD",
            transactionType = "expense",
            paymentMethod = "Debit Card",
            note = "",
            tags = emptyList(),
            latitude = null,
            longitude = null,
            warrantyExpiration = null,
            recurrenceRule = com.osscabinet.ossWallet.domain.model.RecurrenceRule(com.osscabinet.ossWallet.domain.model.RecurrenceType.WEEKLY),
            confidence = 0.8f,
            occurrences = 12,
            lastDate = System.currentTimeMillis()
        )
    )

    MaterialTheme {
        // Mock UI state for preview
        Surface(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(patterns) { pattern ->
                    SuggestionListItem(
                        pattern = pattern,
                        categories = emptyList(),
                        onAcceptAsTemplate = {},
                        onAcceptAsPlannedPayment = {},
                        onDismiss = {}
                    )
                }
            }
        }
    }
}
