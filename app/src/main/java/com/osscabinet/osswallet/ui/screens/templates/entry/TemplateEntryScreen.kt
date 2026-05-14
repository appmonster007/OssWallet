package com.osscabinet.ossWallet.ui.screens.templates.entry

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.screens.templates.entry.components.TemplateEntrySheets
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.screens.templates.entry.components.TemplateFormBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEntryScreen(
    templateId: Long? = null,
    prefill: com.osscabinet.ossWallet.domain.model.dto.TemplatePrefillDTO? = null,
    viewModel: TemplateEntryViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(templateId, prefill) {
        viewModel.initialize(templateId, prefill)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navTriggered) {
        if (state.navTriggered) {
            onBackClick()
        }
    }

    AeonEntryScaffold(
        title = if (state.templateId == null) "New Template" else "Edit Template",
        onBackClick = onBackClick,
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
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(innerPadding)
        )

        TemplateEntrySheets(
            state = state,
            onEvent = viewModel::onEvent
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
