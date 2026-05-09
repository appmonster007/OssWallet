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
import com.osscabinet.ossWallet.ui.viewmodel.TransactionViewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSuggestionsScreen(
    viewModel: TransactionViewModel,
    onBackClick: () -> Unit
) {
    val patterns by viewModel.discoveredPatterns.collectAsStateWithLifecycle()
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
                        onAcceptAsTemplate = {
                            viewModel.saveTemplateFromPattern(it)
                            dismissedPatterns = dismissedPatterns + "${pattern.payee}|${pattern.averageAmount}"
                        },
                        onAcceptAsPlannedPayment = {
                            viewModel.savePlannedPaymentFromPattern(it)
                            dismissedPatterns = dismissedPatterns + "${pattern.payee}|${pattern.averageAmount}"
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
    onAcceptAsTemplate: (DiscoveredPattern) -> Unit,
    onAcceptAsPlannedPayment: (DiscoveredPattern) -> Unit,
    onDismiss: () -> Unit
) {
    val currencyFormatter = remember(pattern.currency) {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance(pattern.currency)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (pattern.payee.isNotEmpty()) pattern.payee else pattern.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currencyFormatter.format(pattern.averageAmount.abs()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
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
                    if (pattern.recurrenceRule != null) {
                        Button(
                            onClick = { onAcceptAsPlannedPayment(pattern) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Track Automatically")
                        }
                    }
                    
                    FilledTonalButton(
                        onClick = { onAcceptAsTemplate(pattern) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Template")
                    }
                }
            }
        }
    }
}
