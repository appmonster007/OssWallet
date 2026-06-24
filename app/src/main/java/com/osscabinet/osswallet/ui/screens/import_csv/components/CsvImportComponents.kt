package com.osscabinet.ossWallet.ui.screens.import_csv.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews

@Composable
fun StepProgressIndicator(stepIndex: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
        ) {
            repeat(4) { index ->
                val isActive = index == stepIndex
                val isCompleted = index < stepIndex
                val color = when {
                    isActive    -> AppColors.primary
                    isCompleted -> AppColors.primary.copy(alpha = 0.6f)
                    else        -> AppColors.outlineVariant
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(color, RoundedCornerShape(2.dp))
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (stepIndex) {
                0    -> "Step 1: Source App"
                1    -> "Step 2: Column Mapping"
                2    -> "Step 3: Accounts & Categories"
                else -> "Step 4: Review & Confirm"
            },
            style = AppTextStyles.overlineLabel,
            color = AppColors.contentSecondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityMappingRow(
    label: String,
    badgeText: String? = null,
    chipText: String? = null,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    isError: Boolean = false,
    showNullOption: Boolean = true,
    showExcludeOption: Boolean = false,
    currencyCode: String? = null,
    onCurrencySelect: (String) -> Unit = {},
    supportedCurrencies: List<String> = emptyList()
) {
    var expanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val isExcluded = selectedId == "__EXCLUDE__"
    val isCreateNew = selectedId == null
    val selectedName = when {
        isExcluded -> "Excluded"
        selectedId != null -> options.find { it.first == selectedId }?.second ?: "Create New"
        else -> "Create New"
    }

    Surface(
        color = AppColors.cardSurface,
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AeonTheme.dimensions.cardPadding, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        label,
                        style = AppTextStyles.listItemTitle,
                        color = if (isError) AppColors.error else AppColors.contentPrimary
                    )
                    if (badgeText != null || chipText != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (chipText != null) {
                                Surface(
                                    color = AppColors.accentContainer,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        chipText,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = AppTextStyles.chipLabel,
                                        color = AppColors.onAccentContainer
                                    )
                                }
                            }
                            if (badgeText != null) {
                                Text(
                                    badgeText,
                                    style = AppTextStyles.chipLabel,
                                    color = AppColors.contentSecondary
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1.4f)) {
                    Surface(
                        onClick = { expanded = true },
                        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                        color = when {
                            isExcluded -> AppColors.error.copy(alpha = 0.08f)
                            isError -> AppColors.errorContainer.copy(alpha = 0.1f)
                            else -> AppColors.surfaceContainerLow
                        },
                        border = BorderStroke(1.dp, when {
                            isExcluded -> AppColors.error.copy(alpha = 0.5f)
                            isError -> AppColors.error
                            else -> AppColors.outlineVariant
                        }),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedName,
                                style = AppTextStyles.caption,
                                color = when {
                                    isExcluded -> AppColors.error
                                    selectedId != null -> AppColors.contentPrimary
                                    else -> AppColors.contentSecondary
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = AppColors.contentSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = AppColors.surfaceContainerHigh,
                        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                        modifier = Modifier.widthIn(min = 160.dp)
                    ) {
                        if (showNullOption) {
                            DropdownMenuItem(
                                text = { Text("Create New", style = AppTextStyles.caption) },
                                onClick = { onSelect(null); expanded = false }
                            )
                        }
                        if (showExcludeOption) {
                            DropdownMenuItem(
                                text = { Text("Exclude account", style = AppTextStyles.caption, color = AppColors.error) },
                                onClick = { onSelect("__EXCLUDE__"); expanded = false }
                            )
                        }
                        options.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name, style = AppTextStyles.caption) },
                                onClick = { onSelect(id); expanded = false }
                            )
                        }
                    }
                }
            }

            // Currency selector for "Create New" accounts
            if (isCreateNew && currencyCode != null) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = AeonTheme.dimensions.cardPadding), color = AppColors.outlineVariant, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AeonTheme.dimensions.cardPadding, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Currency",
                        style = AppTextStyles.overlineLabel,
                        color = AppColors.contentSecondary
                    )
                    Box {
                        Surface(
                            onClick = { currencyExpanded = true },
                            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                            color = AppColors.surfaceContainerLow,
                            border = BorderStroke(1.dp, AppColors.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(currencyCode, style = AppTextStyles.caption)
                                Icon(Icons.Rounded.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false },
                            containerColor = AppColors.surfaceContainerHigh
                        ) {
                            supportedCurrencies.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text(curr, style = AppTextStyles.caption) },
                                    onClick = { onCurrencySelect(curr); currencyExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun StepProgressIndicatorPreview() {
    OssWalletTheme {
        Surface(color = AppColors.screenBackground) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                StepProgressIndicator(stepIndex = 0)
                StepProgressIndicator(stepIndex = 1)
                StepProgressIndicator(stepIndex = 2)
                StepProgressIndicator(stepIndex = 3)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun EntityMappingRowPreview() {
    OssWalletTheme {
        Surface(color = AppColors.screenBackground) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EntityMappingRow(
                    label = "Amazon.in",
                    badgeText = "12 rows matched",
                    options = listOf("1" to "Shopping", "2" to "Food"),
                    selectedId = "1",
                    onSelect = {}
                )
                EntityMappingRow(
                    label = "Unmatched Account",
                    options = listOf("1" to "HDFC Bank", "2" to "Cash"),
                    selectedId = null,
                    onSelect = {},
                    currencyCode = "INR",
                    supportedCurrencies = listOf("INR", "USD", "EUR")
                )
                EntityMappingRow(
                    label = "Excluded Account",
                    options = listOf("1" to "HDFC Bank"),
                    selectedId = "__EXCLUDE__",
                    onSelect = {},
                    showExcludeOption = true
                )
            }
        }
    }
}
