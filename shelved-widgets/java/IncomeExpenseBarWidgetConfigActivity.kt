package com.osscabinet.ossWallet.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
class IncomeExpenseBarWidgetConfigActivity : BaseWidgetConfigActivity<IncomeExpenseBarWidgetConfig>() {

    override val prefsStore by lazy { IncomeExpenseBarWidgetPrefsStore(applicationContext) }
    override val defaultConfig = IncomeExpenseBarWidgetConfig()

    @Composable
    override fun Content(
        config: IncomeExpenseBarWidgetConfig,
        onConfigChange: (IncomeExpenseBarWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        IncomeExpenseBarWidgetConfigScreen(config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IncomeExpenseBarWidgetConfigScreen(
    config: IncomeExpenseBarWidgetConfig,
    onConfigChange: (IncomeExpenseBarWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Income vs Expenses Widget",
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
