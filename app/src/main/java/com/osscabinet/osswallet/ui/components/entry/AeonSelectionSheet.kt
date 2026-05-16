package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors

/**
 * A generic full-screen selection sheet that provides a unified search and selection experience.
 * Used as a base for Account, Category, and other entity selections.
 */
@Composable
fun <T, ID> AeonSelectionSheet(
    title: String,
    onDismiss: () -> Unit,
    items: List<T>,
    selectedIds: Set<ID>,
    onSelectionChange: (Set<ID>) -> Unit,
    idMapper: (T) -> ID,
    labelMapper: (T) -> String,
    modifier: Modifier = Modifier,
    mode: AeonSelectMode = AeonSelectMode.SINGLE,
    iconMapper: (T) -> ImageVector? = { null },
    colorMapper: (T) -> Color? = { null },
    detailMapper: (T) -> String? = { null },
    searchPlaceholder: String = "Search...",
    emptyMessage: String? = null,
    isHierarchical: Boolean = false,
    parentIdMapper: ((T) -> ID?)? = null,
    noneLabel: String? = null,
    noneIcon: ImageVector = Icons.Rounded.Block,
    headerContent: @Composable (ColumnScope.() -> Unit)? = null,
    footerContent: @Composable (ColumnScope.() -> Unit)? = null,
    startExpanded: Boolean = true
) {
    var query by remember { mutableStateOf("") }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    val filteredItems = remember(query, items) {
        if (query.isBlank()) items
        else items.filter { labelMapper(it).contains(query, ignoreCase = true) }
    }

    val filteredIds = remember(filteredItems) { filteredItems.map(idMapper).toSet() }
    val dims = AeonTheme.dimensions

    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = title,
            onBack = onDismiss,
            actions = {
                if (mode == AeonSelectMode.MULTI && filteredIds.isNotEmpty()) {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onSelectionChange(selectedIds + filteredIds)
                    }) {
                        Icon(Icons.Rounded.SelectAll, contentDescription = "Select All")
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onSelectionChange(selectedIds - filteredIds)
                    }) {
                        Icon(Icons.Rounded.Deselect, contentDescription = "Deselect All")
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        val newSelection = selectedIds.toMutableSet()
                        filteredIds.forEach { id ->
                            if (id in newSelection) newSelection.remove(id)
                            else newSelection.add(id)
                        }
                        onSelectionChange(newSelection)
                    }) {
                        Icon(Icons.Rounded.Flip, contentDescription = "Invert Selection")
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = modifier
                    .padding(padding)
                    .padding(horizontal = dims.spacingXl),
                verticalArrangement = Arrangement.spacedBy(dims.spacingSm)
            ) {
                headerContent?.invoke(this)

                AeonSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = searchPlaceholder
                )
                
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(dims.spacingSm)
                    ) {
                        if (filteredItems.isEmpty() && query.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(dims.spacing2xl),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emptyMessage ?: "No matches found for \"$query\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AppColors.contentSecondary
                                )
                            }
                        } else {
                            if (isHierarchical && parentIdMapper != null) {
                                AeonHierarchicalHybridSelector(
                                    items = if (query.isBlank()) items else filteredItems,
                                    mode = mode,
                                    selectedIds = selectedIds,
                                    onSelectionChange = { ids ->
                                        onSelectionChange(ids)
                                        if (mode == AeonSelectMode.SINGLE) onDismiss()
                                    },
                                    idMapper = idMapper,
                                    labelMapper = labelMapper,
                                    parentIdMapper = parentIdMapper,
                                    iconMapper = iconMapper,
                                    colorMapper = colorMapper,
                                    noneLabel = noneLabel,
                                    noneIcon = noneIcon,
                                    startExpanded = startExpanded
                                )
                            } else {
                                AeonHybridSelector(
                                    items = filteredItems,
                                    mode = mode,
                                    selectedIds = selectedIds,
                                    onSelectionChange = { ids ->
                                        onSelectionChange(ids)
                                        if (mode == AeonSelectMode.SINGLE) onDismiss()
                                    },
                                    idMapper = idMapper,
                                    labelMapper = labelMapper,
                                    iconMapper = iconMapper,
                                    colorMapper = colorMapper,
                                    detailMapper = detailMapper
                                )
                            }
                        }
                    }
                }
                
                footerContent?.invoke(this)
            }
        }
    }
}
