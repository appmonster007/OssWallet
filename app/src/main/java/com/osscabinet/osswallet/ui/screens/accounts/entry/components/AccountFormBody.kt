package com.osscabinet.ossWallet.ui.screens.accounts.entry.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
    modifier: Modifier = Modifier
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(state.accentColorHex))
    } catch (_: Exception) {
        AppColors.primary
    }
    var showTypeSelector by remember { mutableStateOf(false) }

    val types = AccountStructureType.entries.toTypedArray()
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = AeonTheme.dimensions.screenPadding,
                    vertical = AeonTheme.dimensions.spacingLg
                ),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
        ) {
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
            if (!state.isEditMode && state.allowedCurrencies.isNotEmpty()) {
                EntrySection(
                    title = "INITIAL BALANCES",
                    icon = Icons.Rounded.AccountBalanceWallet
                ) {
                    state.allowedCurrencies.forEach { currencyCode ->
                        val requester = remember { BringIntoViewRequester() }

                        EntryEditorialInput(
                            value = state.initialBalances[currencyCode] ?: "",
                            onValueChange = { /* Calculator handles this */ },
                            label = "BALANCE ($currencyCode)",
                            icon = Icons.Rounded.AccountBalanceWallet,
                            placeholder = "0.00",
                            iconColor = accentColor,
                            modifier = Modifier
                                .bringIntoViewRequester(requester)
                                .clickable { onEvent(AccountEntryEvent.OpenCalculator(currencyCode)) },
                            enabled = false
                        )

                        // Smart scroll when this currency becomes active in calculator
                        LaunchedEffect(state.showCalculator, state.activeBalanceCurrency) {
                            if (state.showCalculator && state.activeBalanceCurrency == currencyCode) {
                                requester.bringIntoView()
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacing2xl))
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
