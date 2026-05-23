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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.UpcomingPaymentItem
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.ui.components.cards.AeonExpressiveCard
import com.osscabinet.ossWallet.ui.components.management.ManagementSectionHeader
import com.osscabinet.ossWallet.ui.components.scrollBottomPadding
import com.osscabinet.ossWallet.ui.screens.plannedpayments.PlannedPaymentViewModel
import com.osscabinet.ossWallet.ui.screens.templates.TemplateViewModel
import com.osscabinet.ossWallet.ui.theme.AccentBlue
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ColorOnDark
import com.osscabinet.ossWallet.ui.theme.NeutralStatusColor
import com.osscabinet.ossWallet.ui.util.TimeUtils.formatRelativeDate
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
    transactionManager: TransactionManager,
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

    LaunchedEffect(payment) {
        if (payment == null) {
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

    val now = remember { System.currentTimeMillis() }
    val dateFormatter =
        remember { DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault()) }

    val upcomingItems = remember(payment, now) {
        PlannedPaymentService.generateNextN(payment, now, 10)
    }
    val pastOccurrences = remember(occurrences, now) {
        occurrences.filter { it.scheduledDate < now }.sortedByDescending { it.scheduledDate }
    }

    val accountName = remember(payment.accountId, accounts) {
        accounts.find { it.id == payment.accountId }?.name ?: payment.accountId?.take(8)
        ?: "Unknown"
    }
    val categoryName = remember(payment.categoryId, categories) {
        categories.find { it.id == payment.categoryId }?.name ?: "Unknown"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        payment.name,
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
                        }
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = "Save as Template")
                    }
                    IconButton(onClick = { onEditClick(payment.id) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppColors.screenBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(AeonTheme.dimensions.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
        ) {
            // Hero card
            item {
                val accentColor = when (payment.status) {
                    PlannedPaymentStatus.ACTIVE -> when (payment.transactionType) {
                        TransactionType.INCOME -> AppColors.primary
                        TransactionType.TRANSFER -> AccentBlue
                        else -> AppColors.error
                    }

                    PlannedPaymentStatus.PAUSED -> NeutralStatusColor
                    else -> NeutralStatusColor
                }
                val recurrenceText =
                    PlannedPaymentService.formatRecurrenceHuman(payment.schedule.recurrenceRule)
                val formattedAmount =
                    payment.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
                val nextRelative = formatRelativeDate(payment.schedule.nextDueDate, now)

                AeonExpressiveCard(
                    title = payment.name,
                    subtitle = recurrenceText,
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                    ) {
                        Text(
                            formattedAmount,
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
                    Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))

                    Row(horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
                        val statusLabel = payment.status.displayName
                        val statusColor = when (payment.status) {
                            PlannedPaymentStatus.ACTIVE -> accentColor
                            PlannedPaymentStatus.PAUSED -> NeutralStatusColor
                            else -> AppColors.error
                        }
                        Surface(
                            shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
                            color = statusColor.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                statusLabel,
                                style = AppTextStyles.formSectionLabel,
                                color = statusColor,
                                modifier = Modifier.padding(
                                    horizontal = AeonTheme.dimensions.spacingMd,
                                    vertical = AeonTheme.dimensions.spacingXs
                                )
                            )
                        }
                        val typeLabel = payment.transactionType.displayName
                        Surface(
                            shape = RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius),
                            color = AppColors.insetSurface.copy(alpha = 0.5f)
                        ) {
                            Text(
                                typeLabel,
                                style = AppTextStyles.transactionSwipeAction,
                                color = AppColors.contentSecondary,
                                modifier = Modifier.padding(
                                    horizontal = AeonTheme.dimensions.spacingMd,
                                    vertical = AeonTheme.dimensions.spacingXs
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = AppColors.divider.copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.height(AeonTheme.dimensions.spacingSm))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXs)
                    ) {
                        Icon(
                            Icons.Rounded.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = accentColor.copy(alpha = 0.7f)
                        )
                        Text(
                            recurrenceText,
                            style = AppTextStyles.drawerItemLabel,
                            color = AppColors.contentSecondary,
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
                            style = AppTextStyles.chipLabel,
                            color = accentColor
                        )
                    }
                    Spacer(Modifier.height(AeonTheme.dimensions.spacingXs))
                    Text(
                        "Started ${dateFormatter.format(Instant.ofEpochMilli(payment.schedule.startDate))}",
                        style = AppTextStyles.caption,
                        color = AppColors.contentSecondary.copy(alpha = 0.6f)
                    )
                }
            }

            // Metadata card
            val hasMetadata = accounts.isNotEmpty() ||
                    !payment.payee.isNullOrBlank() ||
                    payment.paymentMethod != null ||
                    categoryName != "Unknown" ||
                    payment.tags.isNotEmpty() ||
                    !payment.note.isNullOrBlank()
            if (hasMetadata) {
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
                            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                        ) {
                            if (accounts.isNotEmpty()) {
                                DetailMetaRow(Icons.Rounded.AccountBalance, "Account", accountName)
                            }
                            if (!payment.payee.isNullOrBlank()) {
                                DetailMetaRow(Icons.Rounded.Person, "Payee", payment.payee!!)
                            }
                            if (categoryName != "Unknown") {
                                DetailMetaRow(Icons.Rounded.Category, "Category", categoryName)
                            }
                            if (payment.paymentMethod != null) {
                                DetailMetaRow(
                                    Icons.Rounded.Payments,
                                    "Payment method",
                                    payment.paymentMethod!!.displayName
                                )
                            }
                            if (payment.tags.isNotEmpty()) {
                                DetailMetaRow(
                                    Icons.Rounded.LocalOffer,
                                    "Tags",
                                    payment.tags.joinToString(" · ")
                                )
                            }
                            if (!payment.note.isNullOrBlank()) {
                                DetailMetaRow(
                                    Icons.AutoMirrored.Rounded.Notes,
                                    "Note",
                                    payment.note!!,
                                    singleLine = false
                                )
                            }
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

            // History
            if (pastOccurrences.isNotEmpty()) {
                item {
                    ManagementSectionHeader(
                        title = "HISTORY",
                        count = pastOccurrences.size,
                        sectionColor = AppColors.contentSecondary.copy(alpha = 0.5f)
                    )
                }
                items(pastOccurrences, key = { it.id }) { occ ->
                    PastOccurrenceItem(occ = occ, dateFormatter = dateFormatter, payment = payment)
                }
            }

            item { Spacer(modifier = Modifier.height(scrollBottomPadding())) }
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
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(AeonTheme.dimensions.iconSizeSm),
            tint = AppColors.contentSecondary.copy(alpha = 0.5f)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = AppTextStyles.transactionSwipeAction.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = AppColors.contentSecondary.copy(alpha = 0.45f)
            )
            Text(
                value,
                style = AppTextStyles.chartTooltipValue,
                color = AppColors.contentPrimary,
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

        OccurrenceStatus.SKIPPED -> Triple("Skipped", NeutralStatusColor, Icons.Rounded.Cancel)
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
