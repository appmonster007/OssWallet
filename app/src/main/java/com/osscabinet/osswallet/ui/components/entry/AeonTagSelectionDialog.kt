package com.osscabinet.ossWallet.ui.components.entry

import com.osscabinet.ossWallet.ui.theme.GlassElement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import com.osscabinet.ossWallet.ui.components.AeonColorSelection
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.DefaultAccentColorHex
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import kotlinx.coroutines.delay

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
            delay(200)
            debouncedQuery = searchQuery
        }
    }

    var currentSelection by remember { mutableStateOf(selectedTags) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    var localTags by remember { mutableStateOf(emptyList<Tag>()) }
    val allAvailableTags = remember(tags, localTags) {
        (tags + localTags).distinctBy { it.name }.sortedBy { it.name }
    }

    val filteredTags = remember(allAvailableTags, debouncedQuery) {
        val trimmed = debouncedQuery.trim()
        if (trimmed.isBlank()) allAvailableTags
        else allAvailableTags.filter {
            it.name.contains(trimmed, ignoreCase = true) || it.name.fuzzyMatch(trimmed)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            val dims = AeonTheme.dimensions
            GlassSurface(
                element = GlassElement.ENTRY,
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight()
                    .clickable(enabled = false) {}, // Prevent dismiss when clicking inside
                shape = RoundedCornerShape(28.dp),
                hazeStyle = GlassTokens.popupStyle(),
                glassVariant = GlassVariant.Popup,
                fallbackColor = AppColors.popupSurface,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Select Tags",
                        style = AppTextStyles.heroAmount.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AppColors.contentPrimary
                    )

                    AeonSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search or create...",
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val trimmed = searchQuery.trim()
                        if (trimmed.isNotBlank() && !exactMatchExists && allowCreation) {
                            TagRow(
                                name = "Create \"$trimmed\"",
                                color = Color.Gray.toArgb(), // Placeholder for new tag
                                isSelected = false,
                                isCreation = true,
                                onClick = { showCreateDialog = true }
                            )
                        }

                        filteredTags.forEach { tag ->
                            val isSelected = currentSelection.contains(tag.name)
                            TagRow(
                                name = tag.name,
                                color = tag.colorHex,
                                isSelected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentSelection = if (isSelected) {
                                        currentSelection - tag.name
                                    } else {
                                        currentSelection + tag.name
                                    }
                                }
                            )
                        }

                        if (filteredTags.isEmpty() && trimmed.isNotBlank() && !allowCreation) {
                            Text(
                                "No matches found",
                                modifier = Modifier.padding(16.dp),
                                style = AppTextStyles.caption,
                                color = AppColors.contentSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text(
                                "Cancel",
                                style = AppTextStyles.buttonLabel.copy(color = AppColors.contentSecondary)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                onTagsSelected(currentSelection)
                                onDismiss()
                            },
                            modifier = Modifier.height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.primary,
                                contentColor = AppColors.onPrimary
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 24.dp
                            )
                        ) {
                            Text(
                                "Done",
                                style = AppTextStyles.buttonLabel.copy(fontWeight = FontWeight.Bold)
                            )
                        }
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
private fun TagRow(
    name: String,
    color: Int,
    isSelected: Boolean,
    isCreation: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCreation) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = AppColors.primary,
                modifier = Modifier.size(14.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(color), RoundedCornerShape(2.dp))
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = name,
            style = AppTextStyles.listItemTitle.copy(
                color = if (isSelected) AppColors.primary else AppColors.contentPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = AppColors.primary,
                modifier = Modifier.size(18.dp)
            )
        }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            GlassSurface(
                element = GlassElement.ENTRY,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .widthIn(max = 340.dp)
                    .wrapContentHeight()
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(28.dp),
                hazeStyle = GlassTokens.popupStyle(),
                glassVariant = GlassVariant.Popup,
                fallbackColor = AppColors.popupSurface,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tag_create_dialog_title),
                        style = AppTextStyles.heroAmount.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AppColors.contentPrimary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = {
                            Text(
                                stringResource(R.string.tag_name_label),
                                style = AppTextStyles.sectionOverline
                            )
                        },
                        placeholder = {
                            Text(
                                stringResource(R.string.tag_name_placeholder),
                                style = AppTextStyles.formFieldHint
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                "Cancel",
                                style = AppTextStyles.buttonLabel.copy(color = AppColors.contentSecondary)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
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
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.primary,
                                contentColor = AppColors.onPrimary
                            )
                        ) {
                            Text(
                                "Create",
                                style = AppTextStyles.buttonLabel.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Color.toArgb(): Int {
    return (this.alpha * 255.0f + 0.5f).toInt() shl 24 or
            ((this.red * 255.0f + 0.5f).toInt() shl 16) or
            ((this.green * 255.0f + 0.5f).toInt() shl 8) or
            (this.blue * 255.0f + 0.5f).toInt()
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
