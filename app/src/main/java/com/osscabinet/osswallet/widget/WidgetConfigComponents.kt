package com.osscabinet.ossWallet.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarActionPill
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.components.transactions.FilterEvent
import com.osscabinet.ossWallet.ui.components.transactions.FilterSheetContent
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import kotlinx.coroutines.flow.flowOf
import kotlin.math.roundToInt

// Shared building blocks for the widget config screens (the
// APPWIDGET_CONFIGURE activities shown before a widget is placed).

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WidgetConfigScreen(
    title: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = AppColors.screenBackground,
        topBar = {
            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = title,
                            onBackClick = onBack
                        )
                    }
                ),
                right = AeonTopBarSection(
                    content = {
                        AeonTopBarActionPill {
                            AppBarIconButton(
                                icon = Icons.Rounded.Check,
                                onClick = onSave
                            )
                        }
                    }
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AeonTheme.dimensions.screenPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
        ) {
            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))

            content()
        }
    }
}

@Composable
internal fun WidgetConfigSectionHeader(title: String) {
    Text(
        text = title,
        style = AppTextStyles.formSectionLabel,
        color = AppColors.primary
    )
}

@Composable
internal fun WidgetConfigDivider() {
    HorizontalDivider(color = AppColors.divider.copy(alpha = 0.5f))
}

/**
 * "Theme" section, shared by every widget config screen — forces the widget's own appearance
 * independent of the device's current system setting (System/Light/Dark) and sets how opaque its background is. SYSTEM is the default
 * and needs no runtime color overrides (see WidgetColors/WidgetTheme for why LIGHT/DARK do).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WidgetThemeOptions(
    selected: WidgetTheme,
    onSelect: (WidgetTheme) -> Unit,
    backgroundOpacity: Int,
    onBackgroundOpacityChange: (Int) -> Unit
) {
    WidgetConfigSectionHeader("Theme")
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        WidgetTheme.entries.forEachIndexed { index, theme ->
            SegmentedButton(
                selected = theme == selected,
                onClick = { onSelect(theme) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = WidgetTheme.entries.size),
                label = {
                    Text(
                        when (theme) {
                            WidgetTheme.SYSTEM -> "System"
                            WidgetTheme.LIGHT -> "Light"
                            WidgetTheme.DARK -> "Dark"
                        }
                    )
                }
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Background opacity", style = MaterialTheme.typography.bodyMedium, color = AppColors.onSurface)
        Text("$backgroundOpacity%", style = MaterialTheme.typography.bodyMedium, color = AppColors.onSurfaceVariant)
    }
    Slider(
        value = backgroundOpacity.toFloat(),
        onValueChange = { onBackgroundOpacityChange(it.roundToInt()) },
        valueRange = 0f..100f,
        steps = 19
    )
}

/**
 * "Advanced Filters" section, shared by every widget config screen since
 * they all filter on the same [TransactionFilter]. Shown inline rather than
 * behind a button — [FilterSheetContent] already renders its own timeline
 * selector. Pass `scrollable = false` so that the outer [WidgetConfigScreen]
 * manages single unified scrolling for the page.
 */
@Composable
internal fun WidgetConfigFiltersSection(
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    onClose: () -> Unit
) {
    WidgetConfigSectionHeader("Advanced Filters")
    FilterSheetContent(
        filter = filter,
        accounts = emptyList(),
        allCategories = emptyList(),
        onSearchTransactions = { flowOf(emptyList()) },
        onFilterEvent = { event ->
            when (event) {
                is FilterEvent.Apply -> onFilterChange(event.filter)
                is FilterEvent.Reset -> onFilterChange(TransactionFilter(timelineRange = filter.timelineRange))
                else -> {}
            }
        },
        onSaveFilterClick = { onFilterChange(it) },
        onClose = onClose,
        scrollable = false
    )
}
