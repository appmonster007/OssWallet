package com.osscabinet.ossWallet.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
class CategoryTreemapWidgetConfigActivity : BaseWidgetConfigActivity<CategoryTreemapWidgetConfig>() {

    override val prefsStore by lazy { CategoryTreemapWidgetPrefsStore(applicationContext) }
    override val defaultConfig = CategoryTreemapWidgetConfig()

    @Composable
    override fun Content(
        config: CategoryTreemapWidgetConfig,
        onConfigChange: (CategoryTreemapWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        CategoryTreemapWidgetConfigScreen(config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryTreemapWidgetConfigScreen(
    config: CategoryTreemapWidgetConfig,
    onConfigChange: (CategoryTreemapWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Category Treemap Widget",
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
