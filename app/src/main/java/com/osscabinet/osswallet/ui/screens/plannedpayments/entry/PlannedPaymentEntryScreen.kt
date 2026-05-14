package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components.PlannedPaymentEntrySheets
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components.PlannedPaymentFormBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentEntryScreen(
    plannedPaymentId: Long? = null,
    prefill: com.osscabinet.ossWallet.domain.model.dto.PlannedPaymentPrefillDTO? = null,
    viewModel: PlannedPaymentEntryViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(plannedPaymentId, prefill) {
        viewModel.initialize(plannedPaymentId, prefill)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            onBackClick()
        }
    }

    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Payment" else "New Payment",
        onBackClick = onBackClick,
        onSaveClick = {
            viewModel.savePlannedPayment {
                viewModel.onEvent(PlannedPaymentEntryEvent.TriggerNavigation)
            }
        },
        canSave = state.canSave && !state.navTriggered,
        isEditMode = state.isEditMode,
        onDeleteClick = if (state.isEditMode) {
            { viewModel.onEvent(PlannedPaymentEntryEvent.ShowDeleteDialog(true)) }
        } else null
    ) { innerPadding ->
        PlannedPaymentFormBody(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(innerPadding)
        )

        PlannedPaymentEntrySheets(
            state = state,
            onEvent = viewModel::onEvent
        )

        AeonConfirmDeleteDialog(
            show = state.showDeleteDialog,
            title = "Delete Planned Payment",
            message = "Are you sure you want to delete this planned payment?",
            onConfirm = {
                viewModel.deletePlannedPayment {
                    onBackClick()
                }
            },
            onDismiss = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowDeleteDialog(false)) },
            extraContent = {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.deleteTemplateWithPayment,
                            onCheckedChange = { viewModel.onEvent(PlannedPaymentEntryEvent.SetDeleteTemplateWithPayment(it)) }
                        )
                        Text("Also delete linked template", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    if (state.deleteTemplateWithPayment && state.templateUsageCount > 1) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Warning: This template is shared with ${state.templateUsageCount - 1} other payment(s). Deleting it will also delete those payments.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
