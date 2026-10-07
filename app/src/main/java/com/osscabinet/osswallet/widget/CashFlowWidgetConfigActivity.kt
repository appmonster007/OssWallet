package com.osscabinet.ossWallet.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow

@OptIn(ExperimentalMaterial3Api::class)
class CashFlowWidgetConfigActivity : BaseWidgetConfigActivity<CashFlowWidgetConfig>() {

    override val prefsStore by lazy { CashFlowWidgetPrefsStore(applicationContext) }
    override val defaultConfig = CashFlowWidgetConfig()

    @Composable
    override fun Content(
        config: CashFlowWidgetConfig,
        onConfigChange: (CashFlowWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        CashFlowWidgetConfigScreen(config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashFlowWidgetConfigScreen(
    config: CashFlowWidgetConfig,
    onConfigChange: (CashFlowWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Cash Flow Widget",
        onBack = onBack,
        onSave = onSave
    ) {
        EntrySwitchRow(
            label = "Show Base Conversion",
            subtitle = "Show ≈ equivalent in base currency",
            checked = config.showBaseConversion,
            onCheckedChange = { onConfigChange(config.copy(showBaseConversion = it)) },
            isFlush = true
        )

        WidgetConfigDivider()

        EntrySwitchRow(
            label = "Show Totals Row",
            subtitle = "Show TOTAL footer below currency list",
            checked = config.showTotals,
            onCheckedChange = { onConfigChange(config.copy(showTotals = it)) },
            isFlush = true
        )

        WidgetConfigDivider()

        EntrySwitchRow(
            label = "Show Comparison",
            subtitle = "Compare net flow with previous period",
            checked = config.showComparison,
            onCheckedChange = { onConfigChange(config.copy(showComparison = it)) },
            isFlush = true
        )

        WidgetConfigDivider()

        WidgetConfigFiltersSection(
            filter = config.filter,
            onFilterChange = { onConfigChange(config.copy(filter = it)) },
            onClose = onBack
        )
    }
}
