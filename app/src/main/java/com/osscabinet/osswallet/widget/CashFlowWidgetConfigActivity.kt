package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.AeonDashedButton
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
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
                            val glanceManager = GlanceAppWidgetManager(applicationContext)
                            val glanceId = glanceManager
                                .getGlanceIds(CashFlowGlanceWidget::class.java)
                                .firstOrNull { glanceManager.getAppWidgetId(it) == widgetId }
                            if (glanceId != null) {
                                CashFlowGlanceWidget().update(applicationContext, glanceId)
                            } else {
                                CashFlowGlanceWidget().updateAll(applicationContext)
                            }
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
private fun CashFlowWidgetConfigScreen(
    config: CashFlowWidgetConfig,
    onConfigChange: (CashFlowWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = AppColors.screenBackground,
        topBar = {
            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Cash Flow Widget",
                            onBackClick = onBack
                        )
                    }
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = AeonTheme.dimensions.screenPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
        ) {
            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))

            Text(
                text = "Timeline",
                style = AppTextStyles.formSectionLabel,
                color = AppColors.primary
            )
            CashFlowTimelineOptions(
                selected = config.timelineRange,
                onSelect = { onConfigChange(config.copy(timelineRange = it)) }
            )

            HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))

            EntrySwitchRow(
                label = "Show Base Conversion",
                subtitle = "Show \u2248 equivalent in base currency",
                checked = config.showBaseConversion,
                onCheckedChange = { onConfigChange(config.copy(showBaseConversion = it)) },
                isFlush = true
            )

            HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))

            EntrySwitchRow(
                label = "Show Totals Row",
                subtitle = "Show TOTAL footer below currency list",
                checked = config.showTotals,
                onCheckedChange = { onConfigChange(config.copy(showTotals = it)) },
                isFlush = true
            )

            HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))

            EntrySwitchRow(
                label = "Show Comparison",
                subtitle = "Compare net flow with previous period",
                checked = config.showComparison,
                onCheckedChange = { onConfigChange(config.copy(showComparison = it)) },
                isFlush = true
            )

            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingMd))

            AeonDashedButton(
                onClick = onSave,
                text = "Save Widget",
                icon = Icons.Rounded.CheckCircle
            )

            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingLg))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CashFlowTimelineOptions(
    selected: TimelineRange,
    onSelect: (TimelineRange) -> Unit
) {
    val options: List<Pair<String, TimelineRange>> = listOf(
        "All Time" to TimelineRange.AllTime(),
        "Today" to TimelineRange.ThisPeriod(ThisPeriodType.TODAY),
        "This Week" to TimelineRange.ThisPeriod(ThisPeriodType.WEEK),
        "This Month" to TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
        "This Quarter" to TimelineRange.ThisPeriod(ThisPeriodType.QUARTER),
        "This Year" to TimelineRange.ThisPeriod(ThisPeriodType.YEAR),
        "Last 30 Days" to TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS),
        "Last 6 Months" to TimelineRange.LastPeriod(LastPeriodType.LAST_6_MONTHS),
        "Last 1 Year" to TimelineRange.LastPeriod(LastPeriodType.LAST_1_YEAR),
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (label, range) ->
            FilterChip(
                selected = range == selected,
                onClick = { onSelect(range) },
                label = { Text(label, style = AppTextStyles.chipLabel) }
            )
        }
    }
}
