package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonHierarchicalHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * A generic full-screen selection sheet that provides a unified search and selection experience.
 * Used as a base for Account, Category, and other entity selections.
 */
@Composable
fun <T, ID : Any> AeonSelectionSheet(
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
    var query by rememberSaveable { mutableStateOf("") }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val filteredItems = remember(query, items, labelMapper) {
        if (query.isBlank()) items
        else items.filter { labelMapper(it).contains(query, ignoreCase = true) }
    }

    val filteredIds = remember(filteredItems, idMapper) { filteredItems.map(idMapper).toSet() }
    val dims = AeonTheme.dimensions

    val content = @Composable {
        AeonSelectionScreen(
            title = title,
            onBack = onDismiss,
            actions = {
                if (mode == AeonSelectMode.MULTI && filteredIds.isNotEmpty()) {
                    AppBarIconButton(
                        icon = Icons.Rounded.SelectAll,
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onSelectionChange(selectedIds + filteredIds)
                        }
                    )
                    AppBarIconButton(
                        icon = Icons.Rounded.Deselect,
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onSelectionChange(selectedIds - filteredIds)
                        }
                    )
                    AppBarIconButton(
                        icon = Icons.Rounded.Flip,
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            val newSelection = selectedIds.toMutableSet()
                            filteredIds.forEach { id ->
                                if (id in newSelection) newSelection.remove(id)
                                else newSelection.add(id)
                            }
                            onSelectionChange(newSelection)
                        }
                    )
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

                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(dims.spacingSm)
                    ) {
                        if (filteredItems.isEmpty() && query.isNotBlank()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(dims.spacing2xl),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emptyMessage ?: "No matches found for \"$query\"",
                                        style = AppTextStyles.listItemTitle,
                                        color = AppColors.contentSecondary
                                    )
                                }
                            }
                        } else {
                            if (isHierarchical && parentIdMapper != null && query.isBlank()) {
                                item(key = "hierarchy_root") {
                                    AeonHierarchicalHybridSelector(
                                        items = items,
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
                                }
                            } else {
                                items(filteredItems, key = { idMapper(it) }) { item ->
                                    AeonHybridSelector(
                                        items = listOf(item),
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
                }

                footerContent?.invoke(this)
            }
        }
    }

    if (androidx.compose.ui.platform.LocalInspectionMode.current) {
        content()
    } else {
        AeonFullScreenDialog(onDismissRequest = onDismiss) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonSelectionSheetPreview() {
    OssWalletTheme {
        AeonSelectionSheet(
            title = "Select Item",
            onDismiss = {},
            items = listOf("Apple", "Banana", "Cherry"),
            selectedIds = setOf("Banana"),
            onSelectionChange = {},
            idMapper = { it },
            labelMapper = { it },
            mode = AeonSelectMode.MULTI
        )
    }
}
