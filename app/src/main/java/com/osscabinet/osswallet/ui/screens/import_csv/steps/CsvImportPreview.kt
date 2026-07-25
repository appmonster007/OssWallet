package com.osscabinet.ossWallet.ui.screens.import_csv.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.import_csv.DuplicatePolicy
import com.osscabinet.ossWallet.domain.model.import_csv.ImportPreview
import com.osscabinet.ossWallet.ui.components.AeonDashedButton
import com.osscabinet.ossWallet.ui.components.AeonSectionHeader
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews

@Composable
fun PreviewStep(
    preview: ImportPreview?,
    duplicatePolicy: DuplicatePolicy,
    selectedDuplicates: Set<Int>,
    onSetDuplicatePolicy: (DuplicatePolicy) -> Unit,
    onToggleDuplicate: (Int) -> Unit,
    onImport: () -> Unit,
    isLoading: Boolean
) {
    if (preview == null) return

    var showDuplicatesDetail by remember { mutableStateOf(false) }
    var showErrorsDetail by remember { mutableStateOf(false) }

    val effectiveCount = when (duplicatePolicy) {
        DuplicatePolicy.SKIP -> preview.validRows - preview.duplicates.size
        else -> preview.validRows
    }.coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        ImportSummaryCard(preview, effectiveCount)

        if (preview.unpairedTransferRows > 0) {
            UnpairedTransferWarning(preview.unpairedTransferRows)
        }

        if (preview.duplicates.isNotEmpty()) {
            DuplicatePolicySection(
                preview = preview,
                currentPolicy = duplicatePolicy,
                selectedDuplicates = selectedDuplicates,
                showDetail = showDuplicatesDetail,
                onSetPolicy = onSetDuplicatePolicy,
                onToggleDetail = { showDuplicatesDetail = !showDuplicatesDetail },
                onToggleDuplicate = onToggleDuplicate
            )
        }

        if (preview.parseErrors.isNotEmpty()) {
            ParseErrorsSection(
                errors = preview.parseErrors,
                showDetail = showErrorsDetail,
                onToggleDetail = { showErrorsDetail = !showErrorsDetail }
            )
        }

        SampleTransactionsSection(preview)

        Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
        AeonDashedButton(
            onClick = onImport,
            text = if (isLoading) "Importing…" else "Import $effectiveCount transaction${if (effectiveCount != 1) "s" else ""}",
            icon = Icons.Rounded.CheckCircle,
            enabled = effectiveCount > 0 && !isLoading
        )
        Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
    }
}

@Composable
private fun ImportSummaryCard(preview: ImportPreview, effectiveCount: Int) {
    AeonExpressiveCard(
        title = "Import Summary",
        icon = Icons.Rounded.Info,
        containerColor = AppColors.cardSurface,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem(
                    effectiveCount.toString(),
                    "will be imported",
                    AppColors.primary,
                    Modifier.weight(1f),
                    true
                )
                SummaryItem(
                    preview.totalRows.toString(),
                    "total rows",
                    AppColors.contentSecondary,
                    Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                val dupColor =
                    if (preview.duplicates.isNotEmpty()) AppColors.accent else AppColors.contentSecondary
                SummaryItem(
                    preview.duplicates.size.toString(),
                    "duplicates",
                    dupColor,
                    Modifier.weight(1f)
                )

                val errColor =
                    if (preview.parseErrors.isNotEmpty()) AppColors.error else AppColors.contentSecondary
                SummaryItem(
                    preview.parseErrors.size.toString(),
                    "parse errors",
                    errColor,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    isHero: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            value,
            style = if (isHero) AppTextStyles.heroAmount.copy(fontWeight = FontWeight.Bold) else AppTextStyles.heroAmount,
            color = color
        )
        Text(label, style = AppTextStyles.caption, color = AppColors.contentSecondary)
    }
}

@Composable
private fun UnpairedTransferWarning(count: Int) {
    Surface(
        color = AppColors.errorContainer,
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)
    ) {
        Row(
            modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = AppColors.error,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Column {
                Text(
                    "$count unpaired transfer row${if (count > 1) "s" else ""}",
                    style = AppTextStyles.cardSectionTitle,
                    color = AppColors.onErrorContainer
                )
                Text(
                    "Some transfer transactions are missing their counterpart in this file. These will be imported as outside-wallet transfers.",
                    style = AppTextStyles.caption,
                    color = AppColors.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun DuplicatePolicySection(
    preview: ImportPreview,
    currentPolicy: DuplicatePolicy,
    selectedDuplicates: Set<Int>,
    showDetail: Boolean,
    onSetPolicy: (DuplicatePolicy) -> Unit,
    onToggleDetail: () -> Unit,
    onToggleDuplicate: (Int) -> Unit
) {
    HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))
    Text(
        "DUPLICATE POLICY",
        style = AppTextStyles.formSectionLabel,
        color = AppColors.contentSecondary
    )

    DuplicatePolicy.entries.forEach { policy ->
        DuplicatePolicyItem(
            policy = policy,
            count = preview.duplicates.size,
            isSelected = currentPolicy == policy,
            onSelect = { onSetPolicy(policy) }
        )
    }

    TextButton(onClick = onToggleDetail) {
        Text(
            if (showDetail) "Hide Duplicates" else "Show ${preview.duplicates.size} Duplicates",
            style = AppTextStyles.buttonLabel,
            color = AppColors.primary
        )
    }

    AnimatedVisibility(visible = showDetail) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            preview.duplicates.forEachIndexed { idx, dup ->
                DuplicateRowItem(
                    idx = idx,
                    incomingRowIndex = dup.incomingRowIndex,
                    existingNote = dup.existingNote,
                    incomingValues = dup.incomingValues,
                    isSelected = idx in selectedDuplicates,
                    onToggle = { onToggleDuplicate(idx) }
                )
            }
        }
    }
}

@Composable
private fun DuplicatePolicyItem(
    policy: DuplicatePolicy,
    count: Int,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val (title, icon) = when (policy) {
        DuplicatePolicy.SKIP -> "Skip duplicates" to Icons.Rounded.Info
        DuplicatePolicy.IMPORT_BOTH -> "Import both" to Icons.Rounded.AutoAwesome
        DuplicatePolicy.REPLACE_EXISTING -> "Replace existing" to Icons.Rounded.Warning
    }

    val consequenceText = when (policy) {
        DuplicatePolicy.SKIP -> "$count existing transaction${if (count != 1) "s" else ""} will not be overwritten"
        DuplicatePolicy.IMPORT_BOTH -> "Creates $count additional transaction${if (count != 1) "s" else ""} alongside existing ones"
        DuplicatePolicy.REPLACE_EXISTING -> "$count existing transaction${if (count != 1) "s" else ""} will be permanently updated"
    }
    val consequenceColor =
        if (policy == DuplicatePolicy.REPLACE_EXISTING) AppColors.error else AppColors.contentSecondary

    Surface(
        onClick = onSelect,
        color = if (isSelected) AppColors.accentContainer.copy(alpha = 0.15f) else AppColors.cardSurface,
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        border = if (isSelected) BorderStroke(1.dp, AppColors.accent) else BorderStroke(
            0.5.dp,
            AppColors.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)
        ) {
            RadioButton(selected = isSelected, onClick = onSelect)
            Spacer(Modifier.width(AeonTheme.dimensions.spacingSm))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) AppColors.accent else AppColors.contentSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(AeonTheme.dimensions.spacingSm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) AppColors.contentPrimary else AppColors.contentSecondary
                )
                Text(
                    consequenceText,
                    style = AppTextStyles.caption,
                    color = if (isSelected) AppColors.contentPrimary.copy(alpha = 0.8f) else consequenceColor
                )
            }
        }
    }
}

@Composable
private fun DuplicateRowItem(
    idx: Int,
    incomingRowIndex: Int,
    existingNote: String,
    incomingValues: List<String>,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        color = AppColors.cardSurface,
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AeonTheme.dimensions.cardPadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(AeonTheme.dimensions.spacingMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Existing: ${existingNote.ifBlank { "(no note)" }}",
                    style = AppTextStyles.caption,
                    color = AppColors.contentSecondary
                )
                Text(
                    "Incoming row $incomingRowIndex: ${
                        incomingValues.take(4).joinToString(" · ")
                    }",
                    style = AppTextStyles.caption,
                    color = AppColors.contentPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ParseErrorsSection(
    errors: List<com.osscabinet.ossWallet.domain.model.import_csv.ParseError>,
    showDetail: Boolean,
    onToggleDetail: () -> Unit
) {
    HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))
    TextButton(onClick = onToggleDetail) {
        Text(
            if (showDetail) "Hide ${errors.size} Errors" else "Show ${errors.size} Parse Errors",
            style = AppTextStyles.buttonLabel,
            color = AppColors.error
        )
    }
    AnimatedVisibility(visible = showDetail) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            errors.forEach { err ->
                Text(
                    "Row ${err.rowIndex}: ${err.reason}",
                    style = AppTextStyles.caption,
                    color = AppColors.error
                )
            }
        }
    }
}

@Composable
private fun SampleTransactionsSection(preview: ImportPreview) {
    HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))
    AeonSectionHeader(title = "SAMPLE RESULT")
    Text(
        "Here is how some transactions will look in the app after import.",
        style = AppTextStyles.caption,
        color = AppColors.contentSecondary
    )

    Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
        preview.duplicates.take(2).forEach { dup ->
            SampleTransactionItem(
                title = dup.existingNote.ifBlank { "Transaction" },
                category = "Matched Category",
                amount = dup.incomingValues.find { it.contains(".") || it.toIntOrNull() != null }
                    ?: "0.00",
                date = "Oct 24, 2023",
                isDuplicate = true
            )
        }
        // If not enough duplicates, show some generic ones
        if (preview.duplicates.size < 3) {
            SampleTransactionItem(
                title = "Grocery Shopping",
                category = "Food & Drinks",
                amount = "-45.00",
                date = "Oct 25, 2023",
                isDuplicate = false
            )
        }
    }
}

@Composable
private fun SampleTransactionItem(
    title: String,
    category: String,
    amount: String,
    date: String,
    isDuplicate: Boolean
) {
    Surface(
        color = AppColors.cardSurface,
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        border = BorderStroke(0.5.dp, AppColors.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeonTheme.dimensions.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = AppColors.surfaceContainerHigh
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp),
                    tint = AppColors.primary
                )
            }
            Spacer(Modifier.width(AeonTheme.dimensions.spacingMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = AppTextStyles.listItemTitle,
                    color = AppColors.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    category,
                    style = AppTextStyles.caption,
                    color = AppColors.contentSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    amount,
                    style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold),
                    color = if (amount.startsWith("-")) AppColors.error else AppColors.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDuplicate) {
                        Surface(
                            color = AppColors.accentContainer,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                "Duplicate",
                                modifier = Modifier.padding(horizontal = 4.dp),
                                style = AppTextStyles.overlineLabel,
                                color = AppColors.onAccentContainer
                            )
                        }
                    }
                    Text(
                        date,
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun PreviewStepPreview() {
    OssWalletTheme {
        Surface(color = AppColors.screenBackground) {
            PreviewStep(
                preview = ImportPreview(
                    totalRows = 10,
                    validRows = 8,
                    duplicates = listOf(
                        com.osscabinet.ossWallet.domain.model.import_csv.DuplicateGroup(
                            "key1",
                            "Lunch",
                            3,
                            listOf("2023-10-01", "HDFC", "Food", "200")
                        ),
                        com.osscabinet.ossWallet.domain.model.import_csv.DuplicateGroup(
                            "key2",
                            "Dinner",
                            5,
                            listOf("2023-10-01", "Cash", "Food", "500")
                        )
                    ),
                    allCsvAccounts = emptyList(),
                    newCategories = emptyList(),
                    parseErrors = listOf(
                        com.osscabinet.ossWallet.domain.model.import_csv.ParseError(
                            2,
                            "Invalid amount format"
                        )
                    ),
                    unpairedTransferRows = 1
                ),
                duplicatePolicy = DuplicatePolicy.SKIP,
                selectedDuplicates = emptySet(),
                onSetDuplicatePolicy = {},
                onToggleDuplicate = {},
                onImport = {},
                isLoading = false
            )
        }
    }
}
