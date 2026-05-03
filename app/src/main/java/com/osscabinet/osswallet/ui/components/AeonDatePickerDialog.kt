package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonDateField(
    label: String,
    dateMillis: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault()) }
    val displayText = remember(dateMillis) {
        formatter.format(Instant.ofEpochMilli(dateMillis))
    }

    OutlinedTextField(
        value = displayText,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date")
            }
        },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    )

    if (showPicker) {
        AeonDatePickerDialog(
            selectedDateMillis = dateMillis,
            onDateSelected = { millis ->
                onDateSelected(millis)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonDatePickerDialog(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    label: String = "Date"
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis,
        selectableDates = DatePickerDefaults.AllDates
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(state.selectedDateMillis ?: selectedDateMillis)
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(32.dp)
    ) {
        DatePicker(state = state)
    }
}
