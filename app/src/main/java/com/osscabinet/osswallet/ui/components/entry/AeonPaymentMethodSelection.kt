package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AppIconMap
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Full-screen selection for Payment Methods.
 * Supports both single and multiple selection modes.
 */
@Composable
fun AeonPaymentMethodSelection(
    selectedMethods: Set<PaymentMethod>,
    onSelectionChange: (Set<PaymentMethod>) -> Unit,
    onDismiss: () -> Unit,
    mode: AeonSelectMode = AeonSelectMode.SINGLE,
    title: String = "Payment Method"
) {
    AeonSelectionSheet(
        title = title,
        onDismiss = onDismiss,
        items = PaymentMethod.entries,
        selectedIds = selectedMethods,
        onSelectionChange = onSelectionChange,
        idMapper = { it },
        labelMapper = { it.displayName },
        iconMapper = { AppIconMap[it.iconName] ?: Icons.Rounded.Payments },
        searchPlaceholder = "Search methods...",
        mode = mode
    )
}

@Preview(showBackground = true)
@Composable
fun AeonPaymentMethodSelectionPreview() {
    OssWalletTheme {
        AeonPaymentMethodSelection(
            selectedMethods = setOf(PaymentMethod.CASH),
            onSelectionChange = {},
            onDismiss = {}
        )
    }
}

