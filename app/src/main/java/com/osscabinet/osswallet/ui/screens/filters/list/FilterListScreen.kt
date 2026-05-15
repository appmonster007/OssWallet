package com.osscabinet.ossWallet.ui.screens.filters.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.ui.screens.filters.FilterViewModel
import com.osscabinet.ossWallet.ui.theme.AeonTheme

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
                title = { Text("Saved Filters", style = MaterialTheme.typography.headlineSmall) },
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
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
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingLg))
                    Text(
                        "No saved filters yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
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
                contentPadding = PaddingValues(vertical = AeonTheme.dimensions.screenPadding)
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    filter.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}
