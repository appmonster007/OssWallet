package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AppColors

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
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    trailingAction?.invoke()
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = AppColors.border.copy(alpha = 0.3f),
                focusedContainerColor = AppColors.insetSurface.copy(alpha = 0.1f),
                unfocusedContainerColor = AppColors.insetSurface.copy(alpha = 0.05f)
            )
        )
    } else {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = fieldModifier,
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    trailingAction?.invoke()
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AppColors.transparent,
                unfocusedContainerColor = AppColors.transparent,
                disabledContainerColor = AppColors.transparent,
                focusedIndicatorColor = AppColors.transparent,
                unfocusedIndicatorColor = AppColors.transparent,
            )
        )
    }
}
