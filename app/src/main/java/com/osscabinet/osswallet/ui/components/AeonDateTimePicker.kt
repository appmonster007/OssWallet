package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonDateTimePicker(
    initialDateTime: Long,
    onDateTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    is24Hour: Boolean = com.osscabinet.ossWallet.ui.util.TimeUtils.use24HourFormat
) {
    var showDatePicker by remember { mutableStateOf(true) }
    var selectedDateMillis by remember { mutableStateOf(initialDateTime) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis,
            selectableDates = DatePickerDefaults.AllDates
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val dateMillis = datePickerState.selectedDateMillis ?: initialDateTime
                    val dateCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        .apply { timeInMillis = dateMillis }
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
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = is24Hour
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
            containerColor = AppColors.panelSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AeonDateTimePickerPreview() {
    OssWalletTheme {
        Surface {
            AeonDateTimePicker(
                initialDateTime = System.currentTimeMillis(),
                onDateTimeSelected = {},
                onDismiss = {}
            )
        }
    }
}
