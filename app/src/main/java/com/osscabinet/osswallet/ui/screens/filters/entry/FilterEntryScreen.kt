package com.osscabinet.ossWallet.ui.screens.filters.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Filter
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.ui.components.entry.AeonEntryScaffold
import com.osscabinet.ossWallet.ui.components.transactions.CompactTextField
import com.osscabinet.ossWallet.ui.components.transactions.FilterFields

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterEntryScreen(
    filter: Filter?,
    accounts: List<Account>,
    allCategories: List<Category>,
    onSaveFilter: (Filter) -> Unit,
    onBackClick: () -> Unit
) {
    var filterName by remember { mutableStateOf(filter?.name ?: "") }
    var localFilter by remember { mutableStateOf(filter?.filter ?: TransactionFilter()) }

    var navTriggered by remember { mutableStateOf(false) }

    AeonEntryScaffold(
        title = if (filter == null) "New Filter" else "Edit Filter",
        onBackClick = onBackClick,
        onSaveClick = {
            if (filterName.isNotBlank() && !navTriggered) {
                navTriggered = true
                onSaveFilter(Filter(
                    id = filter?.id ?: 0,
                    name = filterName,
                    filter = localFilter
                ))
            }
        },
        canSave = filterName.isNotBlank() && !navTriggered,
        isEditMode = filter != null
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Filter Name field
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "FILTER NAME",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                CompactTextField(
                    value = filterName,
                    onValueChange = { filterName = it },
                    placeholder = "Enter filter name...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Shared Filter Fields
            FilterFields(
                filter = localFilter,
                onFilterChange = { localFilter = it },
                accounts = accounts,
                allCategories = allCategories
            )
        }
    }
}
