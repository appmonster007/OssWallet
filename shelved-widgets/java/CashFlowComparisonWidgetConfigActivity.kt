package com.osscabinet.ossWallet.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
class CashFlowComparisonWidgetConfigActivity : BaseWidgetConfigActivity<CashFlowComparisonWidgetConfig>() {

    override val prefsStore by lazy { CashFlowComparisonWidgetPrefsStore(applicationContext) }
    override val defaultConfig = CashFlowComparisonWidgetConfig()

    @Composable
    override fun Content(
        config: CashFlowComparisonWidgetConfig,
        onConfigChange: (CashFlowComparisonWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        CashFlowComparisonWidgetConfigScreen(config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashFlowComparisonWidgetConfigScreen(
    config: CashFlowComparisonWidgetConfig,
    onConfigChange: (CashFlowComparisonWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Net Flow Comparison Widget",
        onBack = onBack,
        onSave = onSave
    ) {
        WidgetConfigFiltersSection(
            filter = config.filter,
            onFilterChange = { onConfigChange(config.copy(filter = it)) },
            onClose = onBack
        )
    }
}
