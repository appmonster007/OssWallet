package com.osscabinet.ossWallet.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
class TagUpSetPlotWidgetConfigActivity : BaseWidgetConfigActivity<TagUpSetPlotWidgetConfig>() {

    override val prefsStore by lazy { TagUpSetPlotWidgetPrefsStore(applicationContext) }
    override val defaultConfig = TagUpSetPlotWidgetConfig()

    @Composable
    override fun Content(
        config: TagUpSetPlotWidgetConfig,
        onConfigChange: (TagUpSetPlotWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        TagUpSetPlotWidgetConfigScreen(config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TagUpSetPlotWidgetConfigScreen(
    config: TagUpSetPlotWidgetConfig,
    onConfigChange: (TagUpSetPlotWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Tags Widget",
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
