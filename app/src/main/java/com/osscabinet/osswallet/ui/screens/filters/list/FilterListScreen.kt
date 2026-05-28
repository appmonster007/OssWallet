package com.osscabinet.ossWallet.ui.screens.filters.list

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.screens.filters.FilterViewModel
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterListScreen(
    viewModel: FilterViewModel,
    onBackClick: () -> Unit,
    onAddFilterClick: () -> Unit,
    onEditFilterClick: (Filter) -> Unit
) {
    val filters by viewModel.allFilters.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Filters", style = AppTextStyles.screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddFilterClick,
                containerColor = AppColors.primary,
                contentColor = AppColors.onPrimary,
                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Filter")
            }
        }
    ) { innerPadding ->
        if (filters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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
                    .padding(innerPadding)
                    .padding(horizontal = AeonTheme.dimensions.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.listPadding),
                contentPadding = PaddingValues(
                    top = AeonTheme.dimensions.screenPadding,
                    bottom = scrollBottomPadding()
                )
            ) {
                items(filters) { filter ->
                    FilterItem(
                        filter = filter,
                        onClick = { onEditFilterClick(filter) },
                        onDelete = { viewModel.deleteFilter(filter) }
                    )
                }
            }
        }
    }
}

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
