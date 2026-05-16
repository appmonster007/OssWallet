package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun CurrencySelector(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    supportedCurrencies: List<String>
) {
    var showDialog by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier.clickable { showDialog = true },
            shape = RoundedCornerShape(12.dp),
            color = AppColors.insetSurface.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    selectedCurrency.ifEmpty { "USD" },
                    style = AppTextStyles.drawerItemLabel.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.contentSecondary
                    )
                )
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AppColors.contentSecondary
                )
            }
        }

        if (showDialog) {
            var query by remember { mutableStateOf("") }
            val baseList = if (supportedCurrencies.isEmpty()) listOf("USD") else supportedCurrencies.sorted()
            val filteredList = remember(query, baseList) {
                if (query.isBlank()) baseList
                else baseList.filter { it.contains(query, ignoreCase = true) }
            }

            AeonFullScreenDialog(onDismissRequest = { showDialog = false }) {
                AeonSelectionScreen(
                    title = "Select Currency",
                    onBack = { showDialog = false }
                ) { padding ->
                    Column(modifier = Modifier.padding(padding).padding(horizontal = 24.dp, vertical = 16.dp)) {
                        AeonSearchField(
                            query = query,
                            onQueryChange = { query = it },
                            placeholder = "Search currency code..."
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(modifier = Modifier.weight(1f, fill = false)) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                AeonHybridSelector(
                                    items = filteredList,
                                    mode = AeonSelectMode.SINGLE,
                                    selectedIds = if (selectedCurrency.isNotEmpty()) setOf(
                                        selectedCurrency
                                    ) else emptySet(),
                                    onSelectionChange = { ids ->
                                        ids.firstOrNull()?.let {
                                            onCurrencySelected(it)
                                            showDialog = false
                                        }
                                    },
                                    idMapper = { it },
                                    labelMapper = { it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
