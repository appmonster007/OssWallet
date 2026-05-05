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
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSheet(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val methods = listOf(
        "Cash" to Icons.Rounded.Payments,
        "Credit Card" to Icons.Rounded.CreditCard,
        "Debit Card" to Icons.Rounded.CreditCard,
        "Bank Transfer" to Icons.Rounded.AccountBalance,
        "Digital Wallet" to Icons.Rounded.Wallet,
        "Mobile Payment" to Icons.Rounded.Contactless,
        "Check" to Icons.Rounded.Description,
        "Cryptocurrency" to Icons.Rounded.CurrencyBitcoin,
        "Other" to Icons.Rounded.MoreHoriz
    )

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
            AeonSectionHeader(title = "Payment Method")
            Text(
                text = "Select how you paid for this transaction",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.weight(1f, fill = false)) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AeonHybridSelector(
                        items = methods,
                        mode = AeonSelectMode.SINGLE,
                        selectedIds = setOf(selectedMethod),
                        onSelectionChange = { ids ->
                            ids.firstOrNull()?.let { 
                                onMethodSelected(it)
                                onDismiss()
                            }
                        },
                        idMapper = { it.first },
                        labelMapper = { it.first },
                        iconMapper = { it.second }
                    )
                }
            }
        }
    }
}
