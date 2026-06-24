package com.osscabinet.ossWallet.ui.screens.import_csv.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.import_csv.CsvImportSession
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSourceApp
import com.osscabinet.ossWallet.domain.model.import_csv.OssWalletField
import com.osscabinet.ossWallet.ui.components.AeonDashedButton
import com.osscabinet.ossWallet.ui.components.AeonSectionHeader
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews

@Composable
fun ColumnMappingStep(
    session: CsvImportSession,
    onUpdateMapping: (OssWalletField, Int?) -> Unit,
    onApplyPreset: (ImportSourceApp) -> Unit,
    onNext: () -> Unit,
    isLoading: Boolean
) {
    var customiseMappings by remember { mutableStateOf(false) }

    LaunchedEffect(session.sourceApp) {
        customiseMappings = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        SourceAppDropdown(session.sourceApp, onApplyPreset)
        
        AeonSectionHeader(title = "Preview \u2014 first ${session.previewRows.size} rows")
        CsvPreviewTable(session)

        HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))

        val hasPreset = session.sourceApp == ImportSourceApp.BUDGET_BAKERS || session.sourceApp == ImportSourceApp.CASHEW
        
        if (hasPreset && !customiseMappings) {
            ReadOnlyMappings(session, onCustomise = { customiseMappings = true })
        } else {
            EditableMappings(session, onUpdateMapping)
        }

        Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
        AeonDashedButton(
            onClick = onNext,
            text = "Continue",
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            enabled = !isLoading
        )
        Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
    }
}

@Composable
private fun SourceAppDropdown(
    currentApp: ImportSourceApp?,
    onApplyPreset: (ImportSourceApp) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
    ) {
        Text("Import as:", style = AppTextStyles.caption, color = AppColors.contentSecondary)
        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                color = AppColors.surfaceContainerLow,
                border = BorderStroke(1.dp, AppColors.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        currentApp?.displayName ?: "Select app…",
                        style = AppTextStyles.caption,
                        color = if (currentApp != null) AppColors.contentPrimary else AppColors.contentSecondary
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AppColors.contentSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = AppColors.surfaceContainerHigh,
                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)
            ) {
                ImportSourceApp.entries.forEach { app ->
                    DropdownMenuItem(
                        text = { Text(app.displayName, style = AppTextStyles.caption) },
                        onClick = {
                            onApplyPreset(app)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CsvPreviewTable(session: CsvImportSession) {
    Surface(
        color = AppColors.cardSurface,
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        border = BorderStroke(0.5.dp, AppColors.outlineVariant)
    ) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Column {
                // Header row
                Row(modifier = Modifier.background(AppColors.cardSurfaceRaised)) {
                    session.headers.forEach { header ->
                        Text(
                            text = header,
                            modifier = Modifier.width(120.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                            style = AppTextStyles.tableColumnHeader,
                            color = AppColors.contentSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
                // Data rows
                session.previewRows.take(3).forEachIndexed { rowIdx, row ->
                    val rowBg = if (rowIdx % 2 == 0) AppColors.cardSurface else AppColors.cardSurfaceRaised
                    Row(modifier = Modifier.background(rowBg)) {
                        row.forEach { cell ->
                            Text(
                                text = cell,
                                modifier = Modifier.width(120.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                                style = AppTextStyles.caption,
                                color = AppColors.contentSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyMappings(session: CsvImportSession, onCustomise: () -> Unit) {
    Text("Column Mappings", style = AppTextStyles.formSectionLabel, color = AppColors.contentSecondary)
    OssWalletField.entries.forEach { field ->
        val colIndex = session.mapping[field]
        val colName = if (colIndex != null && colIndex < session.headers.size) session.headers[colIndex] else null
        if (colName != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(field.displayName, style = AppTextStyles.listItemTitle, color = AppColors.contentPrimary, modifier = Modifier.weight(1f))
                Text(colName, style = AppTextStyles.caption, color = AppColors.contentSecondary)
            }
        }
    }
    TextButton(onClick = onCustomise) {
        Text("Customise mappings", style = AppTextStyles.buttonLabel, color = AppColors.primary)
    }
}

@Composable
private fun EditableMappings(session: CsvImportSession, onUpdateMapping: (OssWalletField, Int?) -> Unit) {
    Text("Map Columns", style = AppTextStyles.formSectionLabel, color = AppColors.contentSecondary)
    OssWalletField.entries.forEach { field ->
        FieldMappingItem(field, session, onUpdateMapping)
    }
}

@Composable
private fun FieldMappingItem(
    field: OssWalletField,
    session: CsvImportSession,
    onUpdateMapping: (OssWalletField, Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentColIndex = session.mapping[field]
    val selectedText = if (currentColIndex != null && currentColIndex < session.headers.size) {
        session.headers[currentColIndex]
    } else "Skip"

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = field.displayName,
                modifier = Modifier.weight(1f),
                style = AppTextStyles.listItemTitle,
                color = AppColors.contentPrimary
            )
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1.5f)) {
                Surface(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                    color = AppColors.surfaceContainerLow,
                    border = BorderStroke(1.dp, AppColors.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedText,
                            style = AppTextStyles.caption,
                            color = if (currentColIndex != null) AppColors.contentPrimary else AppColors.contentSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = AppColors.contentSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = AppColors.surfaceContainerHigh,
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                    modifier = Modifier.widthIn(min = 160.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Skip", style = AppTextStyles.caption) },
                        onClick = { onUpdateMapping(field, null); expanded = false }
                    )
                    session.headers.forEachIndexed { index, header ->
                        DropdownMenuItem(
                            text = { Text(header, style = AppTextStyles.caption) },
                            onClick = { onUpdateMapping(field, index); expanded = false }
                        )
                    }
                }
            }
        }
        val helperText = when (field) {
            OssWalletField.IS_TRANSFER -> "Identifies paired transfer rows (e.g. credit card payments)"
            OssWalletField.WARRANTY_MONTHS -> "Number of months warranty remaining on a purchased item"
            OssWalletField.LABELS -> "Comma or pipe-separated tags attached to the transaction"
            else -> null
        }
        if (helperText != null) {
            Text(
                helperText,
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary,
                modifier = Modifier.padding(start = 2.dp, top = 2.dp)
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ColumnMappingStepPreview() {
    OssWalletTheme {
        Surface(color = AppColors.screenBackground) {
            ColumnMappingStep(
                session = CsvImportSession(
                    headers = listOf("date", "account", "category", "amount", "note"),
                    previewRows = listOf(
                        listOf("2023-10-01", "HDFC", "Food", "500", "Dinner"),
                        listOf("2023-10-02", "Cash", "Travel", "100", "Bus")
                    ),
                    mapping = mapOf(
                        OssWalletField.DATE to 0,
                        OssWalletField.ACCOUNT to 1,
                        OssWalletField.CATEGORY to 2,
                        OssWalletField.AMOUNT to 3,
                        OssWalletField.NOTE to 4
                    ),
                    sourceApp = ImportSourceApp.GENERIC
                ),
                onUpdateMapping = { _, _ -> },
                onApplyPreset = {},
                onNext = {},
                isLoading = false
            )
        }
    }
}
