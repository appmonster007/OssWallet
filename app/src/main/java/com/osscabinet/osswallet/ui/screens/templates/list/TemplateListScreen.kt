package com.osscabinet.ossWallet.ui.screens.templates.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode

import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.AeonFloatingActionButton
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.management.AeonListItem
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarActionPill
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.reorderable.ReorderableItem
import com.osscabinet.ossWallet.ui.reorderable.longPressDraggableHandle
import com.osscabinet.ossWallet.ui.reorderable.rememberReorderableLazyListState
import com.osscabinet.ossWallet.ui.reorderable.reorderable
import com.osscabinet.ossWallet.ui.reorderable.reorderableSemantics
import com.osscabinet.ossWallet.ui.components.accounts.AccountSelection
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.screens.accounts.AccountViewModel
import com.osscabinet.ossWallet.ui.screens.templates.TemplateViewModel
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ColorOnDark
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    viewModel: TemplateViewModel,
    accountViewModel: AccountViewModel,
    categoryMap: Map<Long, Category>,
    actions: TemplateListActions
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val starredTemplates by viewModel.starredTemplates.collectAsStateWithLifecycle()

    TemplateListScreenContent(
        templates = templates,
        starredTemplates = starredTemplates,
        categoryMap = categoryMap,
        actions = actions,
        onDeleteTemplate = { viewModel.deleteTemplate(it) },
        onUpdateOrder = { viewModel.updateTemplatesOrder(it) },
        onToggleStar = { viewModel.toggleStar(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreenContent(
    templates: List<Template>,
    starredTemplates: List<Template>,
    categoryMap: Map<Long, Category>,
    actions: TemplateListActions,
    onDeleteTemplate: (Template) -> Unit,
    onUpdateOrder: (List<Template>) -> Unit,
    onToggleStar: (Template) -> Unit
) {
    var editList by remember { mutableStateOf(emptyList<Template>()) }
    var templateToDelete by remember { mutableStateOf<Template?>(null) }
    var isReorderMode by remember { mutableStateOf(false) }
    var navTriggered by remember { mutableStateOf(false) }

    BackHandler(enabled = templateToDelete != null) { templateToDelete = null }
    BackHandler(enabled = isReorderMode) { isReorderMode = false }

    AeonConfirmDeleteDialog(
        show = templateToDelete != null,
        title = "Delete Template",
        message = "Are you sure you want to delete the template \"${templateToDelete?.name}\"?",
        onConfirm = {
            templateToDelete?.let { onDeleteTemplate(it) }
            templateToDelete = null
        },
        onDismiss = { templateToDelete = null }
    )

    LaunchedEffect(starredTemplates) {
        if (editList.isEmpty() || editList.size != starredTemplates.size || editList.map { it.id }
                .toSet() != starredTemplates.map { it.id }.toSet()) {
            editList = starredTemplates
        } else {
            // Update properties but keep order
            val orderMap = editList.withIndex().associate { it.value.id to it.index }
            editList = starredTemplates.sortedBy { orderMap[it.id] ?: Int.MAX_VALUE }
        }
    }

    val navigate: (() -> Unit) -> Unit = { action ->
        if (!navTriggered) {
            navTriggered = true
            action()
        }
    }

    AeonHazeProvider(backgroundColor = AppColors.screenBackground) { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                floatingActionButton = {
                    if (!isReorderMode) {
                        AeonFloatingActionButton(
                            onClick = { navigate { actions.onAddTemplateClick(null) } },
                            icon = Icons.Rounded.Add,
                            contentDescription = "Add Template"
                        )
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                ) {
                    if (isReorderMode) {
                        val reorderState = rememberReorderableLazyListState(
                            onMove = { from, to ->
                                editList = editList.toMutableList().apply { add(to, removeAt(from)) }
                            }
                        )
                        LazyColumn(
                            state = reorderState.lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .reorderable(reorderState),
                            contentPadding = PaddingValues(
                                start = AeonTheme.dimensions.screenPadding,
                                end = AeonTheme.dimensions.screenPadding,
                                top = AeonSize.D64.value + AeonTheme.dimensions.screenPadding,
                                bottom = padding.calculateBottomPadding() + scrollBottomPadding()
                            ),
                            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.listPadding)
                        ) {
                            items(editList, key = { it.id }) { template ->
                                ReorderableItem(
                                    state = reorderState,
                                    key = template.id,
                                    modifier = Modifier
                                        .longPressDraggableHandle(
                                            state = reorderState,
                                            key = template.id,
                                            onDragStopped = { onUpdateOrder(editList) },
                                        )
                                        .reorderableSemantics(state = reorderState, key = template.id),
                                ) { isDragging ->
                                    ReorderTemplateItem(
                                        template = template,
                                        isDragging = isDragging
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding(),
                            contentPadding = PaddingValues(
                                start = AeonTheme.dimensions.screenPadding,
                                end = AeonTheme.dimensions.screenPadding,
                                top = AeonSize.D64.value + AeonTheme.dimensions.screenPadding,
                                bottom = padding.calculateBottomPadding() + scrollBottomPadding()
                            ),
                            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.listPadding)
                        ) {
                            items(templates, key = { it.id }) { template ->
                                TemplateItem(
                                    template = template,
                                    categoryName = categoryMap[template.categoryId]?.name ?: "Unknown",
                                    categoryIconName = categoryMap[template.categoryId]?.iconName,
                                    onEdit = { navigate { actions.onAddTemplateClick(template.id) } },
                                    onDelete = { templateToDelete = template },
                                    onToggleStar = { onToggleStar(template) },
                                    onCreatePlannedPayment = { actions.onCreatePlannedPayment(template) }
                                )
                            }
                        }
                    }
                }
            }

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = if (isReorderMode) "Reorder Starred" else "Templates",
                            onBackClick = if (isReorderMode) {
                                { isReorderMode = false }
                            } else {
                                actions.onBackClick
                            }
                        )
                    }
                ),
                right = AeonTopBarSection(
                    visible = starredTemplates.isNotEmpty(),
                    content = {
                        AeonTopBarActionPill {
                            AppBarIconButton(
                                icon = if (isReorderMode) Icons.Rounded.Check else Icons.Rounded.SwapVert,
                                onClick = { isReorderMode = !isReorderMode }
                            )
                        }
                    }
                )
            )
        }
    }
}

data class TemplateListActions(
    val onBackClick: () -> Unit,
    val onAddTemplateClick: (Long?) -> Unit,
    val onCreatePlannedPayment: (Template) -> Unit
)

@Composable
fun ReorderTemplateItem(
    template: Template,
    isDragging: Boolean = false
) {
    AeonListItem(
        title = template.name,
        icon = Icons.Rounded.Star,
        iconTint = AppColors.primary,
        isDragging = isDragging,
        leadingContent = {
            Icon(
                Icons.Rounded.DragHandle,
                contentDescription = null,
                tint = AppColors.border,
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd)
            )
        }
    )
}

@Composable
fun TemplateItem(
    template: Template,
    categoryName: String,
    categoryIconName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStar: () -> Unit,
    onCreatePlannedPayment: () -> Unit
) {
    val (badgeColor, badgeIcon) = when (template.entryMode) {
        EntryMode.EXPENSE -> AppColors.error to Icons.Rounded.ArrowDownward
        EntryMode.INCOME -> PositiveAmountColor to Icons.Rounded.ArrowUpward
        EntryMode.TRANSFER -> AccentBlue to Icons.Rounded.SwapHoriz
    }

    val amountColor = when (template.entryMode) {
        EntryMode.EXPENSE -> AppColors.error
        EntryMode.INCOME -> PositiveAmountColor
        EntryMode.TRANSFER -> AccentBlue
    }

    val formattedAmount = remember(template.amount, template.currency) {
        CurrencyFormatter.formatAuto(template.amount, template.currency)
    }

    AeonListItem(
        title = template.name,
        subtitle = categoryName,
        icon = getIconByName(categoryIconName),
        iconTint = AppColors.primary,
        onClick = onEdit,
        iconOverlay = {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-4).dp, y = 4.dp),
                shape = CircleShape,
                color = badgeColor,
                border = BorderStroke(2.dp, AppColors.panelSurface)
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = ColorOnDark,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(2.dp)
                )
            }
        },
        trailingContent = {
            Text(
                text = formattedAmount,
                style = AppTextStyles.listItemTitle,
                color = amountColor
            )
        },
        menuItems = { onDismiss ->
            DropdownMenuItem(
                text = { Text("Create Planned Payment") },
                leadingIcon = { Icon(Icons.Rounded.Schedule, null, tint = AppColors.primary) },
                onClick = { onDismiss(); onCreatePlannedPayment() }
            )
            DropdownMenuItem(
                text = { Text(if (template.isStarred) "Unstar" else "Star") },
                leadingIcon = {
                    Icon(
                        if (template.isStarred) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        null,
                        tint = if (template.isStarred) AppColors.primary else AppColors.border
                    )
                },
                onClick = { onDismiss(); onToggleStar() }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = AppColors.error) },
                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = AppColors.error) },
                onClick = { onDismiss(); onDelete() }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDialog(
    template: Template?,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Double, EntryMode, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf(template?.name ?: "") }
    var categoryId by remember { mutableLongStateOf(template?.categoryId ?: Category.SYSTEM_UNKNOWN) }
    var amount by remember { mutableStateOf(template?.amount?.toString() ?: "") }
    var type by remember { mutableStateOf(template?.entryMode ?: EntryMode.EXPENSE) }
    var note by remember { mutableStateOf(template?.note ?: "") }
    var accountId by remember { mutableStateOf(template?.accountId) }

    var showAccountPicker by remember { mutableStateOf(false) }
    val selectedAccount = remember(accountId, accounts) { accounts.find { it.id == accountId } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (template == null) "New Template" else "Edit Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Note: Simplified for now, real implementation should use a category picker
                OutlinedTextField(
                    value = categoryId.toString(),
                    onValueChange = { categoryId = it.toLongOrNull() ?: Category.SYSTEM_UNKNOWN },
                    label = { Text("Category ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Default Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Default Note (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    onClick = { showAccountPicker = true },
                    shape = RoundedCornerShape(AeonTheme.dimensions.spacingSm),
                    border = BorderStroke(1.dp, AppColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            selectedAccount?.name ?: "Default Account (Optional)",
                            color = if (selectedAccount != null) AppColors.contentPrimary else AppColors.border
                        )
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
                ) {
                    FilterChip(
                        selected = type == EntryMode.EXPENSE,
                        onClick = { type = EntryMode.EXPENSE },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = type == EntryMode.INCOME,
                        onClick = { type = EntryMode.INCOME },
                        label = { Text("Income") }
                    )
                    FilterChip(
                        selected = type == EntryMode.TRANSFER,
                        onClick = { type = EntryMode.TRANSFER },
                        label = { Text("Transfer") }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    name,
                    categoryId,
                    amount.toDoubleOrNull() ?: 0.0,
                    type,
                    note.ifBlank { null },
                    accountId
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showAccountPicker) {
        AccountSelection(
            accounts = accounts,
            selectedAccount = selectedAccount,
            onAccountSelected = {
                accountId = it.id
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TemplateItemPreview() {
    OssWalletTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Templates Overview", style = AppTextStyles.cardTitle)
            
            TemplateItem(
                template = Template(
                    name = "Starbucks Coffee",
                    categoryId = 1,
                    amount = BigDecimal("5.50"),
                    entryMode = EntryMode.EXPENSE,
                    currency = "USD",
                    accountId = "1"
                ),
                categoryName = "Food & Drink",
                categoryIconName = "Restaurant",
                onEdit = {},
                onDelete = {},
                onToggleStar = {},
                onCreatePlannedPayment = {}
            )

            TemplateItem(
                template = Template(
                    name = "Monthly Salary",
                    categoryId = 2,
                    amount = BigDecimal("5000.00"),
                    entryMode = EntryMode.INCOME,
                    currency = "USD",
                    accountId = "1",
                    isStarred = true
                ),
                categoryName = "Salary",
                categoryIconName = "Payments",
                onEdit = {},
                onDelete = {},
                onToggleStar = {},
                onCreatePlannedPayment = {}
            )

            TemplateItem(
                template = Template(
                    name = "Savings Transfer",
                    categoryId = 3,
                    amount = BigDecimal("1000.00"),
                    entryMode = EntryMode.TRANSFER,
                    currency = "USD",
                    accountId = "1"
                ),
                categoryName = "Transfer",
                categoryIconName = "SyncAlt",
                onEdit = {},
                onDelete = {},
                onToggleStar = {},
                onCreatePlannedPayment = {}
            )
        }
    }
}
