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

@Composable
fun AeonFilterSelection(
    filters: List<Filter>,
    selectedFilterId: Long?,
    onFilterSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Filter",
    showCreateNew: Boolean = true
) {
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
                    .padding(horizontal = 24.dp)
            ) {
                AeonSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search filters..."
                )
                
                if (showCreateNew) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Surface(
                        onClick = { onFilterSelected(null); onDismiss() },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create New Filter", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filtered) { filter ->
                        Card(
                            onClick = { onFilterSelected(filter.id); onDismiss() },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (filter.id == selectedFilterId) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
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
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(filter.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    val detailText = buildString {
                                        if (filter.filter.categoryIds.isEmpty()) append("All Categories")
                                        else append("${filter.filter.categoryIds.size} Categories")
                                        
                                        append(" • ")
                                        
                                        if (filter.filter.accountIds.isEmpty()) append("All Accounts")
                                        else append("${filter.filter.accountIds.size} Accounts")
                                    }
                                    Text(detailText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                                if (filter.id == selectedFilterId) {
                                    Icon(
                                        Icons.Rounded.FilterList, // Or Check
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
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
