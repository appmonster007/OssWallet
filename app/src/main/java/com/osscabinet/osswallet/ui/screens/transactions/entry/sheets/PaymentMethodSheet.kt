package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSheet(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val methods = listOf(
        "Cash" to Icons.Rounded.Payment,
        "Credit Card" to Icons.Rounded.Payment,
        "Debit Card" to Icons.Rounded.Payment,
        "Bank Transfer" to Icons.Rounded.Payment,
        "Mobile Payment" to Icons.Rounded.Payment,
        "Other" to Icons.Rounded.Payment
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(methods) { (method, icon) ->
                    val isSelected = method == selectedMethod
                    Surface(
                        onClick = { onMethodSelected(method) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = method,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
