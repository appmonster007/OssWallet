package com.osscabinet.ossWallet.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalMaterial3Api::class)
class RecentTransactionsWidgetConfigActivity : BaseWidgetConfigActivity<RecentTransactionsWidgetConfig>() {

    override val prefsStore by lazy { RecentTransactionsWidgetPrefsStore(applicationContext) }
    override val defaultConfig = RecentTransactionsWidgetConfig()

    @Composable
    override fun Content(
        config: RecentTransactionsWidgetConfig,
        onConfigChange: (RecentTransactionsWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        RecentTransactionsWidgetConfigScreen(config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecentTransactionsWidgetConfigScreen(
    config: RecentTransactionsWidgetConfig,
    onConfigChange: (RecentTransactionsWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Recent Activities Widget",
        onBack = onBack,
        onSave = onSave
    ) {
        WidgetConfigSectionHeader("Display Preference")
        Text(
            "Choose what to show as the primary text for each transaction.",
            style = AppTextStyles.caption,
            color = AppColors.contentSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
        ) {
            FilterChip(
                selected = config.primaryDisplay == TransactionDisplayPreference.CATEGORY_FIRST,
                onClick = { onConfigChange(config.copy(primaryDisplay = TransactionDisplayPreference.CATEGORY_FIRST)) },
                label = { Text("Category First", style = AppTextStyles.chipLabel) }
            )
            FilterChip(
                selected = config.primaryDisplay == TransactionDisplayPreference.NOTE_FIRST,
                onClick = { onConfigChange(config.copy(primaryDisplay = TransactionDisplayPreference.NOTE_FIRST)) },
                label = { Text("Note First", style = AppTextStyles.chipLabel) }
            )
        }

        WidgetConfigDivider()

        WidgetConfigSectionHeader("Transaction Limit")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
        ) {
            listOf(5, 10, 15, 20).forEach { limit ->
                FilterChip(
                    selected = config.limit == limit,
                    onClick = { onConfigChange(config.copy(limit = limit)) },
                    label = { Text(limit.toString(), style = AppTextStyles.chipLabel) }
                )
            }
        }

        WidgetConfigDivider()

        WidgetConfigFiltersSection(
            filter = config.filter,
            onFilterChange = { onConfigChange(config.copy(filter = it)) },
            onClose = onBack
        )
    }
}
