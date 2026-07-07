package com.osscabinet.ossWallet.ui.screens.plannedpayments.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import com.osscabinet.ossWallet.domain.model.timeline.ThisPeriodType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarActionPill
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.AppBarIconButton
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.aeonHazeSource
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.management.AeonListItem
import com.osscabinet.ossWallet.ui.components.management.ManagementSectionHeader
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.screens.plannedpayments.PlannedPaymentViewModel
import com.osscabinet.ossWallet.ui.screens.plannedpayments.UndoAction
import com.osscabinet.ossWallet.ui.screens.templates.TemplateViewModel
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ColorOnDark
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


/**
 * A Value Object representing a valid configuration for the Planned Payment Detail screen.
 */
data class PlannedPaymentDetailConfig(
    val plannedPaymentId: Long
)

/**
 * Grouped actions for the Planned Payment Detail screen.
 */
data class PlannedPaymentDetailActions(
    val onEditClick: (Long) -> Unit,
    val onConfirmClick: (Long, Long?) -> Unit,
    val onBackClick: () -> Unit,
    val onTransactionClick: (Long) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentDetailScreen(
    config: PlannedPaymentDetailConfig,
    actions: PlannedPaymentDetailActions,
    viewModel: PlannedPaymentViewModel,
    transactionManager: TransactionManager, // Keep for signature consistency with NavGraph
    templateViewModel: TemplateViewModel? = null,
    accounts: List<Account> = emptyList(),
    categories: List<Category> = emptyList()
) {
    val paymentFlow = remember(config.plannedPaymentId) {
        viewModel.getPaymentById(config.plannedPaymentId)
    }
    val payment by paymentFlow.collectAsStateWithLifecycle(null)
    val lastUndoAction by viewModel.lastUndoAction.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(lastUndoAction) {
        val action = lastUndoAction ?: return@LaunchedEffect
        val message =
            if (action is UndoAction.ConfirmUndo) "Payment confirmed" else "Occurrence skipped"
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoLastAction()
        } else {
            viewModel.clearUndoAction()
        }
    }

    var hasLoadedInitially by remember { mutableStateOf(false) }

    LaunchedEffect(payment) {
        if (payment != null) {
            hasLoadedInitially = true
        }
        if (hasLoadedInitially && payment == null) {
            actions.onBackClick()
        }
    }

    val occurrencesFlow = remember(config.plannedPaymentId) {
        viewModel.getOccurrencesForPayment(config.plannedPaymentId)
    }
    val occurrences by occurrencesFlow.collectAsStateWithLifecycle(emptyList())

    val p = payment
    if (p == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    PlannedPaymentDetailContent(
        payment = p,
        occurrences = occurrences,
        accounts = accounts,
        categories = categories,
        onBackClick = actions.onBackClick,
        onEditClick = { actions.onEditClick(p.id) },
        onSaveAsTemplateClick = {
            templateViewModel?.addTemplate(
                name = p.name,
                categoryId = p.categoryId,
                amount = p.amount,
                entryMode = p.entryMode,
                accountId = p.accountId,
                targetAccountId = p.targetAccountId,
                targetCurrency = p.targetCurrency,
                transferRate = p.transferRate,
                note = p.note,
                payee = p.payee,
                location = p.location,
                paymentMethod = p.paymentMethod,
                currency = p.currency,
                tags = p.tags,
                profileId = p.profileId
            )
        },
        onConfirmOccurrence = { date -> actions.onConfirmClick(p.id, date) },
        onSkipOccurrence = { viewModel.quickSkip(p) },
        onTransactionClick = actions.onTransactionClick,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentDetailContent(
    payment: PlannedPayment,
    occurrences: List<PlannedPaymentOccurrence>,
    accounts: List<Account>,
    categories: List<Category>,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onSaveAsTemplateClick: () -> Unit,
    onConfirmOccurrence: (Long?) -> Unit,
    onSkipOccurrence: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val scope = rememberCoroutineScope()
    val scheduleSummary = remember(payment) {
        PlannedPaymentService.buildScheduleSummary(
            payment.schedule.startDate,
            payment.schedule.recurrenceRule
        )
    }
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val tabs = listOf(
        NavigationTab(
            icon = Icons.Rounded.Info,
            selectedIcon = Icons.Rounded.Info,
            label = "Overview",
            contentDescription = "Overview tab"
        ),
        NavigationTab(
            icon = Icons.Rounded.History,
            selectedIcon = Icons.Rounded.History,
            label = "History",
            contentDescription = "History tab"
        )
    )

    val now = remember { System.currentTimeMillis() }
    val dateFormatter =
        remember { DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault()) }

    var filter by remember {
        mutableStateOf(
            TransactionFilter(
                timelineRange = TimelineRange.ThisPeriod(
                    ThisPeriodType.MONTH,
                    excludeFuture = false
                )
            )
        )
    }

    val upcomingItems = remember(payment, now, filter.timelineRange) {
        val (_, endTime) = TimelineService.getDateRange(filter.timelineRange)
        val effectiveEnd =
            endTime ?: (now + 365L * 24 * 60 * 60 * 1000) // Default to 1 year if All Time
        PlannedPaymentService.generateUntil(payment, now, effectiveEnd)
    }
    val pastOccurrences = remember(occurrences, now) {
        occurrences.filter { it.scheduledDate < now }.sortedByDescending { it.scheduledDate }
    }

    val account =
        remember(payment.accountId, accounts) { accounts.find { it.id == payment.accountId } }
    val category =
        remember(payment.categoryId, categories) { categories.find { it.id == payment.categoryId } }
    val targetAccount = remember(
        payment.targetAccountId,
        accounts
    ) { accounts.find { it.id == payment.targetAccountId } }

    val occurrencesMap = remember(occurrences) {
        occurrences.associateBy { it.scheduledDate }
    }

    val defaultColor = AppColors.primary
    val accountColor = remember(account?.colorHex, defaultColor) {
        account?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: defaultColor
    }

    val targetAccountColor = remember(targetAccount?.colorHex, defaultColor) {
        targetAccount?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
            ?: defaultColor
    }

    val categoryColor = remember(category?.colorHex, defaultColor) {
        category?.colorHex?.let { Color(it) } ?: defaultColor
    }

    val statusColor = remember(payment.status.colorHex) {
        Color(android.graphics.Color.parseColor(payment.status.colorHex))
    }

    val pillHeight = AeonSize.D64.value
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    AeonHazeProvider { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Content Layer
            Scaffold(
                containerColor = Color.Transparent
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .aeonHazeSource(hazeLayers.states[HazeLayer.Overlay])
                        .padding(bottom = paddingValues.calculateBottomPadding())
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.Top
                    ) { page ->
                        when (page) {
                            0 -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = AeonTheme.dimensions.screenPadding,
                                        end = AeonTheme.dimensions.screenPadding,
                                        top = AeonTheme.dimensions.screenPadding + pillHeight,
                                        bottom = scrollBottomPadding()
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
                                ) {
                                    // IMPACT: Hero Card (Compact Read-only)
                                    item {
                                        PlannedPaymentHero(payment = payment)
                                    }

                                    // CONTEXT: Grid Layout (Compact Read-only)
                                    item {
                                        Row(horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                                            CompactDetailCell(
                                                icon = AppIconMap[account?.icon]
                                                    ?: Icons.Rounded.AccountBalance,
                                                label = "ACCOUNT",
                                                value = account?.name ?: "Unknown",
                                                modifier = Modifier.weight(1f),
                                                iconTint = accountColor
                                            )

                                            if (payment.entryMode == EntryMode.TRANSFER) {
                                                CompactDetailCell(
                                                    icon = AppIconMap[targetAccount?.icon]
                                                        ?: Icons.Rounded.AccountBalance,
                                                    label = "TARGET ACCOUNT",
                                                    value = targetAccount?.name ?: "Unknown",
                                                    modifier = Modifier.weight(1f),
                                                    iconTint = targetAccountColor
                                                )
                                            } else {
                                                CompactDetailCell(
                                                    icon = AppIconMap[category?.iconName]
                                                        ?: Icons.Rounded.Category,
                                                    label = "CATEGORY",
                                                    value = category?.name ?: "Unknown",
                                                    modifier = Modifier.weight(1f),
                                                    iconTint = categoryColor
                                                )
                                            }
                                        }
                                    }

                                    // DETAIL: Metadata Card
                                    item {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
                                            color = AppColors.cardSurface,
                                            border = BorderStroke(
                                                0.5.dp,
                                                AppColors.divider.copy(alpha = 0.12f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
                                                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
                                            ) {
                                                // 1. Schedule
                                                DetailMetaRow(
                                                    icon = Icons.Rounded.CalendarToday,
                                                    label = "Schedule",
                                                    value = scheduleSummary,
                                                    singleLine = false
                                                )

                                                // 2. Template
                                                DetailMetaRow(
                                                    icon = Icons.Rounded.Bookmark,
                                                    label = "Linked Template",
                                                    value = if (payment.templateId != null) "Linked Template" else "None"
                                                )

                                                // 3. Note
                                                DetailMetaRow(
                                                    icon = Icons.AutoMirrored.Rounded.Notes,
                                                    label = "Note",
                                                    value = payment.note ?: "No note",
                                                    singleLine = false
                                                )

                                                // 4. Payee
                                                DetailMetaRow(
                                                    icon = Icons.Rounded.Person,
                                                    label = "Payee",
                                                    value = payment.payee ?: "No payee"
                                                )

                                                // 3. Tags (Always shown)
                                                DetailMetaRow(
                                                    icon = Icons.Rounded.LocalOffer,
                                                    label = "Tags",
                                                    value = payment.tags.ifEmpty { setOf("None") }
                                                        .joinToString(" · ")
                                                )

                                                HorizontalDivider(
                                                    thickness = 0.5.dp,
                                                    color = AppColors.divider.copy(alpha = 0.1f)
                                                )

                                                // 4. Payment Status Row (Planned Payment Status)
                                                DetailMetaRow(
                                                    icon = AppIconMap[payment.status.iconName]
                                                        ?: Icons.Rounded.CheckCircle,
                                                    label = "Payment Status",
                                                    value = payment.status.displayName,
                                                    valueColor = statusColor
                                                )

                                                // 5. Grid Row: Payment Method & Transaction Status
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(
                                                        AeonTheme.dimensions.spacingMd
                                                    )
                                                ) {
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        DetailMetaRow(
                                                            icon = Icons.Rounded.Payments,
                                                            label = "Method",
                                                            value = payment.paymentMethod?.displayName
                                                                ?: "Cash"
                                                        )
                                                    }
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        DetailMetaRow(
                                                            icon = Icons.Rounded.CheckCircle,
                                                            label = "Tx Status",
                                                            value = payment.transactionStatus.name
                                                        )
                                                    }
                                                }

                                                if (payment.autoConfirm) {
                                                    DetailMetaRow(
                                                        icon = Icons.Rounded.AutoFixHigh,
                                                        label = "Automation",
                                                        value = "Auto-confirm enabled",
                                                        valueColor = AppColors.primary
                                                    )
                                                }

                                                if (payment.entryMode == EntryMode.TRANSFER) {
                                                    DetailMetaRow(
                                                        icon = Icons.Rounded.Repeat,
                                                        label = "Transfer Rate",
                                                        value = "1 : ${
                                                            payment.transferRate.setScale(
                                                                3,
                                                                RoundingMode.HALF_UP
                                                            ).toPlainString()
                                                        }"
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Upcoming occurrences
                                    if (upcomingItems.isNotEmpty()) {
                                        item {
                                            ManagementSectionHeader(
                                                title = "UPCOMING",
                                                count = upcomingItems.size,
                                                sectionColor = AppColors.primary
                                            )
                                        }
                                        items(
                                            upcomingItems,
                                            key = { "${it.plannedPayment.id}_${it.scheduledDate}" }) { item ->
                                            val matchedOccurrence =
                                                occurrencesMap[item.scheduledDate]
                                            val status = matchedOccurrence?.status
                                                ?: OccurrenceStatus.PENDING
                                            OccurrenceItem(
                                                scheduledDate = item.scheduledDate,
                                                amount = item.plannedPayment.amount,
                                                currency = item.plannedPayment.currency,
                                                isOverdue = item.isOverdue,
                                                status = status,
                                                dateFormatter = dateFormatter,
                                                onConfirm = { onConfirmOccurrence(item.scheduledDate) },
                                                onSkip = onSkipOccurrence
                                            )
                                        }
                                    }

                                    item { Spacer(modifier = Modifier.height(scrollBottomPadding())) }
                                }
                            }

                            1 -> {
                                // History Tab: Past occurrences
                                if (pastOccurrences.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No history yet",
                                            style = AppTextStyles.transactionTitle,
                                            color = AppColors.contentSecondary.copy(alpha = 0.6f)
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(
                                            start = AeonTheme.dimensions.screenPadding,
                                            end = AeonTheme.dimensions.screenPadding,
                                            top = AeonTheme.dimensions.screenPadding + pillHeight,
                                            bottom = scrollBottomPadding()
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
                                    ) {
                                        item {
                                            ManagementSectionHeader(
                                                title = "HISTORY",
                                                count = pastOccurrences.size,
                                                sectionColor = AppColors.contentSecondary.copy(alpha = 0.5f)
                                            )
                                        }
                                        items(pastOccurrences, key = { it.id }) { occ ->
                                            PastOccurrenceItem(
                                                occ = occ,
                                                dateFormatter = dateFormatter,
                                                amount = payment.amount,
                                                currency = payment.currency,
                                                onTransactionClick = onTransactionClick
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }

            // 2. Pill Navigation Layer (sibling of Scaffold — must be outside hazeSource content)
            UnifiedPillNavigation(
                selectedTab = pagerState.currentPage,
                onTabSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                tabs = tabs,
                showFab = false
            )

            // 3. Snackbar Layer (above navigation pill)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = pillHeight + navigationBarHeight + 8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                SnackbarHost(hostState = snackbarHostState)
            }

            // 4. Top Bar Layer
            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = payment.name,
                            onBackClick = onBackClick,
                            tabs = tabs.map { it.label },
                            selectedTabIndex = pagerState.currentPage,
                            onTabSelected = { index ->
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                        )
                    }
                ),
                right = AeonTopBarSection(
                    content = {
                        AeonTopBarActionPill {
                            when (pagerState.currentPage) {
                                0 -> {
                                    AppBarIconButton(
                                        icon = Icons.Rounded.Schedule,
                                        onClick = onSaveAsTemplateClick
                                    )
                                    AppBarIconButton(
                                        icon = Icons.Rounded.Edit,
                                        onClick = onEditClick
                                    )
                                }

                                1 -> {
                                    // No extra actions for history tab yet
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
private fun PlannedPaymentHero(
    payment: PlannedPayment,
    modifier: Modifier = Modifier
) {
    val accentColor = when (payment.entryMode) {
        EntryMode.INCOME -> AppColors.primary
        EntryMode.TRANSFER -> AppColors.accent
        EntryMode.EXPENSE -> AppColors.error
    }

    AeonExpressiveCard(
        title = payment.name,
        subtitle = payment.entryMode.displayName,
        accentColor = accentColor,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
        ) {
            val amountPrefix = when (payment.entryMode) {
                EntryMode.EXPENSE -> "-"
                EntryMode.INCOME -> "+"
                else -> ""
            }
            Text(
                amountPrefix + payment.amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                style = AppTextStyles.heroAmountDisplay,
                color = accentColor
            )
            Text(
                payment.currency,
                style = AppTextStyles.screenSubTitle,
                color = accentColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = AeonTheme.dimensions.spacingXs)
            )
        }
    }
}

@Composable
private fun CompactDetailCell(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    iconTint: Color = AppColors.primary
) {
    Surface(
        shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
        color = AppColors.cardSurface,
        border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.12f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(AeonTheme.dimensions.spacingMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.1f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = iconTint
                    )
                }
            }
            Column {
                Text(
                    label.uppercase(),
                    style = AppTextStyles.caption.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = AppColors.contentSecondary.copy(alpha = 0.5f)
                )
                Text(
                    value,
                    style = AppTextStyles.transactionTitle,
                    color = AppColors.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DetailMetaRow(
    icon: ImageVector,
    label: String,
    value: String,
    singleLine: Boolean = true,
    valueColor: Color = AppColors.contentPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = AppColors.contentSecondary.copy(alpha = 0.5f)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label.uppercase(),
                style = AppTextStyles.caption.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = AppColors.contentSecondary.copy(alpha = 0.45f)
            )
            Text(
                value,
                style = AppTextStyles.chartTooltipValue,
                color = valueColor,
                maxLines = if (singleLine) 1 else 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun OccurrenceItem(
    scheduledDate: Long,
    amount: BigDecimal,
    currency: String,
    isOverdue: Boolean,
    status: OccurrenceStatus,
    dateFormatter: DateTimeFormatter,
    onConfirm: () -> Unit,
    onSkip: () -> Unit
) {
    val dateLabel = remember(scheduledDate, dateFormatter) {
        dateFormatter.format(Instant.ofEpochMilli(scheduledDate))
    }
    val formattedAmount = remember(amount, currency) {
        amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
    }

    val zone = remember { ZoneId.systemDefault() }
    val daysUntil = remember(scheduledDate, zone) {
        val today = java.time.LocalDate.now(zone)
        val scheduledDay = Instant.ofEpochMilli(scheduledDate).atZone(zone).toLocalDate()
        ChronoUnit.DAYS.between(today, scheduledDay).toInt()
    }

    val hintLabel = remember(status, isOverdue, daysUntil) {
        if (status == OccurrenceStatus.PENDING) {
            when {
                isOverdue -> "Overdue"
                daysUntil == 0 -> "Due today"
                daysUntil == 1 -> "Due tomorrow"
                daysUntil > 1 -> "Due in $daysUntil days"
                else -> null
            }
        } else null
    }

    AeonListItem(
        title = dateLabel,
        subtitle = "$formattedAmount $currency${if (hintLabel != null) " · $hintLabel" else ""}",
        icon = Icons.Rounded.CalendarToday,
        iconTint = if (isOverdue) AppColors.error else AppColors.secondary,
        isWarning = isOverdue,
        trailingContent = {
            if (status == OccurrenceStatus.PENDING) {
                Row(horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)) {
                    Surface(
                        onClick = onSkip,
                        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                        color = AppColors.contentSecondary.copy(alpha = 0.08f),
                        border = BorderStroke(
                            0.5.dp,
                            AppColors.divider.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.size(AeonTheme.dimensions.spacing2xl)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.SkipNext,
                                contentDescription = "Skip",
                                modifier = Modifier.size(14.dp),
                                tint = AppColors.contentSecondary.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Surface(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                        color = AppColors.primary,
                        modifier = Modifier.size(AeonTheme.dimensions.spacing2xl)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = "Confirm",
                                modifier = Modifier.size(14.dp),
                                tint = ColorOnDark
                            )
                        }
                    }
                }
            } else {
                val (statusLabel, statusColor, statusIcon) = when (status) {
                    OccurrenceStatus.CONFIRMED -> Triple(
                        "Confirmed",
                        AppColors.primary,
                        Icons.Rounded.CheckCircle
                    )

                    OccurrenceStatus.SKIPPED -> Triple(
                        "Skipped",
                        AppColors.contentSecondary.copy(alpha = 0.4f),
                        Icons.Rounded.Cancel
                    )

                    else -> Triple("Pending", AppColors.secondary, Icons.Rounded.Schedule)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                ) {
                    Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(14.dp))
                    Text(statusLabel, style = AppTextStyles.chipLabel, color = statusColor)
                }
            }
        }
    )
}

@Composable
private fun PastOccurrenceItem(
    occ: PlannedPaymentOccurrence,
    dateFormatter: DateTimeFormatter,
    amount: BigDecimal,
    currency: String,
    onTransactionClick: (Long) -> Unit
) {
    val dateLabel = remember(occ.scheduledDate, dateFormatter) {
        dateFormatter.format(Instant.ofEpochMilli(occ.scheduledDate))
    }
    val formattedAmount = remember(amount, currency) {
        amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
    }
    val (statusLabel, statusColor, statusIcon) = when (occ.status) {
        OccurrenceStatus.CONFIRMED -> Triple(
            "Confirmed",
            AppColors.primary,
            Icons.Rounded.CheckCircle
        )

        OccurrenceStatus.SKIPPED -> Triple(
            "Skipped",
            AppColors.contentSecondary.copy(alpha = 0.5f),
            Icons.Rounded.Cancel
        )

        OccurrenceStatus.PENDING -> Triple(
            "Pending",
            AppColors.contentSecondary,
            Icons.Rounded.CalendarToday
        )
    }

    AeonListItem(
        title = dateLabel,
        subtitle = "$formattedAmount $currency · $statusLabel",
        icon = statusIcon,
        iconTint = statusColor,
        onClick = occ.generatedTransactionId?.let { { onTransactionClick(it) } },
        trailingContent = {
            if (occ.generatedTransactionId != null) {
                Icon(
                    Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = "View Transaction",
                    tint = AppColors.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun PlannedPaymentDetailPreview() {
    val samplePayment = remember {
        PlannedPayment(
            id = 1L,
            name = "Netflix Subscription",
            amount = BigDecimal("15.99"),
            currency = "USD",
            entryMode = EntryMode.EXPENSE,
            categoryId = 1L,
            accountId = "acc_1",
            payee = "Netflix",
            note = "Monthly entertainment plan",
            schedule = PaymentSchedule(
                recurrenceRule = RecurrenceRule(RecurrenceType.MONTHLY),
                startDate = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L,
                nextDueDate = System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000L
            ),
            paymentMethod = PaymentMethod.CREDIT_CARD,
            profileId = 1,
            transactionStatus = TransactionStatus.CLEARED
        )
    }

    val sampleOccurrences = remember {
        listOf(
            PlannedPaymentOccurrence(
                id = 101,
                plannedPaymentId = 1L,
                scheduledDate = System.currentTimeMillis() - 28 * 24 * 60 * 60 * 1000L,
                status = OccurrenceStatus.CONFIRMED
            )
        )
    }

    val sampleAccounts = remember {
        listOf(
            Account(
                id = "acc_1",
                name = "Chase Bank",
                currencyCode = "USD",
                structureType = AccountStructureType.SAVINGS,
                colorHex = "#004D5F",
                icon = "AccountBalance"
            )
        )
    }

    val sampleCategories = remember {
        listOf(
            Category(
                id = 1L,
                name = "Entertainment",
                iconName = "Movie",
                colorHex = android.graphics.Color.BLUE,
                isDefault = true,
                parentId = null
            )
        )
    }

    OssWalletTheme {
        PlannedPaymentDetailContent(
            payment = samplePayment,
            occurrences = sampleOccurrences,
            accounts = sampleAccounts,
            categories = sampleCategories,
            onBackClick = {},
            onEditClick = {},
            onSaveAsTemplateClick = {},
            onConfirmOccurrence = {},
            onSkipOccurrence = {},
            onTransactionClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OccurrenceItemPreview() {
    val samplePayment = remember {
        PlannedPayment(
            id = 1L,
            name = "Netflix Subscription",
            amount = BigDecimal("15.99"),
            currency = "USD",
            entryMode = EntryMode.EXPENSE,
            categoryId = 1L,
            accountId = "acc_1",
            payee = "Netflix",
            note = "Monthly entertainment plan",
            schedule = PaymentSchedule(
                recurrenceRule = RecurrenceRule(RecurrenceType.MONTHLY),
                startDate = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L,
                nextDueDate = System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000L
            ),
            paymentMethod = PaymentMethod.CREDIT_CARD,
            profileId = 1,
            transactionStatus = TransactionStatus.CLEARED
        )
    }

    val sampleItem = remember(samplePayment) {
        UpcomingPaymentItem(
            plannedPayment = samplePayment,
            scheduledDate = System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000L,
            isOverdue = false
        )
    }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault())
    }

    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Pending")
            OccurrenceItem(
                scheduledDate = sampleItem.scheduledDate,
                amount = sampleItem.plannedPayment.amount,
                currency = sampleItem.plannedPayment.currency,
                isOverdue = false,
                status = OccurrenceStatus.PENDING,
                dateFormatter = dateFormatter,
                onConfirm = {},
                onSkip = {}
            )

            Text("Overdue")
            OccurrenceItem(
                scheduledDate = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000L,
                amount = sampleItem.plannedPayment.amount,
                currency = sampleItem.plannedPayment.currency,
                isOverdue = true,
                status = OccurrenceStatus.PENDING,
                dateFormatter = dateFormatter,
                onConfirm = {},
                onSkip = {}
            )

            Text("Confirmed")
            OccurrenceItem(
                scheduledDate = sampleItem.scheduledDate,
                amount = sampleItem.plannedPayment.amount,
                currency = sampleItem.plannedPayment.currency,
                isOverdue = false,
                status = OccurrenceStatus.CONFIRMED,
                dateFormatter = dateFormatter,
                onConfirm = {},
                onSkip = {}
            )
        }
    }
}

