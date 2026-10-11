package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.launch

/**
 * Shared shape for the three widget APPWIDGET_CONFIGURE activities shown before a pinned widget
 * is placed: resolves the widget id, loads/saves config through [prefsStore], and requests a
 * [WidgetRefreshCoordinator] refresh for that widget on save (raw-RemoteViews widgets have no Glance update/updateAll call to
 * make instead — the worker computes fresh data and pushes RemoteViews directly).
 */
abstract class BaseWidgetConfigActivity<T> : ComponentActivity() {

    protected abstract val prefsStore: WidgetPrefsStore<T>
    protected abstract val defaultConfig: T

    @Composable
    protected abstract fun Content(
        config: T,
        onConfigChange: (T) -> Unit,
        onSave: () -> Unit,
        onBack: () -> Unit
    )

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

        setContent {
            OssWalletTheme {
                var config by remember { mutableStateOf(defaultConfig) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(widgetId) {
                    config = prefsStore.load(widgetId)
                }

                Content(
                    config = config,
                    onConfigChange = { config = it },
                    onSave = {
                        scope.launch {
                            prefsStore.save(widgetId, config)
                            WidgetRefreshCoordinator.requestWidgets(applicationContext, listOf(widgetId))
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
