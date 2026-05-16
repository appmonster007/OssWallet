package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AeonScheduleBuilder(
    initialStartDate: Long,
    initialRecurrenceType: RecurrenceType,
    initialInterval: Int,
    initialDayOfWeek: Int?,
    initialDayOfMonth: Int?,
    initialCronExpression: String?,
    onConfirm: (Long, RecurrenceType, Int, Int?, Int?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var stage by remember { mutableIntStateOf(1) }
    var localStart by remember { mutableLongStateOf(initialStartDate) }
    var localType by remember { mutableStateOf(initialRecurrenceType) }
    var localInterval by remember { mutableIntStateOf(initialInterval) }
    var localDayOfWeek by remember { mutableStateOf(initialDayOfWeek) }
    var localDayOfMonth by remember { mutableStateOf(initialDayOfMonth) }
    var cronText by remember { mutableStateOf(initialCronExpression ?: "0 0 * * *") }

    if (stage == 1) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = localStart)
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    localStart = datePickerState.selectedDateMillis ?: localStart
                    stage = 2
                }) { Text("NEXT") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
            shape = RoundedCornerShape(28.dp)
        ) { DatePicker(state = datePickerState) }
    }

    if (stage == 2) {
        data class Preset(val label: String, val type: RecurrenceType, val interval: Int)
        val presets = listOf(
            Preset("One Time", RecurrenceType.ONE_TIME, 1),
            Preset("Daily", RecurrenceType.DAILY, 1),
            Preset("Weekly", RecurrenceType.WEEKLY, 1),
            Preset("Bi-weekly", RecurrenceType.EVERY_N_WEEKS, 2),
            Preset("Monthly", RecurrenceType.MONTHLY, 1),
            Preset("Quarterly", RecurrenceType.EVERY_N_MONTHS, 3),
            Preset("Semi-annually", RecurrenceType.EVERY_N_MONTHS, 6),
            Preset("Yearly", RecurrenceType.YEARLY, 1)
        )
        var selectedTabIndex by remember { mutableIntStateOf(if (localType == RecurrenceType.CRON) 1 else 0) }

        val parsedCronRule =
            remember(cronText) { if (cronText.isNotBlank()) PlannedPaymentService.parseCronToRule(cronText) else null }

        LaunchedEffect(parsedCronRule) {
            parsedCronRule?.let { rule ->
                localType = rule.type
                localInterval = rule.interval
                localDayOfWeek = rule.dayOfWeek
                localDayOfMonth = rule.dayOfMonth
            }
        }

        val needsIntervalLocal = localType in listOf(
            RecurrenceType.EVERY_N_DAYS, RecurrenceType.EVERY_N_WEEKS, RecurrenceType.EVERY_N_MONTHS
        )

        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Frequency", style = AppTextStyles.cardTitle) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { 
                                selectedTabIndex = 0 
                                if (localType == RecurrenceType.CRON) localType = RecurrenceType.MONTHLY
                            },
                            text = { Text("PRESETS") }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { 
                                selectedTabIndex = 1 
                                localType = RecurrenceType.CRON
                            },
                            text = { Text("CUSTOM") }
                        )
                    }

                    if (selectedTabIndex == 0) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            presets.forEach { preset ->
                                val isSelected = localType == preset.type && localInterval == preset.interval
                                EntryActionChip(
                                    icon = when(preset.type) {
                                        RecurrenceType.ONE_TIME -> Icons.Rounded.Done
                                        RecurrenceType.DAILY -> Icons.Rounded.Update
                                        RecurrenceType.WEEKLY -> Icons.Rounded.DateRange
                                        RecurrenceType.MONTHLY -> Icons.Rounded.CalendarMonth
                                        RecurrenceType.YEARLY -> Icons.Rounded.EventAvailable
                                        else -> Icons.Rounded.AutoMode
                                    },
                                    label = preset.label,
                                    isSelected = isSelected,
                                    onClick = {
                                        localType = preset.type
                                        localInterval = preset.interval
                                        localDayOfWeek = null
                                        localDayOfMonth = null
                                    }
                                )
                            }
                        }
                    } else {
                        var showCronHelp by remember { mutableStateOf(false) }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isError = cronText.isNotBlank() && parsedCronRule == null
                                OutlinedTextField(
                                    value = cronText,
                                    onValueChange = { cronText = it },
                                    label = { Text("Cron Expression") },
                                    placeholder = { Text("e.g. 0 0 * * *") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    isError = isError,
                                    supportingText = {
                                        if (isError) {
                                            Text("Invalid cron expression", color = AppColors.error)
                                        } else {
                                            Text("Unix format: Min Hour Day Month Weekday")
                                        }
                                    }
                                )
                                IconButton(
                                    onClick = { showCronHelp = true },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.HelpOutline,
                                        contentDescription = "Cron Help",
                                        tint = if (isError) AppColors.error else AppColors.primary
                                    )
                                }
                            }

                            if (showCronHelp) {
                                Surface(
                                    color = AppColors.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Cron Usage Guide",
                                                style = AppTextStyles.listSectionHeader
                                            )
                                            IconButton(
                                                onClick = { showCronHelp = false },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val helpText = """
                                            Format: min hour day month weekday
                                            • * : any value
                                            • , : value list (1,3,5)
                                            • - : range (1-5)
                                            • / : step values (*/2)
                                            
                                            Examples:
                                            • 0 0 * * * : Daily at midnight
                                            • 0 0 1 * * : 1st of every month
                                            • 0 12 * * 1-5 : Noon on weekdays
                                            • 0 0 */2 * * : Every 2 days
                                        """.trimIndent()
                                        Text(helpText, style = AppTextStyles.caption)
                                    }
                                }
                            }
                        }
                        
                        if (needsIntervalLocal) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Every", modifier = Modifier.weight(1f))
                                FilledIconButton(onClick = { localInterval = (localInterval - 1).coerceAtLeast(1) }) {
                                    Icon(Icons.Rounded.Remove, null)
                                }
                                Text(localInterval.toString(), modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                                FilledIconButton(onClick = { localInterval = (localInterval + 1).coerceAtLeast(1) }) {
                                    Icon(Icons.Rounded.Add, null)
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { stage = 1 }) { Text("BACK") } },
            confirmButton = { 
                TextButton(
                    onClick = { stage = 3 },
                    enabled = localType != RecurrenceType.CRON || (cronText.isNotBlank() && parsedCronRule != null)
                ) { Text("NEXT") } 
            }
        )
    }

    if (stage == 3) {
        val rule = RecurrenceRule(localType, localInterval, localDayOfWeek, localDayOfMonth, if (localType == RecurrenceType.CRON) cronText else null)
        val summary = PlannedPaymentService.buildScheduleSummary(localStart, rule)
        val previewDates = remember(localStart, rule) {
            PlannedPaymentService.generateOccurrencesPreview(localStart, rule, 5)
        }
        
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Confirm Schedule", style = AppTextStyles.cardTitle) },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Verify your recurrence settings before saving.", style = AppTextStyles.transactionTitle)
                    Surface(
                        color = AppColors.accentContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = summary,
                            modifier = Modifier.padding(16.dp),
                            style = AppTextStyles.listItemTitle,
                            color = AppColors.primary
                        )
                    }

                    if (previewDates.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Next occurrences:",
                                style = AppTextStyles.drawerItemLabel.copy(color = AppColors.border)
                            )
                            previewDates.forEach { date ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Event,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = AppColors.border
                                    )
                                    Text(
                                        date,
                                        style = AppTextStyles.caption.copy(color = AppColors.contentSecondary)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { stage = 2 }) { Text("BACK") } },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm(localStart, localType, localInterval, localDayOfWeek, localDayOfMonth, if (localType == RecurrenceType.CRON) cronText else null)
                    onDismiss()
                }) { Text("DONE") }
            }
        )
    }
}
