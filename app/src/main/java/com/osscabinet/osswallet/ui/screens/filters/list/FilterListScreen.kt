package com.osscabinet.ossWallet.ui.screens.filters.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.AeonFloatingActionButton
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
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
    val onEditFilterClick: (Filter) -> Unit
)

@Composable
fun FilterItem(
    filter: Filter,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        color = AppColors.insetSurface.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(AeonTheme.dimensions.cardPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl),
                shape = CircleShape,
                color = AppColors.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.FilterList,
                        contentDescription = null,
                        tint = AppColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    filter.name,
                    style = AppTextStyles.cardSectionTitle
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = AppColors.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}
