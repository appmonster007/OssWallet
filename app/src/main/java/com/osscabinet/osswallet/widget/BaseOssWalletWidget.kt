package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.osscabinet.ossWallet.MainActivity

/**
 * Base class for all OssWallet Glance widgets.
 * Handles common sizing logic, theming, and navigation.
 */
abstract class BaseOssWalletWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val rawId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val opts = AppWidgetManager.getInstance(context).getAppWidgetOptions(rawId)
        val optMaxW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).dp
        val optMaxH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).dp

        provideContent {
            GlanceTheme {
                val localSize = LocalSize.current
                val correctedSize = DpSize(
                    width = if (optMaxW > localSize.width) optMaxW else localSize.width,
                    height = if (optMaxH > localSize.height) optMaxH else localSize.height,
                )
                val prefs = currentState<Preferences>()

                WidgetContainer(context) {
                    RenderContent(context, prefs, correctedSize)
                }
            }
        }
    }

    @Composable
    private fun WidgetContainer(context: Context, content: @Composable () -> Unit) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(32.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clickable(
                    actionStartActivity(
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra(MainActivity.EXTRA_WIDGET_NAV, "dashboard")
                        }
                    )
                ),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            content()
        }
    }

    @Composable
    abstract fun RenderContent(context: Context, prefs: Preferences, size: DpSize)
}
