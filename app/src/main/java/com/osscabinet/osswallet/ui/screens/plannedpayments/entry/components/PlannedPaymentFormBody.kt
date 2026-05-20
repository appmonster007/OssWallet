package com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.RecurrenceRule
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.management.PlannedPaymentService
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryEvent
import com.osscabinet.ossWallet.ui.screens.plannedpayments.entry.PlannedPaymentEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors

/**
 * Main content layout for planned payment entry screen.
 * Optimized with shared components and unified metadata section.
 */
@Composable
fun PlannedPaymentFormBody(
    state: PlannedPaymentEntryState,
    onSearch: (String) -> kotlinx.coroutines.flow.Flow<List<com.osscabinet.ossWallet.domain.model.Transaction>>,
    onEvent: (PlannedPaymentEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val allCurrencies = remember { CurrencyService.getAllCurrencies() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = AeonTheme.dimensions.screenPadding,
                    vertical = AeonTheme.dimensions.spacingLg
                ),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
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
                isTransfer = state.transactionType == TransactionType.TRANSFER,
                targetAccountId = state.targetAccountId,
                targetCurrency = state.targetCurrency,
                onTargetCurrencyClick = { onEvent(PlannedPaymentEntryEvent.ShowTargetCurrencySheet(true)) },
                transferRate = state.transferRate,
                onExchangeRateClick = { onEvent(PlannedPaymentEntryEvent.ShowExchangeRateDialog(true)) },
                supportedCurrencies = allCurrencies
            )

            // CONTEXT: Action cards row: Template, Schedule, Account, Category
            AeonContextGrid {
                // Row 1: Template and Schedule
                EntryActionRow {
                    EntryActionCard(
                        icon = Icons.Rounded.Bookmark,
                        label = "TEMPLATE",
                        value = state.linkedTemplate?.name ?: "None",
                        onClick = { onEvent(PlannedPaymentEntryEvent.ShowTemplateSheet(true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = AppColors.formIconContainerPrimary.copy(alpha = 0.1f),
                        iconTint = AppColors.primary
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
                        iconContainerColor = AppColors.formIconContainerSecondary.copy(alpha = 0.1f),
                        iconTint = AppColors.secondary
                    )
                }

                // Row 2: Account and Category/Target
                AeonEntryContextRow(
                    isTransfer = state.transactionType == TransactionType.TRANSFER,
                    selectedAccount = state.accounts.find { it.id == state.selectedAccountId },
                    onAccountClick = { onEvent(PlannedPaymentEntryEvent.ShowAccountSheet(true)) },
                    selectedCategory = state.categories.find { it.id == state.selectedCategoryId },
                    onCategoryClick = { onEvent(PlannedPaymentEntryEvent.ShowCategorySheet(true)) },
                    targetAccount = state.accounts.find { it.id == state.targetAccountId },
                    onTargetAccountClick = { onEvent(PlannedPaymentEntryEvent.ShowTargetAccountSheet(true)) }
                )
            }

            // DETAIL: Metadata section
            AeonDetailSection(
                note = state.noteUserPart,
                onNoteChange = { onEvent(PlannedPaymentEntryEvent.SetNote(it)) },
                payee = state.payee,
                onPayeeChange = { onEvent(PlannedPaymentEntryEvent.SetPayee(it)) },
                selectedNames = state.selectedTagNames,
                allAvailableTags = state.availableTags,
                onTagClick = { onEvent(PlannedPaymentEntryEvent.ShowTagSheet(true)) },
                onRemoveTag = { tag -> onEvent(PlannedPaymentEntryEvent.SetTags(state.selectedTagNames - tag)) },
                noteSuggestions = state.uniqueNotes,
                payeeSuggestions = state.uniquePayees,
                additionalContent = {
                    AeonEntryMetadataSection(
                        state = AeonMetadataState(
                            paymentMethod = state.paymentMethod,
                            status = state.transactionStatus,
                            isWarrantyEnabled = state.isWarrantyEnabled,
                            warrantyExpiration = state.warrantyExpirationMillis,
                            location = if (state.latitude != null && state.longitude != null) state.latitude to state.longitude else null,
                            linkedTransactions = state.displayLinkedTxs
                        ),
                        accounts = state.accounts,
                        categories = state.categories,
                        onShowLinkedTransactions = { onEvent(PlannedPaymentEntryEvent.ShowLinkedTransactionSheet(true)) },
                        onRemoveLink = { txId -> onEvent(PlannedPaymentEntryEvent.RemoveLinkedTransaction(state.plannedPaymentId ?: -1L, txId)) },
                        onPaymentMethodClick = { onEvent(PlannedPaymentEntryEvent.ShowPaymentMethodSheet(true)) },
                        onStatusClick = { onEvent(PlannedPaymentEntryEvent.SetTransactionStatus(it)) },
                        onWarrantyToggle = { onEvent(PlannedPaymentEntryEvent.SetWarrantyEnabled(it)) },
                        onWarrantyDateClick = { onEvent(PlannedPaymentEntryEvent.ShowWarrantyDatePicker(true)) },
                        onLocationClick = { onEvent(PlannedPaymentEntryEvent.ShowLocationSheet(true)) },
                        locationLabel = "Payment Location",
                        additionalTopContent = {
                             // Planned Payment Status Row
                            val ppStatusColor = Color(android.graphics.Color.parseColor(state.status.colorHex))
                            val ppStatusIcon = com.osscabinet.ossWallet.ui.components.AppIconMap[state.status.iconName] ?: Icons.Rounded.CheckCircle

                            EntryActionChip(
                                icon = ppStatusIcon,
                                label = "Payment: ${state.status.displayName}",
                                onClick = { onEvent(PlannedPaymentEntryEvent.ShowStatusSheet(true)) },
                                modifier = Modifier.fillMaxWidth(),
                                iconTint = ppStatusColor,
                                iconContainerColor = ppStatusColor.copy(alpha = 0.1f)
                            )
                        },
                        additionalBottomContent = {
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
                        }
                    )
                }
            )
        }

        // Dedicated sheets for selecting multi-value parameters
        PlannedPaymentEntrySheets(state = state, onSearch = onSearch, onEvent = onEvent)
    }
}
