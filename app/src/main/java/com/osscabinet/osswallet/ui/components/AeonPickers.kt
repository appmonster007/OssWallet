package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Tag

/**
 * Full-screen selection for Labels.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabelSelection(
    selectedTags: List<String>,
    onTagsChanged: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    allTags: List<Tag>,
    onAddTag: (String) -> Unit
) {
    var tagInput by remember { mutableStateOf("") }
    val sortedTags = remember(allTags) { allTags.sortedBy { it.name.lowercase() } }

    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = "Labels",
            onBack = onDismiss
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Current Tags
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    selectedTags.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = { onTagsChanged(selectedTags - tag) },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                if (selectedTags.isNotEmpty()) Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add custom label (multi-word)") },
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (tagInput.isNotBlank()) {
                                    val newTag = tagInput.trim()
                                    if (newTag !in allTags.map { it.name }) {
                                        onAddTag(newTag)
                                    }
                                    onTagsChanged((selectedTags + newTag).distinct())
                                    tagInput = ""
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add")
                        }
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "SUGGESTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    sortedTags.filter { it.name !in selectedTags }.forEach { tag ->
                        SuggestionChip(
                            onClick = { onTagsChanged((selectedTags + tag.name).distinct()) },
                            label = { Text(tag.name) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-screen selection for Payment Methods.
 */
@Composable
fun PaymentMethodSelection(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val methods = listOf(
        "Cash" to Icons.Rounded.Payments,
        "Credit Card" to Icons.Rounded.CreditCard,
        "Debit Card" to Icons.Rounded.CreditCard,
        "Bank Transfer" to Icons.Rounded.AccountBalance,
        "Digital Wallet" to Icons.Rounded.Wallet,
        "Mobile Payment" to Icons.Rounded.Contactless,
        "Check" to Icons.Rounded.Description,
        "Cryptocurrency" to Icons.Rounded.CurrencyBitcoin,
        "Other" to Icons.Rounded.MoreHoriz
    )

    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = "Payment Method",
            onBack = onDismiss
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(24.dp)) {
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        AeonHybridSelector(
                            items = methods,
                            mode = AeonSelectMode.SINGLE,
                            selectedIds = setOf(selectedMethod),
                            onSelectionChange = { ids ->
                                ids.firstOrNull()?.let { 
                                    onMethodSelected(it)
                                    onDismiss()
                                }
                            },
                            idMapper = { it.first },
                            labelMapper = { it.first },
                            iconMapper = { it.second }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-screen selection for Categories.
 */
@Composable
fun CategorySelection(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = "Classification",
            onBack = onDismiss
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(24.dp)) {
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        AeonHierarchicalHybridSelector(
                            items = categories,
                            mode = AeonSelectMode.SINGLE,
                            selectedIds = if (selectedCategoryId != null) setOf(selectedCategoryId) else emptySet(),
                            onSelectionChange = { ids ->
                                val selectedId = ids.firstOrNull()
                                if (selectedId != null) {
                                    val category = categories.find { it.id == selectedId }
                                    if (category != null) {
                                        onCategorySelected(category)
                                        onDismiss()
                                    }
                                }
                            },
                            idMapper = { it.id },
                            labelMapper = { it.name },
                            parentIdMapper = { it.parentId },
                            iconMapper = { AccountIconMap[it.iconName] },
                            colorMapper = { if (it.colorHex == -1) Color(0xFF9E9E9E) else Color(it.colorHex) }
                        )
                    }
                }
            }
        }
    }
}
