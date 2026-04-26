package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.data.local.entity.TagEntity
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.viewmodel.TagViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLabelsScreen(
    viewModel: TagViewModel,
    onBackClick: () -> Unit
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var tagToDelete by remember { mutableStateOf<TagEntity?>(null) }

    AeonConfirmDeleteDialog(
        show = tagToDelete != null,
        title = "Delete Label",
        message = "Are you sure you want to delete the label \"${tagToDelete?.name}\"? Transactions using this label will not be deleted, but the label will be removed from them.",
        onConfirm = {
            tagToDelete?.let { viewModel.deleteTag(it) }
            tagToDelete = null
        },
        onDismiss = { tagToDelete = null }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Labels", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Label")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AeonSectionHeader(title = "Your Labels")
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(tags.sortedBy { it.name.lowercase() }) { tag ->
                LabelItem(
                    tag = tag,
                    onDelete = { tagToDelete = tag }
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Label") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("Label Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            viewModel.addTag(newTagName.trim())
                            newTagName = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LabelItem(tag: TagEntity, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                tag.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
