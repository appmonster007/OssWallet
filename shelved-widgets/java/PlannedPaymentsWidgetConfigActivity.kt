package com.osscabinet.ossWallet.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalMaterial3Api::class)
class PlannedPaymentsWidgetConfigActivity : BaseWidgetConfigActivity<PlannedPaymentsWidgetConfig>() {

    override val prefsStore by lazy { PlannedPaymentsWidgetPrefsStore(applicationContext) }
    override val defaultConfig = PlannedPaymentsWidgetConfig()

    @Composable
    override fun Content(
        config: PlannedPaymentsWidgetConfig,
        onConfigChange: (PlannedPaymentsWidgetConfig) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    ) {
        PlannedPaymentsWidgetConfigScreen(config, onConfigChange, onSave, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlannedPaymentsWidgetConfigScreen(
    config: PlannedPaymentsWidgetConfig,
    onConfigChange: (PlannedPaymentsWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Planned Payments Widget",
        onBack = onBack,
        onSave = onSave
    ) {
        WidgetConfigSectionHeader("Lookahead window")
        Text(
            text = "How far ahead to show upcoming payments",
            style = AppTextStyles.formFieldNote
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7, 14, 30).forEach { days ->
                FilterChip(
                    selected = config.lookaheadDays == days,
                    onClick = { onConfigChange(config.copy(lookaheadDays = days)) },
                    label = { Text("$days days") }
                )
            }
        }
    }
}
