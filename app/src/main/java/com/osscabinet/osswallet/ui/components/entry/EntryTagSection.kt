package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Tag
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled

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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AeonTheme.dimensions.cardPadding),
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
    ) {
        // Add Tag Button
        Surface(
            onClick = onShowTagSheet,
            shape = RoundedCornerShape(AeonTheme.dimensions.chipCornerRadius),
            color = AppColors.surfaceContainerHigh,
            border = BorderStroke(1.dp, AppColors.outlineVariant),
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
                    style = AppTextStyles.tagLabel,
                    color = AppColors.primary
                )
            }
        }

        // Selected Tags
        val glassEnabled = LocalGlassmorphismEnabled.current
        resolvedTags.forEach { tag ->
            val tagColor = if (tag.colorHex != -1) Color(tag.colorHex) else AppColors.primary

            Surface(
                shape = RoundedCornerShape(AeonTheme.dimensions.chipCornerRadius),
                color = if (glassEnabled) AppColors.transparent else tagColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, tagColor.copy(alpha = 0.4f)),
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tag.name,
                        color = tagColor,
                        style = AppTextStyles.tagLabel
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

@ThemePreviews
@Composable
fun EntryTagSectionPreview() {
    OssWalletTheme {
        EntryTagSection(
            selectedNames = setOf("Groceries", "Urgent", "Work"),
            allAvailableTags = listOf(
                Tag(name = "Groceries", colorHex = android.graphics.Color.GREEN),
                Tag(name = "Urgent", colorHex = android.graphics.Color.RED),
                Tag(name = "Work", colorHex = android.graphics.Color.BLUE)
            ),
            onShowTagSheet = {},
            onRemoveTag = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
