package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.ui.components.AppIconMap

/**
 * Full-screen selection for Transaction Status.
 */
@Composable
fun AeonStatusSelection(
    selectedStatus: TransactionStatus,
    onStatusSelected: (TransactionStatus) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Transaction Status"
) {
    AeonSelectionSheet(
        title = title,
        onDismiss = onDismiss,
        items = TransactionStatus.entries,
        selectedIds = setOf(selectedStatus),
        onSelectionChange = { ids ->
            ids.firstOrNull()?.let { onStatusSelected(it) }
        },
        idMapper = { it },
        labelMapper = { it.displayName },
        detailMapper = { it.description },
        iconMapper = { AppIconMap[it.iconName] ?: Icons.Rounded.CheckCircle },
        colorMapper = { Color(android.graphics.Color.parseColor(it.colorHex)) }
    )
}
