package com.osscabinet.ossWallet.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

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
        WidgetConfigSectionHeader("Font Size")
        FontSizeOptions(
            selected = config.fontSize,
            onSelect = { onConfigChange(config.copy(fontSize = it)) }
        )

        WidgetConfigDivider()

        EntrySwitchRow(
            label = "Show Comparison",
            subtitle = "Compare with previous period",
            checked = config.showComparison,
            onCheckedChange = { onConfigChange(config.copy(showComparison = it)) },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FontSizeOptions(
    selected: String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("Small", "Medium", "Large", "X-Large").forEach { size ->
            FilterChip(
                selected = size == selected,
                onClick = { onSelect(size) },
                label = { Text(size, style = AppTextStyles.chipLabel) }
            )
        }
    }
}
