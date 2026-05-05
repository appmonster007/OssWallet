package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonSectionHeader
import com.osscabinet.ossWallet.ui.components.AccountIconMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSheet(
    selectedStatus: TransactionStatus,
    onStatusSelected: (TransactionStatus) -> Unit,
    onDismiss: () -> Unit
) {
    val statuses = TransactionStatus.entries

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            AeonSectionHeader(title = "Transaction Status")
            Text(
                text = "Mark the current state of this record",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.weight(1f, fill = false)) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AeonHybridSelector(
                        items = statuses,
                        mode = AeonSelectMode.SINGLE,
                        selectedIds = setOf(selectedStatus.name),
                        onSelectionChange = { ids ->
                            ids.firstOrNull()?.let { name ->
                                statuses.find { it.name == name }?.let { 
                                    onStatusSelected(it)
                                    onDismiss()
                                }
                            }
                        },
                        idMapper = { it.name },
                        labelMapper = { it.displayName },
                        detailMapper = { it.description },
                        iconMapper = { AccountIconMap[it.iconName] ?: Icons.Rounded.CheckCircle },
                        colorMapper = { Color(android.graphics.Color.parseColor(it.colorHex)) }
                    )
                }
            }
        }
    }
}
