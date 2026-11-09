package com.osscabinet.ossWallet.ui.screens.accounts.entry.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.osscabinet.ossWallet.ui.components.AeonConfirmDeleteDialog
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
import com.osscabinet.ossWallet.ui.theme.scaledDp
import com.osscabinet.ossWallet.ui.theme.scaledSp
import java.math.BigDecimal

@Composable
fun AccountEntrySheets(
    state: AccountEntryState,
    onEvent: (AccountEntryEvent) -> Unit
) {
    RemoveCurrencyDialog(state, onEvent)
    if (state.showIconSheet) VisualIdentityDialog(state, onEvent)

    val balanceCurrency = state.balanceUpdateCurrency
    if (state.showBalanceUpdateSheet && balanceCurrency != null) {
        BalanceUpdateDialog(balanceCurrency, state, onEvent)
    }

    val activeCurrency = state.activeBalanceCurrency
    if (state.showCalculator && activeCurrency != null && !state.showBalanceUpdateSheet) {
        OpeningBalanceCalculator(activeCurrency, state, onEvent)
    }

    if (state.showCurrencySheet) CurrencyDialog(state, onEvent)
}

// Removing a currency that still holds an opening balance needs confirmation first.
@Composable
private fun RemoveCurrencyDialog(state: AccountEntryState, onEvent: (AccountEntryEvent) -> Unit) {
    val removalWithBalance = state.pendingRemovalWithBalance.sorted().joinToString(", ")
    AeonConfirmDeleteDialog(
        show = removalWithBalance.isNotEmpty(),
        title = "Remove ${state.pendingCurrencyRemoval.sorted().joinToString(", ")}?",
        message = "The opening balance in $removalWithBalance will be deleted with the currency.",
        onConfirm = { onEvent(AccountEntryEvent.ConfirmCurrencyRemoval) },
        onDismiss = { onEvent(AccountEntryEvent.CancelCurrencyRemoval) },
        confirmButtonText = "Remove"
    )
}

@Composable
private fun VisualIdentityDialog(state: AccountEntryState, onEvent: (AccountEntryEvent) -> Unit) {
    val close = { onEvent(AccountEntryEvent.ShowIconSheet(show = false)) }
    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(32.dp),
            color = AppColors.cardSurface,
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
                    Text(text = "Visual Identity", style = AppTextStyles.heroAmount.copy(fontSize = 20.sp))
                    IconButton(onClick = close, modifier = Modifier.size(32.dp)) {
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

/** Sets the balance of one currency, either as a recorded adjustment or by changing the opening balance. */
@Composable
private fun BalanceUpdateDialog(
    currency: String,
    state: AccountEntryState,
    onEvent: (AccountEntryEvent) -> Unit
) {
    val currentTotal = state.systemClosingBalances[currency] ?: BigDecimal.ZERO
    // Before the account exists there is no history to record against, so only "By Initial" applies.
    val modes = listOfNotNull(
        Triple(Icons.Rounded.Add, "By Record", BalanceUpdateMode.RECORD).takeIf { state.accountId != null },
        Triple(Icons.Rounded.Refresh, "By Initial", BalanceUpdateMode.INITIAL_BALANCE)
    )
    val selectedColor =
        if (state.balanceUpdateMode == BalanceUpdateMode.RECORD) AppColors.primary else AppColors.tertiary

    Dialog(
        onDismissRequest = { onEvent(AccountEntryEvent.CloseBalanceUpdate) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(32.dp),
            color = AppColors.cardSurface,
            tonalElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                CalculatorNumpad(
                    amount = state.balanceUpdateInputAmount,
                    allowSigned = true,
                    onAmountChange = { onEvent(AccountEntryEvent.SetBalanceUpdateAmount(it)) },
                    onDone = { onEvent(AccountEntryEvent.ConfirmBalanceUpdate) },
                    header = { calcState, onSave, onHistory ->
                        BalanceUpdateHeader(
                            currency = currency,
                            currentTotal = currentTotal,
                            enteredTotal = calcState.display.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            modes = modes,
                            selectedMode = state.balanceUpdateMode,
                            selectedColor = selectedColor,
                            onModeSelected = { onEvent(AccountEntryEvent.SetBalanceUpdateMode(it)) },
                            onSave = onSave,
                            onHistory = onHistory
                        )
                    }
                )
            }
        }
    }
}

/** Mode selector plus the projection card: entered total, current total and the adjustment between them. */
@Composable
private fun BalanceUpdateHeader(
    currency: String,
    currentTotal: BigDecimal,
    enteredTotal: BigDecimal,
    modes: List<Triple<ImageVector, String, BalanceUpdateMode>>,
    selectedMode: BalanceUpdateMode,
    selectedColor: Color,
    onModeSelected: (BalanceUpdateMode) -> Unit,
    onSave: () -> Unit,
    onHistory: () -> Unit
) {
    val scale = AeonTheme.uiScale
    val cardCorner = scaledDp(28f, scale, min = 20f)
    val cardHPad = scaledDp(20f, scale, min = 14f)
    val cardVPad = scaledDp(24f, scale, min = 16f)
    val iconButtonSize = scaledDp(24f, scale, min = 18f)
    val historyIconSize = scaledDp(18f, scale, min = 13f)
    val confirmIconSize = scaledDp(20f, scale, min = 15f)
    val amountFontSize = scaledSp(44f, scale, min = 33f)
    val deltaFontSize = scaledSp(13f, scale, min = 10f)
    val captionFontSize = scaledSp(10f, scale, min = 9f)
    val arrowIconSize = scaledDp(14f, scale, min = 11f)
    val trendIconSize = scaledDp(11f, scale, min = 9f)
    val afterHeaderSpacer = scaledDp(12f, scale, min = 8f)
    val afterAmountSpacer = scaledDp(16f, scale, min = 10f)
    val columnSpacing = scaledDp(20f, scale, min = 14f)

    val delta = enteredTotal - currentTotal
    val isGain = delta.signum() >= 0
    val deltaColor = if (isGain) AppColors.positive else AppColors.negative
    val captionStyle = AppTextStyles.caption.copy(fontSize = captionFontSize)
    val captionColor = AppColors.contentSecondary.copy(alpha = 0.5f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(columnSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EntryTypeSelector(
            selectedType = selectedMode,
            onTypeSelected = onModeSelected,
            types = modes,
            selectedColor = selectedColor,
            itemWidth = 100.dp
        )

        Surface(
            shape = RoundedCornerShape(cardCorner),
            color = AppColors.recessedSurface,
            border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = cardHPad, vertical = cardVPad),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onHistory,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(iconButtonSize)
                    ) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = "History",
                            tint = AppColors.contentSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(historyIconSize)
                        )
                    }
                    Text(
                        text = currency,
                        style = AppTextStyles.overlineLabel,
                        color = AppColors.contentSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick = onSave,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(iconButtonSize)
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Confirm",
                            tint = selectedColor,
                            modifier = Modifier.size(confirmIconSize)
                        )
                    }
                }

                Spacer(modifier = Modifier.size(afterHeaderSpacer))

                Text(
                    text = enteredTotal.abs().stripTrailingZeros().toPlainString(),
                    style = AppTextStyles.heroAmount.copy(
                        fontSize = amountFontSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.5).sp
                    ),
                    color = if (enteredTotal.signum() < 0) AppColors.negative else selectedColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.size(afterAmountSpacer))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = currentTotal.stripTrailingZeros().toPlainString(),
                            style = AppTextStyles.chartTooltipValue.copy(fontSize = deltaFontSize),
                            color = AppColors.contentSecondary
                        )
                        Text(text = "(Current)", style = captionStyle, color = captionColor)
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(arrowIconSize),
                        tint = AppColors.divider.copy(alpha = 0.6f)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isGain) Icons.AutoMirrored.Rounded.TrendingUp
                                else Icons.AutoMirrored.Rounded.TrendingDown,
                                contentDescription = null,
                                modifier = Modifier.size(trendIconSize),
                                tint = deltaColor
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = (if (isGain) "+" else "") + delta.stripTrailingZeros().toPlainString(),
                                style = AppTextStyles.chartTooltipValue.copy(fontSize = deltaFontSize),
                                color = deltaColor
                            )
                        }
                        Text(text = "(Adjustment)", style = captionStyle, color = captionColor)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpeningBalanceCalculator(
    currency: String,
    state: AccountEntryState,
    onEvent: (AccountEntryEvent) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { onEvent(AccountEntryEvent.CloseCalculator) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                amount = state.initialBalances[currency] ?: "0",
                allowSigned = true,
                onAmountChange = { onEvent(AccountEntryEvent.SetInitialBalance(currency, it)) },
                onDone = { onEvent(AccountEntryEvent.CloseCalculator) }
            )
        }
    }
}

@Composable
private fun CurrencyDialog(state: AccountEntryState, onEvent: (AccountEntryEvent) -> Unit) {
    val close = { onEvent(AccountEntryEvent.ShowCurrencySheet(show = false)) }
    AeonFullScreenDialog(onDismissRequest = close) {
        AeonSelectionScreen(title = "Currencies", onBack = close) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(AeonTheme.dimensions.screenPadding)
            ) {
                CurrencyManagementSection(
                    primaryCurrency = state.primaryCurrency,
                    onPrimaryCurrencyChange = { onEvent(AccountEntryEvent.SetPrimaryCurrency(it)) },
                    allowedCurrencies = state.displayedCurrencies,
                    onAllowedCurrenciesChange = { onEvent(AccountEntryEvent.SetAllowedCurrencies(it)) },
                    accentColorHex = state.accentColorHex,
                    isEditMode = state.isEditMode,
                    currencyUsageMap = state.currencyUsageMap,
                    shortlistedCurrencies = state.shortlistedCurrencies
                )
            }
        }
    }
}
