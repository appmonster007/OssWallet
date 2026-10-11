package com.osscabinet.ossWallet.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
class CategoryPieChartWidgetConfigActivity : BaseWidgetConfigActivity<CategoryPieChartWidgetConfig>() {

    override val prefsStore by lazy { CategoryPieChartWidgetPrefsStore(applicationContext) }
    override val defaultConfig = CategoryPieChartWidgetConfig()

    @Composable
    override fun Content(
        config: CategoryPieChartWidgetConfig,
        onConfigChange: (CategoryPieChartWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        CategoryPieChartWidgetConfigScreen(config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryPieChartWidgetConfigScreen(
    config: CategoryPieChartWidgetConfig,
    onConfigChange: (CategoryPieChartWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Category Pie Chart Widget",
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
