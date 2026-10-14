package com.osscabinet.ossWallet.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow

@OptIn(ExperimentalMaterial3Api::class)
class RecentActivitiesWidgetConfigActivity : BaseWidgetConfigActivity<RecentActivitiesWidgetConfig>() {

    override val prefsStore by lazy { RecentActivitiesWidgetPrefsStore(applicationContext) }
    override val defaultConfig = RecentActivitiesWidgetConfig()

    @Composable
    override fun Content(
        config: RecentActivitiesWidgetConfig,
        onConfigChange: (RecentActivitiesWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        RecentActivitiesWidgetConfigScreen(config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecentActivitiesWidgetConfigScreen(
    config: RecentActivitiesWidgetConfig,
    onConfigChange: (RecentActivitiesWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Recent Activities Widget",
        onBack = onBack,
        onSave = onSave
    ) {
        WidgetConfigSectionHeader("Row Content")
        EntrySwitchRow(
            label = "Show Account",
            checked = config.showAccount,
            onCheckedChange = { onConfigChange(config.copy(showAccount = it)) },
            isFlush = true
        )
        EntrySwitchRow(
            label = "Show Category",
            checked = config.showCategory,
            onCheckedChange = { onConfigChange(config.copy(showCategory = it)) },
            isFlush = true
        )
        EntrySwitchRow(
            label = "Show Timestamp",
            checked = config.showTimestamp,
            onCheckedChange = { onConfigChange(config.copy(showTimestamp = it)) },
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
