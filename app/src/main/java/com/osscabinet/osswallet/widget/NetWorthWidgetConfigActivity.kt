package com.osscabinet.ossWallet.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.osscabinet.ossWallet.domain.model.timeline.LastPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.launch
import com.osscabinet.ossWallet.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
class NetWorthWidgetConfigActivity : ComponentActivity() {

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

        val prefsStore = NetWorthWidgetPrefsStore(applicationContext)

        setContent {
            OssWalletTheme {
                var config by remember { mutableStateOf(NetWorthWidgetConfig()) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(widgetId) {
                    config = prefsStore.load(widgetId)
                }

                NetWorthWidgetConfigScreen(
                    config = config,
                    onConfigChange = { config = it },
                    onSave = {
                        scope.launch {
                            prefsStore.save(widgetId, config)
                            val glanceManager = GlanceAppWidgetManager(applicationContext)
                            val glanceId = glanceManager
                                .getGlanceIds(NetWorthGlanceWidget::class.java)
                                .firstOrNull { glanceManager.getAppWidgetId(it) == widgetId }
                            if (glanceId != null) {
                                NetWorthGlanceWidget().update(applicationContext, glanceId)
                            } else {
                                NetWorthGlanceWidget().updateAll(applicationContext)
                            }
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
private fun NetWorthWidgetConfigScreen(
    config: NetWorthWidgetConfig,
    onConfigChange: (NetWorthWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Net Worth Widget") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader("Timeline")
            TimelineOptions(
                selected = config.timelineRange,
                onSelect = { onConfigChange(config.copy(timelineRange = it)) }
            )

            HorizontalDivider()

            SectionHeader("Font Size")
            FontSizeOptions(
                selected = config.fontSize,
                onSelect = { onConfigChange(config.copy(fontSize = it)) }
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Show Comparison", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Compare with previous period",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.contentSecondary
                    )
                }
                Switch(
                    checked = config.showComparison,
                    onCheckedChange = { onConfigChange(config.copy(showComparison = it)) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Widget")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = AppColors.primary
    )
}

@Composable
private fun TimelineOptions(
    selected: TimelineRange,
    onSelect: (TimelineRange) -> Unit
) {
    val options: List<Pair<String, TimelineRange>> = listOf(
        "All Time" to TimelineRange.AllTime,
        "Today" to TimelineRange.ThisPeriod(ThisPeriodType.TODAY),
        "This Week" to TimelineRange.ThisPeriod(ThisPeriodType.WEEK),
        "This Month" to TimelineRange.ThisPeriod(ThisPeriodType.MONTH),
        "This Quarter" to TimelineRange.ThisPeriod(ThisPeriodType.QUARTER),
        "This Year" to TimelineRange.ThisPeriod(ThisPeriodType.YEAR),
        "Last 30 Days" to TimelineRange.LastPeriod(LastPeriodType.LAST_30_DAYS),
        "Last 6 Months" to TimelineRange.LastPeriod(LastPeriodType.LAST_6_MONTHS),
        "Last 1 Year" to TimelineRange.LastPeriod(LastPeriodType.LAST_1_YEAR),
    )
    ChipGroup(
        options = options,
        isSelected = { _, range -> range::class == selected::class && range == selected },
        onSelect = { _, range -> onSelect(range) }
    )
}

@Composable
private fun FontSizeOptions(
    selected: String,
    onSelect: (String) -> Unit
) {
    val sizes = listOf("Small", "Medium", "Large", "X-Large")
    ChipGroup(
        options = sizes.map { it to it },
        isSelected = { label, _ -> label == selected },
        onSelect = { label, _ -> onSelect(label) }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipGroup(
    options: List<Pair<String, T>>,
    isSelected: (String, T) -> Boolean,
    onSelect: (String, T) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (label, value) ->
            FilterChip(
                selected = isSelected(label, value),
                onClick = { onSelect(label, value) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}
