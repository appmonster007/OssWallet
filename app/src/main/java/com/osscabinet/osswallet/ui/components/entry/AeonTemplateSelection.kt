package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.theme.AppColors

@Composable
fun AeonTemplateSelection(
    templates: List<Template>,
    categories: List<Category>,
    onTemplateSelected: (Template?) -> Unit,
    onDismiss: () -> Unit,
    showCreateNew: Boolean = true
) {
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    
    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = "Link Template",
            onBack = onDismiss
        ) { padding ->
            var query by remember { mutableStateOf("") }
            val filtered = remember(query, templates) {
                if (query.isBlank()) templates
                else templates.filter { it.name.contains(query, ignoreCase = true) || it.name.fuzzyMatch(query) }
            }

            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
                AeonSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search templates..."
                )
                
                if (showCreateNew) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Surface(
                        onClick = { onTemplateSelected(null); onDismiss() },
                        shape = RoundedCornerShape(16.dp),
                        color = AppColors.primary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, AppColors.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = AppColors.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create New Template", color = AppColors.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filtered) { template ->
                        val category = categoryMap[template.categoryId]
                        Card(
                            onClick = { onTemplateSelected(template); onDismiss() },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.insetSurface.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = CircleShape,
                                        color = AppColors.primary.copy(alpha = 0.1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = getIconByName(category?.iconName),
                                                contentDescription = null,
                                                tint = AppColors.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(category?.name ?: "Unknown", style = MaterialTheme.typography.bodySmall, color = AppColors.border)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${if (template.transactionType == TransactionType.INCOME) "+" else "-"}${template.amount} ${template.currency ?: ""}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (template.transactionType == TransactionType.INCOME) PositiveAmountColor else AppColors.error,
                                        fontWeight = FontWeight.Bold
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
