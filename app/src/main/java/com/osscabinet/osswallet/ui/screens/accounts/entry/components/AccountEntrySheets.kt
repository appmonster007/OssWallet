package com.osscabinet.ossWallet.ui.screens.accounts.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad
import com.osscabinet.ossWallet.ui.components.VisualIdentitySection
import com.osscabinet.ossWallet.ui.components.entry.EntryTypeSelector
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryEvent
import com.osscabinet.ossWallet.ui.screens.accounts.entry.AccountEntryState
import com.osscabinet.ossWallet.ui.screens.accounts.entry.BalanceUpdateMode
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.util.toColorInt
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEntrySheets(
    state: AccountEntryState,
    onEvent: (AccountEntryEvent) -> Unit
) {
    val accentColor = try {
        Color(state.accentColorHex.toColorInt())
    } catch (_: Exception) {
        AppColors.primary
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    if (state.showBalanceUpdateSheet && state.balanceUpdateCurrency != null) {
        val currency = state.balanceUpdateCurrency
        Dialog(
            onDismissRequest = { onEvent(AccountEntryEvent.CloseBalanceUpdate) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(32.dp),
                color = AppColors.panelSurface,
                tonalElevation = 12.dp
            ) {
                val currentBaseTotal = state.systemClosingBalances[currency] ?: BigDecimal.ZERO
                val modes = listOf(
                    Triple(Icons.Rounded.Add, "By Record", BalanceUpdateMode.RECORD),
                    Triple(Icons.Rounded.Refresh, "By Initial", BalanceUpdateMode.INITIAL_BALANCE)
                )
                val selectedColor = if (state.balanceUpdateMode == BalanceUpdateMode.RECORD)
                    AppColors.primary else AppColors.tertiary

                Column(modifier = Modifier.padding(vertical = 24.dp)) {
                    CalculatorNumpad(
                        amount = state.balanceUpdateInputAmount,
                        allowSigned = true,
                        onAmountChange = { onEvent(AccountEntryEvent.SetBalanceUpdateAmount(it)) },
                        onDone = { onEvent(AccountEntryEvent.ConfirmBalanceUpdate) },
                        header = { calcState, _, onHistory ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 1. Mode Selector
                                EntryTypeSelector(
                                    selectedType = state.balanceUpdateMode,
                                    onTypeSelected = { onEvent(AccountEntryEvent.SetBalanceUpdateMode(it)) },
                                    types = modes,
                                    selectedColor = selectedColor,
                                    itemWidth = 100.dp
                                )

                                // 2. Projection Card
                                Surface(
                                    shape = RoundedCornerShape(28.dp),
                                    color = AppColors.recessedSurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        AppColors.divider.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            IconButton(
                                                onClick = onHistory,
                                                modifier = Modifier.align(Alignment.TopStart).size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.History,
                                                    contentDescription = "History",
                                                    tint = AppColors.contentSecondary.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            
                                            Text(
                                                text = currency,
                                                style = AppTextStyles.overlineLabel,
                                                color = AppColors.contentSecondary,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }

                                        Spacer(modifier = Modifier.size(12.dp))

                                        // PROJECTED TOTAL
                                        val inputBD = calcState.display.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                        val isNeg = inputBD.signum() < 0
                                        
                                        Text(
                                            text = inputBD.abs().stripTrailingZeros().toPlainString(),
                                            style = AppTextStyles.heroAmount.copy(
                                                fontSize = 44.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = (-1.5).sp
                                            ),
                                            color = if (isNeg) AppColors.negative else selectedColor,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.size(16.dp))

                                        // CONTEXTUAL DELTA ROW
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(horizontalAlignment = Alignment.Start) {
                                                Text(
                                                    text = currentBaseTotal.stripTrailingZeros().toPlainString(),
                                                    style = AppTextStyles.chartTooltipValue.copy(fontSize = 13.sp),
                                                    color = AppColors.contentSecondary
                                                )
                                                Text(
                                                    text = "(Current)",
                                                    style = AppTextStyles.caption.copy(fontSize = 10.sp),
                                                    color = AppColors.contentSecondary.copy(alpha = 0.5f)
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = AppColors.divider.copy(alpha = 0.6f)
                                            )

                                            val delta = inputBD - currentBaseTotal
                                            val isGain = delta.signum() >= 0
                                            Column(horizontalAlignment = Alignment.End) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        if (isGain) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(11.dp),
                                                        tint = if (isGain) AppColors.positive else AppColors.negative
                                                    )
                                                    Spacer(Modifier.size(4.dp))
                                                    Text(
                                                        text = (if (isGain) "+" else "") + delta.stripTrailingZeros().toPlainString(),
                                                        style = AppTextStyles.chartTooltipValue.copy(fontSize = 13.sp),
                                                        color = if (isGain) AppColors.positive else AppColors.negative
                                                    )
                                                }
                                                Text(
                                                    text = "(Adjustment)",
                                                    style = AppTextStyles.caption.copy(fontSize = 10.sp),
                                                    color = AppColors.contentSecondary.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (state.showCalculator && state.activeBalanceCurrency != null && !state.showBalanceUpdateSheet) {
        val activeCurrency = state.activeBalanceCurrency
        ModalBottomSheet(
            onDismissRequest = { onEvent(AccountEntryEvent.CloseCalculator) },
            sheetState = sheetState,
            containerColor = AppColors.cardSurface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.divider.copy(alpha = 0.4f)) }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                CalculatorNumpad(
                    amount = state.initialBalances[activeCurrency] ?: "0",
                    allowSigned = true,
                    onAmountChange = { signedValue ->
                        onEvent(AccountEntryEvent.SetInitialBalance(activeCurrency, signedValue))
                    },
                    onDone = {
                        onEvent(AccountEntryEvent.CloseCalculator)
                    }
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
