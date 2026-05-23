package com.osscabinet.ossWallet.ui.screens.accounts.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad
import com.osscabinet.ossWallet.ui.components.VisualIdentitySection
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
        Dialog(
            onDismissRequest = { onEvent(AccountEntryEvent.ShowIconSheet(show = false)) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(32.dp),
                color = AppColors.panelSurface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Visual Identity",
                            style = AppTextStyles.heroAmount.copy(fontSize = 20.sp)
                        )
                        IconButton(
                            onClick = { onEvent(AccountEntryEvent.ShowIconSheet(show = false)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(20.dp),
                                tint = AppColors.contentSecondary
                            )
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
                        onEvent(
                            AccountEntryEvent.SetInitialBalance(
                                state.activeBalanceCurrency,
                                amount
                            )
                        )
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
        }
    }
}
