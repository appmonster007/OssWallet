package com.osscabinet.ossWallet.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow

@OptIn(ExperimentalMaterial3Api::class)
class BudgetsWidgetConfigActivity : BaseWidgetConfigActivity<BudgetsWidgetConfig>() {

    override val prefsStore by lazy { BudgetsWidgetPrefsStore(applicationContext) }
    override val defaultConfig = BudgetsWidgetConfig()

    @Composable
    override fun Content(
        config: BudgetsWidgetConfig,
        onConfigChange: (BudgetsWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        BudgetsWidgetConfigScreen(config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetsWidgetConfigScreen(
    config: BudgetsWidgetConfig,
    onConfigChange: (BudgetsWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Budgets Widget",
        onBack = onBack,
        onSave = onSave
    ) {
        EntrySwitchRow(
            label = "Show Archived Budgets",
            subtitle = "Include archived budgets in this widget",
            checked = config.showArchived,
            onCheckedChange = { onConfigChange(config.copy(showArchived = it)) },
            isFlush = true
        )

        WidgetConfigDivider()

        WidgetThemeOptions(
            selected = config.theme,
            onSelect = { onConfigChange(config.copy(theme = it)) },
            glass = config.glass,
            onGlassChange = { onConfigChange(config.copy(glass = it)) }
        )

        WidgetConfigDivider()

        WidgetConfigFiltersSection(
            filter = config.filter,
            onFilterChange = { onConfigChange(config.copy(filter = it)) },
            onClose = onBack
        )
    }
}
