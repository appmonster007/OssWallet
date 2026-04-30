package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    initialMillis: Long,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Date"
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onDateSelected(datePickerState.selectedDateMillis) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = datePickerState, title = { Text(title) })
    }
}
