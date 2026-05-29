package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import com.osscabinet.ossWallet.ui.components.AeonColorSelection
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.DefaultAccentColorHex
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import com.osscabinet.ossWallet.ui.util.isDark
import kotlinx.coroutines.delay

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
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            debouncedQuery = ""
        } else {
            delay(300)
            debouncedQuery = searchQuery
        }
    }

    var currentSelection by remember { mutableStateOf(selectedTags) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Track newly added tags locally to ensure they remain searchable if deselected
    var localTags by remember { mutableStateOf(emptyList<Tag>()) }
    val allAvailableTags = remember(tags, localTags) {
        (tags + localTags).distinctBy { it.name }
    }

    val filteredTags = remember(allAvailableTags, debouncedQuery) {
        val trimmed = debouncedQuery.trim()
        if (trimmed.isBlank()) allAvailableTags
        else allAvailableTags.filter {
            it.name.contains(
                trimmed,
                ignoreCase = true
            ) || it.name.fuzzyMatch(trimmed)
        }
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
            shape = RoundedCornerShape(AeonTheme.dimensions.dialogCornerRadius),
            color = AppColors.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.tag_selection_title),
                    style = AppTextStyles.cardTitle,
                    color = AppColors.contentPrimary
                )

                var searchFieldHeight by remember { mutableIntStateOf(0) }
                val density = LocalDensity.current

                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned {
                                searchFieldHeight = it.size.height
                            }
                    ) {
                        AeonSearchField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = stringResource(R.string.tag_search_placeholder),
                        )
                    }

                    // Search Results Dropdown (Using Popup for stability)
                    // Showing always now to allow selection from existing tags even when not searching
                    Popup(
                        alignment = Alignment.TopStart,
                        offset = IntOffset(
                            0,
                            searchFieldHeight + with(density) { 4.dp.roundToPx() })
                    ) {
                        Column { // Changed from Box to Column to provide ColumnScope
                            AnimatedVisibility(
                                visible = searchQuery.isNotEmpty() || currentSelection.isEmpty(), // Show when searching or if nothing selected yet
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .heightIn(max = 250.dp),
                                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                                    color = AppColors.surfaceContainerHighest,
                                    border = BorderStroke(
                                        1.dp,
                                        AppColors.outlineVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(vertical = 4.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        val trimmed = searchQuery.trim()
                                        if (trimmed.isNotBlank() && !exactMatchExists && allowCreation) {
                                            TextButton(
                                                onClick = { showCreateDialog = true },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.Add,
                                                        null,
                                                        tint = AppColors.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        stringResource(
                                                            R.string.tag_create_new,
                                                            trimmed
                                                        ),
                                                        color = AppColors.primary,
                                                        style = AppTextStyles.listItemTitle
                                                    )
                                                }
                                            }
                                        }

                                        filteredTags.forEach { tag ->
                                            val isSelected = currentSelection.any {
                                                it.equals(
                                                    tag.name,
                                                    ignoreCase = true
                                                )
                                            }
                                            TextButton(
                                                onClick = {
                                                    currentSelection = if (isSelected) {
                                                        currentSelection.filter {
                                                            !it.equals(
                                                                tag.name,
                                                                ignoreCase = true
                                                            )
                                                        }.toSet()
                                                    } else {
                                                        currentSelection + tag.name
                                                    }
                                                    // We don't clear searchQuery here so user can keep selecting if they want
                                                    // but usually for single select we might. Tags are multi-select.
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .background(
                                                                Color(tag.colorHex),
                                                                RoundedCornerShape(2.dp)
                                                            )
                                                    )
                                                    Text(
                                                        tag.name,
                                                        color = if (isSelected) AppColors.primary else AppColors.contentPrimary,
                                                        style = AppTextStyles.listItemTitle,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (isSelected) {
                                                        Icon(
                                                            Icons.Rounded.Check,
                                                            null,
                                                            tint = AppColors.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (filteredTags.isEmpty() && trimmed.isNotBlank() && !allowCreation) {
                                            Text(
                                                stringResource(R.string.tag_no_matches),
                                                modifier = Modifier.padding(16.dp),
                                                style = AppTextStyles.caption
                                            )
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
                            val tagColor = allAvailableTags.find {
                                it.name.equals(
                                    tagName,
                                    ignoreCase = true
                                )
                            }?.colorHex?.let { Color(it) }
                                ?: AppColors.primary

                            InputChip(
                                selected = true,
                                onClick = { currentSelection = currentSelection - tagName },
                                label = { Text(tagName) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Rounded.Close,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
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
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onTagsSelected(currentSelection)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius)
                    ) {
                        Text(stringResource(R.string.action_done), style = AppTextStyles.buttonLabel)
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
            shape = RoundedCornerShape(AeonTheme.dimensions.dialogCornerRadius),
            color = AppColors.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.tag_create_dialog_title),
                    style = AppTextStyles.cardTitle,
                    color = AppColors.contentPrimary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.tag_name_label), style = AppTextStyles.sectionOverline) },
                    placeholder = { Text(stringResource(R.string.tag_name_placeholder), style = AppTextStyles.formFieldHint) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
                    singleLine = true,
                    textStyle = AppTextStyles.formFieldValue
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
                        Text(stringResource(R.string.action_cancel))
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
                        shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius)
                    ) {
                        Text(stringResource(R.string.action_create), style = AppTextStyles.buttonLabel)
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
fun AeonTagSelectionDialogPreview() {
    OssWalletTheme {
        AeonTagSelectionDialog(
            tags = listOf(
                Tag(name = "Groceries", colorHex = android.graphics.Color.GREEN),
                Tag(name = "Urgent", colorHex = android.graphics.Color.RED)
            ),
            selectedTags = setOf("Groceries"),
            onTagsSelected = {},
            onAddTag = { _, _ -> },
            onDismiss = {}
        )
    }
}

@ThemePreviews
@Composable
fun AeonTagCreateDialogPreview() {
    OssWalletTheme {
        AeonTagCreateDialog(
            initialName = "New Tag",
            onDismiss = {},
            onConfirm = { _, _ -> }
        )
    }
}
