package com.osscabinet.ossWallet.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import com.osscabinet.ossWallet.domain.service.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.TransactionManager
import com.osscabinet.ossWallet.ui.viewmodel.PlannedPaymentViewModel
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
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
fun PlannedPaymentsScreen(
    viewModel: PlannedPaymentViewModel,
    transactionManager: TransactionManager,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDetailClick: (Long) -> Unit,
    onBackClick: () -> Unit
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
                p.nextDueDate > windowEnd &&
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
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                text = { Text("New Payment") },
                icon = { Icon(Icons.Rounded.Repeat, contentDescription = null) }
            )
        }
    ) { padding ->
        if (payments.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
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
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (overdue.isNotEmpty()) {
                    item {
                        SectionHeader(
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
                            onDelete = { viewModel.delete(payment.id) },
                            onPause = { viewModel.pause(payment) },
                            onResume = { viewModel.resume(payment) },
                            onConfirm = { viewModel.quickConfirm(payment, transactionManager, profileId) },
                            onSkip = { viewModel.quickSkip(payment) }
                        )
                    }
                }

                if (upcoming.isNotEmpty()) {
                    val upcomingNonOverdue = upcoming.filter { !it.isOverdue }
                        .distinctBy { it.plannedPayment.id }
                    if (upcomingNonOverdue.isNotEmpty()) {
                        item {
                            SectionHeader(
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
                                onDelete = { viewModel.delete(item.plannedPayment.id) },
                                onPause = { viewModel.pause(item.plannedPayment) },
                                onResume = { viewModel.resume(item.plannedPayment) },
                                onConfirm = { viewModel.quickConfirm(item.plannedPayment, transactionManager, profileId) },
                                onSkip = { viewModel.quickSkip(item.plannedPayment) }
                            )
                        }
                    }
                }

                if (scheduled.isNotEmpty()) {
                    item {
                        SectionHeader(
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
                            scheduledDate = payment.nextDueDate,
                            onTap = { onDetailClick(payment.id) },
                            onEdit = { onEditClick(payment.id) },
                            onDelete = { viewModel.delete(payment.id) },
                            onPause = { viewModel.pause(payment) },
                            onResume = { viewModel.resume(payment) },
                            onConfirm = { viewModel.quickConfirm(payment, transactionManager, profileId) },
                            onSkip = { viewModel.quickSkip(payment) }
                        )
                    }
                }

                if (paused.isNotEmpty()) {
                    item {
                        SectionHeader(
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
                            onDelete = { viewModel.delete(payment.id) },
                            onPause = { viewModel.pause(payment) },
                            onResume = { viewModel.resume(payment) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, sectionColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(sectionColor)
        )
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp
            ),
            color = sectionColor,
            modifier = Modifier.weight(1f)
        )
        Surface(
            shape = CircleShape,
            color = sectionColor.copy(alpha = 0.12f)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = sectionColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "itemScale"
    )
    var showMenu by remember { mutableStateOf(false) }
    val dateToShow = scheduledDate ?: payment.nextDueDate
    val relativeDateLabel = remember(dateToShow, now) { formatRelativeDate(dateToShow, now) }

    val typeColor = when {
        isOverdue -> RedWarning
        payment.transactionType == "income" -> TealHealthy
        else -> RedWarning
    }
    val dateColor = if (isOverdue) RedWarning else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    val recurrenceLabel = PlannedPaymentService.formatRecurrenceHuman(payment.recurrenceRule)
    val formattedAmount = remember(payment.amount) {
        payment.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isOverdue)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            0.5.dp,
            if (isOverdue) RedWarning.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(onTap) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTap()
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = typeColor.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = typeColor
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    payment.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    recurrenceLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    relativeDateLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = dateColor
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
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
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { showMenu = false; onEdit() }
                    )
                    if (payment.status == PlannedPaymentStatus.ACTIVE) {
                        DropdownMenuItem(
                            text = { Text("Pause") },
                            onClick = { showMenu = false; onPause() }
                        )
                    } else if (payment.status == PlannedPaymentStatus.PAUSED) {
                        DropdownMenuItem(
                            text = { Text("Resume") },
                            onClick = { showMenu = false; onResume() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

internal fun formatRelativeDate(dateMillis: Long, now: Long): String {
    val zone = ZoneId.systemDefault()
    val target = Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val diffDays = ChronoUnit.DAYS.between(target, today)
    return when {
        target == today -> "Today"
        target == today.plusDays(1) -> "Tomorrow"
        target.isAfter(today) -> DateTimeFormatter.ofPattern("MMM d").format(target)
        diffDays == 1L -> "Yesterday"
        diffDays < 7 -> "$diffDays days ago"
        else -> DateTimeFormatter.ofPattern("MMM d").format(target)
    }
}
