package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components.PlannedPaymentFormBody
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

/**
 * A Value Object representing a valid configuration for the Planned Payment Entry screen.
 */
data class PlannedPaymentEntryConfig(
    val plannedPaymentId: Long? = null,
    val prefill: com.osscabinet.ossWallet.domain.model.dto.PlannedPaymentPrefillDTO? = null
)

/**
 * Grouped actions for the Planned Payment Entry screen.
 */
data class PlannedPaymentEntryActions(
    val onBackClick: () -> Unit,
    val onDispose: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentEntryScreen(
    config: PlannedPaymentEntryConfig,
    actions: PlannedPaymentEntryActions,
    viewModel: PlannedPaymentEntryViewModel
) {
    LaunchedEffect(config.plannedPaymentId, config.prefill) {
        viewModel.initialize(config.plannedPaymentId, config.prefill)
    }

    DisposableEffect(Unit) {
        onDispose {
            actions.onDispose()
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            viewModel.onScreenResume()
            actions.onBackClick()
        }
    }

    AeonEntryScaffold(
        title = if (state.isEditMode) "Edit Payment" else "New Payment",
        onBackClick = actions.onBackClick,
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
            onSearch = viewModel::searchTransactions,
            onEvent = viewModel::onEvent,
            contentPadding = innerPadding
        )

        AeonConfirmDeleteDialog(
            show = state.showDeleteDialog,
            title = "Delete Planned Payment",
            message = "Are you sure you want to delete this planned payment?",
            onConfirm = {
                viewModel.deletePlannedPayment()
            },
            onDismiss = { viewModel.onEvent(PlannedPaymentEntryEvent.ShowDeleteDialog(false)) },
            extraContent = {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.deleteTemplateWithPayment,
                            onCheckedChange = {
                                viewModel.onEvent(
                                    PlannedPaymentEntryEvent.SetDeleteTemplateWithPayment(
                                        it
                                    )
                                )
                            }
                        )
                        Text("Also delete linked template", style = AppTextStyles.transactionTitle)
                    }

                    if (state.deleteTemplateWithPayment && state.templateUsageCount > 1) {
                        Surface(
                            color = AppColors.errorContainer.copy(alpha = 0.5f),
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
                                    tint = AppColors.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Warning: This template is shared with ${state.templateUsageCount - 1} other payment(s). Deleting it will also delete those payments.",
                                    style = AppTextStyles.transactionSwipeAction,
                                    color = AppColors.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
