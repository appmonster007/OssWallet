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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    ReportActionBar(
                        state = state,
                        onCreate = viewModel::generate,
                        onShare = { result ->
                            if (!shareFile(context, result)) viewModel.showMessage("No app available to share with.")
                        },
                        onOpen = { result ->
                            if (!openFile(context, result)) viewModel.showMessage("No app available to open this file.")
                        },
                        onSave = { result -> saveLauncher.launch(result.file.name) }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            top = AeonSize.D64.value + AeonTheme.dimensions.screenPadding,
                            bottom = innerPadding.calculateBottomPadding() + 24.dp,
                            start = AeonTheme.dimensions.screenPadding,
                            end = AeonTheme.dimensions.screenPadding
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ReportSummaryCard(
                        state = state,
                        onReset = viewModel::resetFilters
                    )

                    EntrySection(title = "PERIOD", icon = Icons.Rounded.DateRange) {
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

                    EntrySection(title = "SHOW", icon = Icons.Rounded.Tune) {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            ReportKind.entries.forEachIndexed { index, kind ->
                                SegmentedButton(
                                    selected = state.kind == kind,
                                    onClick = { viewModel.setKind(kind) },
                                    shape = SegmentedButtonDefaults.itemShape(index, ReportKind.entries.size)
                                ) { Text(kind.label) }
                            }
                        }
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
                    }

                    EntrySection(
                        title = "ACCOUNTS" + countSuffix(state.selectedAccountIds.size),
                        icon = Icons.Rounded.AccountBalanceWallet
                    ) {
                        ChipPicker(
                            items = state.accounts,
                            itemKey = { it.id },
                            itemLabel = { it.name },
                            selected = { it.id in state.selectedAccountIds },
                            onToggle = { viewModel.toggleAccount(it.id) },
                            allLabel = "All accounts",
                            noneSelected = state.selectedAccountIds.isEmpty(),
                            onClear = viewModel::clearAccounts
                        )
                    }

                    if (state.categories.isNotEmpty()) {
                        EntrySection(
                            title = "CATEGORIES" + countSuffix(state.selectedCategoryIds.size),
                            icon = Icons.Rounded.Category
                        ) {
                            ChipPicker(
                                items = state.categories,
                                itemKey = { it.id },
                                itemLabel = { it.name },
                                selected = { it.id in state.selectedCategoryIds },
                                onToggle = { viewModel.toggleCategory(it.id) },
                                allLabel = "All categories",
                                noneSelected = state.selectedCategoryIds.isEmpty(),
                                onClear = viewModel::clearCategories
                            )
                        }
                    }

                    if (state.tags.isNotEmpty()) {
                        EntrySection(
                            title = "TAGS" + countSuffix(state.selectedTags.size),
                            icon = Icons.Rounded.LocalOffer
                        ) {
                            ChipPicker(
                                items = state.tags,
                                itemKey = { it.name },
                                itemLabel = { it.name },
                                selected = { it.name in state.selectedTags },
                                onToggle = { viewModel.toggleTag(it.name) },
                                allLabel = "All tags",
                                noneSelected = state.selectedTags.isEmpty(),
                                onClear = viewModel::clearTags
                            )
                            if (state.selectedTags.size > 1) {
                                Text(
                                    "Includes transactions with any of the selected tags.",
                                    style = AppTextStyles.caption, color = AppColors.contentSecondary
                                )
                            }
                        }
                    }

                    EntrySection(title = "FORMAT", icon = Icons.Rounded.Description) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FormatCard(
                                title = "PDF", caption = "Formatted summary with category breakdown. Good for sharing.",
                                icon = Icons.Rounded.PictureAsPdf, selected = state.format == ReportFormat.PDF,
                                onClick = { viewModel.setFormat(ReportFormat.PDF) }, modifier = Modifier.weight(1f)
                            )
                            FormatCard(
                                title = "CSV", caption = "Every transaction as a spreadsheet row. Good for taxes.",
                                icon = Icons.Rounded.TableChart, selected = state.format == ReportFormat.CSV,
                                onClick = { viewModel.setFormat(ReportFormat.CSV) }, modifier = Modifier.weight(1f)
                            )
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

private fun countSuffix(n: Int) = if (n > 0) " · $n SELECTED" else ""

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

private fun openFile(context: Context, result: ReportResult): Boolean {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", result.file)
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, result.format.mimeType)
        clipData = ClipData.newRawUri(result.file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(Intent.createChooser(view, result.file.name).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
