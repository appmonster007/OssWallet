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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flip
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
import com.osscabinet.ossWallet.ui.theme.*
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

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
        val trimmed = searchQuery.trim()
        if (trimmed.isBlank()) allTags
        else allTags.filter { it.name.contains(trimmed, ignoreCase = true) || it.name.fuzzyMatch(trimmed) }
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
                    Icon(Icons.Rounded.SelectAll, contentDescription = stringResource(R.string.action_select_all))
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    currentSelection = currentSelection - filteredNames
                }) {
                    Icon(Icons.Rounded.Deselect, contentDescription = stringResource(R.string.action_deselect_all))
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
                    Icon(Icons.Rounded.Flip, contentDescription = stringResource(R.string.action_invert_selection))
                }
            }

            IconButton(onClick = { isManagementMode = !isManagementMode }) {
                Icon(
                    if (isManagementMode) Icons.Rounded.Check else Icons.Rounded.Edit,
                    contentDescription = if (isManagementMode) stringResource(R.string.action_done) else stringResource(R.string.action_edit_tags),
                    tint = if (isManagementMode) AppColors.primary else AppColors.contentPrimary
                )
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
                            val tagColor = allTags.find { it.name.equals(tagName, ignoreCase = true) }?.colorHex?.let { Color(it) } 
                                ?: AppColors.primary
                            
                            InputChip(
                                selected = true,
                                onClick = { currentSelection = currentSelection - tagName },
                                label = { Text(tagName, style = AppTextStyles.drawerItemLabel) },
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
                color = AppColors.recessedSurface,
                border = BorderStroke((dims.spacingXs / 4), AppColors.divider.copy(alpha = 0.3f))
            ) {
                Column {
                    AeonSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = stringResource(R.string.tag_search_placeholder),
                        showBorder = false
                    )
                    
                    // Creation Dropdown (only if no exact match)
                    AnimatedVisibility(visible = searchQuery.isNotBlank() && !exactMatchExists) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = dims.spacingLg),
                                thickness = (dims.spacingXs / 8),
                                color = AppColors.divider.copy(alpha = 0.2f)
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
                                            .border((dims.spacingXs / 4), AppColors.divider.copy(alpha = 0.3f), CircleShape)
                                    )
                                    Text(
                                        stringResource(R.string.tag_create_new, searchQuery.trim()),
                                        style = AppTextStyles.listItemTitle,
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
                                        Text(stringResource(R.string.tag_create_action), style = AppTextStyles.listSectionHeader)
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
                                                    color = if (selectedColor == color) AppColors.primary else AppColors.transparent,
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
                        val isSelected = isSelectionModeEnabled && currentSelection.any { it.equals(tag.name, ignoreCase = true) }
                        
                        EntrySelectionCard(
                            icon = Icons.AutoMirrored.Rounded.Label,
                            label = tag.name,
                            isSelected = isSelected,
                            iconColor = Color(tag.colorHex),
                            selectionMode = if (isManagementMode) EntrySelectionMode.MANAGEMENT else EntrySelectionMode.SINGLE,
                            onClick = {
                                if (isManagementMode) {
                                    // In management mode, clicking selects for delete/edit? 
                                    // Usually we use the trailing icons.
                                } else if (isSelectionModeEnabled) {
                                    currentSelection = if (isSelected) {
                                        currentSelection.filter { !it.equals(tag.name, ignoreCase = true) }.toSet()
                                    } else {
                                        currentSelection + tag.name
                                    }
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
                                                tint = AppColors.primary, 
                                                modifier = Modifier.size(dims.containerCornerRadius)
                                            )
                                        }
                                        IconButton(
                                            onClick = { 
                                                onDeleteTag(tag)
                                            },
                                            modifier = Modifier.size(dims.spacing2xl)
                                        ) {
                                            Icon(
                                                Icons.Rounded.DeleteOutline, 
                                                null, 
                                                tint = AppColors.error, 
                                                modifier = Modifier.size(dims.containerCornerRadius)
                                            )
                                        }
                                    }
                                } else if (isSelected) {
                                    Icon(
                                        Icons.Rounded.CheckCircle, 
                                        null, 
                                        tint = AppColors.primary,
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
                                    tint = AppColors.divider,
                                    modifier = Modifier.size(dims.iconSize2xl)
                                )
                                Spacer(modifier = Modifier.height(dims.spacingSm))
                                Text(
                                    if (searchQuery.isBlank()) stringResource(R.string.tag_no_tags_found) else stringResource(R.string.tag_no_match_query, searchQuery),
                                    style = AppTextStyles.transactionTitle,
                                    color = AppColors.contentSecondary.copy(alpha = 0.6f)
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
                        containerColor = AppColors.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = dims.spacingXs / 2)
                ) {
                    Text(
                        pluralStringResource(R.plurals.tag_done_selected, currentSelection.size, currentSelection.size),
                        style = AppTextStyles.screenSubTitle.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}
