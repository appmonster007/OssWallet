package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import com.osscabinet.ossWallet.ui.components.AeonColorSelection
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.DefaultAccentColorHex
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlin.math.roundToInt
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AeonTagSelectionDialog(
    tags: List<Tag>,
    selectedTags: Set<String>,
    onTagsSelected: (Set<String>) -> Unit,
    onAddTag: (String, Int) -> Unit,
    onDismiss: () -> Unit,
    allowCreation: Boolean = true
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentSelection by remember { mutableStateOf(selectedTags) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Track newly added tags locally to ensure they remain searchable if deselected
    var localTags by remember { mutableStateOf(emptyList<Tag>()) }
    val allAvailableTags = remember(tags, localTags) {
        (tags + localTags).distinctBy { it.name }
    }

    val filteredTags = remember(allAvailableTags, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else allAvailableTags.filter { it.name.fuzzyMatch(searchQuery) }
    }

    val exactMatchExists = remember(allAvailableTags, searchQuery) {
        val trimmed = searchQuery.trim()
        trimmed.isNotEmpty() && allAvailableTags.any { it.name.equals(trimmed, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = AppColors.sheetSurfaceRaised,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select Tags",
                    style = AppTextStyles.calculatorKey,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.contentPrimary
                )

                var searchFieldHeight by remember { mutableIntStateOf(0) }
                val density = LocalDensity.current

                Box {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.onGloballyPositioned { 
                            searchFieldHeight = it.size.height 
                        }
                    ) {
                        AeonSearchField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "Search or create...",
                        )
                    }

                    // Fuzzy Search Results Dropdown (Using Popup for stability)
                    if (searchQuery.isNotBlank()) {
                        Popup(
                            alignment = Alignment.TopStart,
                            offset = IntOffset(0, searchFieldHeight + with(density) { 4.dp.roundToPx() })
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(0.82f), // Slightly narrower than dialog
                                shape = RoundedCornerShape(12.dp),
                                color = AppColors.componentOnCard,
                                tonalElevation = 8.dp,
                                border = BorderStroke(0.5.dp, AppColors.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    if (!exactMatchExists && allowCreation) {
                                        TextButton(
                                            onClick = { showCreateDialog = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(Icons.Rounded.Add, null, tint = AppColors.primary, modifier = Modifier.size(20.dp))
                                                Text("Create \"${searchQuery.trim()}\"", color = AppColors.primary, style = AppTextStyles.listItemTitle)
                                            }
                                        }
                                    }

                                    filteredTags.take(5).forEach { tag ->
                                        val isSelected = tag.name in currentSelection
                                        TextButton(
                                            onClick = {
                                                currentSelection = if (isSelected) currentSelection - tag.name else currentSelection + tag.name
                                                searchQuery = ""
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .background(Color(tag.colorHex), RoundedCornerShape(2.dp))
                                                )
                                                Text(
                                                    tag.name, 
                                                    color = if (isSelected) AppColors.primary else AppColors.contentPrimary,
                                                    style = AppTextStyles.listItemTitle,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isSelected) {
                                                    Icon(Icons.Rounded.Check, null, tint = AppColors.primary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Current Selection FlowRow
                if (currentSelection.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentSelection.forEach { tagName ->
                            val tagColor = allAvailableTags.find { it.name == tagName }?.colorHex?.let { Color(it) } 
                                ?: AppColors.primary
                            
                            InputChip(
                                selected = true,
                                onClick = { currentSelection = currentSelection - tagName },
                                label = { Text(tagName) },
                                trailingIcon = { Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(8.dp),
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = tagColor.copy(alpha = 0.12f),
                                    selectedLabelColor = tagColor,
                                    selectedTrailingIconColor = tagColor
                                ),
                                border = InputChipDefaults.inputChipBorder(
                                    selected = true,
                                    enabled = true,
                                    selectedBorderColor = tagColor.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onTagsSelected(currentSelection)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("DONE")
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AeonTagCreateDialog(
            initialName = searchQuery.trim(),
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, color ->
                onAddTag(name, color)
                localTags = localTags + Tag(name = name, colorHex = color)
                currentSelection = currentSelection + name
                searchQuery = ""
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun AeonTagCreateDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var accentColorHex by remember { mutableStateOf(DefaultAccentColorHex) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = AppColors.sheetSurfaceRaised,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Create New Tag",
                    style = AppTextStyles.calculatorKey,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.contentPrimary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("TAG NAME") },
                    placeholder = { Text("e.g. Shopping, Travel...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                AeonColorSelection(
                    accentColorHex = accentColorHex,
                    onColorSelected = { accentColorHex = it },
                    showTitle = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            if (name.isNotBlank()) {
                                val colorInt = try {
                                    android.graphics.Color.parseColor(accentColorHex)
                                } catch (_: Exception) {
                                    android.graphics.Color.parseColor(DefaultAccentColorHex)
                                }
                                onConfirm(name.trim(), colorInt)
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CREATE")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AeonTagSelectionDialogPreview() {
    val tags = listOf(
        Tag(1, "Shopping", 0xFF004D5F.toInt()),
        Tag(2, "Travel", 0xFF1A237E.toInt()),
        Tag(3, "Food", 0xFF1B5E20.toInt()),
        Tag(4, "Healthcare", 0xFFB71C1C.toInt()),
        Tag(5, "Education", 0xFF4A148C.toInt())
    )
    
    OssWalletTheme {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            AeonTagSelectionDialog(
                tags = tags,
                selectedTags = setOf("Shopping", "Food"),
                onTagsSelected = {},
                onAddTag = { _, _ -> },
                onDismiss = {}
            )
        }
    }
}

@Preview
@Composable
fun AeonTagCreateDialogPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            AeonTagCreateDialog(
                initialName = "Work",
                onDismiss = {},
                onConfirm = { _, _ -> }
            )
        }
    }
}
