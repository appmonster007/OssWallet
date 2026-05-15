package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.ui.components.*

@Composable
fun AeonCurrencySelection(
    selectedCurrencies: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    mode: AeonSelectMode = AeonSelectMode.SINGLE,
    title: String = "Select Currency"
) {
    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = title,
            onBack = onDismiss
        ) { padding ->
            val allCurrencies = remember { CurrencyService.getAllCurrencies() }
            var query by remember { mutableStateOf("") }
            val filtered = remember(query) {
                if (query.isBlank()) allCurrencies
                else allCurrencies.filter { it.contains(query, ignoreCase = true) }
            }

            Column(modifier = Modifier.padding(padding).padding(horizontal = 24.dp)) {
                AeonSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search currency..."
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        AeonHybridSelector(
                            items = filtered,
                            mode = mode,
                            selectedIds = selectedCurrencies,
                            onSelectionChange = { ids ->
                                onSelectionChange(ids)
                                if (mode == AeonSelectMode.SINGLE) onDismiss()
                            },
                            idMapper = { it },
                            labelMapper = { it },
                            iconMapper = { Icons.Rounded.CurrencyExchange }
                        )
                    }
                }
            }
        }
    }
}
