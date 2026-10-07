package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class RecentTransactionsWidgetConfigActivity : ComponentActivity() {

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

        val prefsStore = RecentTransactionsWidgetPrefsStore(applicationContext)

        setContent {
            OssWalletTheme {
                var config by remember { mutableStateOf(RecentTransactionsWidgetConfig()) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(widgetId) {
                    config = prefsStore.load(widgetId)
                }

                RecentTransactionsWidgetConfigScreen(
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
internal fun RecentTransactionsWidgetConfigScreen(
    config: RecentTransactionsWidgetConfig,
    onConfigChange: (RecentTransactionsWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    WidgetConfigScreen(
        title = "Recent Activities Widget",
        onBack = onBack,
        onSave = onSave
    ) {
        WidgetConfigSectionHeader("Display Preference")
        Text(
            "Choose what to show as the primary text for each transaction.",
            style = AppTextStyles.caption,
            color = AppColors.contentSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
        ) {
            FilterChip(
                selected = config.primaryDisplay == TransactionDisplayPreference.CATEGORY_FIRST,
                onClick = { onConfigChange(config.copy(primaryDisplay = TransactionDisplayPreference.CATEGORY_FIRST)) },
                label = { Text("Category First", style = AppTextStyles.chipLabel) }
            )
            FilterChip(
                selected = config.primaryDisplay == TransactionDisplayPreference.NOTE_FIRST,
                onClick = { onConfigChange(config.copy(primaryDisplay = TransactionDisplayPreference.NOTE_FIRST)) },
                label = { Text("Note First", style = AppTextStyles.chipLabel) }
            )
        }

        WidgetConfigDivider()

        WidgetConfigSectionHeader("Transaction Limit")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
        ) {
            listOf(5, 10, 15, 20).forEach { limit ->
                FilterChip(
                    selected = config.limit == limit,
                    onClick = { onConfigChange(config.copy(limit = limit)) },
                    label = { Text(limit.toString(), style = AppTextStyles.chipLabel) }
                )
            }
        }

        WidgetConfigDivider()

        WidgetConfigFiltersSection(
            filter = config.filter,
            onFilterChange = { onConfigChange(config.copy(filter = it)) },
            onClose = onBack
        )
    }
}
