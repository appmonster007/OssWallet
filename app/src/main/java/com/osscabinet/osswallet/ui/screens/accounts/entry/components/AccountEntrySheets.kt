package com.osscabinet.ossWallet.ui.screens.accounts.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.osscabinet.ossWallet.ui.components.entry.EntryActionChip
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryEvent
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryState
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import java.math.BigDecimal

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
        val activeCurrency = state.activeBalanceCurrency
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
                    amount = state.initialBalances[activeCurrency]?.let {
                        it.toBigDecimalOrNull()?.abs()?.stripTrailingZeros()?.toPlainString() ?: "0"
                    } ?: "0",
                    onAmountChange = { magnitude ->
                        val currentSigned = state.initialBalances[activeCurrency]?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        val isNegative = currentSigned.signum() < 0
                        val newSigned = if (isNegative && magnitude.isNotEmpty() && magnitude != "0") "-$magnitude" else magnitude
                        onEvent(AccountEntryEvent.SetInitialBalance(activeCurrency, newSigned))
                    },
                    onDone = {
                        val newSigned = state.initialBalances[activeCurrency] ?: ""
                        val origSigned = state.calculatorOriginalValue ?: ""
                        val newBD = newSigned.toBigDecimalOrNull()
                        val origBD = origSigned.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        if (state.isEditMode && newBD != null && newBD.compareTo(origBD) != 0) {
                            onEvent(AccountEntryEvent.ShowAdjustmentDialog(activeCurrency, newSigned))
                        } else {
                            onEvent(AccountEntryEvent.CloseCalculator)
                        }
                    }
                )
            }
        }
    }

    if (state.showAdjustmentDialog && state.adjustmentDialogCurrency != null) {
        val currency = state.adjustmentDialogCurrency
        val origBD = state.calculatorOriginalValue?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val newBD = state.adjustmentDialogNewValue?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val delta = newBD - origBD
        val isInflow = delta.signum() > 0
        val deltaStr = "${if (isInflow) "+" else ""}${delta.stripTrailingZeros().toPlainString()} $currency"

        ModalBottomSheet(
            onDismissRequest = { onEvent(AccountEntryEvent.DismissAdjustmentDialog) },
            containerColor = AppColors.cardSurface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.divider.copy(alpha = 0.4f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
            ) {
                Text(
                    text = "Apply balance change ($deltaStr)",
                    style = AppTextStyles.cardSectionTitle,
                    modifier = Modifier.padding(bottom = AeonTheme.dimensions.spacingXs)
                )

                HorizontalDivider(color = AppColors.divider.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(AeonTheme.dimensions.spacingXs))

                EntryActionChip(
                    icon = if (isInflow) Icons.Rounded.CallReceived else Icons.Rounded.CallMade,
                    label = "Add Adjustment Transaction",
                    onClick = {
                        onEvent(AccountEntryEvent.ConfirmAsNewTransaction(
                            currency = currency,
                            originalValue = state.calculatorOriginalValue ?: "",
                            newValue = state.adjustmentDialogNewValue ?: ""
                        ))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    iconTint = if (isInflow) AppColors.positive else AppColors.negative,
                    iconContainerColor = if (isInflow) AppColors.positive.copy(alpha = 0.1f)
                                         else AppColors.negative.copy(alpha = 0.1f)
                )

                EntryActionChip(
                    icon = Icons.Rounded.EditNote,
                    label = "Update Opening Balance",
                    onClick = {
                        onEvent(AccountEntryEvent.ConfirmAsInitialBalanceUpdate(
                            currency = currency,
                            newValue = state.adjustmentDialogNewValue ?: ""
                        ))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    iconTint = AppColors.primary,
                    iconContainerColor = AppColors.primary.copy(alpha = 0.1f)
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
