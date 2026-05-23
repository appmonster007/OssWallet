package com.osscabinet.ossWallet.ui.screens.transactions.entry

import android.Manifest
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
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
    initialType: TransactionType? = null,
    initialTemplateId: Long? = null,
    prefillDto: com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO? = null,
    viewModel: TransactionEntryViewModel,
    onBackClick: () -> Unit,
    onDispose: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        viewModel.initialize(
            transactionId = transactionId,
            accountId = accountId,
            initialType = initialType ?: TransactionType.EXPENSE,
            initialTemplateId = initialTemplateId,
            prefillDto = prefillDto
        )
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(prefillDto) {
        if (prefillDto != null && (prefillDto != state.prefillDto)) {
            viewModel.onEvent(TransactionEntryEvent.ApplyPrefill(prefillDto))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearPrefillDto()
            onDispose()
        }
    }

    LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.onScreenResume()
    }

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            onBackClick()
        }
    }

    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Transaction" else "New Transaction",
        onBackClick = onBackClick,
        onSaveClick = {
            viewModel.saveTransaction {
                viewModel.onEvent(TransactionEntryEvent.TriggerNavigation)
            }
        },
        canSave = state.canSave && !state.navTriggered,
        isEditMode = state.isEditMode,
        onDeleteClick = if (state.isEditMode) {
            {
                viewModel.deleteTransaction { onBackClick() }
            }
        } else null
    ) { innerPadding ->
        if (!state.isEditMode && state.mapsAccessEnabled && state.gpsLocation == null) {
            LocationAutoFiller { coords ->
                viewModel.onEvent(TransactionEntryEvent.SetLocation(coords))
            }
        }

        TransactionFormBody(
            state = state,
            onEvent = viewModel::onEvent,
            onSearch = viewModel::searchTransactions,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LocationAutoFiller(
    onLocationDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (!hasAttempted) {
            if (locationPermissionState.status.isGranted) {
                hasAttempted = true
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            onLocationDetected("${it.latitude},${it.longitude}")
                        }
                    }
                } catch (_: SecurityException) {
                }
            } else {
                locationPermissionState.launchPermissionRequest()
            }
        }
    }
}
