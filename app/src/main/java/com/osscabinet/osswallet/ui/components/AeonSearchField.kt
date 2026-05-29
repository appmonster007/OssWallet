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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
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
    val fieldModifier = modifier
        .fillMaxWidth()
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)

    if (showBorder) {
        OutlinedTextField(
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
            shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.primary,
                unfocusedBorderColor = AppColors.outline,
                focusedContainerColor = AppColors.surfaceContainerLow,
                unfocusedContainerColor = AppColors.surfaceContainerLow,
                cursorColor = AppColors.primary
            )
        )
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
