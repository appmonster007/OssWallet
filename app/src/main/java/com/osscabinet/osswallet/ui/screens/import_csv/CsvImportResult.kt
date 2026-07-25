package com.osscabinet.ossWallet.ui.screens.import_csv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSummary
import com.osscabinet.ossWallet.ui.components.AeonDashedButton
import com.osscabinet.ossWallet.ui.components.AeonHeroHeader
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import com.osscabinet.ossWallet.ui.theme.WarningColor
import kotlinx.coroutines.delay

private const val UNDO_WINDOW_MILLIS = 10 * 60 * 1000L

@Composable
fun ImportResultScreen(
    result: ImportSummary,
    resultTimestamp: Long? = null,
    onUndo: (() -> Unit)? = null,
    onViewTransactions: () -> Unit,
    onReviewMappings: () -> Unit,
    onBack: () -> Unit
) {
    val isFullSuccess = result.imported > 0 && result.errors.isEmpty()
    val isPartial = result.imported > 0 && result.errors.isNotEmpty()
    val isZero = result.imported == 0

    val (iconName, iconTint, headline, subLine) = when {
        isFullSuccess -> Quad(
            "CheckCircle",
            AppColors.positive,
            "Import complete",
            "${result.imported} transaction${if (result.imported != 1) "s" else ""} added"
        )

        isPartial -> Quad(
            "Warning",
            WarningColor,
            "Import finished with issues",
            "${result.imported} added \u00b7 ${result.errors.size} failed"
        )

        else -> Quad(
            "Info",
            AppColors.contentSecondary,
            "Nothing was imported",
            if (result.skipped > 0) "All rows already exist in OssWallet"
            else "All rows failed to parse \u2014 check your column mappings"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.screenBackground)
            .statusBarsPadding()
            .padding(AeonTheme.dimensions.screenPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        Spacer(Modifier.height(AeonTheme.dimensions.spacingXl))

        AeonHeroHeader(
            title = headline,
            subtitle = subLine,
            iconName = iconName,
            accentColorHex = String.format("#%06X", 0xFFFFFF and iconTint.toArgb()),
            isLeftAligned = false
        )

        if (result.skipped > 0 && !isZero) {
            Text(
                "${result.skipped} duplicate row${if (result.skipped != 1) "s" else ""} were skipped",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary
            )
        }

        if (result.transfersRerouted > 0) {
            Text(
                "${result.transfersRerouted} transfer${if (result.transfersRerouted != 1) "s" else ""} re-routed to outside-wallet",
                style = AppTextStyles.caption,
                color = AppColors.contentSecondary
            )
        }

        if (isPartial) {
            ErrorDetails(result.errors)
        }

        Spacer(Modifier.height(AeonTheme.dimensions.spacingLg))

        if (!isZero && onUndo != null && resultTimestamp != null) {
            UndoImportButton(resultTimestamp = resultTimestamp, onUndo = onUndo)
        }

        if (!isZero) {
            AeonDashedButton(
                onClick = onViewTransactions,
                text = "View transactions",
                icon = Icons.AutoMirrored.Rounded.ArrowForward
            )
        } else {
            AeonDashedButton(
                onClick = onReviewMappings,
                text = "Review mappings",
                icon = Icons.Rounded.AttachFile
            )
        }

        TextButton(onClick = onBack) {
            Text(
                "Back to Settings",
                style = AppTextStyles.buttonLabel,
                color = AppColors.contentSecondary
            )
        }
    }
}

@Composable
private fun UndoImportButton(resultTimestamp: Long, onUndo: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(resultTimestamp) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val remainingMillis = (resultTimestamp + UNDO_WINDOW_MILLIS - now).coerceAtLeast(0)
    if (remainingMillis <= 0) return

    val remainingSeconds = remainingMillis / 1000
    val label = "Undo this import (${remainingSeconds / 60}:${(remainingSeconds % 60).toString().padStart(2, '0')} left)"

    AeonDashedButton(
        onClick = onUndo,
        text = label,
        icon = Icons.AutoMirrored.Rounded.Undo
    )
    Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
}

@Composable
private fun ErrorDetails(errors: List<com.osscabinet.ossWallet.domain.model.import_csv.ParseError>) {
    var showErrors by remember { mutableStateOf(false) }
    TextButton(onClick = { showErrors = !showErrors }) {
        Text(
            if (showErrors) "Hide errors" else "Show ${errors.size} errors",
            style = AppTextStyles.buttonLabel,
            color = AppColors.error
        )
    }
    AnimatedVisibility(visible = showErrors) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

@ThemePreviews
@Composable
private fun ImportResultScreenSuccessPreview() {
    OssWalletTheme {
        ImportResultScreen(
            result = ImportSummary(
                imported = 10,
                skipped = 2,
                errors = emptyList(),
                transfersRerouted = 0
            ),
            onViewTransactions = {},
            onReviewMappings = {},
            onBack = {}
        )
    }
}

@ThemePreviews
@Composable
private fun ImportResultScreenPartialPreview() {
    OssWalletTheme {
        ImportResultScreen(
            result = ImportSummary(
                imported = 5,
                skipped = 1,
                errors = listOf(
                    com.osscabinet.ossWallet.domain.model.import_csv.ParseError(
                        3,
                        "Missing category"
                    )
                ),
                transfersRerouted = 2
            ),
            onViewTransactions = {},
            onReviewMappings = {},
            onBack = {}
        )
    }
}

private data class Quad<out A, out B, out C, out D>(val a: A, val b: B, val c: C, val d: D)
