package com.osscabinet.ossWallet.ui.screens.budgets.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.*
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryEvent
import com.osscabinet.ossWallet.ui.screens.budgets.entry.BudgetEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

/**
 * Budget Entry Form Body with Optimized Information Hierarchy.
 * BUDGET CONTEXT (Top): Name, Amount, Period, and Note.
 * TRACKING FILTER (Below): Detailed criteria for matching transactions.
 */
@Composable
fun BudgetFormBody(
    state: BudgetEntryState,
    onEvent: (BudgetEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = AeonTheme.dimensions
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dims.screenPadding, vertical = dims.spacingLg),
            verticalArrangement = Arrangement.spacedBy(dims.spacingLg)
        ) {
            // IMPACT: Primary Identity (Name & Limit)
            AeonImpactHeaderCard(
                transactionType = if (state.selectedTrackTypes.contains(TransactionType.EXPENSE)) TransactionType.EXPENSE else TransactionType.INCOME,
                onTypeSelected = { },
                amount = state.limitAmount,
                currency = state.currency,
                onCurrencyClick = { onEvent(BudgetEntryEvent.ShowNumpad(show = true)) },
                onAmountClick = { onEvent(BudgetEntryEvent.ShowNumpad(show = true)) },
                name = state.name,
                onNameChange = { onEvent(BudgetEntryEvent.SetName(it)) },
                nameLabel = "BUDGET NAME",
                showTypeSelector = false,
                amountColor = AppColors.primary
            )

            // BUDGET CONTEXT: The core parameters of the budget itself
            EntrySection(title = "BUDGET PARAMETERS", icon = Icons.Rounded.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(dims.spacingLg)) {
                    // Period is a high-priority structural parameter
                    AeonTimelinePeriodSelector(
                        currentRange = state.timelineRange,
                        onRangeSelected = { onEvent(BudgetEntryEvent.SetTimelineRange(it)) },
                        swipeEntireComponent = false
                    )

                    // Budget's own note
                    EntryEditorialInput(
                        value = state.note,
                        onValueChange = { onEvent(BudgetEntryEvent.SetNote(it)) },
                        label = "BUDGET NOTE",
                        icon = Icons.AutoMirrored.Rounded.Notes,
                        placeholder = "Brief description or purpose..."
                    )
                }
            }

            // TRACKING FILTER: Criteria used to "catch" transactions
            EntrySection(title = "TRACKING LOGIC", icon = Icons.Rounded.FilterList) {
                Column(verticalArrangement = Arrangement.spacedBy(dims.spacingLg)) {
                    // Filter Preset
                    val filterObj = state.availableFilters.find { it.id == state.filterId }
                    EntryActionCard(
                        icon = Icons.Rounded.Tune,
                        label = "FILTER PRESET",
                        value = filterObj?.name ?: "No Preset Applied",
                        onClick = { onEvent(BudgetEntryEvent.ShowFilterSheet(show = true)) },
                        modifier = Modifier.fillMaxWidth(),
                        iconContainerColor = AppColors.formIconContainerPrimary.copy(alpha = 0.1f),
                        iconTint = AppColors.primary
                    )

                    AeonContextGrid {
                        // Accounts & Categories
                        EntryActionRow {
                            val accountObj = if (state.selectedAccountIds.size == 1) state.accounts.find { it.id == state.selectedAccountIds.first() } else null
                            EntryActionCard(
                                icon = getAccountIconByName(accountObj?.icon),
                                label = "ACCOUNTS",
                                value = when {
                                    state.selectedAccountIds.isEmpty() -> "All Accounts"
                                    (state.accounts.isNotEmpty() && (state.selectedAccountIds.size == state.accounts.size)) -> "All Accounts"
                                    accountObj != null -> accountObj.name
                                    else -> "${state.selectedAccountIds.size} Selected"
                                },
                                onClick = { onEvent(BudgetEntryEvent.ShowAccountSheet(show = true)) },
                                modifier = Modifier.weight(1f)
                            )

                            val categoryObj = if (state.selectedCategoryIds.size == 1) state.categories.find { it.id == state.selectedCategoryIds.first() } else null
                            EntryActionCard(
                                icon = categoryObj?.let { getIconByName(it.iconName) } ?: Icons.Rounded.Category,
                                label = "CATEGORIES",
                                value = when {
                                    state.selectedCategoryIds.isEmpty() -> "All Categories"
                                    (state.categories.isNotEmpty() && (state.selectedCategoryIds.size == state.categories.size)) -> "All Categories"
                                    categoryObj != null -> categoryObj.name
                                    else -> "${state.selectedCategoryIds.size} Selected"
                                },
                                onClick = { onEvent(BudgetEntryEvent.ShowCategorySheet(show = true)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Flows & Currencies
                        EntryActionRow {
                            EntryActionCard(
                                icon = Icons.AutoMirrored.Rounded.ShowChart,
                                label = "FLOWS",
                                value = if (state.selectedTrackTypes.size >= 2 || state.selectedTrackTypes.isEmpty()) "All Flows" else state.selectedTrackTypes.first().displayName,
                                onClick = { onEvent(BudgetEntryEvent.ShowTrackTypeSheet(show = true)) },
                                modifier = Modifier.weight(1f)
                            )

                            EntryActionCard(
                                icon = Icons.Rounded.CurrencyExchange,
                                label = "CURRENCIES",
                                value = if (state.selectedCurrencies.isEmpty()) "All Currencies" else if (state.selectedCurrencies.size == 1) state.selectedCurrencies.first() else "${state.selectedCurrencies.size} Selected",
                                onClick = { onEvent(BudgetEntryEvent.ShowCurrencyFilterSheet(show = true)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // RESTORED: Advanced Search Queries with Regex Toggles
                    Column(verticalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
                        // Note Query
                        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingSm)) {
                            EntrySwitchRow(
                                label = "Regex for Note",
                                checked = state.isNoteRegex,
                                onCheckedChange = { onEvent(BudgetEntryEvent.SetNoteRegex(it)) },
                                icon = Icons.Rounded.Code,
                                subtitle = "Supports /pattern/flags"
                            )
                            EntryEditorialInput(
                                value = state.queryNote,
                                onValueChange = { onEvent(BudgetEntryEvent.SetQueryNote(it)) },
                                label = "NOTE QUERY",
                                icon = Icons.AutoMirrored.Rounded.Notes,
                                placeholder = if (state.isNoteRegex) "/pattern/i" else "Match note text..."
                            )
                        }

                        // Payee Query
                        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingSm)) {
                            EntrySwitchRow(
                                label = "Regex for Payee",
                                checked = state.isPayeeRegex,
                                onCheckedChange = { onEvent(BudgetEntryEvent.SetPayeeRegex(it)) },
                                icon = Icons.Rounded.Code,
                                subtitle = "Supports /pattern/flags"
                            )
                            EntryEditorialInput(
                                value = state.queryPayee,
                                onValueChange = { onEvent(BudgetEntryEvent.SetQueryPayee(it)) },
                                label = "PAYEE QUERY",
                                icon = Icons.Rounded.Person,
                                placeholder = if (state.isPayeeRegex) "/pattern/i" else "Match payee name..."
                            )
                        }
                    }

                    // Tags
                    EntryTagSection(
                        tags = state.selectedTags.map { name -> 
                            state.availableTags.find { it.name == name } ?: com.osscabinet.ossWallet.domain.model.Tag(name = name)
                        },
                        onShowTagSheet = { onEvent(BudgetEntryEvent.ShowTagSheet(show = true)) },
                        onRemoveTag = { tag -> onEvent(BudgetEntryEvent.SetTags(state.selectedTags - tag)) }
                    )

                    // RESTORED: Payment Method and Status Filters
                    EntryPaymentMethodStatusRow(
                        paymentMethod = when {
                            state.selectedPaymentMethods.isEmpty() -> "All Methods"
                            state.selectedPaymentMethods.size == PaymentMethod.entries.size -> "All Methods"
                            else -> "${state.selectedPaymentMethods.size} Selected"
                        },
                        onPaymentMethodClick = { onEvent(BudgetEntryEvent.ShowPaymentMethodSheet(show = true)) },
                        statusLabel = when {
                            state.selectedStatuses.isEmpty() -> "All Statuses"
                            state.selectedStatuses.size == TransactionStatus.entries.size -> "All Statuses"
                            else -> "${state.selectedStatuses.size} Selected"
                        },
                        onStatusClick = { onEvent(BudgetEntryEvent.ShowStatusSheet(show = true)) }
                    )

                    // Repayment Linking
                    EntryActionCard(
                        icon = Icons.Rounded.Link,
                        label = "LINKED TRANSACTIONS",
                        value = if (state.linkedTransactionIds.isEmpty()) "None" else "${state.linkedTransactionIds.size} Linked",
                        onClick = { onEvent(BudgetEntryEvent.ShowLinkSheet(show = true)) },
                        modifier = Modifier.fillMaxWidth(),
                        iconContainerColor = AppColors.formIconContainerTertiary.copy(alpha = 0.1f),
                        iconTint = AppColors.tertiary
                    )

                    // Constraints
                    AeonContextGrid {
                        EntryActionRow {
                            EntryActionCard(
                                icon = Icons.Rounded.ArrowDownward,
                                label = "MIN AMOUNT",
                                value = state.minAmount.ifEmpty { "Any" },
                                onClick = { onEvent(BudgetEntryEvent.ShowMinAmountSheet(show = true)) },
                                modifier = Modifier.weight(1f)
                            )
                            EntryActionCard(
                                icon = Icons.Rounded.ArrowUpward,
                                label = "MAX AMOUNT",
                                value = state.maxAmount.ifEmpty { "Any" },
                                onClick = { onEvent(BudgetEntryEvent.ShowMaxAmountSheet(show = true)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Warranty
                    EntryActionCard(
                        icon = Icons.Rounded.Verified,
                        label = "WARRANTY STATUS",
                        value = when (state.hasWarranty) {
                            null -> "Any"
                            true -> "Has Warranty"
                            false -> "No Warranty"
                        },
                        onClick = { onEvent(BudgetEntryEvent.SetHasWarranty(if (state.hasWarranty == true) false else if (state.hasWarranty == false) null else true)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (state.hasWarranty == true) {
                        val formatter = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }
                        AeonContextGrid {
                            EntryActionRow {
                                EntryActionCard(
                                    icon = Icons.Rounded.CalendarToday,
                                    label = "WARRANTY START",
                                    value = state.warrantyExpirationStart?.let { formatter.format(java.util.Date(it)) } ?: "Select",
                                    onClick = { onEvent(BudgetEntryEvent.ShowWarrantyStartPicker(show = true)) },
                                    modifier = Modifier.weight(1f)
                                )
                                EntryActionCard(
                                    icon = Icons.Rounded.CalendarToday,
                                    label = "WARRANTY END",
                                    value = state.warrantyExpirationEnd?.let { formatter.format(java.util.Date(it)) } ?: "Select",
                                    onClick = { onEvent(BudgetEntryEvent.ShowWarrantyEndPicker(show = true)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    EntrySwitchRow(
                        label = "Exclude Future",
                        checked = state.excludeFuture,
                        onCheckedChange = { onEvent(BudgetEntryEvent.SetExcludeFuture(it)) },
                        icon = Icons.Rounded.Schedule,
                        subtitle = "Exclude transactions with future dates"
                    )
                }
            }

            // MANAGEMENT: Meta Configuration
            EntrySection(title = "MANAGEMENT", icon = Icons.Rounded.Settings) {
                Column(verticalArrangement = Arrangement.spacedBy(dims.spacingLg)) {
                    Column(verticalArrangement = Arrangement.spacedBy(dims.spacingSm)) {
                        Text(
                            text = "Alert threshold at ${(state.alertThreshold * 100).toInt()}%",
                            style = AppTextStyles.transactionAmount
                        )
                        Slider(
                            value = state.alertThreshold,
                            onValueChange = { onEvent(BudgetEntryEvent.SetAlertThreshold(it)) },
                            valueRange = 0.5f..1.0f,
                            steps = 9
                        )
                    }

                    EntrySwitchRow(
                        label = "Roll over balance",
                        checked = state.rollover,
                        onCheckedChange = { onEvent(BudgetEntryEvent.SetRollover(it)) },
                        icon = Icons.Rounded.Autorenew,
                        subtitle = "Carry forward unused amounts to next period"
                    )
                }
            }
        }

        BudgetEntrySheets(state = state, onEvent = onEvent)

        if (state.showSharedFilterDialog) {
            SharedFilterConfirmationDialog(
                onDismiss = { onEvent(BudgetEntryEvent.ShowSharedFilterDialog(show = false)) },
                onConfirmUpdateShared = { onEvent(BudgetEntryEvent.ConfirmSaveWithSharedFilter(updateShared = true)) },
                onConfirmCreateNew = { onEvent(BudgetEntryEvent.ConfirmSaveWithSharedFilter(updateShared = false)) }
            )
        }
    }
}
