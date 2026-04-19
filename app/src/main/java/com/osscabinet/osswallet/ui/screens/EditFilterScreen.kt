package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.FilterEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.toDomain
import com.osscabinet.ossWallet.domain.model.toEntity
import com.osscabinet.ossWallet.ui.components.transactions.CompactTextField
import com.osscabinet.ossWallet.ui.components.transactions.FilterFields
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFilterScreen(
    filterEntity: FilterEntity?,
    accounts: List<Account>,
    allCategories: List<CategoryEntity>,
    onSaveFilter: (FilterEntity) -> Unit,
    onBackClick: () -> Unit
) {
    var filterName by remember { mutableStateOf(filterEntity?.name ?: "") }
    var localFilter by remember { mutableStateOf(filterEntity?.toDomain() ?: TransactionFilter()) }

    var navTriggered by remember { mutableStateOf(false) }

    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (filterEntity == null) "New Filter" else "Edit Filter") },
                navigationIcon = {
                    IconButton(onClick = { navigate(onBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (filterName.isNotBlank()) {
                            navigate {
                                onSaveFilter(localFilter.toEntity(filterName, filterEntity?.id ?: 0))
                            }
                        }
                    }) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
