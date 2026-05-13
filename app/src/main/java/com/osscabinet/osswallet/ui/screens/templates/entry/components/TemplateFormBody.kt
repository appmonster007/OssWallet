package com.osscabinet.ossWallet.ui.screens.templates.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryEvent
import com.osscabinet.ossWallet.ui.screens.templates.entry.TemplateEntryState

/**
 * Main content layout for template entry screen.
 */
@Composable
fun TemplateFormBody(
    state: TemplateEntryState,
    onEvent: (TemplateEntryEvent) -> Unit,
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
            TemplateHeaderCard(
                state = state,
                onEvent = onEvent
            )

            TemplateActionCards(
                state = state,
                onEvent = onEvent
            )

            TemplateDetailSection(
                state = state,
                onEvent = onEvent
            )
        }
    }
}
