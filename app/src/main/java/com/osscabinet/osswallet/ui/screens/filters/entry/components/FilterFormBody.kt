package com.osscabinet.ossWallet.ui.screens.filters.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.AeonTimelinePeriodSelector
import com.osscabinet.ossWallet.ui.components.entry.AeonAmountData
import com.osscabinet.ossWallet.ui.components.entry.AeonContextGrid
import com.osscabinet.ossWallet.ui.components.entry.AeonImpactHeaderCard
import com.osscabinet.ossWallet.ui.components.entry.AeonTextFieldState
import com.osscabinet.ossWallet.ui.components.entry.EntryActionCard
import com.osscabinet.ossWallet.ui.components.entry.EntryField
import com.osscabinet.ossWallet.ui.components.entry.EntryActionRow
import com.osscabinet.ossWallet.ui.components.entry.EntryEditorialInput
import com.osscabinet.ossWallet.ui.components.entry.EntrySection
import com.osscabinet.ossWallet.ui.components.entry.EntrySwitchRow
import com.osscabinet.ossWallet.ui.components.entry.EntryTagSection
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryEvent
import com.osscabinet.ossWallet.ui.screens.filters.entry.FilterEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.flow.flowOf

/**
 * Filter Entry Form Body.
 * Standardized with Budget, Loan, and Template entry screens following the "Architect Slate" design.
 * Clear separation between Core Criteria and Advanced Search.
 */
@Composable
fun FilterFormBody(
    state: FilterEntryState,
    onSearch: (String) -> kotlinx.coroutines.flow.Flow<List<Transaction>>,
    onEvent: (FilterEntryEvent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    val dims = AeonTheme.dimensions
    Box(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dims.screenPadding),
            verticalArrangement = Arrangement.spacedBy(dims.spacingLg)
        ) {
            Spacer(Modifier.height(contentPadding.calculateTopPadding()))
            // IMPACT: Header card for the Filter Name
            AeonImpactHeaderCard(
                transactionType = TransactionType.EXPENSE,
                onTypeSelected = { },
                source = AeonAmountData(
                    value = "",
                    currency = "",
                    color = AppColors.primary
                ),
                nameState = AeonTextFieldState(
                    value = state.name,
                    onValueChange = { onEvent(FilterEntryEvent.SetName(it)) },
                    label = "FILTER NAME",
                    isError = state.errors.contains(EntryField.NAME)
                ),
                showTypeSelector = false,
                showAmount = false,
                errors = state.errors
            )

            // CORE CRITERIA: Primary structural filter logic
            EntrySection(title = "CORE CRITERIA", icon = Icons.Rounded.Tune) {
                // Default Period for this filter
                AeonTimelinePeriodSelector(
                    currentRange = state.timelineRange,
                    onRangeSelected = { onEvent(FilterEntryEvent.SetTimelineRange(it)) }
                )
            }

            // PRIMARY FILTERS: The context grid matching FilterFields logic
            AeonContextGrid {
                // Flows & Accounts selection
                EntryActionRow {
                    EntryActionCard(
                        icon = Icons.AutoMirrored.Rounded.ShowChart,
                        label = "FLOWS",
                        value = if (state.selectedTransactionTypes.size >= 2 || state.selectedTransactionTypes.isEmpty()) "All Flows" else state.selectedTransactionTypes.first().displayName,
                        onClick = { onEvent(FilterEntryEvent.ShowTypeSheet(show = true)) },
                        modifier = Modifier.weight(1f)
                    )

                    val activeAccountIds = state.accounts.filter { !it.isArchived }.map { it.id }.toSet()
                    val accountObj =
                        if (state.selectedAccountIds.size == 1) state.accounts.find { it.id == state.selectedAccountIds.first() } else null
                    val accountLabel = when {
                        state.selectedAccountIds.isEmpty() ||
                            state.selectedAccountIds == activeAccountIds -> "All Active"
                        state.selectedAccountIds.size == state.accounts.size -> "All Accounts"
                        accountObj != null -> accountObj.name
                        else -> "${state.selectedAccountIds.size} Accounts"
                    }
                    EntryActionCard(
                        icon = getAccountIconByName(accountObj?.icon),
                        label = "ACCOUNTS",
                        value = accountLabel,
                        onClick = { onEvent(FilterEntryEvent.ShowAccountSheet(show = true)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Categories & Currencies selection
                EntryActionRow {
                    val categoryObj =
                        if (state.selectedCategoryIds.size == 1) state.categories.find { it.id == state.selectedCategoryIds.first() } else null
                    EntryActionCard(
                        icon = categoryObj?.let { getIconByName(it.iconName) }
                            ?: Icons.Rounded.Category,
                        label = "CATEGORIES",
                        value = when {
                            state.selectedCategoryIds.isEmpty() || (state.categories.isNotEmpty() && state.selectedCategoryIds.size == state.categories.size) -> "All Categories"
                            categoryObj != null -> categoryObj.name
                            else -> "${state.selectedCategoryIds.size} Selected"
                        },
                        onClick = { onEvent(FilterEntryEvent.ShowCategorySheet(show = true)) },
                        modifier = Modifier.weight(1f)
                    )

                    EntryActionCard(
                        icon = Icons.Rounded.CurrencyExchange,
                        label = "CURRENCIES",
                        value = if (state.selectedCurrencies.isEmpty()) "All Currencies" else if (state.selectedCurrencies.size == 1) state.selectedCurrencies.first() else "${state.selectedCurrencies.size} Selected",
                        onClick = { onEvent(FilterEntryEvent.ShowCurrencySheet(show = true)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Status & Methods selection
                EntryActionRow {
                    EntryActionCard(
                        icon = Icons.AutoMirrored.Rounded.FactCheck,
                        label = "STATUS",
                        value = if (state.selectedStatuses.isEmpty() || state.selectedStatuses.size == TransactionStatus.entries.size) "All Statuses" else "${state.selectedStatuses.size} Selected",
                        onClick = { onEvent(FilterEntryEvent.ShowStatusSheet(show = true)) },
                        modifier = Modifier.weight(1f),
                        iconContainerColor = AppColors.formIconContainerSecondary,
                        iconTint = AppColors.onSecondaryContainer
                    )

                    EntryActionCard(
                        icon = Icons.Rounded.Payments,
                        label = "METHODS",
                        value = if (state.selectedPaymentMethods.isEmpty() || state.selectedPaymentMethods.size == PaymentMethod.entries.size) "Any Method" else "${state.selectedPaymentMethods.size} Selected",
                        onClick = { onEvent(FilterEntryEvent.ShowPaymentMethodSheet(show = true)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // SEARCH & TAGS: Detailed text queries
            EntrySection(title = "SEARCH & TAGS", icon = Icons.Rounded.Search) {
                Column(verticalArrangement = Arrangement.spacedBy(dims.spacingLg)) {
                    // Note and Payee Queries with Regex support
                    Column(verticalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
                        // Note Query
                        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingSm)) {
                            EntrySwitchRow(
                                label = "Regex for Note",
                                checked = state.isNoteRegex,
                                onCheckedChange = { onEvent(FilterEntryEvent.SetNoteRegex(it)) },
                                icon = Icons.Rounded.Code,
                                subtitle = "Matches notes with regex",
                            )
                            EntryEditorialInput(
                                value = state.queryNote,
                                onValueChange = { onEvent(FilterEntryEvent.SetQueryNote(it)) },
                                label = "NOTE QUERY",
                                icon = Icons.AutoMirrored.Rounded.Notes,
                                placeholder = if (state.isNoteRegex) "/pattern/i" else "Search note...",
                                isSelected = state.queryNote.isNotEmpty()
                            )
                        }

                        // Payee Query
                        Column(verticalArrangement = Arrangement.spacedBy(dims.spacingSm)) {
                            EntrySwitchRow(
                                label = "Regex for Payee",
                                checked = state.isPayeeRegex,
                                onCheckedChange = { onEvent(FilterEntryEvent.SetPayeeRegex(it)) },
                                icon = Icons.Rounded.Code,
                                subtitle = "Matches payees with regex",
                            )
                            EntryEditorialInput(
                                value = state.queryPayee,
                                onValueChange = { onEvent(FilterEntryEvent.SetQueryPayee(it)) },
                                label = "PAYEE QUERY",
                                icon = Icons.Rounded.Person,
                                placeholder = if (state.isPayeeRegex) "/pattern/i" else "Search payee...",
                                isSelected = state.queryPayee.isNotEmpty()
                            )
                        }
                    }

                    // Multi-select Tags
                    EntryTagSection(
                        selectedNames = state.selectedTags,
                        allAvailableTags = state.availableTags,
                        onShowTagSheet = { onEvent(FilterEntryEvent.ShowTagSheet(show = true)) },
                        onRemoveTag = { tag -> onEvent(FilterEntryEvent.SetTags(state.selectedTags - tag)) },
                        onTagsSelected = { onEvent(FilterEntryEvent.SetTags(it)) },
                        onAddTag = { name, _ -> onEvent(FilterEntryEvent.SetTags(state.selectedTags + name)) }
                    )
                }
            }

            // AMOUNT & RECORDS: Numerical constraints and links
            EntrySection(title = "AMOUNT & RECORDS", icon = Icons.Rounded.AttachMoney) {
                Column(verticalArrangement = Arrangement.spacedBy(dims.spacingLg)) {
                    AeonContextGrid {
                        EntryActionRow {
                            EntryActionCard(
                                icon = Icons.Rounded.ArrowDownward,
                                label = "MIN AMOUNT",
                                value = state.minAmount.ifEmpty { "Any" },
                                onClick = { onEvent(FilterEntryEvent.ShowMinAmountSheet(show = true)) },
                                modifier = Modifier.weight(1f)
                            )
                            EntryActionCard(
                                icon = Icons.Rounded.ArrowUpward,
                                label = "MAX AMOUNT",
                                value = state.maxAmount.ifEmpty { "Any" },
                                onClick = { onEvent(FilterEntryEvent.ShowMaxAmountSheet(show = true)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    EntryActionCard(
                        icon = Icons.Rounded.Link,
                        label = "LINKED RECORDS",
                        value = if (state.linkedTransactionIds.isEmpty()) "None Linked" else "${state.linkedTransactionIds.size} Linked",
                        onClick = { onEvent(FilterEntryEvent.ShowLinkSheet(show = true)) },
                        modifier = Modifier.fillMaxWidth(),
                        iconContainerColor = AppColors.formIconContainerTertiary,
                        iconTint = AppColors.onTertiaryContainer
                    )
                }
            }

            // WARRANTY: Warranty status and dates
            EntrySection(title = "WARRANTY", icon = Icons.Rounded.VerifiedUser) {
                Column(verticalArrangement = Arrangement.spacedBy(dims.spacingLg)) {
                    EntryActionCard(
                        icon = Icons.Rounded.Verified,
                        label = "PROTECTION STATUS",
                        value = when (state.hasWarranty) {
                            null -> "Any Status"
                            true -> "Has Warranty"
                            false -> "No Warranty"
                        },
                        onClick = {
                            onEvent(
                                FilterEntryEvent.SetHasWarranty(
                                    when (state.hasWarranty) {
                                        null -> true
                                        true -> false
                                        false -> null
                                    }
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (state.hasWarranty == true) {
                        val formatter = remember {
                            java.text.SimpleDateFormat(
                                "MMM dd, yyyy",
                                java.util.Locale.getDefault()
                            )
                        }
                        AeonContextGrid {
                            EntryActionRow {
                                EntryActionCard(
                                    icon = Icons.Rounded.CalendarToday,
                                    label = "WARRANTY START",
                                    value = state.warrantyExpirationStart?.let {
                                        formatter.format(
                                            java.util.Date(it)
                                        )
                                    } ?: "Select",
                                    onClick = {
                                        onEvent(
                                            FilterEntryEvent.ShowWarrantyStartPicker(
                                                show = true
                                            )
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                EntryActionCard(
                                    icon = Icons.Rounded.CalendarToday,
                                    label = "WARRANTY END",
                                    value = state.warrantyExpirationEnd?.let {
                                        formatter.format(
                                            java.util.Date(
                                                it
                                            )
                                        )
                                    } ?: "Select",
                                    onClick = { onEvent(FilterEntryEvent.ShowWarrantyEndPicker(show = true)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
        }

        FilterEntrySheets(state = state, onSearch = onSearch, onEvent = onEvent)
    }
}

@Preview(showBackground = true)
@Composable
fun FilterFormBodyPreview() {
    OssWalletTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            FilterFormBody(
                state = FilterEntryState(
                    name = "Recent Groceries"
                ),
                onSearch = { flowOf(emptyList()) },
                onEvent = {}
            )
        }
    }
}
