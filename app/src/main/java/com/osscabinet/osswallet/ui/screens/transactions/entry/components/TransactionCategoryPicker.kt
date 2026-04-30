package com.osscabinet.ossWallet.ui.screens.transactions.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.ui.components.categoryIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCategoryPicker(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (String, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    val isSelected = category.id == selectedCategoryId
                    val categoryColor = category.colorHex?.let { Color(it) }
                        ?: MaterialTheme.colorScheme.primary

                    Surface(
                        onClick = { onCategorySelected(category.name, category.id) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) {
                            categoryColor.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(2.dp, categoryColor)
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Use category icon from iconName if available
                            val categoryIcon = categoryIcons[category.iconName] ?: Icons.Rounded.Category

                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = categoryColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    categoryIcon,
                                    contentDescription = null,
                                    tint = categoryColor,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = categoryColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
