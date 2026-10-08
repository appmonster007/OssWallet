package com.osscabinet.ossWallet.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
class CategorySpendingTrendWidgetConfigActivity : BaseWidgetConfigActivity<CategorySpendingTrendWidgetConfig>() {

    override val prefsStore by lazy { CategorySpendingTrendWidgetPrefsStore(applicationContext) }
    override val defaultConfig = CategorySpendingTrendWidgetConfig()

    @Composable
    override fun Content(
        config: CategorySpendingTrendWidgetConfig,
        onConfigChange: (CategorySpendingTrendWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        CategorySpendingTrendWidgetConfigScreen(config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategorySpendingTrendWidgetConfigScreen(
    config: CategorySpendingTrendWidgetConfig,
    onConfigChange: (CategorySpendingTrendWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Category Trend Widget",
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
