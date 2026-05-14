package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.ui.components.AppIconMap

/**
 * Full-screen selection for Payment Methods.
 */
@Composable
fun AeonPaymentMethodSelection(
    selectedMethod: PaymentMethod,
    onMethodSelected: (PaymentMethod) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Payment Method"
) {
    AeonSelectionSheet(
        title = title,
        onDismiss = onDismiss,
        items = PaymentMethod.entries,
        selectedIds = setOf(selectedMethod),
        onSelectionChange = { ids ->
            ids.firstOrNull()?.let { onMethodSelected(it) }
        },
        idMapper = { it },
        labelMapper = { it.displayName },
        iconMapper = { AppIconMap[it.iconName] ?: Icons.Rounded.Payments },
        searchPlaceholder = "Search methods..."
    )
}
