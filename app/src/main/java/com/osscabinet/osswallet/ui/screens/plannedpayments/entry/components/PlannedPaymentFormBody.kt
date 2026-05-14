package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryEvent
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryState

/**
 * Main content layout for planned payment entry screen.
 * Optimized with shared components and inlined logic to reduce component bloat.
 */
@Composable
fun PlannedPaymentFormBody(
    state: PlannedPaymentEntryState,
    onEvent: (PlannedPaymentEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val allCurrencies = remember { CurrencyService.getAllCurrencies() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // IMPACT: Type selector + Amount input + Title
            AeonImpactHeaderCard(
                transactionType = state.transactionType,
                onTypeSelected = { onEvent(PlannedPaymentEntryEvent.SetTransactionType(it)) },
                amount = state.amount,
                currency = state.currency,
                onCurrencyClick = { onEvent(PlannedPaymentEntryEvent.ShowCurrencySheet(true)) },
                onAmountClick = { onEvent(PlannedPaymentEntryEvent.ShowNumpad(true)) },
                name = state.name,
                onNameChange = { onEvent(PlannedPaymentEntryEvent.SetName(it)) },
                nameLabel = "PAYMENT NAME",
                isTransfer = state.transactionType == 2,
                targetAccountId = state.targetAccountId,
                targetCurrency = state.targetCurrency,
                onTargetCurrencyClick = { onEvent(PlannedPaymentEntryEvent.ShowTargetCurrencySheet(true)) },
                transferRate = state.transferRate,
                onExchangeRateClick = { onEvent(PlannedPaymentEntryEvent.ShowExchangeRateDialog(true)) },
                supportedCurrencies = allCurrencies
            )

            // CONTEXT: Action cards row: Template, Schedule, Account, Category
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Template and Schedule
                AeonContextGrid {
                    EntryActionCard(
                        icon = Icons.Rounded.Bookmark,
                        label = "TEMPLATE",
                        value = state.linkedTemplate?.name ?: "None",
                        onClick = { onEvent(PlannedPaymentEntryEvent.ShowTemplateSheet(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    val scheduleSummary = PlannedPaymentService.buildScheduleSummary(
                        state.startDateMillis,
                        RecurrenceRule(state.recurrenceType, state.recurrenceInterval, state.dayOfWeek, state.dayOfMonth, state.cronExpression)
                    )

                    EntryActionCard(
                        icon = Icons.Rounded.CalendarToday,
                        label = "SCHEDULE",
                        value = scheduleSummary,
                        onClick = { onEvent(PlannedPaymentEntryEvent.ShowScheduleBuilder(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Row 2: Account and Category/Target
                AeonContextGrid {
                    val selectedAccount = state.accounts.find { it.id == state.selectedAccountId }
                    val accountIcon = getAccountIconByName(selectedAccount?.icon)
                    val accountDisplay = when {
                        state.selectedAccountId == null -> "Outside Wallet"
                        selectedAccount != null -> selectedAccount.name
                        else -> "Select"
                    }
                    EntryActionCard(
                        icon = accountIcon,
                        label = if (state.transactionType == 2) "FROM" else "ACCOUNT",
                        value = accountDisplay,
                        onClick = { onEvent(PlannedPaymentEntryEvent.ShowAccountSheet(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    if (state.transactionType == 2) {
                        val targetAccount = state.accounts.find { it.id == state.targetAccountId }
                        val targetDisplay = targetAccount?.name ?: "Select"
                        val targetIcon = getAccountIconByName(targetAccount?.icon)
                        EntryActionCard(
                            icon = targetIcon,
                            label = "TO",
                            value = targetDisplay,
                            onClick = { onEvent(PlannedPaymentEntryEvent.ShowTargetAccountSheet(true)) },
                            modifier = Modifier.weight(1f),
                            iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                            iconTint = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        val categoryObj = state.categories.find { it.id == state.selectedCategoryId }
                        val categoryIcon = getIconByName(categoryObj?.iconName)
                        EntryActionCard(
                            icon = categoryIcon,
                            label = "CATEGORY",
                            value = categoryObj?.name ?: "Unknown",
                            onClick = { onEvent(PlannedPaymentEntryEvent.ShowCategorySheet(true)) },
                            modifier = Modifier.weight(1f),
                            iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                            iconTint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // DETAIL: Metadata section
            AeonDetailSection(
                note = state.noteUserPart,
                onNoteChange = { onEvent(PlannedPaymentEntryEvent.SetNote(it)) },
                payee = state.payee,
                onPayeeChange = { onEvent(PlannedPaymentEntryEvent.SetPayee(it)) },
                selectedTags = state.selectedTagNames,
                onTagClick = { onEvent(PlannedPaymentEntryEvent.ShowTagSheet(true)) },
                onRemoveTag = { tag -> onEvent(PlannedPaymentEntryEvent.SetTags(state.selectedTagNames - tag)) },
                noteSuggestions = state.uniqueNotes,
                payeeSuggestions = state.uniquePayees,
                additionalContent = {
                    // Linked Transactions
                    EntryLinkedTransactionsSection(
                        linkedTransactions = state.displayLinkedTxs,
                        accounts = state.accounts,
                        categories = state.categories,
                        is24Hour = state.use24HourFormat,
                        onShowSheet = { onEvent(PlannedPaymentEntryEvent.ShowLinkedTransactionSheet(true)) },
                        onRemoveLink = { txId ->
                            onEvent(PlannedPaymentEntryEvent.RemoveLinkedTransaction(state.plannedPaymentId ?: -1L, txId))
                        }
                    )

                    // Planned Payment Status Row
                    val ppStatusColor = Color(android.graphics.Color.parseColor(state.status.colorHex))
                    val ppStatusIcon = AppIconMap[state.status.iconName] ?: Icons.Rounded.CheckCircle

                    EntryActionChip(
                        icon = ppStatusIcon,
                        label = "Payment: ${state.status.displayName}",
                        onClick = { onEvent(PlannedPaymentEntryEvent.ShowStatusSheet(true)) },
                        modifier = Modifier.fillMaxWidth(),
                        iconTint = ppStatusColor,
                        iconContainerColor = ppStatusColor.copy(alpha = 0.1f)
                    )

                    // Payment Method and Transaction Status row
                    val paymentIcon = AppIconMap[state.paymentMethod.iconName] ?: Icons.Rounded.Payments
                    
                    val txStatusColor = Color(android.graphics.Color.parseColor(state.transactionStatus.colorHex))
                    val txStatusIcon = AppIconMap[state.transactionStatus.iconName] ?: Icons.Rounded.CheckCircle

                    EntryPaymentMethodStatusRow(
                        paymentMethod = state.paymentMethod.displayName,
                        onPaymentMethodClick = { onEvent(PlannedPaymentEntryEvent.ShowPaymentMethodSheet(true)) },
                        statusLabel = state.transactionStatus.displayName,
                        onStatusClick = { onEvent(PlannedPaymentEntryEvent.ShowTransactionStatusSheet(true)) },
                        paymentMethodIcon = paymentIcon,
                        statusIcon = txStatusIcon,
                        statusIconTint = txStatusColor,
                        statusContainerColor = txStatusColor.copy(alpha = 0.1f)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )

                    // End Date and Auto Confirmation
                    EntrySwitchRow(
                        label = "Has End Date",
                        checked = state.hasEndDate,
                        onCheckedChange = { onEvent(PlannedPaymentEntryEvent.SetHasEndDate(it)) },
                        icon = Icons.Rounded.EventBusy
                    )

                    if (state.hasEndDate) {
                        EntryDateCard(
                            icon = Icons.Rounded.CalendarToday,
                            label = "END DATE",
                            dateMillis = state.endDateMillis,
                            onClick = { onEvent(PlannedPaymentEntryEvent.ShowEndDatePicker(true)) }
                        )
                    }

                    EntrySwitchRow(
                        label = "Auto-confirm Transaction",
                        checked = state.autoConfirm,
                        onCheckedChange = { onEvent(PlannedPaymentEntryEvent.SetAutoConfirm(it)) },
                        icon = Icons.Rounded.AutoFixHigh
                    )

                    // Warranty
                    EntryWarrantySection(
                        enabled = state.isWarrantyEnabled,
                        onToggle = { onEvent(PlannedPaymentEntryEvent.SetWarrantyEnabled(it)) },
                        expirationMillis = state.warrantyExpirationMillis,
                        onDateClick = { onEvent(PlannedPaymentEntryEvent.ShowWarrantyDatePicker(true)) }
                    )

                    // Location
                    EntryLocationRow(
                        coords = if (state.latitude != null && state.longitude != null) {
                            state.latitude to state.longitude
                        } else null,
                        onShowLocationSheet = { onEvent(PlannedPaymentEntryEvent.ShowLocationSheet(true)) },
                        label = "Payment Location"
                    )
                }
            )
        }
    }
}
