package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

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
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.ui.components.AccountIconMap
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionAccountPicker(
    accounts: List<Account>,
    selectedAccountId: String?,
    onAccountSelected: (Account?) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Account",
    allowOutsideWallet: Boolean = false
) {
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    
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
            AeonSectionHeader(title = title)
            Text(
                text = "Choose the source or destination of funds",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.weight(1f, fill = false)) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (allowOutsideWallet) {
                        AeonHybridSelector(
                            items = listOf(null),
                            mode = AeonSelectMode.SINGLE,
                            selectedIds = if (selectedAccountId == null) setOf("null") else emptySet(),
                            onSelectionChange = { 
                                onAccountSelected(null)
                                onDismiss()
                            },
                            idMapper = { "null" },
                            labelMapper = { "Outside of Wallet" },
                            detailMapper = { "External transfer" },
                            iconMapper = { Icons.Rounded.Public },
                            colorMapper = { tertiaryColor }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    AeonHybridSelector(
                        items = accounts,
                        mode = AeonSelectMode.SINGLE,
                        selectedIds = if (selectedAccountId != null) setOf(selectedAccountId) else emptySet(),
                        onSelectionChange = { ids ->
                            ids.firstOrNull()?.let { id ->
                                onAccountSelected(accounts.find { it.id == id })
                                onDismiss()
                            }
                        },
                        idMapper = { it.id },
                        labelMapper = { it.name },
                        detailMapper = { it.currencyCode },
                        iconMapper = { AccountIconMap[it.icon] },
                        colorMapper = { Color(android.graphics.Color.parseColor(it.colorHex)) }
                    )
                }
            }
        }
    }
}
