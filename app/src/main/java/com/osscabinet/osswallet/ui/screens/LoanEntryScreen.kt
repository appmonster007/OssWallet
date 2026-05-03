package com.osscabinet.ossWallet.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.service.CurrencyService
import com.osscabinet.ossWallet.ui.components.AeonDatePickerDialog
import com.osscabinet.ossWallet.ui.components.AeonEntryActionCard
import com.osscabinet.ossWallet.ui.components.CurrencySelector
import com.osscabinet.ossWallet.ui.screens.transactions.entry.components.EditorialInput
import com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets.CalculatorSheet
import com.osscabinet.ossWallet.ui.viewmodel.LoanViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanEntryScreen(
    loanId: Long?,
    viewModel: LoanViewModel,
    accounts: List<Account> = emptyList(),
    onBackClick: () -> Unit
) {
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val profileId by viewModel.currentProfileId.collectAsStateWithLifecycle()
    val existing = remember(loanId, loans) {
        loanId?.let { id -> loans.find { it.id == id } }
    }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var principalText by remember(existing) { mutableStateOf(existing?.principalAmount?.toPlainString() ?: "") }
    var currency by remember(existing) { mutableStateOf(existing?.currency ?: "USD") }
    var loanType by remember(existing) { mutableStateOf(existing?.loanType ?: LoanType.BORROWED) }
    var counterparty by remember(existing) { mutableStateOf(existing?.lenderOrBorrowerName ?: "") }
    var interestText by remember(existing) { mutableStateOf(existing?.interestRate?.toString() ?: "") }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }

    val now = remember { System.currentTimeMillis() }
    var startDateMillis by remember(existing) { mutableLongStateOf(existing?.startDate ?: now) }
    var hasSettlementDate by remember(existing) { mutableStateOf(existing?.expectedSettlementDate != null) }
    var settlementDateMillis by remember(existing) {
        mutableLongStateOf(existing?.expectedSettlementDate ?: now)
    }

    var linkedAccountId by remember(existing) {
        mutableStateOf(
            existing?.linkedAccountId?.ifBlank { null }
                ?: accounts.firstOrNull()?.id
                ?: ""
        )
    }

    val isEditing = loanId != null

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showSettlementDatePicker by remember { mutableStateOf(false) }
    var showNumpad by remember { mutableStateOf(false) }

    val allCurrencies = remember { CurrencyService.getAllCurrencies() }
    val canSave = name.isNotBlank()
    val tealColor = Color(0xFF004D40)
    val orangeColor = Color(0xFFE65100)
    val amountColor = if (loanType == LoanType.LENT) tealColor else orangeColor

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Loan" else "New Loan",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(
                            onClick = {
                                viewModel.deleteLoan(loanId!!)
                                onBackClick()
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val principal = principalText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val rate = interestText.toFloatOrNull()
                            val loan = (existing ?: viewModel.buildDefaultLoan(profileId)).copy(
                                name = name,
                                principalAmount = principal,
                                currency = currency,
                                loanType = loanType,
                                lenderOrBorrowerName = counterparty,
                                interestRate = rate,
                                note = note,
                                startDate = startDateMillis,
                                expectedSettlementDate = if (hasSettlementDate) settlementDateMillis else null,
                                linkedAccountId = linkedAccountId
                            )
                            viewModel.saveLoan(loan)
                            onBackClick()
                        },
                        enabled = canSave,
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->

        if (showStartDatePicker) {
            AeonDatePickerDialog(
                selectedDateMillis = startDateMillis,
                onDateSelected = { startDateMillis = it; showStartDatePicker = false },
                onDismiss = { showStartDatePicker = false }
            )
        }
        if (showSettlementDatePicker) {
            AeonDatePickerDialog(
                selectedDateMillis = settlementDateMillis,
                onDateSelected = { settlementDateMillis = it; showSettlementDatePicker = false },
                onDismiss = { showSettlementDatePicker = false }
            )
        }
        if (showNumpad) {
            CalculatorSheet(
                currentAmount = principalText,
                transactionType = if (loanType == LoanType.LENT) 1 else 0,
                decimalPrecision = 2,
                onAmountChanged = { principalText = it },
                onDismiss = { showNumpad = false }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card: loan type selector + amount
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LoanTypeSelector(
                        loanType = loanType,
                        onTypeSelected = { loanType = it }
                    )

                    // Amount row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CurrencySelector(
                            selectedCurrency = currency,
                            onCurrencySelected = { currency = it },
                            supportedCurrencies = allCurrencies
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showNumpad = true }
                        ) {
                            Text(
                                text = if (principalText.isEmpty()) "0.00" else principalText,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-1).sp
                                ),
                                color = if (principalText.isEmpty()) amountColor.copy(alpha = 0.3f) else amountColor
                            )
                        }
                    }
                }
            }

            // Row: Account + Start Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (accounts.isNotEmpty()) {
                    var accountExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = accountExpanded,
                        onExpandedChange = { accountExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        AeonEntryActionCard(
                            icon = Icons.Rounded.AccountBalance,
                            label = "ACCOUNT",
                            value = accounts.find { it.id == linkedAccountId }?.name ?: "Select",
                            onClick = { accountExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            iconTint = MaterialTheme.colorScheme.primary
                        )
                        ExposedDropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false }
                        ) {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.name) },
                                    onClick = { linkedAccountId = account.id; accountExpanded = false }
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                AeonEntryActionCard(
                    icon = Icons.Rounded.CalendarToday,
                    label = "START DATE",
                    value = formatLoanEntryDate(startDateMillis),
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f),
                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }

            // Settlement date row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.EventAvailable,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Expected Settlement",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (hasSettlementDate) formatLoanEntryDate(settlementDateMillis) else "Not set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = hasSettlementDate,
                        onCheckedChange = { hasSettlementDate = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color(0xFF004D40)
                        )
                    )
                }
            }

            if (hasSettlementDate) {
                LoanEntryDateCard(
                    icon = Icons.Rounded.CalendarMonth,
                    label = "SETTLEMENT DATE",
                    dateMillis = settlementDateMillis,
                    onClick = { showSettlementDatePicker = true }
                )
            }

            // Details card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    LoanEntrySectionHeader(icon = Icons.Rounded.MoreHoriz, label = "LOAN DETAILS")

                    EditorialInput(
                        value = name,
                        onValueChange = { name = it },
                        label = "NAME",
                        icon = Icons.Rounded.Title
                    )
                    EditorialInput(
                        value = counterparty,
                        onValueChange = { counterparty = it },
                        label = if (loanType == LoanType.LENT) "LENT TO" else "BORROWED FROM",
                        icon = Icons.Rounded.Person
                    )
                    EditorialInput(
                        value = interestText,
                        onValueChange = { interestText = it },
                        label = "INTEREST RATE % / YEAR",
                        icon = Icons.Rounded.Percent,
                        placeholder = "Optional"
                    )
                    EditorialInput(
                        value = note,
                        onValueChange = { note = it },
                        label = "NOTE",
                        icon = Icons.AutoMirrored.Rounded.Notes,
                        maxLines = 4
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanTypeSelector(
    loanType: LoanType,
    onTypeSelected: (LoanType) -> Unit
) {
    val tealColor = Color(0xFF004D40)
    val orangeColor = Color(0xFFE65100)

    val indicatorColor by animateColorAsState(
        targetValue = if (loanType == LoanType.LENT) tealColor else orangeColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "indicatorColor"
    )

    val indicatorOffset by animateDpAsState(
        targetValue = if (loanType == LoanType.LENT) 0.dp else 120.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicatorOffset"
    )

    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                RoundedCornerShape(32.dp)
            )
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(120.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(indicatorColor)
        )

        Row(modifier = Modifier.fillMaxHeight()) {
            listOf(
                LoanType.LENT to (Icons.Rounded.ArrowUpward to "Lent"),
                LoanType.BORROWED to (Icons.Rounded.ArrowDownward to "Borrowed")
            ).forEach { (type, iconLabel) ->
                val (icon, label) = iconLabel
                val isSelected = loanType == type
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "scale"
                )
                Row(
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTypeSelected(type) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanEntrySectionHeader(icon: ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun LoanEntryDateCard(
    icon: ImageVector,
    label: String,
    dateMillis: Long,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = formatLoanEntryDate(dateMillis),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

private fun formatLoanEntryDate(millis: Long): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(java.util.Date(millis))
}
