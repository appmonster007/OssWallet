package com.osscabinet.ossWallet.ui.screens.plannedpayments.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
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
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.management.ManagementSectionHeader
import com.osscabinet.ossWallet.ui.components.navigation.NavigationTab
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.screens.plannedpayments.PlannedPaymentViewModel
import com.osscabinet.ossWallet.ui.screens.templates.TemplateViewModel
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import dev.chrisbanes.haze.hazeSource
import com.osscabinet.ossWallet.ui.theme.ColorOnDark
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentDetailScreen(
    plannedPaymentId: Long,
    viewModel: PlannedPaymentViewModel,
    transactionManager: TransactionManager, // Keep for signature consistency with NavGraph
    templateViewModel: TemplateViewModel? = null,
    accounts: List<Account> = emptyList(),
    categories: List<Category> = emptyList(),
    onEditClick: (Long) -> Unit,
    onConfirmClick: (Long, Long?) -> Unit,
    onBackClick: () -> Unit
) {
    val payments by viewModel.plannedPayments.collectAsStateWithLifecycle()
    val payment =
        remember(plannedPaymentId, payments) { payments.find { it.id == plannedPaymentId } }

    var hasLoadedInitially by remember { mutableStateOf(false) }

    LaunchedEffect(payments) {
        if (payments.isNotEmpty()) {
            hasLoadedInitially = true
        }
        
        // If we previously had the payment (or at least loaded the list) and now it's gone, navigate back.
        if (hasLoadedInitially && payment == null) {
            onBackClick()
        }
    }

    val occurrencesFlow = remember(plannedPaymentId) {
        viewModel.getOccurrencesForPayment(plannedPaymentId)
    }
    val occurrences by occurrencesFlow.collectAsStateWithLifecycle(emptyList())

    if (payment == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    PlannedPaymentDetailContent(
        payment = payment,
        occurrences = occurrences,
        accounts = accounts,
        categories = categories,
        onBackClick = onBackClick,
        onEditClick = { onEditClick(payment.id) },
        onSaveAsTemplateClick = {
            templateViewModel?.addTemplate(
                name = payment.name,
                categoryId = payment.categoryId,
                amount = payment.amount,
                transactionType = payment.transactionType,
                accountId = payment.accountId,
                targetAccountId = payment.targetAccountId,
                targetCurrency = payment.targetCurrency,
                transferRate = payment.transferRate,
                note = payment.note,
                payee = payment.payee,
                location = payment.location,
                paymentMethod = payment.paymentMethod,
                currency = payment.currency,
                tags = payment.tags,
                profileId = payment.profileId
            )
        },
        onConfirmOccurrence = { date -> onConfirmClick(payment.id, date) },
        onSkipOccurrence = { viewModel.quickSkip(payment) }
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
    onSkipOccurrence: () -> Unit
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
                timelineRange = TimelineRange.ThisPeriod(ThisPeriodType.MONTH, excludeFuture = false)
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

    val account = remember(payment.accountId, accounts) { accounts.find { it.id == payment.accountId } }
    val category = remember(payment.categoryId, categories) { categories.find { it.id == payment.categoryId } }
    val targetAccount = remember(payment.targetAccountId, accounts) { accounts.find { it.id == payment.targetAccountId } }

    val pillHeight = AeonSize.D64.value

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
                        .hazeSource(hazeLayers.states[HazeLayer.Overlay])
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
                                                icon = AppIconMap[account?.icon] ?: Icons.Rounded.AccountBalance,
                                                label = "ACCOUNT",
                                                value = account?.name ?: "Unknown",
                                                modifier = Modifier.weight(1f),
                                                iconTint = account?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: AppColors.primary
                                            )

                                            if (payment.transactionType == TransactionType.TRANSFER) {
                                                CompactDetailCell(
                                                    icon = AppIconMap[targetAccount?.icon] ?: Icons.Rounded.AccountBalance,
                                                    label = "TARGET ACCOUNT",
                                                    value = targetAccount?.name ?: "Unknown",
                                                    modifier = Modifier.weight(1f),
                                                    iconTint = targetAccount?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: AppColors.primary
                                                )
                                            } else {
                                                CompactDetailCell(
                                                    icon = AppIconMap[category?.iconName] ?: Icons.Rounded.Category,
                                                    label = "CATEGORY",
                                                    value = category?.name ?: "Unknown",
                                                    modifier = Modifier.weight(1f),
                                                    iconTint = category?.colorHex?.let { Color(it) } ?: AppColors.primary
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
                                            border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.12f))
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
                                                    value = payment.tags.ifEmpty { setOf("None") }.joinToString(" · ")
                                                )

                                                HorizontalDivider(
                                                    thickness = 0.5.dp,
                                                    color = AppColors.divider.copy(alpha = 0.1f)
                                                )

                                                // 4. Payment Status Row (Planned Payment Status)
                                                val statusColor = Color(android.graphics.Color.parseColor(payment.status.colorHex))
                                                DetailMetaRow(
                                                    icon = AppIconMap[payment.status.iconName] ?: Icons.Rounded.CheckCircle,
                                                    label = "Payment Status",
                                                    value = payment.status.displayName,
                                                    valueColor = statusColor
                                                )

                                                // 5. Grid Row: Payment Method & Transaction Status
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                                                ) {
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        DetailMetaRow(
                                                            icon = Icons.Rounded.Payments,
                                                            label = "Method",
                                                            value = payment.paymentMethod?.displayName ?: "Cash"
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

                                                if (payment.transactionType == TransactionType.TRANSFER) {
                                                    DetailMetaRow(
                                                        icon = Icons.Rounded.Repeat,
                                                        label = "Transfer Rate",
                                                        value = "1 : ${payment.transferRate.setScale(3, RoundingMode.HALF_UP).toPlainString()}"
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
                                            val matched_occurrence =
                                                occurrences.find { it.scheduledDate == item.scheduledDate }
                                            val status = matched_occurrence?.status ?: OccurrenceStatus.PENDING
                                            OccurrenceItem(
                                                item = item,
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
                                                payment = payment
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    UnifiedPillNavigation(
                        selectedTab = pagerState.currentPage,
                        onTabSelected = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        tabs = tabs,
                        showFab = false
                    )
                }
            }

            // 2. Top Bar Layer
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
    val accentColor = when (payment.transactionType) {
        TransactionType.INCOME -> AppColors.primary
        TransactionType.TRANSFER -> AppColors.accent
        TransactionType.EXPENSE -> AppColors.error
    }

    AeonExpressiveCard(
        title = payment.name,
        subtitle = payment.transactionType.displayName,
        accentColor = accentColor,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
        ) {
            val amountPrefix = when (payment.transactionType) {
                TransactionType.EXPENSE -> "-"
                TransactionType.INCOME -> "+"
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
    item: UpcomingPaymentItem,
    status: OccurrenceStatus,
    dateFormatter: DateTimeFormatter,
    onConfirm: () -> Unit,
    onSkip: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val today = java.time.LocalDate.now(zone)
    val scheduledDay = Instant.ofEpochMilli(item.scheduledDate).atZone(zone).toLocalDate()
    val canConfirmToday = !scheduledDay.isAfter(today)
    val daysUntil = ChronoUnit.DAYS.between(today, scheduledDay).toInt()

    val iconColor = if (item.isOverdue) AppColors.error else AppColors.secondary
    val dateLabel = dateFormatter.format(Instant.ofEpochMilli(item.scheduledDate))
    val formattedAmount =
        item.plannedPayment.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()

    Surface(
        color = if (item.isOverdue)
            AppColors.errorContainer.copy(alpha = 0.12f)
        else
            AppColors.cardSurface,
        shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
        border = BorderStroke(
            0.5.dp,
            if (item.isOverdue) AppColors.error.copy(alpha = 0.2f)
            else AppColors.divider.copy(alpha = 0.15f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeonTheme.dimensions.spacingMd),
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                        tint = iconColor
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateLabel,
                    style = AppTextStyles.transactionAmount,
                    color = if (item.isOverdue) AppColors.error else AppColors.contentPrimary
                )
                Text(
                    "$formattedAmount ${item.plannedPayment.currency}",
                    style = AppTextStyles.transactionSwipeAction,
                    color = AppColors.contentSecondary.copy(alpha = 0.6f)
                )
            }
            when (status) {
                OccurrenceStatus.PENDING -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)) {
                        Surface(
                            onClick = onSkip,
                            shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                            color = AppColors.contentSecondary.copy(alpha = 0.08f),
                            border = BorderStroke(
                                0.5.dp,
                                AppColors.divider.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.height(AeonTheme.dimensions.spacing2xl)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingMd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Skip", style = AppTextStyles.drawerItemLabel)
                            }
                        }
                        if (canConfirmToday) {
                            Surface(
                                onClick = onConfirm,
                                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                                color = AppColors.primary,
                                modifier = Modifier.height(AeonTheme.dimensions.spacing2xl)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingMd),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                                ) {
                                    Icon(
                                        Icons.Rounded.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = ColorOnDark
                                    )
                                    Text(
                                        "Confirm",
                                        style = AppTextStyles.drawerItemLabel.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ColorOnDark
                                        )
                                    )
                                }
                            }
                        } else {
                            val hintLabel = when {
                                daysUntil == 1 -> "Due tomorrow"
                                daysUntil > 0 -> "Due in $daysUntil days"
                                else -> "Due soon"
                            }
                            Surface(
                                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                                color = AppColors.insetSurface.copy(alpha = 0.4f),
                                modifier = Modifier.height(AeonTheme.dimensions.spacing2xl)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                                ) {
                                    Icon(
                                        Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = AppColors.contentSecondary.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        hintLabel,
                                        style = AppTextStyles.transactionSwipeAction,
                                        color = AppColors.contentSecondary.copy(
                                            alpha = 0.5f
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                OccurrenceStatus.CONFIRMED -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = AppColors.primary,
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                        )
                        Text(
                            "Confirmed",
                            style = AppTextStyles.chipLabel,
                            color = AppColors.primary
                        )
                    }
                }

                OccurrenceStatus.SKIPPED -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                    ) {
                        Icon(
                            Icons.Rounded.Cancel,
                            contentDescription = null,
                            tint = AppColors.contentSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm)
                        )
                        Text(
                            "Skipped",
                            style = AppTextStyles.transactionSwipeAction,
                            color = AppColors.contentSecondary.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PastOccurrenceItem(
    occ: PlannedPaymentOccurrence,
    dateFormatter: DateTimeFormatter,
    payment: PlannedPayment
) {
    val dateLabel = dateFormatter.format(Instant.ofEpochMilli(occ.scheduledDate))
    val formattedAmount = payment.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
    val (statusLabel, statusColor, statusIcon) = when (occ.status) {
        OccurrenceStatus.CONFIRMED -> Triple(
            "Confirmed",
            AppColors.primary,
            Icons.Rounded.CheckCircle
        )

        OccurrenceStatus.SKIPPED -> Triple("Skipped", AppColors.contentSecondary.copy(alpha = 0.5f), Icons.Rounded.Cancel)
        OccurrenceStatus.PENDING -> Triple(
            "Pending",
            AppColors.contentSecondary,
            Icons.Rounded.CalendarToday
        )
    }

    Surface(
        color = AppColors.cardSurface,
        shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
        border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeonTheme.dimensions.spacingMd),
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.1f),
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        statusIcon,
                        contentDescription = null,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
                        tint = statusColor
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateLabel,
                    style = AppTextStyles.transactionAmount
                )
                Text(
                    "$formattedAmount ${payment.currency}",
                    style = AppTextStyles.caption,
                    color = AppColors.contentSecondary.copy(alpha = 0.6f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
            ) {
                Icon(
                    statusIcon,
                    contentDescription = null,
                    tint = statusColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    statusLabel,
                    style = AppTextStyles.chipLabel,
                    color = statusColor
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlannedPaymentDetailPreview() {
    val samplePayment = PlannedPayment(
        id = 1L,
        name = "Netflix Subscription",
        amount = BigDecimal("15.99"),
        currency = "USD",
        transactionType = TransactionType.EXPENSE,
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

    val sampleOccurrences = listOf(
        PlannedPaymentOccurrence(
            id = 101,
            plannedPaymentId = 1L,
            scheduledDate = System.currentTimeMillis() - 28 * 24 * 60 * 60 * 1000L,
            status = OccurrenceStatus.CONFIRMED
        )
    )

    val sampleAccounts = listOf(
        Account(
            id = "acc_1",
            name = "Chase Bank",
            currencyCode = "USD",
            structureType = AccountStructureType.SAVINGS,
            colorHex = "#004D5F",
            icon = "AccountBalance"
        )
    )

    val sampleCategories = listOf(
        Category(
            id = 1L,
            name = "Entertainment",
            iconName = "Movie",
            colorHex = android.graphics.Color.BLUE,
            isDefault = true,
            parentId = null
        )
    )

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
            onSkipOccurrence = {}
        )
    }
}
