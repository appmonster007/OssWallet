package com.osscabinet.ossWallet.ui.screens.plannedpayments.list

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.navigation.UnifiedPillNavigation
import com.osscabinet.ossWallet.ui.components.management.ManagementListItem
import com.osscabinet.ossWallet.ui.components.management.ManagementSectionHeader
import com.osscabinet.ossWallet.ui.util.TimeUtils.formatRelativeDate
import com.osscabinet.ossWallet.ui.screens.plannedpayments.PlannedPaymentViewModel
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF004D40).copy(alpha = 0.15f)
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFB3261E).copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                },
                label = "swipeBg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (isStartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Confirm",
                        tint = Color(0xFF004D40),
                        modifier = Modifier.size(28.dp)
                    )

                    SwipeToDismissBoxValue.EndToStart -> Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Skip",
                        tint = Color(0xFFB3261E),
                        modifier = Modifier.size(28.dp)
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

private val TealHealthy = Color(0xFF004D40)
private val RedWarning = Color(0xFFB3261E)

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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Planned Payments",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (payments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Repeat,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            "No Planned Payments",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tap 'New Payment' to schedule\nrecurring bills and subscriptions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (overdue.isNotEmpty()) {
                        item {
                            ManagementSectionHeader(
                                title = "OVERDUE",
                                count = overdue.size,
                                sectionColor = RedWarning
                            )
                        }
                        items(overdue, key = { it.id }) { payment ->
                            SwipeablePaymentItem(
                                payment = payment,
                                now = now,
                                isOverdue = true,
                                onTap = { onDetailClick(payment.id) },
                                onEdit = { onEditClick(payment.id) },
                                onDelete = { viewModel.delete(payment.id, false) },
                                onPause = { viewModel.pause(payment) },
                                onResume = { viewModel.resume(payment) },
                                onConfirm = {
                                    if (onConfirmClick != null) {
                                        onConfirmClick(payment.id, payment.schedule.nextDueDate)
                                    } else {
                                        viewModel.quickConfirm(
                                            payment,
                                            transactionManager,
                                            profileId
                                        )
                                    }
                                },
                                onSkip = { viewModel.quickSkip(payment) }
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
                                    sectionColor = MaterialTheme.colorScheme.primary
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
                                    onDelete = { viewModel.delete(item.plannedPayment.id, false) },
                                    onPause = { viewModel.pause(item.plannedPayment) },
                                    onResume = { viewModel.resume(item.plannedPayment) },
                                    onConfirm = {
                                        if (onConfirmClick != null) {
                                            onConfirmClick(item.plannedPayment.id, item.scheduledDate)
                                        } else {
                                            viewModel.quickConfirm(
                                                item.plannedPayment,
                                                transactionManager,
                                                profileId
                                            )
                                        }
                                    },
                                    onSkip = { viewModel.quickSkip(item.plannedPayment) }
                                )
                            }
                        }
                    }

                    if (scheduled.isNotEmpty()) {
                        item {
                            ManagementSectionHeader(
                                title = "SCHEDULED",
                                count = scheduled.size,
                                sectionColor = MaterialTheme.colorScheme.secondary
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
                                onDelete = { viewModel.delete(payment.id, false) },
                                onPause = { viewModel.pause(payment) },
                                onResume = { viewModel.resume(payment) },
                                onConfirm = {
                                    if (onConfirmClick != null) {
                                        onConfirmClick(payment.id, payment.schedule.nextDueDate)
                                    } else {
                                        viewModel.quickConfirm(
                                            payment,
                                            transactionManager,
                                            profileId
                                        )
                                    }
                                },
                                onSkip = { viewModel.quickSkip(payment) }
                            )
                        }
                    }

                    if (paused.isNotEmpty()) {
                        item {
                            ManagementSectionHeader(
                                title = "PAUSED",
                                count = paused.size,
                                sectionColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        items(paused, key = { it.id }) { payment ->
                            SwipeablePaymentItem(
                                payment = payment,
                                now = now,
                                isOverdue = false,
                                onTap = { onDetailClick(payment.id) },
                                onEdit = { onEditClick(payment.id) },
                                onDelete = { viewModel.delete(payment.id, false) },
                                onPause = { viewModel.pause(payment) },
                                onResume = { viewModel.resume(payment) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }

            UnifiedPillNavigation(
                selectedTab = 0,
                onTabSelected = {},
                tabs = emptyList(),
                onFabClick = onAddClick,
                fabIcon = Icons.Rounded.Repeat,
                showFab = true
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

    val typeColor = when {
        isOverdue -> RedWarning
        payment.transactionType == TransactionType.INCOME -> TealHealthy
        else -> RedWarning
    }
    val dateColor =
        if (isOverdue) RedWarning else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    val recurrenceLabel = PlannedPaymentService.formatRecurrenceHuman(payment.schedule.recurrenceRule)
    val formattedAmount = remember(payment.amount) {
        payment.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
    }
    
    val statusIcon = AppIconMap[payment.status.iconName] ?: Icons.Rounded.Repeat
    val statusColor = if (isOverdue) RedWarning else Color(android.graphics.Color.parseColor(payment.status.colorHex))

    ManagementListItem(
        onClick = onTap,
        icon = statusIcon,
        iconTint = statusColor,
        title = payment.name,
        subtitle = recurrenceLabel,
        isWarning = isOverdue,
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formattedAmount,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = typeColor
                )
                Text(
                    payment.currency,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { onDismiss(); onDelete() }
            )
        },
        additionalContent = {
            Text(
                relativeDateLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = dateColor
            )
        }
    )
}

