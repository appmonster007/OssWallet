package com.osscabinet.ossWallet.ui.screens.plannedpayments.list

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.PaymentSchedule
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.RecurrenceType
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonTopAppBar
import com.osscabinet.ossWallet.ui.components.AeonTopBarNavTitlePill
import com.osscabinet.ossWallet.ui.components.AeonTopBarSection
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.ContourBackground
import com.osscabinet.ossWallet.ui.components.management.ManagementListItem
import com.osscabinet.ossWallet.ui.components.management.ManagementSectionHeader
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.screens.plannedpayments.PlannedPaymentViewModel
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import dev.chrisbanes.haze.hazeSource
import com.osscabinet.ossWallet.ui.util.TimeUtils.formatRelativeDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeablePaymentItem(
    payment: PlannedPayment,
    now: Long,
    isOverdue: Boolean,
    scheduledDate: Long? = null,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (onConfirm != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm()
                    }
                    false
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    if (onSkip != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSkip()
                    }
                    false
                }

                else -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = onConfirm != null,
        enableDismissFromEndToStart = onSkip != null,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isStartToEnd = direction == SwipeToDismissBoxValue.StartToEnd
            val bgColor by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> AppColors.primary.copy(alpha = 0.15f)
                    SwipeToDismissBoxValue.EndToStart -> AppColors.error.copy(alpha = 0.15f)
                    else -> AppColors.cardSurface
                },
                label = "swipeBg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius))
                    .background(bgColor)
                    .padding(horizontal = AeonTheme.dimensions.spacingXl),
                contentAlignment = if (isStartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Confirm",
                        tint = AppColors.primary,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)
                    )

                    SwipeToDismissBoxValue.EndToStart -> Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Skip",
                        tint = AppColors.error,
                        modifier = Modifier.size(AeonTheme.dimensions.iconSizeLg)
                    )

                    else -> {}
                }
            }
        }
    ) {
        PlannedPaymentListItem(
            payment = payment,
            now = now,
            isOverdue = isOverdue,
            scheduledDate = scheduledDate,
            onTap = onTap,
            onEdit = onEdit,
            onDelete = onDelete,
            onPause = onPause,
            onResume = onResume
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentListScreen(
    onBackClick: () -> Unit,
    viewModel: PlannedPaymentViewModel,
    transactionManager: TransactionManager,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDetailClick: (Long) -> Unit,
    onConfirmClick: ((Long, Long?) -> Unit)? = null
) {
    val payments by viewModel.plannedPayments.collectAsStateWithLifecycle()
    val profileId by viewModel.currentProfileId.collectAsStateWithLifecycle()

    PlannedPaymentListContent(
        payments = payments,
        profileId = profileId,
        onBackClick = onBackClick,
        onAddClick = onAddClick,
        onEditClick = onEditClick,
        onDetailClick = onDetailClick,
        onDelete = { id -> viewModel.delete(id, false) },
        onPause = { viewModel.pause(it) },
        onResume = { viewModel.resume(it) },
        onConfirm = { payment, date ->
            if (onConfirmClick != null) {
                onConfirmClick(payment.id, date)
            } else {
                viewModel.quickConfirm(payment, profileId)
            }
        },
        onSkip = { viewModel.quickSkip(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentListContent(
    payments: List<PlannedPayment>,
    profileId: Int,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDetailClick: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onPause: (PlannedPayment) -> Unit,
    onResume: (PlannedPayment) -> Unit,
    onConfirm: (PlannedPayment, Long?) -> Unit,
    onSkip: (PlannedPayment) -> Unit
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val now = remember { System.currentTimeMillis() }

    val windowEnd = remember(now) { now + 30L * 24 * 60 * 60 * 1000 }
    val overdue = remember(payments, now) { PlannedPaymentService.getOverdue(payments, now) }
    val upcoming = remember(payments, now) {
        PlannedPaymentService.generateUpcoming(
            payments.filter { it.status == PlannedPaymentStatus.ACTIVE },
            now,
            30
        )
    }
    val scheduled = remember(payments, now, windowEnd) {
        val upcomingIds = upcoming.map { it.plannedPayment.id }.toSet()
        val overdueIds = overdue.map { it.id }.toSet()
        payments.filter { p ->
            p.status == PlannedPaymentStatus.ACTIVE &&
                    p.schedule.nextDueDate > windowEnd &&
                    p.id !in upcomingIds &&
                    p.id !in overdueIds
        }
    }
    val paused = remember(payments) { payments.filter { it.status == PlannedPaymentStatus.PAUSED } }

    AeonHazeProvider { hazeLayers ->
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.screenBackground)
                        .hazeSource(hazeLayers.states[HazeLayer.Overlay])
                ) {
                    ContourBackground()

                    if (payments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(top = AeonSize.D64.value),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AppColors.primary.copy(alpha = 0.08f),
                                    modifier = Modifier.size(AeonSize.D64.value) // Using AeonSize
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.Repeat,
                                            contentDescription = null,
                                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl),
                                            tint = AppColors.primary
                                        )
                                    }
                                }
                                Text(
                                    "No Planned Payments",
                                    style = AppTextStyles.calculatorKey,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Tap 'New Payment' to schedule\nrecurring bills and subscriptions",
                                    style = AppTextStyles.transactionTitle,
                                    color = AppColors.contentSecondary.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
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
                                bottom = innerPadding.calculateBottomPadding() + scrollBottomPadding()
                            ),
                            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
                        ) {
                            if (overdue.isNotEmpty()) {
                                item {
                                    ManagementSectionHeader(
                                        title = "OVERDUE",
                                        count = overdue.size,
                                        sectionColor = AppColors.error
                                    )
                                }
                                items(overdue, key = { it.id }) { payment ->
                                    SwipeablePaymentItem(
                                        payment = payment,
                                        now = now,
                                        isOverdue = true,
                                        onTap = { onDetailClick(payment.id) },
                                        onEdit = { onEditClick(payment.id) },
                                        onDelete = { onDelete(payment.id) },
                                        onPause = { onPause(payment) },
                                        onResume = { onResume(payment) },
                                        onConfirm = { onConfirm(payment, payment.schedule.nextDueDate) },
                                        onSkip = { onSkip(payment) }
                                    )
                                }
                            }

                            if (upcoming.isNotEmpty()) {
                                val upcomingNonOverdue = upcoming.filter { !it.isOverdue }
                                    .distinctBy { it.plannedPayment.id }
                                if (upcomingNonOverdue.isNotEmpty()) {
                                    item {
                                        ManagementSectionHeader(
                                            title = "UPCOMING",
                                            count = upcomingNonOverdue.size,
                                            sectionColor = AppColors.primary
                                        )
                                    }
                                    items(
                                        upcomingNonOverdue,
                                        key = { "${it.plannedPayment.id}_${it.scheduledDate}" }
                                    ) { item ->
                                        SwipeablePaymentItem(
                                            payment = item.plannedPayment,
                                            now = now,
                                            isOverdue = false,
                                            scheduledDate = item.scheduledDate,
                                            onTap = { onDetailClick(item.plannedPayment.id) },
                                            onEdit = { onEditClick(item.plannedPayment.id) },
                                            onDelete = { onDelete(item.plannedPayment.id) },
                                            onPause = { onPause(item.plannedPayment) },
                                            onResume = { onResume(item.plannedPayment) },
                                            onConfirm = { onConfirm(item.plannedPayment, item.scheduledDate) },
                                            onSkip = { onSkip(item.plannedPayment) }
                                        )
                                    }
                                }
                            }

                            if (scheduled.isNotEmpty()) {
                                item {
                                    ManagementSectionHeader(
                                        title = "SCHEDULED",
                                        count = scheduled.size,
                                        sectionColor = AppColors.secondary
                                    )
                                }
                                items(scheduled, key = { it.id }) { payment ->
                                    SwipeablePaymentItem(
                                        payment = payment,
                                        now = now,
                                        isOverdue = false,
                                        scheduledDate = payment.schedule.nextDueDate,
                                        onTap = { onDetailClick(payment.id) },
                                        onEdit = { onEditClick(payment.id) },
                                        onDelete = { onDelete(payment.id) },
                                        onPause = { onPause(payment) },
                                        onResume = { onResume(payment) },
                                        onConfirm = { onConfirm(payment, payment.schedule.nextDueDate) },
                                        onSkip = { onSkip(payment) }
                                    )
                                }
                            }

                            if (paused.isNotEmpty()) {
                                item {
                                    ManagementSectionHeader(
                                        title = "PAUSED",
                                        count = paused.size,
                                        sectionColor = AppColors.contentSecondary.copy(alpha = 0.4f)
                                    )
                                }
                                items(paused, key = { it.id }) { payment ->
                                    SwipeablePaymentItem(
                                        payment = payment,
                                        now = now,
                                        isOverdue = false,
                                        onTap = { onDetailClick(payment.id) },
                                        onEdit = { onEditClick(payment.id) },
                                        onDelete = { onDelete(payment.id) },
                                        onPause = { onPause(payment) },
                                        onResume = { onResume(payment) }
                                    )
                                }
                            }

                            item { Spacer(modifier = Modifier.height(scrollBottomPadding())) }
                        }
                    }
                }
            }

            UnifiedPillNavigation(
                selectedTab = 0,
                onTabSelected = {},
                tabs = emptyList(),
                fabPaddingBottom = 16.dp + navigationBarHeight,
                onFabClick = onAddClick,
                fabIcon = Icons.Rounded.Repeat,
                showFab = true
            )

            AeonTopAppBar(
                left = AeonTopBarSection(
                    content = {
                        AeonTopBarNavTitlePill(
                            title = "Planned Payments",
                            onBackClick = onBackClick
                        )
                    }
                )
            )
        }
    }
}

@Composable
private fun PlannedPaymentListItem(
    payment: PlannedPayment,
    now: Long,
    isOverdue: Boolean,
    scheduledDate: Long? = null,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    val dateToShow = scheduledDate ?: payment.schedule.nextDueDate
    val relativeDateLabel = remember(dateToShow, now) { formatRelativeDate(dateToShow, now) }
    val isPaused = payment.status == PlannedPaymentStatus.PAUSED

    val typeColor = when {
        isPaused -> AppColors.contentSecondary.copy(alpha = 0.5f)
        isOverdue -> AppColors.error
        payment.transactionType == TransactionType.INCOME -> AppColors.primary
        payment.transactionType == TransactionType.TRANSFER -> AccentBlue
        else -> AppColors.error
    }

    val recurrenceLabel =
        PlannedPaymentService.formatRecurrenceHuman(payment.schedule.recurrenceRule)
    val formattedAmount = remember(payment.amount, payment.currency) {
        com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAuto(payment.amount, payment.currency)
    }

    val statusIcon = AppIconMap[payment.status.iconName] ?: Icons.Rounded.Repeat
    val statusColor = when {
        isPaused -> AppColors.contentSecondary.copy(alpha = 0.4f)
        isOverdue -> AppColors.error
        else -> Color(android.graphics.Color.parseColor(payment.status.colorHex))
    }

    ManagementListItem(
        onClick = onTap,
        icon = statusIcon,
        iconTint = statusColor,
        title = payment.name,
        subtitle = recurrenceLabel,
        isWarning = isOverdue,
        modifier = Modifier.graphicsLayer { alpha = if (isPaused) 0.6f else 1f },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    formattedAmount,
                    style = AppTextStyles.listItemTitle.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    ),
                    color = typeColor
                )
                Text(
                    relativeDateLabel.uppercase(),
                    style = AppTextStyles.transactionStatusMicro,
                    color = when {
                        isPaused -> AppColors.contentSecondary.copy(alpha = 0.5f)
                        isOverdue -> AppColors.error
                        else -> AppColors.primary
                    }
                )
            }
        },
        menuItems = { onDismiss ->
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = { onDismiss(); onEdit() }
            )
            if (payment.status == PlannedPaymentStatus.ACTIVE) {
                DropdownMenuItem(
                    text = { Text("Pause") },
                    onClick = { onDismiss(); onPause() }
                )
            } else if (payment.status == PlannedPaymentStatus.PAUSED) {
                DropdownMenuItem(
                    text = { Text("Resume") },
                    onClick = { onDismiss(); onResume() }
                )
            }
            DropdownMenuItem(
                text = { Text("Delete", color = AppColors.error) },
                onClick = { onDismiss(); onDelete() }
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun PlannedPaymentListPreview() {
    val now = System.currentTimeMillis()
    val mockPayments = listOf(
        PlannedPayment(
            id = 1,
            name = "Rent",
            amount = BigDecimal("1500.00"),
            currency = "USD",
            transactionType = TransactionType.EXPENSE,
            categoryId = 1,
            status = PlannedPaymentStatus.ACTIVE,
            schedule = PaymentSchedule(
                startDate = now - 30L * 24 * 60 * 60 * 1000,
                nextDueDate = now - 2L * 24 * 60 * 60 * 1000, // Overdue
                recurrenceRule = RecurrenceRule(RecurrenceType.MONTHLY, 1)
            )
        ),
        PlannedPayment(
            id = 2,
            name = "Internet",
            amount = BigDecimal("60.00"),
            currency = "USD",
            transactionType = TransactionType.EXPENSE,
            categoryId = 2,
            status = PlannedPaymentStatus.ACTIVE,
            schedule = PaymentSchedule(
                startDate = now,
                nextDueDate = now + 2L * 24 * 60 * 60 * 1000, // Upcoming
                recurrenceRule = RecurrenceRule(RecurrenceType.MONTHLY, 1)
            )
        ),
        PlannedPayment(
            id = 3,
            name = "Salary",
            amount = BigDecimal("5000.00"),
            currency = "USD",
            transactionType = TransactionType.INCOME,
            categoryId = 3,
            status = PlannedPaymentStatus.ACTIVE,
            schedule = PaymentSchedule(
                startDate = now,
                nextDueDate = now + 15L * 24 * 60 * 60 * 1000, // Scheduled
                recurrenceRule = RecurrenceRule(RecurrenceType.MONTHLY, 1)
            )
        ),
        PlannedPayment(
            id = 4,
            name = "Netflix",
            amount = BigDecimal("15.99"),
            currency = "USD",
            transactionType = TransactionType.EXPENSE,
            categoryId = 4,
            status = PlannedPaymentStatus.PAUSED,
            schedule = PaymentSchedule(
                startDate = now,
                nextDueDate = now + 5L * 24 * 60 * 60 * 1000,
                recurrenceRule = RecurrenceRule(RecurrenceType.MONTHLY, 1)
            )
        )
    )

    OssWalletTheme {
        PlannedPaymentListContent(
            payments = mockPayments,
            profileId = 1,
            onBackClick = {},
            onAddClick = {},
            onEditClick = {},
            onDetailClick = {},
            onDelete = {},
            onPause = {},
            onResume = {},
            onConfirm = { _, _ -> },
            onSkip = {}
        )
    }
}
