package com.osscabinet.ossWallet.ui.screens.transactions.entry

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import android.Manifest
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.TransactionFormBody
import com.osscabinet.ossWallet.ui.screens.transactions.entry.TransactionEntryEvent

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
    initialTemplateId: Long? = null,
    prefillDto: com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO? = null,
    viewModel: TransactionEntryViewModel,
    onBackClick: () -> Unit
) {
    // ... [existing logic remains the same]
    LaunchedEffect(Unit) {
        viewModel.initialize(
            transactionId = transactionId,
            accountId = accountId,
            initialType = initialType,
            initialTemplateId = initialTemplateId,
            prefillDto = prefillDto
        )
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(prefillDto) {
        if (prefillDto != null && prefillDto != state.prefillDto) {
            viewModel.onEvent(TransactionEntryEvent.ApplyPrefill(prefillDto))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearPrefillDto()
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (state.isEditMode) "Edit Transaction" else "New Transaction",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
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
                        enabled = state.canSave && !state.navTriggered
                    ) {
                        Icon(
                            Icons.Rounded.Done,
                            contentDescription = "Save",
                            tint = if (state.canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (!state.isEditMode && state.mapsAccessEnabled && state.locationCoords == null) {
            LocationAutoFiller(
                onLocationDetected = { coords ->
                    viewModel.onEvent(TransactionEntryEvent.SetLocation(coords))
                }
            )
        }

        TransactionFormBody(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LocationAutoFiller(
    onLocationDetected: (Pair<Double, Double>) -> Unit
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
                            onLocationDetected(it.latitude to it.longitude)
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
