package com.osscabinet.ossWallet.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
class LoansWidgetConfigActivity : BaseWidgetConfigActivity<LoansWidgetConfig>() {

    override val prefsStore by lazy { LoansWidgetPrefsStore(applicationContext) }
    override val defaultConfig = LoansWidgetConfig()

    @Composable
    override fun Content(
        config: LoansWidgetConfig,
        onConfigChange: (LoansWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        LoansWidgetConfigScreen(config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoansWidgetConfigScreen(
    config: LoansWidgetConfig,
    onConfigChange: (LoansWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Loans Widget",
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
