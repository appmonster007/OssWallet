package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class CashFlowWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val widgetId = intent.extras
            ?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(RESULT_CANCELED)

        val prefsStore = CashFlowWidgetPrefsStore(applicationContext)

        setContent {
            OssWalletTheme {
                var config by remember { mutableStateOf(CashFlowWidgetConfig()) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(widgetId) {
                    config = prefsStore.load(widgetId)
                }

                CashFlowWidgetConfigScreen(
                    config = config,
                    onConfigChange = { config = it },
                    onSave = {
                        scope.launch {
                            prefsStore.save(widgetId, config)
                            // Raw RemoteViews widget: no Glance update/updateAll call needed,
                            // the worker computes fresh data and pushes RemoteViews directly.
                            WidgetRefreshWorker.enqueueNow(applicationContext)
                            val resultValue = Intent().putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId
                            )
                            setResult(RESULT_OK, resultValue)
                            finish()
                        }
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashFlowWidgetConfigScreen(
    config: CashFlowWidgetConfig,
    onConfigChange: (CashFlowWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Cash Flow Widget",
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

        EntrySwitchRow(
            label = "Show Totals Row",
            subtitle = "Show TOTAL footer below currency list",
            checked = config.showTotals,
            onCheckedChange = { onConfigChange(config.copy(showTotals = it)) },
            isFlush = true
        )

        WidgetConfigDivider()

        EntrySwitchRow(
            label = "Show Comparison",
            subtitle = "Compare net flow with previous period",
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
