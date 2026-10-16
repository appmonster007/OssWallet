package com.osscabinet.ossWallet.ui.screens.reports

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.service.report.ReportCategoryTotal
import com.osscabinet.ossWallet.domain.service.report.ReportCurrencyTotals
import com.osscabinet.ossWallet.domain.service.report.ReportPeriod
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.entry.EntrySection
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

private const val COLLAPSED_CHIP_COUNT = 12

/** What the report would contain right now, so the filters can be tuned before anything is generated. */
@Composable
fun ReportSummaryCard(state: ReportsUiState, onReset: () -> Unit) {
    val preview = state.preview
    val filtered = state.period != ReportPeriod.ThisMonth || state.selectedAccountIds.isNotEmpty() ||
        state.selectedCategoryIds.isNotEmpty() || state.selectedTags.isNotEmpty() ||
        state.kind != ReportKind.ALL || state.includeTransfers

    EntrySection(title = "PREVIEW", icon = Icons.Rounded.Insights, modifier = Modifier.animateContentSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        preview == null && state.previewLoading -> "Counting…"
                        preview == null -> "Couldn't preview"
                        preview.rowCount == 0 -> "No transactions"
                        preview.rowCount == 1 -> "1 transaction"
                        else -> "${preview.rowCount} transactions"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppColors.contentPrimary
                )
                preview?.let {
                    Text(it.rangeText, style = AppTextStyles.caption, color = AppColors.contentSecondary)
                }
            }
            if (state.previewLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else if (filtered) TextButton(onClick = onReset) { Text("Reset") }
        }

        if (preview != null && preview.rowCount == 0) {
            Text(
                "Nothing matches these filters. Try a longer period or clear some filters.",
                style = AppTextStyles.caption, color = AppColors.contentSecondary
            )
        }

        if (preview != null && preview.totals.isNotEmpty()) {
            HorizontalDivider(color = AppColors.outlineVariant.copy(alpha = 0.4f))
            preview.totals.forEach { t ->
                CurrencyTotalsRow(t, showCurrency = preview.totals.size > 1)
            }
            val top = preview.topCategories.groupBy { it.currency }
            if (top.isNotEmpty()) {
                HorizontalDivider(color = AppColors.outlineVariant.copy(alpha = 0.4f))
                Text("TOP SPENDING", style = AppTextStyles.caption, color = AppColors.contentSecondary)
                top.forEach { (currency, list) ->
                    list.take(2).forEach { CategoryBar(it, currency) }
                }
            }
        }
    }
}

@Composable
private fun CurrencyTotalsRow(t: ReportCurrencyTotals, showCurrency: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (showCurrency) Text(t.currency, style = AppTextStyles.caption, color = AppColors.contentSecondary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Income", ReportPdfWriter.money(t.income, t.currency, signed = true), AppColors.positive)
            Stat("Expenses", ReportPdfWriter.money(t.expense, t.currency, signed = true), AppColors.negative)
            Stat(
                "Net", ReportPdfWriter.money(t.net, t.currency, signed = true),
                if (t.net.signum() < 0) AppColors.negative else AppColors.contentPrimary
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = AppTextStyles.caption, color = AppColors.contentSecondary)
        Text(value, style = AppTextStyles.transactionTitle, color = color, maxLines = 1)
    }
}

@Composable
private fun CategoryBar(item: ReportCategoryTotal, currency: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                item.category, style = AppTextStyles.caption, color = AppColors.contentPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Text(
                ReportPdfWriter.money(item.amount, currency) + "  ${(item.share * 100).toInt()}%",
                style = AppTextStyles.caption, color = AppColors.contentSecondary, maxLines = 1
            )
        }
        LinearProgressIndicator(
            progress = { item.share.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = AppColors.primary,
            trackColor = AppColors.surfaceContainerHigh,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
    }
}

/**
 * Multi-select chips with an "all" chip, plus search and show-more once the list is long.
 * Selecting nothing means everything, so the "all" chip is the selected state of an empty selection.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipPicker(
    items: List<T>,
    itemKey: (T) -> Any,
    itemLabel: (T) -> String,
    selected: (T) -> Boolean,
    onToggle: (T) -> Unit,
    allLabel: String,
    noneSelected: Boolean,
    onClear: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val searchable = items.size > COLLAPSED_CHIP_COUNT
    val matches = if (query.isBlank()) items else items.filter { itemLabel(it).contains(query.trim(), ignoreCase = true) }
    // Selected chips stay visible when collapsed, otherwise a choice could hide itself.
    val visible = when {
        query.isNotBlank() || expanded || !searchable -> matches
        else -> matches.filterIndexed { i, item -> i < COLLAPSED_CHIP_COUNT || selected(item) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (searchable) {
            AeonSearchField(query = query, onQueryChange = { query = it }, placeholder = "Search…")
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = noneSelected, onClick = onClear, label = { Text(allLabel) })
            visible.forEach { item ->
                androidx.compose.runtime.key(itemKey(item)) {
                    FilterChip(selected = selected(item), onClick = { onToggle(item) }, label = { Text(itemLabel(item)) })
                }
            }
        }
        if (query.isNotBlank() && matches.isEmpty()) {
            Text("Nothing matches “${query.trim()}”.", style = AppTextStyles.caption, color = AppColors.contentSecondary)
        }
        if (searchable && query.isBlank() && matches.size > visible.size) {
            TextButton(onClick = { expanded = true }) { Text("Show all ${matches.size}") }
        } else if (searchable && query.isBlank() && expanded) {
            TextButton(onClick = { expanded = false }) { Text("Show fewer") }
        }
    }
}

@Composable
fun FormatCard(
    title: String,
    caption: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) AppColors.primary.copy(alpha = 0.12f) else AppColors.surfaceContainerHigh,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) AppColors.primary else AppColors.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = if (selected) AppColors.primary else AppColors.contentSecondary)
            Text(title, style = AppTextStyles.transactionTitle, color = AppColors.contentPrimary)
            Text(caption, style = AppTextStyles.caption, color = AppColors.contentSecondary)
        }
    }
}

/** Pinned to the bottom: create, then share / open / save once the file exists. */
@Composable
fun ReportActionBar(
    state: ReportsUiState,
    onCreate: () -> Unit,
    onShare: (ReportResult) -> Unit,
    onOpen: (ReportResult) -> Unit,
    onSave: (ReportResult) -> Unit
) {
    val result = state.result
    val count = state.preview?.rowCount ?: 0
    Surface(color = AppColors.surfaceContainerLow, tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.message?.let { Text(it, style = AppTextStyles.caption, color = AppColors.contentSecondary) }
            if (result != null) {
                Text(
                    "${result.format.label} ready · ${result.rowCount} transactions · ${fileSize(result.file.length())}",
                    style = AppTextStyles.caption, color = AppColors.contentSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { onShare(result) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.padding(end = 4.dp).size(18.dp))
                        Text("Share", maxLines = 1)
                    }
                    OutlinedButton(onClick = { onOpen(result) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.padding(end = 4.dp).size(18.dp))
                        Text("Open", maxLines = 1)
                    }
                    OutlinedButton(onClick = { onSave(result) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.padding(end = 4.dp).size(18.dp))
                        Text("Save", maxLines = 1)
                    }
                }
            } else {
                Button(
                    onClick = onCreate,
                    enabled = !state.generating && count > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.generating) {
                        CircularProgressIndicator(Modifier.padding(end = 8.dp).size(18.dp), strokeWidth = 2.dp)
                    }
                    Text(
                        when {
                            state.generating -> "Creating…"
                            count > 0 -> "Create ${state.format.label} report · $count transactions"
                            else -> "Create ${state.format.label} report"
                        }
                    )
                }
            }
        }
    }
}

private fun fileSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}
