package com.osscabinet.ossWallet.ui.screens.templates.entry

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.templates.entry.components.TemplateFormBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEntryScreen(
    viewModel: TemplateEntryViewModel,
    config: TemplateEntryConfig,
    actions: TemplateEntryActions
) {
    LaunchedEffect(config.templateId, config.prefill) {
        viewModel.initialize(config.templateId, config.prefill)
    }

    DisposableEffect(Unit) {
        onDispose {
            actions.onDispose()
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            actions.onBackClick()
        }
    }

    AeonEntryScaffold(
        title = if (state.templateId == null) "New Template" else "Edit Template",
        onBackClick = actions.onBackClick,
        onSaveClick = {
            viewModel.saveTemplate {
                viewModel.onEvent(TemplateEntryEvent.TriggerNavigation)
            }
        },
        canSave = state.canSave && !state.navTriggered,
        isEditMode = state.isEditMode,
        onDeleteClick = if (state.isEditMode) {
            { viewModel.onEvent(TemplateEntryEvent.ShowDeleteDialog(true)) }
        } else null
    ) { innerPadding ->
        TemplateFormBody(
            state = state,
            onSearch = viewModel::searchTransactions,
            onEvent = viewModel::onEvent,
            contentPadding = innerPadding
        )

        AeonConfirmDeleteDialog(
            show = state.showDeleteDialog,
            title = "Delete Template",
            message = if (state.isLinkedToPlannedPayment) {
                "This template is linked to one or more planned payments. Deleting it will also delete those planned payments. Are you sure?"
            } else {
                "Are you sure you want to delete this template?"
            },
            onConfirm = {
                viewModel.deleteTemplate()
            },
            onDismiss = { viewModel.onEvent(TemplateEntryEvent.ShowDeleteDialog(false)) }
        )
    }
}

data class TemplateEntryConfig(
    val templateId: Long? = null,
    val prefill: com.osscabinet.ossWallet.domain.model.dto.TemplatePrefillDTO? = null
)

data class TemplateEntryActions(
    val onBackClick: () -> Unit,
    val onDispose: () -> Unit = {}
)
