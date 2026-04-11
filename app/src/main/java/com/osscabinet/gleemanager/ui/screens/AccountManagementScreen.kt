package com.osscabinet.gleemanager.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.domain.model.Account
import com.osscabinet.gleemanager.ui.viewmodel.AccountViewModel
import com.osscabinet.gleemanager.ui.util.isDark

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                editList.forEachIndexed { index, account ->
                    AccountManagementCard(
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
                        onDelete = { viewModel.deleteAccount(account.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (editList.size % 2 != 0) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AccountManagementCard(
    account: Account,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accountColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
    val contentColor = if (accountColor.isDark()) Color.White else Color.Black
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
        modifier = modifier.then(
            if (account.isArchived) Modifier.alpha(0.6f) else Modifier
        ),
        shape = RoundedCornerShape(24.dp),
        color = accountColor,
        contentColor = contentColor,
        border = if (account.isArchived) BorderStroke(1.dp, contentColor.copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
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
                        modifier = Modifier.size(18.dp),
                        tint = contentColor.copy(alpha = 0.7f)
                    )
                    Text(
                        account.name,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (account.isArchived) {
                    Icon(
                        Icons.Rounded.Archive,
                        contentDescription = "Archived",
                        modifier = Modifier.size(14.dp),
                        tint = contentColor.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                account.structureType,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move Up", tint = contentColor)
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move Down", tint = contentColor)
                }
                IconButton(onClick = onToggleArchive, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (account.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                        contentDescription = if (account.isArchived) "Unarchive" else "Archive",
                        tint = contentColor
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = contentColor)
                }
            }
        }
    }
}
