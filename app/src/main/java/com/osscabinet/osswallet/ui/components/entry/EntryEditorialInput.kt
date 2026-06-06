package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
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
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
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
    isFlush: Boolean = false,
    isError: Boolean = false,
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
        val interactionSource = remember { MutableInteractionSource() }
        GlassSurface(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled
                ) {
                    if (enabled) focusRequester.requestFocus()
                },
            shape = if (isFlush) RoundedCornerShape(0.dp) else RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
            hazeStyle = GlassTokens.cardStyle(),
            fallbackColor = if (isFlush) AppColors.transparent else AppColors.surfaceContainerLow,
            borderColor = if (isFocused) AppColors.primary else if (isFlush) Color.Transparent else AppColors.outlineVariant.copy(alpha = 0.2f),
            borderAlpha = if (isFocused) 0.5f else if (isFlush) 0f else 0.1f,
            borderWidth = if (isFocused) 1.dp else if (isFlush) 0.dp else 0.5.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isFlush) 0.dp else AeonTheme.dimensions.cardPadding,
                        vertical = if (isFlush) AeonTheme.dimensions.spacingMd else AeonTheme.dimensions.cardPadding
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            style = AppTextStyles.overlineLabel,
                            color = if (isError) AppColors.error else AppColors.contentSecondary
                        )
                        if (isError) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.Error,
                                contentDescription = null,
                                tint = AppColors.error,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    Box(modifier = Modifier.padding(top = 2.dp)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = AppTextStyles.formFieldHint,
                                color = AppColors.contentSecondary
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            enabled = enabled,
                            textStyle = AppTextStyles.formFieldValue,
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
            modifier = Modifier.background(AppColors.surfaceContainerHigh)
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
