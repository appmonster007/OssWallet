package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Editorial-style text input with icon and label for entry screens.
 * Following the "Architect Slate" design specification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorialInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    suggestions: List<String> = emptyList(),
    maxLines: Int = 4,
    placeholder: String = "Tap to enter...",
    iconColor: Color = AppColors.primary,
    enabled: Boolean = true,
    additionalContent: @Composable ColumnScope.() -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val filteredSuggestions = remember(value, suggestions) {
        if (value.isBlank()) suggestions.take(5)
        else suggestions.filter {
            it.contains(value, ignoreCase = true) || it.fuzzyMatch(value)
        }.take(5)
    }

    LaunchedEffect(filteredSuggestions, isFocused) {
        expanded = isFocused && filteredSuggestions.isNotEmpty()
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { },
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
            shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
            color = AppColors.recessedSurface,
            border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.2f)),
            onClick = { focusRequester.requestFocus() }
        ) {
            Row(
                modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.08f),
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = AppTextStyles.transactionSwipeAction.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 10.sp
                        ),
                        color = AppColors.contentSecondary.copy(alpha = 0.5f)
                    )

                    Box(modifier = Modifier.padding(top = 2.dp)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = AppTextStyles.listItemTitle,
                                color = AppColors.contentSecondary.copy(alpha = 0.3f)
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            enabled = enabled,
                            textStyle = AppTextStyles.listItemTitle.copy(
                                color = if (enabled) AppColors.contentPrimary else AppColors.contentPrimary.copy(
                                    alpha = 0.8f
                                ),
                                fontWeight = FontWeight.Medium
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            ),
                            maxLines = maxLines,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (enabled) Modifier.focusRequester(focusRequester) else Modifier)
                                .onFocusChanged { isFocused = it.isFocused }
                        )
                    }

                    additionalContent()
                }
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppColors.componentOnCard)
        ) {
            filteredSuggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onValueChange(suggestion)
                        expanded = false
                        focusManager.clearFocus()
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EntryEditorialInputPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            EntryEditorialInput(
                value = "Lunch with friends",
                onValueChange = {},
                label = "NOTE",
                icon = Icons.Rounded.Edit,
                suggestions = listOf("Coffee", "Groceries", "Dinner")
            )
        }
    }
}
