package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AppIconMap

/**
 * Full-screen selection for Transaction Status.
 * Supports both single and multiple selection modes.
 */
@Composable
fun AeonStatusSelection(
    selectedStatuses: Set<TransactionStatus>,
    onSelectionChange: (Set<TransactionStatus>) -> Unit,
    onDismiss: () -> Unit,
    mode: AeonSelectMode = AeonSelectMode.SINGLE,
    title: String = "Transaction Status"
) {
    AeonSelectionSheet(
        title = title,
        onDismiss = onDismiss,
        items = TransactionStatus.entries,
        selectedIds = selectedStatuses,
        onSelectionChange = onSelectionChange,
        idMapper = { it },
        labelMapper = { it.displayName },
        detailMapper = { it.description },
        iconMapper = { AppIconMap[it.iconName] ?: Icons.Rounded.CheckCircle },
        colorMapper = { Color(android.graphics.Color.parseColor(it.colorHex)) },
        mode = mode
    )
}
