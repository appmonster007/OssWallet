package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.entry.EntrySelectionCard
import com.osscabinet.ossWallet.ui.components.entry.EntrySelectionMode

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
fun TagSelection(
    tags: List<Tag>,
    selectedTags: List<String>,
    onTagsSelected: (List<String>) -> Unit,
    onAddTag: (String, Int) -> Unit,
    onEditTag: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onDismiss: () -> Unit
) {
    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        TagSelectionContent(
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
fun TagSelectionContent(
    tags: List<Tag>,
    selectedTags: List<String>,
    onTagsSelected: (List<String>) -> Unit,
    onAddTag: (String, Int) -> Unit,
    onEditTag: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Transaction Tags",
    isSelectionModeEnabled: Boolean = true
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentSelection by remember { mutableStateOf(selectedTags.toSet()) }
    var isManagementMode by remember { mutableStateOf(!isSelectionModeEnabled) }
    
    // Track newly added tags locally for immediate feedback
    var localTags by remember { mutableStateOf(emptyList<Tag>()) }
    val allTags = remember(tags, localTags) {
        (tags + localTags).distinctBy { it.name }.sortedBy { it.name }
    }
    
    val filteredTags = remember(allTags, searchQuery) {
        if (searchQuery.isBlank()) allTags
        else allTags.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val exactMatchExists = remember(allTags, searchQuery) {
        val trimmed = searchQuery.trim()
        trimmed.isNotEmpty() && allTags.any { it.name.equals(trimmed, ignoreCase = true) }
    }

    AeonSelectionScreen(
        title = title,
        onBack = onDismiss,
        actions = {
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
                .padding(horizontal = 24.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentSelection.forEach { tagName ->
                            val tagColor = allTags.find { it.name == tagName }?.colorHex?.let { Color(it) } 
                                ?: MaterialTheme.colorScheme.primary
                            
                            InputChip(
                                selected = true,
                                onClick = { currentSelection = currentSelection - tagName },
                                label = { Text(tagName, style = MaterialTheme.typography.labelLarge) },
                                trailingIcon = { Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(12.dp),
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = tagColor.copy(alpha = 0.12f),
                                    selectedLabelColor = tagColor,
                                    selectedTrailingIconColor = tagColor
                                ),
                                border = InputChipDefaults.inputChipBorder(
                                    selected = true,
                                    enabled = true,
                                    selectedBorderColor = tagColor.copy(alpha = 0.4f),
                                    selectedBorderWidth = 1.dp
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Search and Create Row
            var selectedColor by remember { mutableStateOf(TagPresetColors.first()) }
            
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Search, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search or create...", style = MaterialTheme.typography.bodyLarge) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    
                    // Creation Dropdown (only if no exact match)
                    AnimatedVisibility(visible = searchQuery.isNotBlank() && !exactMatchExists) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                            
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(selectedColor, CircleShape)
                                            .border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
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
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("CREATE", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(TagPresetColors) { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(color, CircleShape)
                                                .clickable { selectedColor = color }
                                                .border(
                                                    width = if (selectedColor == color) 2.5.dp else 0.dp,
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

            Spacer(modifier = Modifier.height(24.dp))

            // Tags List
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { onEditTag(tag) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Edit, 
                                                null, 
                                                tint = MaterialTheme.colorScheme.primary, 
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { 
                                                onDeleteTag(tag)
                                                localTags = localTags.filter { it.name != tag.name }
                                                currentSelection = currentSelection - tag.name
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.DeleteOutline, 
                                                null, 
                                                tint = MaterialTheme.colorScheme.error, 
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                } else if (isSelected) {
                                    Icon(
                                        Icons.Rounded.CheckCircle, 
                                        null, 
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    // Empty spacer to maintain layout
                                    Spacer(modifier = Modifier.size(24.dp))
                                }
                            }
                        )
                    }
                    
                    if (filteredTags.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Label, 
                                    null, 
                                    tint = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (searchQuery.isBlank()) "No tags found" else "No match for \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    
                    // Bottom padding for scroll
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (isSelectionModeEnabled && !isManagementMode) {
                Button(
                    onClick = {
                        onTagsSelected(currentSelection.toList())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
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
