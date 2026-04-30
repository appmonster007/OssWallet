package com.osscabinet.ossWallet.ui.screens.transactions.entry

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionFormBody

/**
 * Main entry point for transaction entry screen.
 * Uses dedicated TransactionEntryViewModel for state management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryScreen(
    transactionId: Long? = null,
    accountId: String? = null,
    initialType: Int = 0,
    initialTemplate: String? = null,
    prefillDto: com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO? = null,
    viewModel: TransactionEntryViewModel,
    onBackClick: () -> Unit
) {
    // Initialize ViewModel on first composition
    LaunchedEffect(Unit) {
        viewModel.initialize(
            transactionId = transactionId,
            accountId = accountId,
            initialType = initialType,
            initialTemplate = initialTemplate,
            prefillDto = prefillDto
        )
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    // Clear prefill DTO when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearPrefillDto()
        }
    }

    // Reset navigation trigger when returning to this screen
    LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.onScreenResume()
    }

    // Handle navigation
    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(if (state.isEditMode) "Edit Transaction" else "New Transaction")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (state.isEditMode) {
                        IconButton(
                            onClick = {
                                viewModel.deleteTransaction { onBackClick() }
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.saveTransaction { viewModel.onEvent(TransactionEntryEvent.TriggerNavigation) }
                        },
                        enabled = state.canSave
                    ) {
                        Icon(Icons.Rounded.Done, contentDescription = "Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        TransactionFormBody(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
