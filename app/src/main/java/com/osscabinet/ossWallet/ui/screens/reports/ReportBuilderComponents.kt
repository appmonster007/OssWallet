package com.osscabinet.ossWallet.ui.screens.reports

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.service.report.ReportCategoryTotal
import com.osscabinet.ossWallet.domain.service.report.ReportCurrencyTotals
import com.osscabinet.ossWallet.ui.components.entry.EntrySection
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

// Column weights shared by the header and every currency row, so the numbers line up.
private const val CURRENCY_COL = 0.7f
private const val VALUE_COL = 1.1f

/** What the report would contain right now, so the filters can be tuned before anything is generated. */
@Composable
fun ReportSummaryCard(state: ReportsUiState, onReset: () -> Unit) {
    val preview = state.preview

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
                preview?.let { Text(it.rangeText, style = AppTextStyles.caption, color = AppColors.contentSecondary) }
            }
            if (state.previewLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else if (state.isFiltered) TextButton(onClick = onReset) { Text("Reset") }
        }

        if (preview != null && preview.rowCount == 0) {
            Text(
                "Nothing matches these filters. Try a longer period or clear some filters.",
                style = AppTextStyles.caption, color = AppColors.contentSecondary
            )
        }

        if (preview != null && preview.totals.isNotEmpty()) {
            Rule()
            TotalsTable(preview.totals)
            val top = preview.topCategories.groupBy { it.currency }
            if (top.isNotEmpty()) {
                Rule()
                Text("TOP SPENDING", style = AppTextStyles.caption, color = AppColors.contentSecondary)
                top.forEach { (currency, list) ->
                    if (top.size > 1) {
                        Text(currency, style = AppTextStyles.caption, color = AppColors.contentSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    list.take(3).forEach { CategoryRow(it, currency) }
                }
            }
        }
    }
}

@Composable
private fun Rule() = HorizontalDivider(color = AppColors.outlineVariant.copy(alpha = 0.4f))

/** One header row, then one row per currency; every numeric column is right-aligned to the same edge. */
@Composable
private fun TotalsTable(totals: List<ReportCurrencyTotals>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth()) {
            androidx.compose.foundation.layout.Spacer(Modifier.weight(CURRENCY_COL))
            HeaderCell("Income")
            HeaderCell("Expenses")
            HeaderCell("Net")
        }
        totals.forEach { t ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    t.currency,
                    style = AppTextStyles.transactionTitle, color = AppColors.contentPrimary,
                    modifier = Modifier.weight(CURRENCY_COL)
                )
                ValueCell(ReportPdfWriter.money(t.income, t.currency, signed = true, showCode = false), AppColors.positive)
                ValueCell(ReportPdfWriter.money(t.expense, t.currency, signed = true, showCode = false), AppColors.negative)
                ValueCell(
                    ReportPdfWriter.money(t.net, t.currency, signed = true, showCode = false),
                    if (t.net.signum() < 0) AppColors.negative else AppColors.contentPrimary,
                    bold = true
                )
            }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(text: String) {
    Text(
        text, style = AppTextStyles.caption, color = AppColors.contentSecondary,
        textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.weight(VALUE_COL)
    )
}

@Composable
private fun RowScope.ValueCell(text: String, color: Color, bold: Boolean = false) {
    Text(
        text,
        style = AppTextStyles.transactionTitle.copy(fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal),
        color = color, textAlign = TextAlign.End, maxLines = 1, softWrap = false,
        modifier = Modifier.weight(VALUE_COL)
    )
}

@Composable
private fun CategoryRow(item: ReportCategoryTotal, currency: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.category, style = AppTextStyles.transactionTitle, color = AppColors.contentPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Text(
                ReportPdfWriter.money(item.amount, currency, showCode = false),
                style = AppTextStyles.transactionTitle, color = AppColors.contentPrimary,
                textAlign = TextAlign.End, maxLines = 1, softWrap = false
            )
            // Fixed width so the amounts above stay aligned however many digits the percentage has.
            Text(
                "${(item.share * 100).toInt()}%",
                style = AppTextStyles.caption, color = AppColors.contentSecondary,
                textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(44.dp)
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

/** Pinned to the bottom: create, then share / open / save (icon buttons) once the file exists. */
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("${result.format.label} ready", style = AppTextStyles.transactionTitle, color = AppColors.contentPrimary)
                        Text(
                            "${result.rowCount} transactions · ${fileSize(result.file.length())}",
                            style = AppTextStyles.caption, color = AppColors.contentSecondary, maxLines = 1
                        )
                    }
                    FilledIconButton(onClick = { onShare(result) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share report")
                    }
                    FilledTonalIconButton(onClick = { onOpen(result) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open report")
                    }
                    FilledTonalIconButton(onClick = { onSave(result) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Download, contentDescription = "Save report to a folder")
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
