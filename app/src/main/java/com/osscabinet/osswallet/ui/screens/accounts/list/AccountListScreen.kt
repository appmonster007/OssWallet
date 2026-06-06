package com.osscabinet.ossWallet.ui.screens.accounts.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAuto
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.AeonExtendedFloatingActionButton
import com.osscabinet.ossWallet.ui.components.AeonFloatingActionButton
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarActionPill
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.components.ReorderableList
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ColorOnDark
import com.osscabinet.ossWallet.ui.theme.ColorOnLight
import com.osscabinet.ossWallet.ui.util.isDark
import java.math.BigDecimal

/**
 * Grouped actions for the Account List screen.
 */
data class AccountListActions(
    val onBackClick: () -> Unit,
    val onAddAccountClick: () -> Unit,
    val onAccountClick: (String) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    actions: AccountListActions,
    viewModel: AccountViewModel
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val balances by viewModel.accountBalances.collectAsStateWithLifecycle()
    var editList by remember { mutableStateOf(emptyList<Account>()) }

    var navTriggered by remember { mutableStateOf(false) }
    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    LaunchedEffect(accounts) {
        if (editList.isEmpty() || editList.size != accounts.size || editList.map { it.id }
                .toSet() != accounts.map { it.id }.toSet()) {
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

    BackHandler(enabled = showMoreMenu) { showMoreMenu = false }
    BackHandler(enabled = showMultiDeleteDialog) { showMultiDeleteDialog = false }
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false; selectedAccountIds = emptySet()
    }

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
                        Text(
                            account.name,
                            style = AppTextStyles.transactionTitle.copy(fontWeight = FontWeight.Bold)
                        )
                        val balance = balances[account.id] ?: BigDecimal.ZERO
                        Text(
                            formatAuto(balance, account.currencyCode),
                            style = AppTextStyles.caption,
                            color = if (balance < BigDecimal.ZERO) AppColors.error else AppColors.contentPrimary
                        )
                    }
                }
            }
        }
    )

    val pillHeight = AeonSize.D64.value

    AeonHazeProvider { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
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
                            onClick = { navigate(actions.onAddAccountClick) },
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
                        .statusBarsPadding()
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .background(AppColors.screenBackground)
                        .graphicsLayer(clip = false),
                    contentPadding = PaddingValues(
                        top = pillHeight + AeonTheme.dimensions.screenPadding,
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
                        onAccountClick = { navigate { actions.onAccountClick(account.id) } }
                    )
                }
            }

            // 2. Top Bar Layer (Layer 3 - Floating Pills)
            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = if (isSelectionMode) "${selectedAccountIds.size} Selected" else "Manage Accounts",
                            onBackClick = if (isSelectionMode) {
                                { isSelectionMode = false; selectedAccountIds = emptySet() }
                            } else {
                                { navigate(actions.onBackClick) }
                            }
                        )
                    }
                ),
                right = AeonTopBarSection(
                    content = {
                        if (!isSelectionMode) {
                            AeonTopBarActionPill {
                                Box {
                                    AppBarIconButton(
                                        icon = Icons.Rounded.MoreHoriz,
                                        onClick = { showMoreMenu = true }
                                    )
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
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Rounded.Checklist,
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
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
    val accountColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (_: Exception) {
        AppColors.primary
    }
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
                        style = AppTextStyles.managementItemTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        account.structureType.displayName,
                        style = AppTextStyles.transactionSwipeAction,
                        color = AppColors.border
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = !account.isArchived,
                    onCheckedChange = { onToggleArchive() },
                    modifier = Modifier.scale(0.7f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AppColors.primary,
                        checkedTrackColor = AppColors.switchTrackOn,
                        checkedBorderColor = AppColors.primary,
                        uncheckedThumbColor = AppColors.switchThumbOff,
                        uncheckedTrackColor = AppColors.switchTrackOff,
                        uncheckedBorderColor = AppColors.outline
                    ),
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
