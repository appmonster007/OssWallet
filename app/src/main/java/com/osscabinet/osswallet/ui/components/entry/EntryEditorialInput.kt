package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors

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
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
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
                    color = AppColors.primary.copy(alpha = 0.08f),
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = AppColors.primary,
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
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
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppColors.contentSecondary.copy(alpha = 0.3f)
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = AppColors.contentPrimary,
                                fontWeight = FontWeight.Medium
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            ),
                            maxLines = maxLines,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
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
