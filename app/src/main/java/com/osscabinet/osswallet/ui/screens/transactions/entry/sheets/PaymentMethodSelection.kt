package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.domain.util.fuzzyMatch

/**
 * Full-screen selection for Payment Methods.
 * Replaced PaymentMethodSheet for better immersion.
 */
@Composable
fun PaymentMethodSelection(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    
    val baseMethods = listOf(
        "Cash" to Icons.Rounded.Payments,
        "Credit Card" to Icons.Rounded.CreditCard,
        "Debit Card" to Icons.Rounded.CreditCard,
        "Bank Transfer" to Icons.Rounded.AccountBalance,
        "Digital Wallet" to Icons.Rounded.Wallet,
        "Mobile Payment" to Icons.Rounded.Contactless,
        "Check" to Icons.Rounded.Description,
        "Cryptocurrency" to Icons.Rounded.CurrencyBitcoin,
        "Other" to Icons.Rounded.MoreHoriz
    )

    val filteredMethods = remember(query) {
        if (query.isBlank()) baseMethods
        else baseMethods.filter { it.first.contains(query, ignoreCase = true) || it.first.fuzzyMatch(query) }
    }

    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = "Payment Method",
            onBack = onDismiss
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Select how you paid for this transaction",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                AeonSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search methods..."
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.weight(1f, fill = false)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        AeonHybridSelector(
                            items = filteredMethods,
                            mode = AeonSelectMode.SINGLE,
                            selectedIds = setOf(selectedMethod),
                            onSelectionChange = { ids ->
                                ids.firstOrNull()?.let { 
                                    onMethodSelected(it)
                                    onDismiss()
                                }
                            },
                            idMapper = { it.first },
                            labelMapper = { it.first },
                            iconMapper = { it.second }
                        )
                    }
                }
            }
        }
    }
}
