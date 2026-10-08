package com.osscabinet.ossWallet.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow

@OptIn(ExperimentalMaterial3Api::class)
class CurrencyBalancesWidgetConfigActivity : BaseWidgetConfigActivity<CurrencyBalancesWidgetConfig>() {

    override val prefsStore by lazy { CurrencyBalancesWidgetPrefsStore(applicationContext) }
    override val defaultConfig = CurrencyBalancesWidgetConfig()

    @Composable
    override fun Content(
        config: CurrencyBalancesWidgetConfig,
        onConfigChange: (CurrencyBalancesWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        CurrencyBalancesWidgetConfigScreen(config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurrencyBalancesWidgetConfigScreen(
    config: CurrencyBalancesWidgetConfig,
    onConfigChange: (CurrencyBalancesWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Balances Widget",
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

        WidgetConfigFiltersSection(
            filter = config.filter,
            onFilterChange = { onConfigChange(config.copy(filter = it)) },
            onClose = onBack
        )
    }
}
