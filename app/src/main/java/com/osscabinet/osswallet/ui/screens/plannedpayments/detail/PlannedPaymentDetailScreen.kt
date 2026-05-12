package com.osscabinet.ossWallet.ui.screens.plannedpayments.detail

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentOccurrence
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.management.ManagementSectionHeader
import com.osscabinet.ossWallet.ui.util.TimeUtils.formatRelativeDate
import com.osscabinet.ossWallet.ui.viewmodel.PlannedPaymentViewModel
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val TealHealthy = Color(0xFF004D40)
private val RedWarning = Color(0xFFB3261E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedPaymentDetailScreen(
    plannedPaymentId: Long,
    viewModel: PlannedPaymentViewModel,
    transactionManager: TransactionManager,
    templateViewModel: com.osscabinet.ossWallet.ui.viewmodel.TemplateViewModel? = null,
    accounts: List<Account> = emptyList(),
    categories: List<Category> = emptyList(),
    onEditClick: (Long) -> Unit,
    onConfirmClick: (Long, Long?) -> Unit,
    onBackClick: () -> Unit
) {
    val payments by viewModel.plannedPayments.collectAsStateWithLifecycle()
    val payment =
        remember(plannedPaymentId, payments) { payments.find { it.id == plannedPaymentId } }

    val occurrencesFlow = remember(plannedPaymentId) {
        viewModel.getOccurrencesForPayment(plannedPaymentId)
    }
    val occurrences by occurrencesFlow.collectAsStateWithLifecycle(emptyList())
    val profileId by viewModel.currentProfileId.collectAsStateWithLifecycle()

    if (payment == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val now = remember { System.currentTimeMillis() }
    val dateFormatter =
        remember { DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault()) }

    val upcomingItems = remember(payment, now) {
        PlannedPaymentService.generateNextN(payment, now, 10)
    }
    val pastOccurrences = remember(occurrences, now) {
        occurrences.filter { it.scheduledDate < now }.sortedByDescending { it.scheduledDate }
    }

    val accountName = remember(payment.template.accountId, accounts) {
        accounts.find { it.id == payment.template.accountId }?.name ?: payment.template.accountId?.take(8) ?: "Unknown"
    }
    val categoryName = remember(payment.template.categoryId, categories) {
        categories.find { it.id == payment.template.categoryId }?.name ?: "Unknown"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        payment.template.name,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val t = payment.template
                            templateViewModel?.addTemplate(
                                name = t.name,
                                categoryId = t.categoryId,
                                amount = t.amount,
                                transactionType = t.transactionType,
                                accountId = t.accountId,
                                targetAccountId = t.targetAccountId,
                                targetCurrency = t.targetCurrency,
                                transferRate = t.transferRate,
                                note = t.note,
                                payee = t.payee,
                                location = t.location,
                                paymentMethod = t.paymentMethod,
                                currency = t.currency,
                                tags = t.tags,
                                profileId = payment.profileId
                            )
                        }
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = "Save as Template")
                    }
                    IconButton(onClick = { onEditClick(payment.id) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── Hero card ───────────────────────────────────────────────────
            item {
                val accentColor = when (payment.status) {
                    PlannedPaymentStatus.ACTIVE -> if (payment.template.transactionType == 1) TealHealthy else RedWarning
                    PlannedPaymentStatus.PAUSED -> Color.Gray
                    else -> Color.Gray
                }
                val recurrenceText =
                    PlannedPaymentService.formatRecurrenceHuman(payment.schedule.recurrenceRule)
                val formattedAmount =
                    payment.template.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
                val nextRelative = formatRelativeDate(payment.schedule.nextDueDate, now)

                AeonExpressiveCard(
                    title = payment.template.name,
                    subtitle = recurrenceText,
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            formattedAmount,
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    if (payment.template.currency != null) {
                        Text(
                            payment.template.currency,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = accentColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    }
                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val statusLabel =
                            payment.status.name.lowercase().replaceFirstChar { it.uppercase() }
                        val statusColor = when (payment.status) {
                            PlannedPaymentStatus.ACTIVE -> accentColor
                            PlannedPaymentStatus.PAUSED -> Color.Gray
                            else -> MaterialTheme.colorScheme.error
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = statusColor.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                statusLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        val typeLabel = when (payment.template.transactionType) {
                            1 -> "Income"
                            2 -> "Transfer"
                            else -> "Expense"
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                typeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = accentColor.copy(alpha = 0.7f)
                        )
                        Text(
                            recurrenceText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = accentColor.copy(alpha = 0.6f)
                        )
                        Text(
                            "Next: $nextRelative",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = accentColor
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Started ${dateFormatter.format(Instant.ofEpochMilli(payment.schedule.startDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // ─── Metadata card ───────────────────────────────────────────────
            val hasMetadata = accounts.isNotEmpty() ||
                    !payment.template.payee.isNullOrBlank() ||
                    !payment.template.paymentMethod.isNullOrBlank() ||
                    categoryName != "Unknown" ||
                    payment.template.tags.isNotEmpty() ||
                    !payment.template.note.isNullOrBlank()
            if (hasMetadata) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (accounts.isNotEmpty()) {
                                DetailMetaRow(Icons.Rounded.AccountBalance, "Account", accountName)
                            }
                            if (!payment.template.payee.isNullOrBlank()) {
                                DetailMetaRow(Icons.Rounded.Person, "Payee", payment.template.payee!!)
                            }
                            if (categoryName != "Unknown") {
                                DetailMetaRow(Icons.Rounded.Category, "Category", categoryName)
                            }
                            if (!payment.template.paymentMethod.isNullOrBlank()) {
                                DetailMetaRow(
                                    Icons.Rounded.Payments,
                                    "Payment method",
                                    payment.template.paymentMethod!!
                                )
                            }
                            if (payment.template.tags.isNotEmpty()) {
                                DetailMetaRow(
                                    Icons.Rounded.LocalOffer,
                                    "Tags",
                                    payment.template.tags.joinToString(" · ")
                                )
                            }
                            if (!payment.template.note.isNullOrBlank()) {
                                DetailMetaRow(
                                    Icons.AutoMirrored.Rounded.Notes,
                                    "Note",
                                    payment.template.note!!,
                                    singleLine = false
                                )
                            }
                        }
                    }
                }
            }

            // ─── Upcoming occurrences ────────────────────────────────────────
            if (upcomingItems.isNotEmpty()) {
                item {
                    ManagementSectionHeader(
                        title = "UPCOMING",
                        count = upcomingItems.size,
                        sectionColor = MaterialTheme.colorScheme.primary
                    )
                }
                items(
                    upcomingItems,
                    key = { "${it.plannedPayment.id}_${it.scheduledDate}" }) { item ->
                    val matchedOccurrence =
                        occurrences.find { it.scheduledDate == item.scheduledDate }
                    val status = matchedOccurrence?.status ?: OccurrenceStatus.PENDING
                    OccurrenceItem(
                        item = item,
                        status = status,
                        dateFormatter = dateFormatter,
                        onConfirm = {
                            onConfirmClick(payment.id, item.scheduledDate)
                        },
                        onSkip = {
                            viewModel.quickSkip(payment)
                        }
                    )
                }
            }

            // ─── History ─────────────────────────────────────────────────────
            if (pastOccurrences.isNotEmpty()) {
                item {
                    ManagementSectionHeader(
                        title = "HISTORY",
                        count = pastOccurrences.size,
                        sectionColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                items(pastOccurrences, key = { it.id }) { occ ->
                    PastOccurrenceItem(occ = occ, dateFormatter = dateFormatter, payment = payment)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun DetailMetaRow(
    icon: ImageVector,
    label: String,
    value: String,
    singleLine: Boolean = true
) {
    Row(
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
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

    val iconColor = if (item.isOverdue) RedWarning else MaterialTheme.colorScheme.secondary
    val dateLabel = dateFormatter.format(Instant.ofEpochMilli(item.scheduledDate))
    val formattedAmount =
        item.plannedPayment.template.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()

    Surface(
        color = if (item.isOverdue)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            0.5.dp,
            if (item.isOverdue) RedWarning.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = iconColor
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (item.isOverdue) RedWarning else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "$formattedAmount ${item.plannedPayment.template.currency ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            when (status) {
                OccurrenceStatus.PENDING -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = onSkip,
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                            border = BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Skip", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (canConfirmToday) {
                            Surface(
                                onClick = onConfirm,
                                shape = RoundedCornerShape(16.dp),
                                color = TealHealthy,
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                    Text(
                                        "Confirm",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
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
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        hintLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
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
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = TealHealthy,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Confirmed",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TealHealthy
                        )
                    }
                }

                OccurrenceStatus.SKIPPED -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Cancel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Skipped",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
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
    val formattedAmount = payment.template.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
    val (statusLabel, statusColor, statusIcon) = when (occ.status) {
        OccurrenceStatus.CONFIRMED -> Triple("Confirmed", TealHealthy, Icons.Rounded.CheckCircle)
        OccurrenceStatus.SKIPPED -> Triple("Skipped", Color.Gray, Icons.Rounded.Cancel)
        OccurrenceStatus.PENDING -> Triple(
            "Pending",
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Rounded.CalendarToday
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        statusIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = statusColor
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    "$formattedAmount ${payment.template.currency ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    statusIcon,
                    contentDescription = null,
                    tint = statusColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = statusColor
                )
            }
        }
    }
}
