package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.AeonSearchField

val TagPresetColors = listOf(
    Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFFAB47BC), Color(0xFF7E57C2),
    Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF29B6F6), Color(0xFF26C6DA),
    Color(0xFF26A69A), Color(0xFF66BB6A), Color(0xFF9CCC65), Color(0xFFD4E157),
    Color(0xFFFFEE58), Color(0xFFFFCA28), Color(0xFFFFA726), Color(0xFFFF7043)
)

/**
 * Full-screen selection for Transaction Tags.
 */
@Composable
fun AeonTagSelection(
    tags: List<Tag>,
    selectedTags: Set<String>,
    onTagsSelected: (Set<String>) -> Unit,
    onAddTag: (String, Int) -> Unit,
    onEditTag: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onDismiss: () -> Unit
) {
    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonTagSelectionContent(
            tags = tags,
            selectedTags = selectedTags,
            onTagsSelected = onTagsSelected,
            onAddTag = onAddTag,
            onEditTag = onEditTag,
            onDeleteTag = onDeleteTag,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AeonTagSelectionContent(
    tags: List<Tag>,
    selectedTags: Set<String>,
    onTagsSelected: (Set<String>) -> Unit,
    onAddTag: (String, Int) -> Unit,
    onEditTag: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Transaction Tags",
    isSelectionModeEnabled: Boolean = true
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentSelection by remember { mutableStateOf(selectedTags) }
    var isManagementMode by remember { mutableStateOf(!isSelectionModeEnabled) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val dims = AeonTheme.dimensions
    
    // Track newly added tags locally for immediate feedback
    var localTags by remember { mutableStateOf(emptyList<Tag>()) }
    val allTags = remember(tags, localTags) {
        (tags + localTags).distinctBy { it.name }.sortedBy { it.name }
    }
    
    val filteredTags = remember(allTags, searchQuery) {
        if (searchQuery.isBlank()) allTags
        else allTags.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val filteredNames = remember(filteredTags) { filteredTags.map { it.name }.toSet() }

    val exactMatchExists = remember(allTags, searchQuery) {
        val trimmed = searchQuery.trim()
        trimmed.isNotEmpty() && allTags.any { it.name.equals(trimmed, ignoreCase = true) }
    }

    AeonSelectionScreen(
        title = title,
        onBack = onDismiss,
        actions = {
            if (isSelectionModeEnabled && !isManagementMode && filteredNames.isNotEmpty()) {
                IconButton(onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    currentSelection = currentSelection + filteredNames
                }) {
                    Icon(Icons.Rounded.SelectAll, contentDescription = "Select All")
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    currentSelection = currentSelection - filteredNames
                }) {
                    Icon(Icons.Rounded.Deselect, contentDescription = "Deselect All")
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    val newSelection = currentSelection.toMutableSet()
                    filteredNames.forEach { name ->
                        if (name in newSelection) newSelection.remove(name)
                        else newSelection.add(name)
                    }
                    currentSelection = newSelection
                }) {
                    Icon(Icons.Rounded.Flip, contentDescription = "Invert Selection")
                }
            }

            if (isSelectionModeEnabled) {
                IconButton(onClick = { isManagementMode = !isManagementMode }) {
                    Icon(
                        if (isManagementMode) Icons.Rounded.Check else Icons.Rounded.Edit,
                        contentDescription = if (isManagementMode) "Done" else "Edit Tags",
                        tint = if (isManagementMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = dims.spacingXl)
        ) {
            // Selected Tags Area
            AnimatedVisibility(
                visible = isSelectionModeEnabled && currentSelection.isNotEmpty() && !isManagementMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dims.spacingSm),
                        verticalArrangement = Arrangement.spacedBy(dims.spacingSm)
                    ) {
                        currentSelection.forEach { tagName ->
                            val tagColor = allTags.find { it.name == tagName }?.colorHex?.let { Color(it) } 
                                ?: MaterialTheme.colorScheme.primary
                            
                            InputChip(
                                selected = true,
                                onClick = { currentSelection = currentSelection - tagName },
                                label = { Text(tagName, style = MaterialTheme.typography.labelLarge) },
                                trailingIcon = { Icon(Icons.Rounded.Close, null, modifier = Modifier.size(dims.iconSizeXs + 4.dp)) },
                                shape = RoundedCornerShape(dims.spacingMd),
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = tagColor.copy(alpha = 0.12f),
                                    selectedLabelColor = tagColor,
                                    selectedTrailingIconColor = tagColor
                                ),
                                border = InputChipDefaults.inputChipBorder(
                                    selected = true,
                                    enabled = true,
                                    selectedBorderColor = tagColor.copy(alpha = 0.4f),
                                    selectedBorderWidth = (dims.spacingXs / 4)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(dims.containerCornerRadius))
                }
            }

            // Search and Create Row
            var selectedColor by remember { mutableStateOf(TagPresetColors.first()) }
            
            Surface(
                shape = RoundedCornerShape(dims.spacingXl),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke((dims.spacingXs / 4), MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column {
                    AeonSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search or create...",
                        showBorder = false
                    )
                    
                    // Creation Dropdown (only if no exact match)
                    AnimatedVisibility(visible = searchQuery.isNotBlank() && !exactMatchExists) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = dims.spacingLg),
                                thickness = (dims.spacingXs / 8),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                            
                            Column(modifier = Modifier.padding(dims.spacingLg)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(dims.spacingMd)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(dims.spacingXl)
                                            .background(selectedColor, CircleShape)
                                            .border((dims.spacingXs / 4), Color.Black.copy(alpha = 0.1f), CircleShape)
                                    )
                                    Text(
                                        "Create \"${searchQuery.trim()}\"",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )
                                    Button(
                                        onClick = {
                                            val newTag = Tag(0, searchQuery.trim(), selectedColor.toArgb())
                                            localTags = localTags + newTag
                                            if (isSelectionModeEnabled) {
                                                currentSelection = currentSelection + newTag.name
                                            }
                                            onAddTag(newTag.name, newTag.colorHex)
                                            searchQuery = ""
                                        },
                                        shape = RoundedCornerShape(dims.spacingMd),
                                        contentPadding = PaddingValues(horizontal = dims.spacingLg, vertical = 0.dp),
                                        modifier = Modifier.height(dims.spacingXl + dims.spacingMd)
                                    ) {
                                        Text("CREATE", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(dims.spacingLg))
                                
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(dims.spacingMd)
                                ) {
                                    items(TagPresetColors) { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(dims.spacing2xl)
                                                .background(color, CircleShape)
                                                .clickable { selectedColor = color }
                                                .border(
                                                    width = if (selectedColor == color) (dims.spacingXs * 0.625f) else 0.dp,
                                                    color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dims.spacingXl))

            // Tags List
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(dims.spacingSm)
                ) {
                    filteredTags.forEach { tag ->
                        val isSelected = isSelectionModeEnabled && tag.name in currentSelection
                        
                        EntrySelectionCard(
                            icon = Icons.AutoMirrored.Rounded.Label,
                            label = tag.name,
                            isSelected = isSelected,
                            iconColor = Color(tag.colorHex),
                            selectionMode = if (isManagementMode) EntrySelectionMode.MANAGEMENT else EntrySelectionMode.SINGLE,
                            onClick = {
                                if (isManagementMode) {
                                    onDeleteTag(tag)
                                    localTags = localTags.filter { it.name != tag.name }
                                    currentSelection = currentSelection - tag.name
                                } else if (isSelectionModeEnabled) {
                                    currentSelection = if (isSelected) currentSelection - tag.name else currentSelection + tag.name
                                }
                            },
                            trailingContent = {
                                if (isManagementMode) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(dims.spacingXs)) {
                                        IconButton(
                                            onClick = { onEditTag(tag) },
                                            modifier = Modifier.size(dims.spacing2xl)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Edit, 
                                                null, 
                                                tint = MaterialTheme.colorScheme.primary, 
                                                modifier = Modifier.size(dims.containerCornerRadius)
                                            )
                                        }
                                        IconButton(
                                            onClick = { 
                                                onDeleteTag(tag)
                                                localTags = localTags.filter { it.name != tag.name }
                                                currentSelection = currentSelection - tag.name
                                            },
                                            modifier = Modifier.size(dims.spacing2xl)
                                        ) {
                                            Icon(
                                                Icons.Rounded.DeleteOutline, 
                                                null, 
                                                tint = MaterialTheme.colorScheme.error, 
                                                modifier = Modifier.size(dims.containerCornerRadius)
                                            )
                                        }
                                    }
                                } else if (isSelected) {
                                    Icon(
                                        Icons.Rounded.CheckCircle, 
                                        null, 
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(dims.spacingXl)
                                    )
                                } else {
                                    // Empty spacer to maintain layout
                                    Spacer(modifier = Modifier.size(dims.spacingXl))
                                }
                            }
                        )
                    }
                    
                    if (filteredTags.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = dims.spacing3xl),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Label, 
                                    null, 
                                    tint = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(dims.iconSize2xl)
                                )
                                Spacer(modifier = Modifier.height(dims.spacingSm))
                                Text(
                                    if (searchQuery.isBlank()) "No tags found" else "No match for \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    
                    // Bottom padding for scroll
                    Spacer(modifier = Modifier.height(dims.spacingXl))
                }
            }

            if (isSelectionModeEnabled && !isManagementMode) {
                Button(
                    onClick = {
                        onTagsSelected(currentSelection)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dims.iconSize3xl)
                        .padding(bottom = dims.spacingSm),
                    shape = RoundedCornerShape(dims.spacingXl),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = dims.spacingXs / 2)
                ) {
                    Text(
                        "Done (${currentSelection.size} selected)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}
