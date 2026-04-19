package com.osscabinet.ossWallet.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.ui.components.AeonDashedButton
import com.osscabinet.ossWallet.ui.components.AeonExtendedFloatingActionButton
import com.osscabinet.ossWallet.ui.components.AeonFloatingActionButton
import com.osscabinet.ossWallet.ui.components.ReorderableList
import com.osscabinet.ossWallet.ui.util.formatCurrencyRight
import com.osscabinet.ossWallet.ui.util.isDark
import com.osscabinet.ossWallet.ui.viewmodel.AccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    onBackClick: () -> Unit,
    onAddAccountClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    viewModel: AccountViewModel
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var editList by remember { mutableStateOf(emptyList<Account>()) }

    var navTriggered by remember { mutableStateOf(false) }
    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    LaunchedEffect(accounts) {
        if (editList.isEmpty() || editList.size != accounts.size || editList.map { it.id }.toSet() != accounts.map { it.id }.toSet()) {
            editList = accounts
        } else {
            // Update properties but keep order
            val orderMap = editList.withIndex().associate { it.value.id to it.index }
            editList = accounts.sortedBy { orderMap[it.id] ?: Int.MAX_VALUE }
        }
    }
    
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedAccountIds by remember { mutableStateOf(setOf<String>()) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showMultiDeleteDialog by remember { mutableStateOf(false) }

    val selectedAccounts = remember(selectedAccountIds, accounts) {
        accounts.filter { it.id in selectedAccountIds }
    }

    if (showMultiDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showMultiDeleteDialog = false },
            title = { Text("Delete Selected Accounts") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to delete these accounts? This cannot be undone.")
                    Spacer(modifier = Modifier.height(8.dp))
                    selectedAccounts.forEach { account ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(account.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(formatCurrencyRight(account.balance, account.currencyCode), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedAccountIds.forEach { viewModel.deleteAccount(it) }
                    selectedAccountIds = emptySet()
                    isSelectionMode = false
                    showMultiDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMultiDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isSelectionMode) "${selectedAccountIds.size} Selected" else "Manage Accounts", 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = if (isSelectionMode) { { isSelectionMode = false; selectedAccountIds = emptySet() } } else { { navigate(onBackClick) } }) {
                        Icon(
                            if (isSelectionMode) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack, 
                            contentDescription = if (isSelectionMode) "Exit Selection" else "Back"
                        )
                    }
                },
                actions = {
                    if (!isSelectionMode) {
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Rounded.MoreHoriz, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Select to Delete") },
                                    onClick = {
                                        isSelectionMode = true
                                        showMoreMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.Checklist, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isSelectionMode) {
                if (selectedAccountIds.isNotEmpty()) {
                    AeonExtendedFloatingActionButton(
                        onClick = { showMultiDeleteDialog = true },
                        icon = Icons.Rounded.Delete,
                        text = "Delete Selected (${selectedAccountIds.size})",
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                }
            } else {
                AeonFloatingActionButton(
                    onClick = { navigate(onAddAccountClick) },
                    icon = Icons.Rounded.Add,
                    contentDescription = "Add New Account"
                )
            }
        }
    ) { innerPadding ->
        // Reorderable List of Accounts
        ReorderableList(
            items = editList,
            onReorder = { newList ->
                editList = newList
            },
            onDragEnd = { newList ->
                viewModel.updateAccountsOrder(newList)
            },
            key = { it.id },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background)
                .graphicsLayer(clip = false),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 100.dp, // Space for the FAB
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) { account, isDragging ->
            val index = editList.indexOf(account)
            AccountManagementListItem(
                account = account,
                isSelectionMode = isSelectionMode,
                isSelected = account.id in selectedAccountIds,
                isDragging = isDragging,
                onSelect = {
                    selectedAccountIds = if (account.id in selectedAccountIds) {
                        selectedAccountIds - account.id
                    } else {
                        selectedAccountIds + account.id
                    }
                },
                onMoveUp = {
                    if (index > 0) {
                        val newList = editList.toMutableList()
                        val item = newList.removeAt(index)
                        newList.add(index - 1, item)
                        editList = newList
                        viewModel.updateAccountsOrder(newList)
                    }
                },
                onMoveDown = {
                    if (index < editList.size - 1) {
                        val newList = editList.toMutableList()
                        val item = newList.removeAt(index)
                        newList.add(index + 1, item)
                        editList = newList
                        viewModel.updateAccountsOrder(newList)
                    }
                },
                onToggleArchive = { viewModel.toggleArchiveAccount(account) },
                onAccountClick = { navigate { onAccountClick(account.id) } }
            )
        }
    }
}

@Composable
fun AccountManagementListItem(
    account: Account,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isDragging: Boolean = false,
    onSelect: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleArchive: () -> Unit,
    onAccountClick: () -> Unit
) {
    val accountColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
    val contentColor = if (accountColor.isDark()) Color.White else Color.Black

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (account.isArchived) 0.6f else 1f)
            .clickable(enabled = isSelectionMode) { onSelect() },
        shape = RoundedCornerShape(16.dp),
        color = when {
            isDragging -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            1.dp, 
            if (isSelected || isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        ),
        shadowElevation = if (isDragging) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() }
                )
            } else {
                // Drag handle icon
                Icon(
                    Icons.Rounded.DragHandle,
                    contentDescription = "Drag to reorder",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            // Account Icon and Info (Clickable)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !isSelectionMode) { onAccountClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Account Icon
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = accountColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val icon = when(account.icon) {
                            "Savings" -> Icons.Rounded.Savings
                            "CreditCard" -> Icons.Rounded.CreditCard
                            "AccountBalance" -> Icons.Rounded.AccountBalance
                            "Wallet" -> Icons.Rounded.Wallet
                            "RocketLaunch" -> Icons.Rounded.RocketLaunch
                            "Payments" -> Icons.Rounded.Payments
                            "Paid" -> Icons.Rounded.Paid
                            "AccountBalanceWallet" -> Icons.Rounded.AccountBalanceWallet
                            "MonetizationOn" -> Icons.Rounded.MonetizationOn
                            "Analytics" -> Icons.Rounded.Analytics
                            "Foundation" -> Icons.Rounded.Foundation
                            "Stars" -> Icons.Rounded.Stars
                            "Diamond" -> Icons.Rounded.Diamond
                            "AccountTree" -> Icons.Rounded.AccountTree
                            "AutoGraph" -> Icons.Rounded.AutoGraph
                            else -> Icons.Rounded.AccountBalance
                        }
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = contentColor
                        )
                    }
                }

                // Account Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        account.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        account.structureType,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = !account.isArchived,
                    onCheckedChange = { onToggleArchive() },
                    modifier = Modifier.scale(0.7f),
                    thumbContent = {
                        Icon(
                            if (account.isArchived) Icons.Rounded.Archive else Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                )
            }
        }
    }
}
