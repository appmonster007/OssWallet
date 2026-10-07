package com.osscabinet.ossWallet.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

// Shared building blocks for the three widget config screens (the
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
        // Deliberately not verticalScroll()'d: WidgetConfigFiltersSection below
        // ends in a weight(1f) FilterSheetContent, which scrolls internally.
        // Scrolling this outer Column too would nest two same-axis scrollables
        // and crash with "measured with infinity maximum height constraints".
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AeonTheme.dimensions.screenPadding),
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
 * "Advanced Filters" section, shared by every widget config screen since
 * they all filter on the same [TransactionFilter]. Shown inline rather than
 * behind a button — [FilterSheetContent] already renders its own timeline
 * selector, so a separate timeline picker above this would just duplicate it.
 *
 * Must be the last child in [WidgetConfigScreen]'s content: it takes the
 * remaining space via [ColumnScope.weight] so [FilterSheetContent]'s own
 * internal scroll gets a bounded height to scroll within, instead of an
 * infinite one from an outer scrolling container.
 */
@Composable
internal fun ColumnScope.WidgetConfigFiltersSection(
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
        modifier = Modifier.weight(1f)
    )
}
