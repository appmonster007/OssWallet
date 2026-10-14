package com.osscabinet.ossWallet.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.osscabinet.ossWallet.ui.components.cards.CardPerspectiveSettings
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow

@OptIn(ExperimentalMaterial3Api::class)
class NetWorthWidgetConfigActivity : BaseWidgetConfigActivity<NetWorthWidgetConfig>() {

    override val prefsStore by lazy { NetWorthWidgetPrefsStore(applicationContext) }
    override val defaultConfig = NetWorthWidgetConfig()

    @Composable
    override fun Content(
        config: NetWorthWidgetConfig,
        onConfigChange: (NetWorthWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        NetWorthWidgetConfigScreen(config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NetWorthWidgetConfigScreen(
    config: NetWorthWidgetConfig,
    onConfigChange: (NetWorthWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Net Worth Widget",
        onBack = onBack,
        onSave = onSave
    ) {
        EntrySwitchRow(
            label = "Show Comparison",
            subtitle = "Compare with previous period",
            checked = config.showComparison,
            onCheckedChange = { onConfigChange(config.copy(showComparison = it)) },
            isFlush = true
        )

        WidgetConfigDivider()

        // Same perspective/exclude-internal-transfers/split-transfers controls as the real
        // NetWorthCard's settings sheet (CardPerspectiveSettings) — reused directly rather
        // than re-implemented, so behavior stays identical.
        CardPerspectiveSettings(
            perspective = config.perspective,
            onPerspectiveChange = { onConfigChange(config.copy(perspective = it)) },
            excludeInternalTransfers = config.excludeInternalTransfers,
            onExcludeInternalTransfersChange = { onConfigChange(config.copy(excludeInternalTransfers = it)) },
            splitTransfers = config.splitTransfers,
            onSplitTransfersChange = { onConfigChange(config.copy(splitTransfers = it)) }
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
