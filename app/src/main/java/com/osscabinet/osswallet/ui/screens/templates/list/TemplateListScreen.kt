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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.AeonFloatingActionButton
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.components.ContourBackground
import com.osscabinet.ossWallet.ui.components.ReorderableList
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
import dev.chrisbanes.haze.hazeSource
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ColorOnDark
import com.osscabinet.ossWallet.ui.theme.PositiveAmountColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    onBackClick: () -> Unit,
    viewModel: TemplateViewModel,
    accountViewModel: AccountViewModel,
    categoryMap: Map<Long, Category>,
    onAddTemplateClick: (Long?) -> Unit,
    onCreatePlannedPayment: (Template) -> Unit
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val starredTemplates by viewModel.starredTemplates.collectAsStateWithLifecycle()

    TemplateListScreenContent(
        templates = templates,
        starredTemplates = starredTemplates,
        categoryMap = categoryMap,
        onBackClick = onBackClick,
        onAddTemplateClick = onAddTemplateClick,
        onDeleteTemplate = { viewModel.deleteTemplate(it) },
        onUpdateOrder = { viewModel.updateTemplatesOrder(it) },
        onToggleStar = { viewModel.toggleStar(it) },
        onCreatePlannedPayment = onCreatePlannedPayment
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreenContent(
    templates: List<Template>,
    starredTemplates: List<Template>,
    categoryMap: Map<Long, Category>,
    onBackClick: () -> Unit,
    onAddTemplateClick: (Long?) -> Unit,
    onDeleteTemplate: (Template) -> Unit,
    onUpdateOrder: (List<Template>) -> Unit,
    onToggleStar: (Template) -> Unit,
    onCreatePlannedPayment: (Template) -> Unit
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

    AeonHazeProvider { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                floatingActionButton = {
                    if (!isReorderMode) {
                        AeonFloatingActionButton(
                            onClick = { navigate { onAddTemplateClick(null) } },
                            icon = Icons.Rounded.Add,
                            contentDescription = "Add Template"
                        )
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.screenBackground)
                        .hazeSource(hazeLayers.states[HazeLayer.Overlay])
                ) {
                    ContourBackground()

                    if (isReorderMode) {
                        ReorderableList(
                            items = editList,
                            key = { it.id },
                            onReorder = { newList -> editList = newList },
                            onDragEnd = { newList -> onUpdateOrder(newList) },
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
                        ) { template, isDragging ->
                            ReorderTemplateItem(
                                template = template,
                                isDragging = isDragging
                            )
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
                                    onEdit = { navigate { onAddTemplateClick(template.id) } },
                                    onDelete = { templateToDelete = template },
                                    onToggleStar = { onToggleStar(template) },
                                    onCreatePlannedPayment = { onCreatePlannedPayment(template) }
                                )
                            }
                        }
                    }
                }
            }

            AeonTopAppBar(
                title = if (isReorderMode) "Reorder Starred" else "Templates",
                onBackClick = if (isReorderMode) {
                    { isReorderMode = false }
                } else {
                    onBackClick
                },
                actions = {
                    if (starredTemplates.isNotEmpty()) {
                        AppBarIconButton(
                            icon = if (isReorderMode) Icons.Rounded.Check else Icons.Rounded.SwapVert,
                            onClick = { isReorderMode = !isReorderMode }
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun ReorderTemplateItem(
    template: Template,
    isDragging: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        color = if (isDragging) AppColors.selectionContainer.copy(alpha = 0.9f)
        else AppColors.insetSurface.copy(alpha = 0.3f),
        border = if (isDragging) BorderStroke(1.dp, AppColors.primary) else null,
        shadowElevation = if (isDragging) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeonTheme.dimensions.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.DragHandle,
                contentDescription = null,
                tint = AppColors.border,
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd)
            )
            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
            Icon(Icons.Rounded.Star, contentDescription = null, tint = AppColors.primary)
            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
            Text(
                template.name,
                modifier = Modifier.weight(1f),
                style = AppTextStyles.screenSubTitle
            )
        }
    }
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
    val (badgeColor, badgeIcon) = when (template.transactionType) {
        TransactionType.EXPENSE -> AppColors.error to Icons.Rounded.ArrowDownward
        TransactionType.INCOME -> PositiveAmountColor to Icons.Rounded.ArrowUpward
        TransactionType.TRANSFER -> AccentBlue to Icons.Rounded.SwapHoriz
    }

    Card(
        onClick = onEdit,
        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppColors.insetSurface.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeonTheme.dimensions.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl),
                    shape = CircleShape,
                    color = AppColors.primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getIconByName(categoryIconName),
                            contentDescription = null,
                            tint = AppColors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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
            }
            Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingLg))
            Column(modifier = Modifier.weight(1f)) {
                Text(template.name, style = AppTextStyles.cardSectionTitle)
                Text(categoryName, style = AppTextStyles.caption, color = AppColors.border)
            }
            IconButton(onClick = onCreatePlannedPayment) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = "Create Planned Payment",
                    tint = AppColors.primary
                )
            }
            IconButton(onClick = onToggleStar) {
                Icon(
                    imageVector = if (template.isStarred) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Star",
                    tint = if (template.isStarred) AppColors.primary else AppColors.border
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete",
                    tint = AppColors.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDialog(
    template: Template?,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Double, TransactionType, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf(template?.name ?: "") }
    var categoryId by remember { mutableLongStateOf(template?.categoryId ?: Category.SYSTEM_UNKNOWN) }
    var amount by remember { mutableStateOf(template?.amount?.toString() ?: "") }
    var type by remember { mutableStateOf(template?.transactionType ?: TransactionType.EXPENSE) }
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
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Income") }
                    )
                    FilterChip(
                        selected = type == TransactionType.TRANSFER,
                        onClick = { type = TransactionType.TRANSFER },
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
