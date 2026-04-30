package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryState
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionTagSection

/**
 * Main content layout for transaction entry screen.
 * Composed of focused, single-responsibility sub-components.
 */
@Composable
fun TransactionFormBody(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Type selector + Amount input
            TransactionHeaderCard(
                state = state,
                onEvent = onEvent
            )

            // Action cards row: Template, Account, Category, etc.
            TransactionActionCards(
                state = state,
                onEvent = onEvent
            )

            // Metadata: Note, Payee, Linked transactions, etc.
            TransactionTagSection(
                state = state,
                onEvent = onEvent
            )
        }

        // Bottom sheets and dialogs
        TransactionEntrySheets(
            state = state,
            onEvent = onEvent
        )
    }
}
