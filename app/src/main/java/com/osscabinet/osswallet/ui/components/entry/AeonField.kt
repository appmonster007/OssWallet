package com.osscabinet.ossWallet.ui.components.entry

/**
 * Encapsulates the state, actions, and validation for a text input field.
 */
data class AeonTextFieldState(
    val value: String,
    val onValueChange: (String) -> Unit,
    val isError: Boolean = false,
    val label: String? = null,
    val placeholder: String? = null
)

/**
 * Encapsulates the state, actions, and validation for a selection-based field.
 */
data class AeonSelectorState<T>(
    val selection: T?,
    val onClick: () -> Unit,
    val isError: Boolean = false,
    val label: String? = null
)
