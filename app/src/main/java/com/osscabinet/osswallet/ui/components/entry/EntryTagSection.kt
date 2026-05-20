package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

/**
 * Reusable tag selection section for entry screens.
 * Following the "Architect Slate" design specification.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryTagSection(
    selectedNames: Set<String>,
    allAvailableTags: List<Tag>,
    onShowTagSheet: () -> Unit,
    onRemoveTag: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "ADD TAG"
) {
    val resolvedTags = remember(selectedNames, allAvailableTags) {
        selectedNames.map { name ->
            allAvailableTags.find { it.name.equals(name, ignoreCase = true) }
                ?: Tag(name = name)
        }
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Add Tag Button
        Surface(
            onClick = onShowTagSheet,
            shape = RoundedCornerShape(12.dp),
            color = AppColors.recessedSurface,
            border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.2f)),
            modifier = Modifier.height(32.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Label,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = AppColors.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = AppTextStyles.formSectionLabel
                )
            }
        }

        // Selected Tags
        resolvedTags.forEach { tag ->
            val tagColor = if (tag.colorHex != -1) Color(tag.colorHex) else AppColors.primary
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = tagColor.copy(alpha = 0.12f),
                border = BorderStroke(0.5.dp, tagColor.copy(alpha = 0.4f)),
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tag.name,
                        color = tagColor,
                        style = AppTextStyles.cardDelta
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onRemoveTag(tag.name) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Remove tag",
                            modifier = Modifier.size(12.dp),
                            tint = tagColor
                        )
                    }
                }
            }
        }
    }
}
