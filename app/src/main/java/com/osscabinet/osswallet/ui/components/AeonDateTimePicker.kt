package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.*
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonDateTimePicker(
    initialDateTime: Long,
    onDateTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(true) }
    var selectedDateMillis by remember { mutableStateOf(initialDateTime) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val dateMillis = datePickerState.selectedDateMillis ?: initialDateTime
                    val dateCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateMillis }
                    val resultCal = Calendar.getInstance().apply {
                        timeInMillis = selectedDateMillis
                        set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
                        set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
                    }
                    selectedDateMillis = resultCal.timeInMillis
                    showDatePicker = false
                }) { Text("NEXT") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("CANCEL") }
            },
            shape = RoundedCornerShape(32.dp)
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE)
        )
        
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(Calendar.MINUTE, timePickerState.minute)
                    onDateTimeSelected(calendar.timeInMillis)
                    onDismiss()
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = true }) { Text("BACK") }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

fun formatDateTime(millis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}
