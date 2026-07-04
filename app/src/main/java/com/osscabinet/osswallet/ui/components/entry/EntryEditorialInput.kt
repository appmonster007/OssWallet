package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Error
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import com.osscabinet.ossWallet.ui.components.AeonGlassHost
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.copyStyle
import com.osscabinet.ossWallet.ui.theme.scaledDp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Editorial-style text input for entry forms.
 * Rebuilt from scratch to ensure absolute parity with EntryActionCard design.
 * Features glassmorphism, focus-aware borders, haptics, and auto-suggestions.
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
    placeholder: String = "Enter text...",
    iconColor: Color = AppColors.primary,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    isFlush: Boolean = false,
    isError: Boolean = false,
    additionalContent: @Composable ColumnScope.() -> Unit = {}
) {
    // 1. STATE & ANIMATION
    var expanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val glassEnabled = LocalGlassmorphismEnabled.current
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed && enabled) 0.98f else 1f, label = "cardScale")

    // 2. SUGGESTION LOGIC
    val filteredSuggestions = remember(value, suggestions) {
        if (value.isBlank()) suggestions.take(5)
        else suggestions.filter {
            it.contains(value, ignoreCase = true) || it.fuzzyMatch(value)
        }.take(5)
    }

    LaunchedEffect(filteredSuggestions, isFocused) {
        expanded = isFocused && filteredSuggestions.isNotEmpty()
    }

    // 3. HIGHLIGHT STATE (FOCUS OR SELECTION)
    val isHighlighted = isFocused || isSelected

    // 4. COLOR & BORDER LOGIC (PARITY WITH EntryActionCard & EntrySection)
    // Non-glass: solid container colors. Glass: blur + subtle color wash.
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFlush -> AppColors.transparent
            isHighlighted && !glassEnabled -> AppColors.surfaceContainerHighest // Solid highlight
            else -> AppColors.surfaceContainerLow // Base matches EntrySection
        },
        label = "bgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isFlush -> Color.Transparent
            isError -> AppColors.error
            isHighlighted -> iconColor
            glassEnabled -> AppColors.outlineVariant.copy(alpha = 0.2f)
            else -> AppColors.outlineVariant
        },
        label = "borderColor"
    )

    val config = GlassTokens.configForVariant(GlassVariant.Card)
    val borderWidth by animateDpAsState(
        targetValue = when {
            isFlush -> 0.dp
            isHighlighted || isError || !glassEnabled -> config.surface.borderWidth.dp
            else -> config.surface.borderWidth.dp
        },
        label = "borderWidth"
    )

    // 5. UI STRUCTURE
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (!it) expanded = false },
        modifier = modifier.fillMaxWidth()
    ) {
        GlassSurface(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled
                ) {
                    if (enabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        focusRequester.requestFocus()
                    }
                },
            shape = if (isFlush) RoundedCornerShape(0.dp) else RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
            hazeStyle = GlassTokens.cardStyle().copyStyle(bgColor = AppColors.surfaceContainerLow),
            glassVariant = GlassVariant.Card,
            fallbackColor = backgroundColor,
            borderColor = borderColor,
            borderWidth = borderWidth,
            shadowElevation = if (isFlush) 0.dp else null
        ) {
            // Glass-mode color wash: parity with EntryActionCard
            if (glassEnabled && isHighlighted && !isFlush) {
                Box(modifier = Modifier.fillMaxSize().background(iconColor.copy(alpha = 0.1f)))
            }

            AeonGlassHost(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = if (isFlush) 0.dp else AeonTheme.dimensions.cardPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ICON CONTAINER (MATCHING EntryActionCard)
                Surface(
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingSm)) // Precise spacing parity

                // CONTENT COLUMN
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = if (isFlush) 0.dp else AeonTheme.dimensions.spacingSm),
                    verticalArrangement = Arrangement.Center
                ) {
                    // LABEL (MATCHING EntryActionCard)
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

                    Spacer(modifier = Modifier.height(2.dp))

                    // INPUT FIELD (STYLE MATCHING EntryActionCard'S VALUE)
                    Box(modifier = Modifier.fillMaxWidth()) {
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
                            textStyle = AppTextStyles.formFieldValue.copy(
                                color = if (enabled) AppColors.contentPrimary else AppColors.contentSecondary
                            ),
                            cursorBrush = SolidColor(if (enabled) AppColors.contentPrimary else AppColors.contentSecondary),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            ),
                            maxLines = maxLines,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .then(if (enabled) Modifier.focusRequester(focusRequester) else Modifier)
                                .onFocusChanged {
                                    if (it.isFocused && !isFocused) {
                                        coroutineScope.launch {
                                            delay(250) // Wait for keyboard expansion
                                            if (isFocused) {
                                                bringIntoViewRequester.bringIntoView(
                                                    rect = Rect(0f, 0f, 0f, 1500f)
                                                )
                                            }
                                        }
                                    }
                                    isFocused = it.isFocused
                                }
                        )
                    }

                    additionalContent()
                }
            }
            }
        }

        // SUGGESTIONS MENU
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
