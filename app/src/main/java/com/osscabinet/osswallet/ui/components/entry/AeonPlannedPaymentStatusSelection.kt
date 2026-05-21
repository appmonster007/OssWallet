package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.ui.components.AppIconMap
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Full-screen selection for Planned Payment Status.
 */
@Composable
fun AeonPlannedPaymentStatusSelection(
    selectedStatus: PlannedPaymentStatus,
    onStatusSelected: (PlannedPaymentStatus) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Payment Status"
) {
    AeonSelectionSheet(
        title = title,
        onDismiss = onDismiss,
        items = PlannedPaymentStatus.entries,
        selectedIds = setOf(selectedStatus),
        onSelectionChange = { ids ->
            ids.firstOrNull()?.let { onStatusSelected(it) }
        },
        idMapper = { it },
        labelMapper = { it.displayName },
        iconMapper = { AppIconMap[it.iconName] ?: Icons.Rounded.CheckCircle },
        colorMapper = { Color(android.graphics.Color.parseColor(it.colorHex)) }
    )
}

@Preview(showBackground = true)
@Composable
fun AeonPlannedPaymentStatusSelectionPreview() {
    OssWalletTheme {
        AeonPlannedPaymentStatusSelection(
            selectedStatus = PlannedPaymentStatus.ACTIVE,
            onStatusSelected = {},
            onDismiss = {}
        )
    }
}

