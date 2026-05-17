package com.osscabinet.ossWallet.ui.screens.accounts.entry.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.osscabinet.ossWallet.ui.components.*
import com.osscabinet.ossWallet.ui.components.entry.*
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryEvent
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEntrySheets(
    state: AccountEntryState,
    onEvent: (AccountEntryEvent) -> Unit
) {
    if (state.showIconSheet) {
        Dialog(onDismissRequest = { onEvent(AccountEntryEvent.ShowIconSheet(show = false)) }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(vertical = 24.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = AppColors.panelSurface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(AeonTheme.dimensions.screenPadding)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXl)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Visual Identity",
                            style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { onEvent(AccountEntryEvent.ShowIconSheet(show = false)) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }

                    VisualIdentitySection(
                        selectedIconName = state.selectedIconName,
                        onIconSelected = { onEvent(AccountEntryEvent.SetIcon(it)) },
                        accentColorHex = state.accentColorHex,
                        onColorSelected = { onEvent(AccountEntryEvent.SetColor(it)) }
                    )
                }
            }
        }
    }

    if (state.showCalculator && state.activeBalanceCurrency != null) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(AccountEntryEvent.CloseCalculator) },
            containerColor = AppColors.cardSurface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.divider.copy(alpha = 0.4f)) }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                CalculatorNumpad(
                    amount = state.initialBalances[state.activeBalanceCurrency] ?: "0",
                    onAmountChange = { amount, _ -> 
                        onEvent(AccountEntryEvent.SetInitialBalance(state.activeBalanceCurrency, amount))
                    },
                    onDone = { onEvent(AccountEntryEvent.CloseCalculator) }
                )
            }
        }
    }

    if (state.showCurrencySheet) {
        AeonFullScreenDialog(onDismissRequest = { onEvent(AccountEntryEvent.ShowCurrencySheet(show = false)) }) {
            AeonSelectionScreen(
                title = "Currencies",
                onBack = { onEvent(AccountEntryEvent.ShowCurrencySheet(show = false)) }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(AeonTheme.dimensions.screenPadding)
                ) {
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
        }
    }
}
