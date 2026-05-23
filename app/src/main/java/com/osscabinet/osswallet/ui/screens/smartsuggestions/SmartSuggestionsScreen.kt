package com.osscabinet.ossWallet.ui.screens.smartsuggestions

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
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.service.discovery.DiscoveredPattern
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatShortCurrency
import com.osscabinet.ossWallet.ui.screens.transactions.TransactionViewModel
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

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
                        modifier = Modifier.size(AeonTheme.dimensions.iconSize3xl),
                        tint = AppColors.border
                    )
                    Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingLg))
                    Text(
                        "No suggestions available yet",
                        style = AppTextStyles.listItemTitle,
                        color = AppColors.border
                    )
                    Text(
                        "Continue tracking to see patterns",
                        style = AppTextStyles.caption,
                        color = AppColors.border
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = AeonTheme.dimensions.screenPadding,
                    end = AeonTheme.dimensions.screenPadding,
                    top = padding.calculateTopPadding() + AeonTheme.dimensions.spacingSm,
                    bottom = scrollBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.listPadding)
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
        shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.cardSurface
        )
    ) {
        Column(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
            // Header Row: Type Indicator + Dismiss Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(AeonTheme.dimensions.spacingSm),
                    color = AppColors.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingSm, vertical = AeonTheme.dimensions.spacingXs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeXs),
                            tint = AppColors.onSecondaryContainer
                        )
                        Text(
                            "Smart Suggestion",
                            style = AppTextStyles.transactionSwipeAction,
                            color = AppColors.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                        tint = AppColors.border
                    )
                }
            }

            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingLg))

            // Main Info Row: Payee + Abbreviated Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pattern.payee.ifEmpty { pattern.name },
                    style = AppTextStyles.screenTitle,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                val amountColor = when (pattern.transactionType) {
                    TransactionType.INCOME -> PositiveAmountColor
                    TransactionType.TRANSFER -> AccentBlue
                    else -> AppColors.error
                }
                Text(
                    text = formatShortCurrency(pattern.averageAmount.abs(), pattern.currency),
                    style = AppTextStyles.screenTitle,
                    fontWeight = FontWeight.Black,
                    color = amountColor
                )
            }

            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))

            val recommendation = when {
                pattern.recurrenceRule != null -> "Recurring ${pattern.recurrenceRule.type.name.lowercase()} payment detected."
                else -> "This seems to be a common transaction."
            }

            Text(
                text = recommendation,
                style = AppTextStyles.transactionTitle,
                color = AppColors.contentSecondary
            )

            // Detailed Preview Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AeonTheme.dimensions.spacingMd),
                shape = RoundedCornerShape(AeonTheme.dimensions.listPadding),
                color = AppColors.insetSurface.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(AeonTheme.dimensions.spacingMd)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
                        val categoryName = categories.find { it.id == pattern.categoryId }?.name ?: "Unknown"
                        DetailItem("Category", categoryName, Modifier.weight(1f))
                        DetailItem("Account", pattern.accountId ?: "Outside Wallet", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))
                    Row(horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
                        DetailItem("Method", pattern.paymentMethod.displayName, Modifier.weight(1f))
                        if (pattern.note.isNotEmpty()) {
                            DetailItem("Note", pattern.note, Modifier.weight(1f))
                        }
                    }
                    if (pattern.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))
                        DetailItem("Tags", pattern.tags.joinToString(", "), Modifier.fillMaxWidth())
                    }
                }
            }
            
            Text(
                text = "Confidence: ${(pattern.confidence * 100).toInt()}% based on ${pattern.occurrences} occurrences.",
                style = AppTextStyles.transactionSwipeAction,
                color = AppColors.border
            )

            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingLg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
            ) {
                if (pattern.recurrenceRule != null && !pattern.existsAsPlannedPayment) {
                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { onAcceptAsPlannedPayment(pattern) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AeonTheme.dimensions.listPadding)
                        ) {
                            Text("Save Planned", style = AppTextStyles.transactionSwipeAction)
                        }
                        Text(
                            "Auto-creates records for future occurrences",
                            style = AppTextStyles.transactionSwipeAction,
                            color = AppColors.border,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = AeonTheme.dimensions.spacingXs)
                        )
                    }
                }
                
                if (!pattern.existsAsTemplate) {
                    Column(modifier = Modifier.weight(1f)) {
                        FilledTonalButton(
                            onClick = { onAcceptAsTemplate(pattern) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AeonTheme.dimensions.listPadding)
                        ) {
                            Text("Save Template", style = AppTextStyles.transactionSwipeAction)
                        }
                        Text(
                            "Shortcut for one-tap manual entry",
                            style = AppTextStyles.transactionSwipeAction,
                            color = AppColors.border,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = AeonTheme.dimensions.spacingXs)
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
        Text(label, style = AppTextStyles.transactionSwipeAction, color = AppColors.border)
        Text(value, style = AppTextStyles.caption, maxLines = 1)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SmartSuggestionsScreenPreview() {
    val patterns = listOf(
        DiscoveredPattern(
            name = "Netflix Subscription",
            payee = "Netflix",
            categoryId = 1,
            accountId = "Savings",
            averageAmount = BigDecimal("-15.99"),
            currency = "USD",
            transactionType = TransactionType.EXPENSE,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            note = "Monthly plan",
            tags = setOf("subscription", "streaming"),
            warrantyExpiration = null,
            recurrenceRule = RecurrenceRule(RecurrenceType.MONTHLY),
            confidence = 0.95f,
            occurrences = 5,
            lastDate = System.currentTimeMillis()
        ),
        DiscoveredPattern(
            name = "Weekly Groceries",
            payee = "Whole Foods",
            categoryId = 2,
            accountId = "Checking",
            averageAmount = BigDecimal("-85.40"),
            currency = "USD",
            transactionType = TransactionType.EXPENSE,
            paymentMethod = PaymentMethod.DEBIT_CARD,
            note = "",
            tags = emptySet<String>(),
            warrantyExpiration = null,
            recurrenceRule = RecurrenceRule(RecurrenceType.WEEKLY),
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
