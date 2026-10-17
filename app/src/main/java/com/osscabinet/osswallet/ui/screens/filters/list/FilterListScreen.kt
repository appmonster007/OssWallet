package com.osscabinet.ossWallet.ui.screens.filters.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.ui.components.AeonStatusChip
import com.osscabinet.ossWallet.domain.service.search.FilterSummary
import com.osscabinet.ossWallet.ui.components.AeonFloatingActionButton
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.management.AeonListItem
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.screens.filters.FilterViewModel
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterListScreen(
    viewModel: FilterViewModel,
    actions: FilterListActions
) {
    val filters by viewModel.allFilters.collectAsStateWithLifecycle()

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                floatingActionButton = {
                    AeonFloatingActionButton(
                        onClick = actions.onAddFilterClick,
                        icon = Icons.Rounded.Add,
                        contentDescription = "Add Filter"
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                ) {
                    if (filters.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(top = AeonSize.D64.value),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(AeonTheme.dimensions.iconSize3xl),
                                    tint = AppColors.contentSecondary.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingLg))
                                Text(
                                    "No saved filters yet",
                                    style = AppTextStyles.listItemTitle,
                                    color = AppColors.border
                                )
                                Text(
                                    "Save a search, or a set of Records filters, to reuse it in one tap.",
                                    style = AppTextStyles.caption,
                                    color = AppColors.border,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.screenPadding, vertical = AeonTheme.dimensions.spacingXs)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding(),
                            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.listPadding),
                            contentPadding = PaddingValues(
                                start = AeonTheme.dimensions.screenPadding,
                                end = AeonTheme.dimensions.screenPadding,
                                top = AeonSize.D64.value + AeonTheme.dimensions.screenPadding,
                                bottom = innerPadding.calculateBottomPadding() + scrollBottomPadding()
                            )
                        ) {
                            items(filters) { filter ->
                                FilterItem(
                                    filter = filter,
                                    onClick = { actions.onEditFilterClick(filter) },
                                    onOpen = { actions.onOpenInRecords(filter) },
                                    onDuplicate = { viewModel.duplicateFilter(filter) },
                                    onDelete = { viewModel.deleteFilter(filter) }
                                )
                            }
                        }
                    }
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Saved Filters",
                            onBackClick = actions.onBackClick
                        )
                    }
                )
            )
        }
    }
}

data class FilterListActions(
    val onBackClick: () -> Unit,
    val onAddFilterClick: () -> Unit,
    val onEditFilterClick: (Filter) -> Unit,
    val onOpenInRecords: (Filter) -> Unit = {}
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterItem(
    filter: Filter,
    onClick: () -> Unit,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    AeonListItem(
        title = filter.name,
        icon = Icons.Rounded.FilterList,
        iconTint = AppColors.primary,
        onClick = onClick,
        additionalContent = {
            // What the filter selects, one small pill each, like the status pills elsewhere.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterSummary.parts(filter.filter).forEach { part ->
                    AeonStatusChip(text = part, color = AppColors.primary)
                }
            }
        },
        menuItems = { dismiss ->
            DropdownMenuItem(
                text = { Text("Show in Records") },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
                onClick = { dismiss(); onOpen() }
            )
            DropdownMenuItem(
                text = { Text("Duplicate") },
                leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                onClick = { dismiss(); onDuplicate() }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = AppColors.error) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = AppColors.error) },
                onClick = { dismiss(); onDelete() }
            )
        }
    )
}
