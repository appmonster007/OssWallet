package com.osscabinet.ossWallet.ui.screens.transactions.entry

import android.Manifest
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import com.osscabinet.ossWallet.location.LastLocationProvider
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.ReceiptsSection
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionFormBody

/**
 * Main entry point for transaction entry screen.
 * Handles state orchestration, permission requests for location, and delete confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryScreen(
    viewModel: TransactionEntryViewModel,
    config: TransactionEntryConfig,
    actions: TransactionEntryActions
) {
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    // Initialize state on entry
    LaunchedEffect(Unit) {
        viewModel.initialize(
            transactionId = config.transactionId,
            accountId = config.accountId,
            initialType = config.initialType ?: EntryMode.EXPENSE,
            initialTemplateId = config.initialTemplateId,
            prefillDto = config.prefillDto
        )
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()

    // Cleanup when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearPrefillDto()
            actions.onDispose()
        }
    }

    // Handle screen resume for any periodic updates (e.g. settings changes)
    LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.onScreenResume()
    }

    // Navigation trigger from ViewModel
    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            actions.onBackClick()
        }
    }

    TransactionEntryContent(
        state = state,
        onEvent = viewModel::onEvent,
        onSearch = viewModel::searchTransactions,
        onBackClick = actions.onBackClick,
        onSaveClick = {
            viewModel.saveTransaction {
                viewModel.onEvent(TransactionEntryEvent.TriggerNavigation)
            }
        },
        onDeleteConfirm = {
            viewModel.deleteTransaction { actions.onBackClick() }
        },
        showDeleteConfirmation = showDeleteConfirmation,
        onShowDeleteConfirmation = { showDeleteConfirmation = it },
        extraContent = {
            ReceiptsSection(
                state = receipts,
                onPicked = viewModel::onReceiptPicked,
                onRemove = viewModel::removeReceipt,
                onDismissError = viewModel::clearReceiptError,
                readBytes = viewModel::readReceipt
            )
        }
    )
}

/**
 * Stateless version of the Transaction Entry Screen for easier testing and previews.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryContent(
    state: TransactionEntryState,
    onEvent: (TransactionEntryEvent) -> Unit,
    onSearch: (String) -> kotlinx.coroutines.flow.Flow<List<com.osscabinet.ossWallet.domain.model.Transaction>>,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
    showDeleteConfirmation: Boolean,
    onShowDeleteConfirmation: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    extraContent: @Composable () -> Unit = {}
) {
    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Transaction" else "New Transaction",
        onBackClick = onBackClick,
        onSaveClick = onSaveClick,
        canSave = state.canSave && !state.navTriggered,
        isEditMode = state.isEditMode,
        onDeleteClick = if (state.isEditMode) {
            {
                onShowDeleteConfirmation(true)
            }
        } else null,
        modifier = modifier
    ) { innerPadding ->
        // Trigger auto-location if enabled and not in edit mode
        if (!state.isEditMode && state.mapsAccessEnabled && state.gpsLocation == null) {
            LocationAutoFiller { coords ->
                onEvent(TransactionEntryEvent.SetLocation(coords))
            }
        }

        TransactionFormBody(
            state = state,
            onEvent = onEvent,
            onSearch = onSearch,
            contentPadding = innerPadding,
            extraContent = extraContent
        )
    }

    // Confirmation dialog for deletion
    AeonConfirmDeleteDialog(
        show = showDeleteConfirmation,
        title = "Delete Transaction?",
        message = "Are you sure you want to delete this transaction? This action cannot be undone.",
        onConfirm = {
            onShowDeleteConfirmation(false)
            onDeleteConfirm()
        },
        onDismiss = { onShowDeleteConfirmation(false) }
    )
}

/**
 * Configuration for initializing the Transaction Entry Screen.
 */
data class TransactionEntryConfig(
    val transactionId: Long? = null,
    val accountId: String? = null,
    val initialType: EntryMode? = null,
    val initialTemplateId: Long? = null,
    val prefillDto: TransactionPrefillDTO? = null
)

/**
 * Actions passed from the parent navigator.
 */
data class TransactionEntryActions(
    val onBackClick: () -> Unit,
    val onDispose: () -> Unit = {}
)

/**
 * Helper component that attempts to fetch current GPS coordinates once permissions are granted.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LocationAutoFiller(
    onLocationDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var hasAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (!hasAttempted) {
            if (locationPermissionState.status.isGranted) {
                hasAttempted = true
                try {
                    LastLocationProvider.getLastLocation(context) { location ->
                        location?.let {
                            onLocationDetected("${it.latitude},${it.longitude}")
                        }
                    }
                } catch (_: SecurityException) {
                    // Handle case where permission is revoked at runtime
                }
            } else {
                locationPermissionState.launchPermissionRequest()
            }
        }
    }
}
