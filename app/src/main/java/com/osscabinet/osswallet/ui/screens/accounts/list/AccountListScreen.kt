package com.osscabinet.ossWallet.ui.screens.accounts.list

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
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatCurrencyRight
import com.osscabinet.ossWallet.ui.theme.*
import com.osscabinet.ossWallet.ui.util.isDark
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
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

    AeonConfirmDeleteDialog(
        show = showMultiDeleteDialog,
        title = "Delete Selected Accounts",
        message = "Are you sure you want to delete these accounts? This cannot be undone.",
        onConfirm = {
            selectedAccountIds.forEach { viewModel.deleteAccount(it) }
            selectedAccountIds = emptySet()
            isSelectionMode = false
            showMultiDeleteDialog = false
        },
        onDismiss = { showMultiDeleteDialog = false },
        extraContent = {
            Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
                selectedAccounts.forEach { account ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(account.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(formatCurrencyRight(BigDecimal.ZERO, account.currencyCode), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    )

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
                        containerColor = AppColors.error,
                        contentColor = AppColors.onError
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
                .background(AppColors.screenBackground)
                .graphicsLayer(clip = false),
            contentPadding = PaddingValues(
                top = AeonTheme.dimensions.screenPadding,
                bottom = 100.dp,
                start = AeonTheme.dimensions.screenPadding,
                end = AeonTheme.dimensions.screenPadding
            ),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
        ) { account, isDragging ->
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
    onToggleArchive: () -> Unit,
    onAccountClick: () -> Unit
) {
    val accountColor = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (_: Exception) { AppColors.primary }
    val contentColor = if (accountColor.isDark()) ColorOnDark else ColorOnLight

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (account.isArchived) 0.6f else 1f)
            .clickable(enabled = isSelectionMode) { onSelect() },
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        color = when {
            isDragging -> AppColors.selectionContainer.copy(alpha = 0.9f)
            isSelected -> AppColors.selectionContainer.copy(alpha = 0.5f)
            else -> AppColors.insetSurface.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            1.dp, 
            if (isSelected || isDragging) AppColors.primary else AppColors.insetSurface
        ),
        shadowElevation = if (isDragging) AeonTheme.dimensions.spacingSm else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(AeonTheme.dimensions.spacingMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() }
                )
            } else {
                Icon(
                    Icons.Rounded.DragHandle,
                    contentDescription = "Drag to reorder",
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd),
                    tint = AppColors.border
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(AeonTheme.dimensions.spacingSm))
                    .clickable(enabled = !isSelectionMode) { onAccountClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
            ) {
                Surface(
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl),
                    shape = CircleShape,
                    color = accountColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val icon = AppIconMap[account.icon] ?: Icons.Rounded.AccountBalance
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd),
                            tint = contentColor
                        )
                    }
                }

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
                        color = AppColors.border
                    )
                }
            }

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
