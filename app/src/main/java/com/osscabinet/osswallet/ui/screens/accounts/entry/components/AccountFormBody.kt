package com.osscabinet.ossWallet.ui.screens.accounts.entry.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryState
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryEvent
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Composable
fun AccountFormBody(
    state: AccountEntryState,
    onEvent: (AccountEntryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = try { Color(android.graphics.Color.parseColor(state.accentColorHex)) } catch (_: Exception) { AppColors.primary }
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
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
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
                        onAllowedCurrenciesChange = { onEvent(AccountEntryEvent.SetAllowedCurrencies(it)) },
                        accentColorHex = state.accentColorHex,
                        isEditMode = state.isEditMode,
                        currencyUsageMap = state.currencyUsageMap
                    )
                }
            }

            // DETAIL: Initial balances with Calculator integration
            if (!state.isEditMode && state.allowedCurrencies.isNotEmpty()) {
                EntrySection(title = "INITIAL BALANCES", icon = Icons.Rounded.AccountBalanceWallet) {
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


@Preview(showBackground = true, widthDp = 400, heightDp = 2500, name = "Account Entry Screen - Create")
@Composable
fun AccountEntryCreatePreview() {
    OssWalletTheme {
        Surface {
            AccountFormBody(
                state = AccountEntryState(
                    accountName = "Global Savings",
                    structureType = AccountStructureType.SAVINGS,
                    primaryCurrency = "USD",
                    allowedCurrencies = setOf("USD", "EUR", "GBP"),
                    initialBalances = mapOf("USD" to "1000.00", "EUR" to "500.00", "GBP" to "250.00"),
                    selectedIconName = "Savings",
                    accentColorHex = "#004D5F",
                    isEditMode = false
                ),
                onEvent = {}
            )
        }
    }
}


