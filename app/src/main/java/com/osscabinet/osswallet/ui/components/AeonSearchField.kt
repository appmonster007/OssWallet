package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * A standardized search field for selection screens.
 */
@Composable
fun AeonSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    focusRequester: FocusRequester? = null,
    showBorder: Boolean = true,
    trailingAction: @Composable (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val fieldModifier = modifier
        .fillMaxWidth()
        .onFocusChanged { isFocused = it.isFocused }
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)

    if (showBorder) {
        GlassSurface(
            modifier = fieldModifier,
            shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
            hazeStyle = GlassTokens.popupStyle(),
            glassVariant = com.osscabinet.ossWallet.ui.theme.GlassVariant.Popup,
            fallbackColor = AppColors.surfaceContainerLow,
            borderColor = if (isFocused) AppColors.primary else AppColors.outline.copy(alpha = 0.1f),
            borderAlpha = if (isFocused) 0.5f else 0.1f,
            borderWidth = if (isFocused) 1.dp else 0.5.dp,
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, style = AppTextStyles.formFieldHint) },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = AppColors.contentSecondary
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(20.dp),
                                    tint = AppColors.contentSecondary
                                )
                            }
                        }
                        trailingAction?.invoke()
                    }
                },
                singleLine = true,
                textStyle = AppTextStyles.formFieldValue,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppColors.transparent,
                    unfocusedContainerColor = AppColors.transparent,
                    disabledContainerColor = AppColors.transparent,
                    focusedIndicatorColor = AppColors.transparent,
                    unfocusedIndicatorColor = AppColors.transparent,
                    cursorColor = AppColors.primary
                )
            )
        }
    } else {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = fieldModifier,
            placeholder = { Text(placeholder, style = AppTextStyles.formFieldHint) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = AppColors.contentSecondary) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(20.dp),
                                tint = AppColors.contentSecondary
                            )
                        }
                    }
                    trailingAction?.invoke()
                }
            },
            singleLine = true,
            textStyle = AppTextStyles.formFieldValue,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AppColors.transparent,
                unfocusedContainerColor = AppColors.transparent,
                disabledContainerColor = AppColors.transparent,
                focusedIndicatorColor = AppColors.transparent,
                unfocusedIndicatorColor = AppColors.transparent,
                cursorColor = AppColors.primary
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AeonSearchFieldPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AeonSearchField(
                query = "Groceries",
                onQueryChange = {}
            )
        }
    }
}
