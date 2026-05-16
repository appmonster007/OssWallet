package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun SharedFilterConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirmUpdateShared: () -> Unit,
    onConfirmCreateNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Shared Filter Detected",
                style = AppTextStyles.heroAmount
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                Text(
                    text = "This filter is also used by other budgets or loans. How would you like to save your changes?",
                    style = AppTextStyles.transactionTitle
                )
                
                Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingSm))
                
                // Option 1: Update Shared
                Surface(
                    onClick = onConfirmUpdateShared,
                    shape = MaterialTheme.shapes.medium,
                    color = AppColors.accentContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null)
                        Column {
                            Text(text = "Update Shared Filter", style = AppTextStyles.listSectionHeader)
                            Text(text = "Apply changes to all linked entities", style = AppTextStyles.caption)
                        }
                    }
                }
                
                // Option 2: Create New (Fork)
                Surface(
                    onClick = onConfirmCreateNew,
                    shape = MaterialTheme.shapes.medium,
                    color = AppColors.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.CallSplit, contentDescription = null)
                        Column {
                            Text(text = "Create Private Copy", style = AppTextStyles.listSectionHeader)
                            Text(text = "Only apply changes to this item", style = AppTextStyles.caption)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}
