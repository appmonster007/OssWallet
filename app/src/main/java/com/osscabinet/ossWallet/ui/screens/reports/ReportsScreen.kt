package com.osscabinet.ossWallet.ui.screens.reports

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.service.report.ReportPeriod
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.entry.EntrySection
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBackClick: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pickingRange by remember { mutableStateOf(false) }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(state.format.mimeType)
    ) { uri -> if (uri != null) viewModel.saveTo(uri.toString()) }

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(containerColor = Color.Transparent) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            top = AeonSize.D64.value + AeonTheme.dimensions.screenPadding,
                            bottom = innerPadding.calculateBottomPadding() + 48.dp,
                            start = AeonTheme.dimensions.screenPadding,
                            end = AeonTheme.dimensions.screenPadding
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EntrySection(title = "PERIOD", icon = Icons.Rounded.Assessment) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReportPeriod.presets.forEach { p ->
                                FilterChip(
                                    selected = state.period == p,
                                    onClick = { viewModel.setPeriod(p) },
                                    label = { Text(p.label) }
                                )
                            }
                            FilterChip(
                                selected = state.period is ReportPeriod.Custom,
                                onClick = { pickingRange = true },
                                label = { Text((state.period as? ReportPeriod.Custom)?.label ?: "Custom range") }
                            )
                        }
                    }

                    EntrySection(title = "ACCOUNTS", icon = Icons.Rounded.Assessment) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.selectedAccountIds.isEmpty(),
                                onClick = viewModel::clearAccounts,
                                label = { Text("All accounts") }
                            )
                            state.accounts.forEach { a ->
                                FilterChip(
                                    selected = a.id in state.selectedAccountIds,
                                    onClick = { viewModel.toggleAccount(a.id) },
                                    label = { Text(a.name) }
                                )
                            }
                        }
                    }

                    if (state.tags.isNotEmpty()) {
                        EntrySection(title = "TAGS", icon = Icons.Rounded.Assessment) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = state.selectedTags.isEmpty(),
                                    onClick = viewModel::clearTags,
                                    label = { Text("All tags") }
                                )
                                state.tags.forEach { t ->
                                    FilterChip(
                                        selected = t.name in state.selectedTags,
                                        onClick = { viewModel.toggleTag(t.name) },
                                        label = { Text(t.name) }
                                    )
                                }
                            }
                            if (state.selectedTags.size > 1) {
                                Text(
                                    "Includes transactions with any of the selected tags.",
                                    style = AppTextStyles.caption, color = AppColors.contentSecondary
                                )
                            }
                        }
                    }

                    EntrySection(title = "OPTIONS", icon = Icons.Rounded.Assessment) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Include transfers", style = AppTextStyles.transactionTitle, color = AppColors.contentPrimary)
                                Text(
                                    "Moves between your own accounts. Left out of income and expense totals either way.",
                                    style = AppTextStyles.caption, color = AppColors.contentSecondary
                                )
                            }
                            Switch(checked = state.includeTransfers, onCheckedChange = viewModel::setIncludeTransfers)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReportFormat.entries.forEach { f ->
                                FilterChip(
                                    selected = state.format == f,
                                    onClick = { viewModel.setFormat(f) },
                                    label = { Text(f.label) }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = viewModel::generate,
                        enabled = !state.generating,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.generating) {
                            CircularProgressIndicator(Modifier.padding(end = 8.dp).then(Modifier), strokeWidth = 2.dp)
                        }
                        Text(if (state.generating) "Creating…" else "Create report")
                    }

                    state.message?.let { Text(it, style = AppTextStyles.caption, color = AppColors.contentSecondary) }

                    state.result?.let { result ->
                        EntrySection(title = "READY", icon = Icons.Rounded.Assessment) {
                            Text(
                                "${result.rowCount} transactions · ${result.format.label}",
                                style = AppTextStyles.transactionTitle, color = AppColors.contentPrimary
                            )
                            result.totals.forEach { t ->
                                Text(
                                    "${t.currency}: income ${ReportPdfWriter.money(t.income, t.currency)}, " +
                                        "expenses ${ReportPdfWriter.money(t.expense, t.currency)}, " +
                                        "net ${ReportPdfWriter.money(t.net, t.currency)}",
                                    style = AppTextStyles.caption, color = AppColors.contentSecondary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = {
                                    if (!shareFile(context, result)) viewModel.showMessage("No app available to share with.")
                                }) {
                                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                    Text("Share")
                                }
                                OutlinedButton(onClick = { saveLauncher.launch(result.file.name) }) {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                    Text("Save to…")
                                }
                            }
                        }
                    }
                }
            }

            if (pickingRange) {
                val pickerState = rememberDateRangePickerState()
                DatePickerDialog(
                    onDismissRequest = { pickingRange = false },
                    confirmButton = {
                        TextButton(
                            enabled = pickerState.selectedStartDateMillis != null,
                            onClick = {
                                val start = pickerState.selectedStartDateMillis
                                if (start != null) {
                                    // The picker reports UTC midnight of the chosen days.
                                    val a = Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate()
                                    val b = pickerState.selectedEndDateMillis
                                        ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: a
                                    viewModel.setPeriod(ReportPeriod.Custom(a, maxOf(a, b)))
                                }
                                pickingRange = false
                            }
                        ) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { pickingRange = false }) { Text("Cancel") } }
                ) {
                    DateRangePicker(state = pickerState, modifier = Modifier.weight(1f, fill = false))
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = { AeonTopBarNavTitlePill(title = "Reports", onBackClick = onBackClick) }
                )
            )
        }
    }
}

private fun maxOf(a: LocalDate, b: LocalDate): LocalDate = if (b.isBefore(a)) a else b

private fun shareFile(context: Context, result: ReportResult): Boolean {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", result.file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = result.format.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(result.file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(Intent.createChooser(send, "Share report").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
