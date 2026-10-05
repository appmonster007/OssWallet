package com.osscabinet.ossWallet.ui.screens.import_csv.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.import_csv.CsvImportSession
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSourceApp
import com.osscabinet.ossWallet.ui.components.AeonDashedButton
import com.osscabinet.ossWallet.ui.components.AeonHeroHeader
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.DefaultAccentColorHex
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews

@Composable
fun SourceSelectionStep(
    session: CsvImportSession?,
    onLoadFile: () -> Unit,
    onConfirm: (ImportSourceApp) -> Unit,
    fileLoadError: String?,
    onApplyDelimiterAndEncoding: (Char, String) -> Unit = { _, _ -> }
) {
    var selected by remember(session?.sourceApp) {
        mutableStateOf(session?.sourceApp ?: ImportSourceApp.GENERIC)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        if (session == null) {
            FileNotLoadedContent(fileLoadError, onLoadFile)
        } else {
            SourceAppSelectionContent(
                session,
                selected,
                onConfirm = { selected = it },
                onContinue = { onConfirm(selected) },
                onApplyDelimiterAndEncoding = onApplyDelimiterAndEncoding
            )
        }
    }
}

@Composable
private fun FileNotLoadedContent(fileLoadError: String?, onLoadFile: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXl)
    ) {
        if (fileLoadError != null) {
            val errorColor = AppColors.error
            val errorAccentHex = remember(errorColor) {
                String.format("#%06X", 0xFFFFFF and errorColor.toArgb())
            }
            AeonHeroHeader(
                title = "File Error",
                subtitle = "Could not open CSV file",
                iconName = "BrokenImage",
                accentColorHex = errorAccentHex,
                isLeftAligned = false
            )
            Text(
                "The file may have been moved or is not a valid CSV. Try choosing a different file.",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingLg)
            )
        } else {
            AeonHeroHeader(
                title = "Import Transactions",
                subtitle = "Supports BudgetBakers, Cashew and Generic CSV",
                iconName = "FileOpen",
                accentColorHex = DefaultAccentColorHex,
                isLeftAligned = false
            )
            Text(
                "Supported formats include Wallet by BudgetBakers, Cashew, Money Manager, and any CSV with a valid header row.",
                style = AppTextStyles.caption,
                textAlign = TextAlign.Center,
                color = AppColors.contentSecondary,
                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingLg)
            )
            SupportedAppsChips()
        }

        AeonDashedButton(
            onClick = onLoadFile,
            text = if (fileLoadError != null) "Try Another File" else "Choose CSV File",
            icon = Icons.Rounded.AttachFile
        )
    }
}

@Composable
private fun SupportedAppsChips() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
    ) {
        listOf("BudgetBakers", "Cashew", "Any CSV").forEach { label ->
            Surface(
                color = AppColors.cardSurfaceRaised,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(
                        horizontal = AeonTheme.dimensions.spacingMd,
                        vertical = 6.dp
                    ),
                    style = AppTextStyles.chipLabel,
                    color = AppColors.contentSecondary
                )
            }
        }
    }
}

@Composable
private fun SourceAppSelectionContent(
    session: CsvImportSession,
    selected: ImportSourceApp,
    onConfirm: (ImportSourceApp) -> Unit,
    onContinue: () -> Unit,
    onApplyDelimiterAndEncoding: (Char, String) -> Unit
) {
    Text(
        "Where is this file from?",
        style = AppTextStyles.heroAmount,
        color = AppColors.contentPrimary
    )
    Text(
        "Select the app you exported from. OssWallet uses this to apply the right import rules.",
        style = AppTextStyles.caption,
        color = AppColors.contentSecondary
    )

    val sourceApp = session.sourceApp
    if (sourceApp != null && sourceApp != ImportSourceApp.GENERIC) {
        AutoDetectedBadge(sourceApp.displayName)
    }

    ImportSourceApp.entries.forEach { app ->
        SourceAppItem(
            app = app,
            isSelected = selected == app,
            onClick = { onConfirm(app) }
        )
    }

    BackupWarningCard()

    AdvancedDelimiterEncodingSection(session, onApplyDelimiterAndEncoding)

    Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
    AeonDashedButton(
        onClick = onContinue,
        text = "Continue",
        icon = Icons.AutoMirrored.Rounded.ArrowForward
    )
    Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
}

private val commonEncodings = listOf("UTF-8", "Windows-1252", "ISO-8859-1", "UTF-16")
private val delimiterOptions = listOf(',' to "Comma (,)", ';' to "Semicolon (;)", '\t' to "Tab")

@Composable
private fun AdvancedDelimiterEncodingSection(
    session: CsvImportSession,
    onApply: (Char, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var delimiter by remember(session.delimiter) { mutableStateOf(session.delimiter) }
    var encoding by remember(session.encoding) { mutableStateOf(session.encoding) }
    var delimiterMenuOpen by remember { mutableStateOf(false) }
    var encodingMenuOpen by remember { mutableStateOf(false) }

    TextButton(onClick = { expanded = !expanded }) {
        Icon(
            imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = AppColors.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            "Advanced: delimiter & encoding",
            style = AppTextStyles.caption,
            color = AppColors.primary
        )
    }

    if (expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Delimiter",
                    style = AppTextStyles.overlineLabel,
                    color = AppColors.contentSecondary
                )
                Box {
                    Surface(
                        onClick = { delimiterMenuOpen = true },
                        color = AppColors.surfaceContainerLow,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)
                    ) {
                        Text(
                            delimiterOptions.find { it.first == delimiter }?.second
                                ?: delimiter.toString(),
                            modifier = Modifier.padding(
                                horizontal = AeonTheme.dimensions.spacingMd,
                                vertical = AeonTheme.dimensions.spacingSm
                            ),
                            style = AppTextStyles.caption
                        )
                    }
                    DropdownMenu(
                        expanded = delimiterMenuOpen,
                        onDismissRequest = { delimiterMenuOpen = false }) {
                        delimiterOptions.forEach { (char, label) ->
                            DropdownMenuItem(
                                text = { Text(label, style = AppTextStyles.caption) },
                                onClick = {
                                    delimiter = char
                                    delimiterMenuOpen = false
                                    onApply(char, encoding)
                                }
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Encoding",
                    style = AppTextStyles.overlineLabel,
                    color = AppColors.contentSecondary
                )
                Box {
                    Surface(
                        onClick = { encodingMenuOpen = true },
                        color = AppColors.surfaceContainerLow,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)
                    ) {
                        Text(
                            encoding,
                            modifier = Modifier.padding(
                                horizontal = AeonTheme.dimensions.spacingMd,
                                vertical = AeonTheme.dimensions.spacingSm
                            ),
                            style = AppTextStyles.caption
                        )
                    }
                    DropdownMenu(
                        expanded = encodingMenuOpen,
                        onDismissRequest = { encodingMenuOpen = false }) {
                        commonEncodings.forEach { enc ->
                            DropdownMenuItem(
                                text = { Text(enc, style = AppTextStyles.caption) },
                                onClick = {
                                    encoding = enc
                                    encodingMenuOpen = false
                                    onApply(delimiter, enc)
                                }
                            )
                        }
                    }
                }
            }
            Text(
                "If the columns above look garbled, the source app may use a different delimiter or encoding — changing these re-reads the file.",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary
            )
        }
    }
}

@Composable
private fun BackupWarningCard() {
    Surface(
        color = AppColors.errorContainer.copy(alpha = 0.1f),
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        border = BorderStroke(0.5.dp, AppColors.error.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = AppColors.error,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    "Recommendation: Take a Backup",
                    style = AppTextStyles.listItemTitle,
                    color = AppColors.error
                )
                Text(
                    "Large imports can significantly change your data. We recommend taking a manual backup from Data Management settings before proceeding, as imports cannot be easily undone.",
                    style = AppTextStyles.caption,
                    color = AppColors.contentSecondary
                )
            }
        }
    }
}

@Composable
private fun AutoDetectedBadge(appName: String) {
    Surface(
        color = AppColors.primaryContainer,
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)
    ) {
        Row(
            modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = AppColors.primary
            )
            Text(
                "Auto-detected as $appName",
                style = AppTextStyles.caption,
                color = AppColors.onPrimaryContainer
            )
        }
    }
}

@Composable
fun SourceAppItem(
    app: ImportSourceApp,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        color = if (isSelected) AppColors.primaryContainer else AppColors.cardSurface,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 0.5.dp,
            if (isSelected) AppColors.primary else AppColors.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
        ) {
            RadioButton(selected = isSelected, onClick = onClick)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.displayName,
                    style = AppTextStyles.listItemTitle,
                    color = AppColors.contentPrimary
                )
                Text(
                    when (app) {
                        ImportSourceApp.BUDGET_BAKERS -> "Wallet by BudgetBakers — semicolon-delimited"
                        ImportSourceApp.CASHEW -> "Cashew — comma-delimited with subcategories"
                        ImportSourceApp.MONEY_MANAGER -> "Money Manager — standard CSV"
                        ImportSourceApp.GENERIC -> "Any other CSV with a header row"
                    },
                    style = AppTextStyles.caption,
                    color = AppColors.contentSecondary
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SourceSelectionStepPreview() {
    OssWalletTheme {
        Surface(color = AppColors.screenBackground) {
            SourceSelectionStep(
                session = null,
                onLoadFile = {},
                onConfirm = {},
                fileLoadError = null
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SourceSelectionStepFileErrorPreview() {
    OssWalletTheme {
        Surface(color = AppColors.screenBackground) {
            SourceSelectionStep(
                session = null,
                onLoadFile = {},
                onConfirm = {},
                fileLoadError = "Unable to read the selected file."
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SourceSelectionStepLoadedPreview() {
    OssWalletTheme {
        Surface(color = AppColors.screenBackground) {
            SourceSelectionStep(
                session = CsvImportSession(
                    headers = listOf("Date", "Account", "Amount"),
                    previewRows = emptyList(),
                    mapping = emptyMap(),
                    sourceApp = ImportSourceApp.BUDGET_BAKERS
                ),
                onLoadFile = {},
                onConfirm = {},
                fileLoadError = null
            )
        }
    }
}
