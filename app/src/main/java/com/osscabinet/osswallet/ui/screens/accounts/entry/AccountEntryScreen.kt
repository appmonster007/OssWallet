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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.accounts.entry.components.AccountFormBody
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * A Value Object representing a valid configuration for the Account Entry screen.
 */
data class AccountEntryConfig(
    val accountId: String? = null,
    val prefillName: String? = null,
    val prefillCurrency: String? = null,
    val isDraftMode: Boolean = false
)

/**
 * Grouped actions for the Account Entry screen.
 */
data class AccountEntryActions(
    val onBackClick: () -> Unit,
    val onDone: (result: AccountEntryResult) -> Unit
)

/** The outcome of tapping Save on the Account Entry screen. */
sealed class AccountEntryResult {
    /** The account was written to the database; [accountId] is its id. */
    data class Persisted(val accountId: String) : AccountEntryResult()

    /** Draft mode suppressed the write — nothing was persisted, the caller owns when
     *  (and whether) [draft] is actually created. */
    data class Draft(val draft: AccountEntryDraft) : AccountEntryResult()
}

/** A fully-configured, not-yet-persisted account, as captured from the entry form. */
@Serializable
data class AccountEntryDraft(
    val name: String,
    val structureType: String, // AccountStructureType.name
    val primaryCurrency: String,
    val allowedCurrencies: List<String>,
    val initialBalances: Map<String, String>,
    val iconName: String,
    val colorHex: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEntryScreen(
    config: AccountEntryConfig,
    actions: AccountEntryActions,
    viewModel: AccountEntryViewModel
) {
    LaunchedEffect(config.accountId) {
        viewModel.initialize(
            config.accountId,
            config.prefillName,
            config.prefillCurrency,
            config.isDraftMode
        )
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
            state.pendingResult?.let { actions.onDone(it) }
        }
    }

    AccountEntryContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBackClick = actions.onBackClick,
        onSaveClick = {
            viewModel.saveAccount { result ->
                viewModel.onEvent(AccountEntryEvent.TriggerNavigation(result))
            }
        },
        onDeleteClick = if (state.isEditMode) {
            {
                viewModel.deleteAccount {
                    actions.onBackClick()
                }
            }
        } else null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountEntryContent(
    state: AccountEntryState,
    onEvent: (AccountEntryEvent) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: (() -> Unit)?
) {
    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Account" else "New Account",
        onBackClick = onBackClick,
        onSaveClick = onSaveClick,
        canSave = state.canSave && !state.navTriggered,
        isEditMode = state.isEditMode
    ) { innerPadding ->
        AccountFormBody(
            state = state,
            onEvent = onEvent,
            contentPadding = innerPadding,
            onDeleteClick = onDeleteClick
        )
    }
}

@Preview(showBackground = true, heightDp = 1200, widthDp = 380)
@Composable
private fun AccountEntryScreenPreview() {
    OssWalletTheme {
        AccountEntryContent(
            state = AccountEntryState(
                accountName = "Savings Account",
                structureType = AccountStructureType.SAVINGS,
                allowedCurrencies = setOf("USD", "EUR", "GBP", "JPY"),
                shortlistedCurrencies = listOf(
                    "USD",
                    "EUR",
                    "GBP",
                    "INR",
                    "JPY",
                    "AUD",
                    "CAD",
                    "CHF",
                    "CNY",
                    "BRL"
                ),
                systemClosingBalances = mapOf(
                    "USD" to BigDecimal("1500.0498769876987698769876"),
                    "EUR" to BigDecimal("450.00"),
                    "GBP" to BigDecimal("120.00"),
                    "JPY" to BigDecimal("15000")
                )
            ),
            onEvent = {},
            onBackClick = {},
            onSaveClick = {},
            onDeleteClick = null
        )
    }
}
