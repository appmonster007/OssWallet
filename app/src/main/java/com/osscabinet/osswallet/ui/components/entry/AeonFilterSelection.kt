package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun AeonFilterSelection(
    filters: List<Filter>,
    selectedFilterId: Long?,
    onFilterSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Filter",
    showCreateNew: Boolean = true
) {
    val dims = AeonTheme.dimensions
    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = title,
            onBack = onDismiss
        ) { padding ->
            var query by remember { mutableStateOf("") }
            val filtered = remember(query, filters) {
                if (query.isBlank()) filters
                else filters.filter { it.name.contains(query, ignoreCase = true) || it.name.fuzzyMatch(query) }
            }

            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = dims.spacingXl)
            ) {
                AeonSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search filters..."
                )
                
                if (showCreateNew) {
                    Spacer(modifier = Modifier.height(dims.spacingLg))
                    
                    Surface(
                        onClick = { onFilterSelected(null); onDismiss() },
                        shape = RoundedCornerShape(dims.componentCornerRadius),
                        color = AppColors.primary.copy(alpha = 0.1f),
                        border = BorderStroke((dims.spacingXs / 4), AppColors.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(dims.spacingLg),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = AppColors.primary)
                            Spacer(modifier = Modifier.width(dims.spacingSm))
                            Text("Create New Filter", color = AppColors.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(dims.spacingXl))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(dims.spacingMd),
                    contentPadding = PaddingValues(bottom = dims.spacingXl)
                ) {
                    items(filtered) { filter ->
                        Card(
                            onClick = { onFilterSelected(filter.id); onDismiss() },
                            shape = RoundedCornerShape(dims.componentCornerRadius),
                            colors = CardDefaults.cardColors(
                                containerColor = if (filter.id == selectedFilterId) 
                                    AppColors.selectionContainer 
                                else 
                                    AppColors.insetSurface.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(dims.spacingLg),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(dims.iconSizeXl),
                                    shape = CircleShape,
                                    color = AppColors.primary.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.FilterList,
                                            contentDescription = null,
                                            tint = AppColors.primary,
                                            modifier = Modifier.size(dims.containerCornerRadius)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(dims.spacingLg))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(filter.name, style = AppTextStyles.screenSubTitle, fontWeight = FontWeight.Bold)
                                    val detailText = buildString {
                                        if (filter.filter.categoryIds.isEmpty()) append("All Categories")
                                        else append("${filter.filter.categoryIds.size} Categories")
                                        
                                        append(" • ")
                                        
                                        if (filter.filter.accountIds.isEmpty()) append("All Accounts")
                                        else append("${filter.filter.accountIds.size} Accounts")
                                    }
                                    Text(detailText, style = AppTextStyles.caption, color = AppColors.border)
                                }
                                if (filter.id == selectedFilterId) {
                                    Icon(
                                        Icons.Rounded.FilterList, // Or Check
                                        contentDescription = null,
                                        tint = AppColors.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
