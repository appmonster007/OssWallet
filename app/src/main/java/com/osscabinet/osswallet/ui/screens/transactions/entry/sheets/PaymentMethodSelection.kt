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
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.AppIconMap

/**
 * Full-screen selection for Payment Methods.
 * Replaced PaymentMethodSheet for better immersion.
 */
@Composable
fun PaymentMethodSelection(
    selectedMethod: PaymentMethod,
    onMethodSelected: (PaymentMethod) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    
    val baseMethods = PaymentMethod.entries

    val filteredMethods = remember(query) {
        if (query.isBlank()) baseMethods
        else baseMethods.filter { it.displayName.contains(query, ignoreCase = true) || it.displayName.fuzzyMatch(query) }
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
                            selectedIds = setOf(selectedMethod.name),
                            onSelectionChange = { ids ->
                                ids.firstOrNull()?.let { 
                                    onMethodSelected(PaymentMethod.valueOf(it))
                                    onDismiss()
                                }
                            },
                            idMapper = { it.name },
                            labelMapper = { it.displayName },
                            iconMapper = { AppIconMap[it.iconName] ?: Icons.Rounded.Payments }
                        )
                    }
                }
            }
        }
    }
}
