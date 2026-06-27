package com.osscabinet.ossWallet.ui.screens.import_csv.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.import_csv.CsvImportSession
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSourceApp
import com.osscabinet.ossWallet.ui.components.AeonDashedButton
import com.osscabinet.ossWallet.ui.components.AeonHeroHeader
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Composable
fun SourceSelectionStep(
    session: CsvImportSession?,
    onLoadFile: () -> Unit,
    onConfirm: (ImportSourceApp) -> Unit,
    fileLoadError: String?
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
            SourceAppSelectionContent(session, selected, onConfirm = { selected = it }, onContinue = { onConfirm(selected) })
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
            AeonHeroHeader(
                title = "File Error",
                subtitle = "Could not open CSV file",
                iconName = "BrokenImage",
                accentColorHex = "#E53935",
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
                accentColorHex = "#004D5F",
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
    onContinue: () -> Unit
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

    Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
    AeonDashedButton(
        onClick = onContinue,
        text = "Continue",
        icon = Icons.AutoMirrored.Rounded.ArrowForward
    )
    Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))
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
private fun SourceAppItem(
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
                        ImportSourceApp.CASHEW        -> "Cashew — comma-delimited with subcategories"
                        ImportSourceApp.MONEY_MANAGER -> "Money Manager — standard CSV"
                        ImportSourceApp.GENERIC       -> "Any other CSV with a header row"
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
