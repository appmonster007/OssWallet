package com.osscabinet.gleemanager.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.util.isDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    onBackClick: () -> Unit,
    onAddAccountClick: () -> Unit,
    viewModel: AccountViewModel
) {
    val accounts by viewModel.accounts.collectAsState()
    var editList by remember(accounts) { mutableStateOf(accounts) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Accounts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAddAccountClick) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Account")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(editList, key = { _, account -> account.id }) { index, account ->
                AccountManagementItem(
                    account = account,
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
                    onDelete = { viewModel.deleteAccount(account.id) }
                )
            }
        }
    }
}

@Composable
fun AccountManagementItem(
    account: Account,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val accountColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete '${account.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (account.isArchived) 0.1f else 0.3f),
        border = if (account.isArchived) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reorder Controls
            Column {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move Up", tint = MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move Down", tint = MaterialTheme.colorScheme.outline)
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            // Account Icon
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = accountColor.copy(alpha = if (account.isArchived) 0.3f else 1f)
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
                        tint = if (account.isArchived) accountColor else (if (accountColor.isDark()) Color.White else Color.Black)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    account.name, 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.Bold,
                    color = if (account.isArchived) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (account.isArchived) "Archived" else account.structureType, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Actions
            Row {
                IconButton(onClick = onToggleArchive) {
                    Icon(
                        if (account.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                        contentDescription = if (account.isArchived) "Unarchive" else "Archive",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
