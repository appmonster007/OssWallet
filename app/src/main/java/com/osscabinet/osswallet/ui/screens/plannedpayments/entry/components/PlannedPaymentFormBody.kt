package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryEvent
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryState

/**
 * Main content layout for planned payment entry screen.
 * Following the structure and component reuse of TransactionFormBody.
 */
@Composable
fun PlannedPaymentFormBody(
    state: PlannedPaymentEntryState,
    onEvent: (PlannedPaymentEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Type selector + Amount input + Title
            PlannedPaymentHeaderCard(
                state = state,
                onEvent = onEvent
            )

            // Action cards row: Template, Schedule, Account, Category
            PlannedPaymentActionCards(
                state = state,
                onEvent = onEvent
            )

            // Metadata: Note, Payee, Location, Warranty, Tags
            PlannedPaymentDetailSection(
                state = state,
                onEvent = onEvent
            )
        }

        // Bottom sheets and dialogs are handled in the Screen level for PlannedPayment
        // but we could also move them here if we wanted to match TransactionFormBody exactly.
        // For now, keeping them in Screen to avoid moving too much logic at once.
    }
}
