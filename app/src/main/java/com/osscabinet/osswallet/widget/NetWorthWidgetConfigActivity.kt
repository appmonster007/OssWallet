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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
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
import com.osscabinet.ossWallet.ui.components.AeonDashedButton
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.TransactionFilterSheet
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

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
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppColors.screenBackground,
        topBar = {
            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Net Worth Widget",
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

            SectionHeader("Timeline")
            AeonTimelinePeriodSelector(
                currentRange = config.filter.timelineRange,
                onRangeSelected = {
                    onConfigChange(config.copy(filter = config.filter.copy(timelineRange = it)))
                },
                swipeEntireComponent = true,
                isFlush = true,
                modifier = Modifier.fillMaxWidth()
            )

            AeonDashedButton(
                onClick = { showFilterSheet = true },
                text = "Advanced Filters",
                icon = Icons.Filled.Tune
            )

            HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))

            SectionHeader("Font Size")
            FontSizeOptions(
                selected = config.fontSize,
                onSelect = { onConfigChange(config.copy(fontSize = it)) }
            )

            HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))

            EntrySwitchRow(
                label = "Show Comparison",
                subtitle = "Compare with previous period",
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

    if (showFilterSheet) {
        TransactionFilterSheet(
            filter = config.filter,
            accounts = emptyList(),
            categories = emptyList(),
            onSearchTransactions = { flowOf(emptyList()) },
            onFilterEvent = { event ->
                when (event) {
                    is FilterEvent.Apply -> onConfigChange(config.copy(filter = event.filter))
                    is FilterEvent.Reset -> onConfigChange(
                        config.copy(filter = TransactionFilter(timelineRange = config.filter.timelineRange))
                    )
                    else -> {}
                }
            },
            onSaveFilterClick = { onConfigChange(config.copy(filter = it)) },
            onClose = { showFilterSheet = false }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = AppTextStyles.formSectionLabel,
        color = AppColors.primary
    )
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
