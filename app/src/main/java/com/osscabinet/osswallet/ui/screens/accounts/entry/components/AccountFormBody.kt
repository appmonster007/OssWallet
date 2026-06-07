package com.osscabinet.ossWallet.ui.screens.accounts.entry.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import java.math.BigDecimal
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonHeroHeader
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.entry.EntryEditorialInput
import com.osscabinet.ossWallet.ui.components.entry.EntrySection
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryEvent
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.color
import com.osscabinet.ossWallet.ui.util.icon

@Composable
fun AccountFormBody(
    state: AccountEntryState,
    onEvent: (AccountEntryEvent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    onDeleteClick: (() -> Unit)? = null
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(state.accentColorHex))
    } catch (_: Exception) {
        AppColors.primary
    }
    var showTypeSelector by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val types = AccountStructureType.entries.toTypedArray()
    val scrollState = rememberScrollState()

    if (showDeleteConfirm) {
        AeonConfirmDeleteDialog(
            show = true,
            title = "Delete Account?",
            message = "This will permanently delete this account and all its transactions. This action cannot be undone.",
            onConfirm = {
                showDeleteConfirm = false
                onDeleteClick?.invoke()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    Box(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = AeonTheme.dimensions.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
        ) {
            Spacer(Modifier.height(contentPadding.calculateTopPadding()))
            // IMPACT: Interactive Hero Identity
            AeonHeroHeader(
                title = state.accountName,
                subtitle = null,
                iconName = state.selectedIconName,
                accentColorHex = state.accentColorHex,
                isLeftAligned = true,
                onTitleChange = { onEvent(AccountEntryEvent.SetName(it)) },
                onIconClick = { onEvent(AccountEntryEvent.ShowIconSheet(show = true)) }
            )

            // CONTEXT: Portfolio Architecture (Consolidated)
            EntrySection(title = "PORTFOLIO ARCHITECTURE", icon = Icons.Rounded.AccountTree) {
                Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
                    // Structure Type Dropdown
                    Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
                        Text(
                            text = "STRUCTURE TYPE",
                            style = AppTextStyles.sectionOverline,
                            color = AppColors.contentSecondary.copy(alpha = 0.5f)
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTypeSelector = true },
                            shape = RoundedCornerShape(24.dp),
                            color = AppColors.recessedSurface,
                            border = BorderStroke(0.5.dp, AppColors.divider.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val currentType = state.structureType
                                Surface(
                                    shape = CircleShape,
                                    color = currentType.color.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = currentType.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = currentType.color
                                        )
                                    }
                                }

                                Text(
                                    text = currentType.displayName,
                                    style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Select Type",
                                    tint = AppColors.contentSecondary
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = AeonTheme.dimensions.spacingSm),
                        thickness = (AeonTheme.dimensions.spacingXs / 8),
                        color = AppColors.divider.copy(alpha = 0.15f)
                    )

                    // Asset Management (Currencies)
                    CurrencyManagementSection(
                        primaryCurrency = state.primaryCurrency,
                        onPrimaryCurrencyChange = { onEvent(AccountEntryEvent.SetPrimaryCurrency(it)) },
                        allowedCurrencies = state.allowedCurrencies,
                        onAllowedCurrenciesChange = {
                            onEvent(
                                AccountEntryEvent.SetAllowedCurrencies(
                                    it
                                )
                            )
                        },
                        accentColorHex = state.accentColorHex,
                        isEditMode = state.isEditMode,
                        currencyUsageMap = state.currencyUsageMap,
                        shortlistedCurrencies = state.shortlistedCurrencies
                    )
                }
            }

            // DETAIL: Initial balances with Calculator integration
            if (state.allowedCurrencies.isNotEmpty()) {
                EntrySection(
                    title = if (state.isEditMode) "ACCOUNT BALANCES" else "INITIAL BALANCES",
                    icon = Icons.Rounded.AccountBalanceWallet
                ) {
                    state.allowedCurrencies.forEach { currencyCode ->
                        val requester = remember { BringIntoViewRequester() }
                        val isInflow = (state.initialBalances[currencyCode]?.toBigDecimalOrNull() ?: BigDecimal.ZERO).signum() >= 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
                        ) {
                            EntryEditorialInput(
                                value = state.initialBalances[currencyCode]?.let {
                                    it.toBigDecimalOrNull()?.abs()?.stripTrailingZeros()?.toPlainString() ?: ""
                                } ?: "",
                                onValueChange = { /* Calculator handles this */ },
                                label = "BALANCE ($currencyCode)",
                                icon = Icons.Rounded.AccountBalanceWallet,
                                placeholder = "0.00",
                                iconColor = accentColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .bringIntoViewRequester(requester)
                                    .clickable { onEvent(AccountEntryEvent.OpenCalculator(currencyCode)) },
                                enabled = false
                            )

                            Surface(
                                onClick = { onEvent(AccountEntryEvent.ToggleInitialBalanceDirection(currencyCode)) },
                                shape = CircleShape,
                                color = if (isInflow) AppColors.positive.copy(alpha = 0.12f)
                                        else AppColors.negative.copy(alpha = 0.12f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isInflow) AppColors.positive.copy(alpha = 0.3f)
                                    else AppColors.negative.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isInflow) Icons.Rounded.CallReceived else Icons.Rounded.CallMade,
                                        contentDescription = if (isInflow) "Inflow" else "Outflow",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isInflow) AppColors.positive else AppColors.negative
                                    )
                                }
                            }
                        }

                        // Smart scroll when this currency becomes active in calculator
                        LaunchedEffect(state.showCalculator, state.activeBalanceCurrency) {
                            if (state.showCalculator && state.activeBalanceCurrency == currencyCode) {
                                requester.bringIntoView()
                            }
                        }
                    }
                }
            }

            // DANGER ZONE: Destruction Management
            if (state.isEditMode) {
                EntrySection(title = "DANGER ZONE", icon = Icons.Rounded.DeleteForever) {
                    Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                        Text(
                            text = "Deleting this account will permanently remove all associated transactions and data. This cannot be undone.",
                            style = AppTextStyles.caption,
                            color = AppColors.border
                        )

                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.error),
                            border = BorderStroke(1.dp, AppColors.error.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Delete Account", style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacing2xl))
            Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
        }

        // Selection Screens / Sheets (Kept only for fallback or specific sub-selections if needed)
        // In this hybrid mode, most sheets are unnecessary, but we'll keep the component for logic safety
        AccountEntrySheets(
            state = state,
            onEvent = onEvent
        )

        if (showTypeSelector) {
            AeonFullScreenDialog(onDismissRequest = { showTypeSelector = false }) {
                AeonSelectionScreen(
                    title = "Select Structure Type",
                    onBack = { showTypeSelector = false }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .padding(AeonTheme.dimensions.screenPadding)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                    ) {
                        AeonHybridSelector(
                            items = types.toList(),
                            mode = AeonSelectMode.SINGLE,
                            selectedIds = setOf(state.structureType),
                            onSelectionChange = { ids ->
                                ids.firstOrNull()?.let {
                                    onEvent(AccountEntryEvent.SetStructureType(it))
                                    showTypeSelector = false
                                }
                            },
                            idMapper = { it },
                            labelMapper = { it.displayName },
                            iconMapper = { it.icon },
                            colorMapper = { it.color }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountFormBodyPreview() {
    OssWalletTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AccountFormBody(
                state = AccountEntryState(
                    accountName = "Savings Account",
                    structureType = AccountStructureType.SAVINGS,
                    primaryCurrency = "USD",
                    allowedCurrencies = setOf("USD", "EUR"),
                    initialBalances = mapOf("USD" to "1000.00", "EUR" to "500.00"),
                    accentColorHex = "#004D5F"
                ),
                onEvent = {}
            )
        }
    }
}
