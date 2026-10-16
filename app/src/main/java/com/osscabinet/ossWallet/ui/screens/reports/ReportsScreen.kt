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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.entry.EntrySection
import com.osscabinet.ossWallet.ui.components.transactions.FilterFields
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer

@Composable
fun ReportsScreen(
    onBackClick: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(state.format.mimeType)
    ) { uri -> if (uri != null) viewModel.saveTo(uri.toString()) }

    val currencies = remember(state.accounts) {
        state.accounts.flatMap { it.allowedCurrencies.ifEmpty { listOf(it.currencyCode) } }.distinct().sorted()
    }

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
                    ReportSummaryCard(state = state, onReset = viewModel::resetFilters)

                    EntrySection(title = "PERIOD", icon = Icons.Rounded.DateRange) {
                        // The same period control the Records filter uses (presets, rolling windows, custom range).
                        AeonTimelinePeriodSelector(
                            currentRange = state.filter.timelineRange,
                            onRangeSelected = { viewModel.setFilter(state.filter.copy(timelineRange = it)) },
                            swipeEntireComponent = true,
                            isFlush = true
                        )
                    }

                    // Same filter fields as the Records filter, minus warranty and linked records.
                    // They bring their own section cards, so they sit directly in the page.
                    FilterFields(
                        filter = state.filter,
                        onFilterChange = viewModel::setFilter,
                        accounts = state.accounts,
                        allCategories = state.categories,
                        allTags = state.tags,
                        onSearchTransactions = viewModel::searchTransactions,
                        allAvailableCurrencies = currencies,
                        filterRules = ReportsViewModel.RULES
                    )

                    EntrySection(title = "OPTIONS", icon = Icons.Rounded.Tune) {
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

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = { AeonTopBarNavTitlePill(title = "Reports", onBackClick = onBackClick) }
                )
            )
        }
    }
}

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
