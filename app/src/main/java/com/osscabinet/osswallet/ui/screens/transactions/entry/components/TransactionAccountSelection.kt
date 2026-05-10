package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.AeonSearchField

@Composable
fun TransactionAccountSelection(
    accounts: List<Account>,
    selectedAccountId: String?,
    onAccountSelected: (Account?) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Account",
    allowOutsideWallet: Boolean = false
) {
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    var query by remember { mutableStateOf("") }
    
    val filteredAccounts = remember(query, accounts) {
        if (query.isBlank()) accounts
        else accounts.filter { it.name.contains(query, ignoreCase = true) }
    }
    
    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = title,
            onBack = onDismiss
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
                AeonSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search accounts…"
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f, fill = false)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        if (allowOutsideWallet && query.isBlank()) {
                            AeonHybridSelector(
                                items = listOf(null),
                                mode = AeonSelectMode.SINGLE,
                                selectedIds = if (selectedAccountId == null) setOf("null") else emptySet(),
                                onSelectionChange = { 
                                    onAccountSelected(null)
                                    onDismiss()
                                },
                                idMapper = { "null" },
                                labelMapper = { "Outside of Wallet" },
                                detailMapper = { "External transfer" },
                                iconMapper = { Icons.Rounded.Public },
                                colorMapper = { tertiaryColor }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (filteredAccounts.isEmpty() && query.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Text(
                                    text = "No accounts match \"$query\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            AeonHybridSelector(
                                items = filteredAccounts,
                                mode = AeonSelectMode.SINGLE,
                                selectedIds = if (selectedAccountId != null) setOf(selectedAccountId) else emptySet(),
                                onSelectionChange = { ids ->
                                    ids.firstOrNull()?.let { id ->
                                        onAccountSelected(accounts.find { it.id == id })
                                        onDismiss()
                                    }
                                },
                                idMapper = { it.id },
                                labelMapper = { it.name },
                                detailMapper = { it.currencyCode },
                                iconMapper = { getAccountIconByName(it.icon) },
                                colorMapper = { Color(android.graphics.Color.parseColor(it.colorHex)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
