package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
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
    val formatter =
        remember { DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault()) }
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

@Preview(showBackground = true)
@Composable
fun AeonDateFieldPreview() {
    OssWalletTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            AeonDateField(
                label = "Due Date",
                dateMillis = System.currentTimeMillis(),
                onDateSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonDatePickerDialogPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AeonDatePickerDialog(
                selectedDateMillis = System.currentTimeMillis(),
                onDateSelected = {},
                onDismiss = {}
            )
        }
    }
}
