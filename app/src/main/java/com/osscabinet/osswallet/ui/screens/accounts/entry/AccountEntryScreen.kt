package com.osscabinet.ossWallet.ui.screens.accounts.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.accounts.entry.components.AccountFormBody

/**
 * A Value Object representing a valid configuration for the Account Entry screen.
 */
data class AccountEntryConfig(
    val accountId: String? = null
)

/**
 * Grouped actions for the Account Entry screen.
 */
data class AccountEntryActions(
    val onBackClick: () -> Unit,
    val onDone: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEntryScreen(
    config: AccountEntryConfig,
    actions: AccountEntryActions,
    viewModel: AccountEntryViewModel
) {
    LaunchedEffect(config.accountId) {
        viewModel.initialize(config.accountId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    // Show a loader until the account is available in edit mode
    if (state.isEditMode && state.existingAccount == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            actions.onDone()
        }
    }

    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Account" else "New Account",
        onBackClick = actions.onBackClick,
        onSaveClick = {
            viewModel.saveAccount {
                viewModel.onEvent(AccountEntryEvent.TriggerNavigation)
            }
        },
        canSave = state.canSave && !state.navTriggered,
        isEditMode = state.isEditMode
    ) { innerPadding ->
        AccountFormBody(
            state = state,
            onEvent = viewModel::onEvent,
            contentPadding = innerPadding,
            onDeleteClick = if (state.isEditMode) {
                {
                    viewModel.deleteAccount {
                        actions.onBackClick()
                    }
                }
            } else null
        )
    }
}
