package com.osscabinet.ossWallet.ui.screens.accounts.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEntryScreen(
    accountId: String? = null,
    viewModel: AccountEntryViewModel,
    onBackClick: () -> Unit,
    onDone: () -> Unit
) {
    LaunchedEffect(accountId) {
        viewModel.initialize(accountId)
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
            onDone()
        }
    }

    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Account" else "New Account",
        onBackClick = onBackClick,
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
            modifier = Modifier.padding(innerPadding),
            onDeleteClick = if (state.isEditMode) {
                {
                    viewModel.deleteAccount {
                        onBackClick()
                    }
                }
            } else null
        )
    }
}
