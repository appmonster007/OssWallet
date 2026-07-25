package com.osscabinet.ossWallet.ui.screens.import_csv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.import_csv.ImportSourceApp
import com.osscabinet.ossWallet.domain.repository.RestorableEntity
import com.osscabinet.ossWallet.ui.screens.import_csv.components.EntityMappingRow
import com.osscabinet.ossWallet.ui.screens.import_csv.steps.SourceAppItem
import com.osscabinet.ossWallet.ui.screens.settings.RestorableEntitySelectorDialog
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews

/**
 * These previews are designed to demonstrate the "Interactive" or "Popup" states of the UI components
 * by forcing internal state or creating similar layouts.
 */

@ThemePreviews
@Composable
fun Preview_SourceAppSelection_Dropdown() {
    OssWalletTheme {
        Surface(color = AppColors.screenBackground) {
            Column(modifier = Modifier.padding(16.dp)) {
                EntityMappingRow(
                    label = "HDFC Savings",
                    chipText = "Matched",
                    options = listOf("1" to "HDFC Bank"),
                    selectedId = "1",
                    onSelect = {},
                    currencyCode = "INR",
                    supportedCurrencies = listOf("INR", "USD", "EUR", "GBP")
                )
            }
        }
    }
}

@ThemePreviews
@Composable
fun Preview_EntityMapping_SelectionDropdown() {
    OssWalletTheme {
        Surface(color = AppColors.screenBackground) {
            Column(modifier = Modifier.padding(16.dp)) {
                EntityMappingRow(
                    label = "Unknown Source",
                    badgeText = "Skipping 42 rows",
                    options = listOf("1" to "Personal Account"),
                    selectedId = "__EXCLUDE__",
                    onSelect = {},
                    showExcludeOption = true
                )
            }
        }
    }
}

@ThemePreviews
@Composable
fun Preview_ImportSourceApp_Comparison() {
    OssWalletTheme {
        Surface(color = AppColors.screenBackground) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImportSourceApp.entries.forEach { app ->
                    SourceAppItem(
                        app = app,
                        isSelected = app == ImportSourceApp.BUDGET_BAKERS,
                        onClick = {}
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
fun Preview_ImportProgress_Visible() {
    OssWalletTheme {
        Surface(color = AppColors.screenBackground) {
            Column(modifier = Modifier.padding(16.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.cardSurface,
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(AeonTheme.dimensions.cardPadding)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AppColors.primary
                            )
                            Text(
                                text = "Importing 1,240 transactions…",
                                style = AppTextStyles.listItemTitle,
                                color = AppColors.contentPrimary
                            )
                        }
                        Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
                        LinearProgressIndicator(
                            progress = { 0.65f },
                            modifier = Modifier.fillMaxWidth(),
                            color = AppColors.primary,
                            trackColor = AppColors.outlineVariant,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
fun Preview_RestorableEntitySelector() {
    OssWalletTheme {
        RestorableEntitySelectorDialog(
            selectedEntities = setOf(
                RestorableEntity.TRANSACTIONS,
                RestorableEntity.ACCOUNTS,
                RestorableEntity.CATEGORIES
            ),
            onSelectionChanged = {},
            onConfirm = {},
            onDismiss = {}
        )
    }
}
