package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.getAccountIconByName

@Composable
fun AeonAccountSelection(
    accounts: List<Account>,
    selectedAccountIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    mode: AeonSelectMode = AeonSelectMode.SINGLE,
    title: String = "Select Account",
    allowOutsideWallet: Boolean = false,
    description: String? = null
) {
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val dims = AeonTheme.dimensions

    AeonSelectionSheet(
        title = title,
        onDismiss = onDismiss,
        items = accounts,
        selectedIds = selectedAccountIds,
        onSelectionChange = onSelectionChange,
        idMapper = { it.id },
        labelMapper = { it.name },
        mode = mode,
        iconMapper = { getAccountIconByName(it.icon) },
        colorMapper = { Color(android.graphics.Color.parseColor(it.colorHex)) },
        detailMapper = { it.currencyCode },
        searchPlaceholder = "Search accounts...",
        headerContent = {
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = dims.spacingSm)
                )
            }
            
            if (allowOutsideWallet) {
                AeonHybridSelector(
                    items = listOf(null),
                    mode = mode,
                    selectedIds = if (selectedAccountIds.contains("")) setOf(null) else emptySet<String?>(),
                    onSelectionChange = { 
                        onSelectionChange(selectedAccountIds + "") // Simplistic mapping for "Outside"
                        if (mode == AeonSelectMode.SINGLE) onDismiss()
                    },
                    idMapper = { null },
                    labelMapper = { "Outside of Wallet" },
                    detailMapper = { "External transfer" },
                    iconMapper = { Icons.Rounded.Public },
                    colorMapper = { tertiaryColor }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = dims.spacingSm),
                    thickness = (dims.spacingXs / 8),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            }
        }
    )
}
