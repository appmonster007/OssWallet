package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.ripple
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import java.math.RoundingMode
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.TransactionEntryService
import com.osscabinet.ossWallet.ui.components.CalculatorNumpad
import com.osscabinet.ossWallet.ui.components.CurrencySelector
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import com.osscabinet.ossWallet.ui.components.getAccountIconByName
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.LocalAppFont
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.copyStyle
import com.osscabinet.ossWallet.ui.util.formatCurrency
import java.math.BigDecimal

@Composable
fun QuickAddAmountStep(
    amount: BigDecimal,
    amountOverride: BigDecimal? = null,
    template: Template?,
    category: Category?,
    categories: List<Category> = emptyList(),
    account: Account?,
    toAccount: Account? = null,
    accounts: List<Account> = emptyList(),
    accountBalance: BigDecimal,
    toAccountBalance: BigDecimal = BigDecimal.ZERO,
    entryMode: EntryMode,
    exchangeRates: ExchangeRateSnapshot,
    baseCurrency: String,
    selectedCurrency: String,
    selectedToCurrency: String = "",
    isAmountOverridden: Boolean = false,
    activeAmountSide: AmountEditingSide = AmountEditingSide.SOURCE,
    onCurrencySelected: (String, Boolean) -> Unit,
    onAmountChange: (BigDecimal) -> Unit,
    onSetAmountFocus: (AmountEditingSide) -> Unit = {},
    onToggleAmountOverride: () -> Unit = {},
    onSwapAccounts: () -> Unit = {},
    onAccountSelected: (String, Boolean) -> Unit = { _, _ -> },
    onCategorySelected: (Category) -> Unit = {},
    onToggleEntryMode: () -> Unit = {},
    onToggleTransfer: () -> Unit = {},
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencies = remember(account) {
        if (account == null || account.id.isBlank()) {
            CurrencyService.getAllCurrencies()
        } else {
            (listOf(account.currencyCode) + account.allowedCurrencies).distinct()
                .filter { it.isNotBlank() }
        }
    }

    val toCurrencies = remember(toAccount) {
        if (toAccount == null || toAccount.id.isBlank()) {
            CurrencyService.getAllCurrencies()
        } else {
            (listOf(toAccount.currencyCode) + toAccount.allowedCurrencies).distinct()
                .filter { it.isNotBlank() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Editorial Context Strip: [Icon] Template/Category · Account + Balance
        EditorialContextStrip(
            template = template,
            category = category,
            categories = categories,
            account = account,
            toAccount = toAccount,
            accounts = accounts,
            accountBalance = accountBalance,
            toAccountBalance = toAccountBalance,
            entryMode = entryMode,
            onSwapAccounts = onSwapAccounts,
            onAccountSelected = onAccountSelected,
            onCategorySelected = onCategorySelected,
            onToggleEntryMode = onToggleEntryMode,
            onToggleTransfer = onToggleTransfer,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        key(activeAmountSide, account?.id, toAccount?.id) {
            CalculatorNumpad(
                keyHeight = 48.dp,
                amount = if (activeAmountSide == AmountEditingSide.TARGET) {
                    (amountOverride ?: BigDecimal.ZERO).stripTrailingZeros().toPlainString()
                } else {
                    amount.stripTrailingZeros().toPlainString()
                },
                onAmountChange = {
                    val valBD = it.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    onAmountChange(valBD)
                },
                onDone = onDone,
                showDoneButton = false,
                modifier = Modifier.fillMaxWidth(),
                header = { state, _, _ ->
                    // LIVE EVALUATION: Use ghostResult if present (during expression typing)
                    val liveValueString = if (state.ghostResult.isNotEmpty()) state.ghostResult else state.display
                    val liveAmount = liveValueString.toBigDecimalOrNull() ?: BigDecimal.ZERO

                    val sync = if (entryMode == EntryMode.TRANSFER) {
                        TransactionEntryService.syncTransferState(
                            sourceAmount = if (activeAmountSide == AmountEditingSide.SOURCE) liveAmount else amount,
                            targetAmount = if (activeAmountSide == AmountEditingSide.TARGET) liveAmount else (amountOverride ?: BigDecimal.ZERO),
                            sourceCurrency = selectedCurrency,
                            targetCurrency = selectedToCurrency,
                            isFixed = isAmountOverridden,
                            activeSide = activeAmountSide,
                            exchangeRates = exchangeRates
                        )
                    } else {
                        TransactionEntryService.TransferSyncResult(liveAmount, BigDecimal.ZERO, BigDecimal.ONE)
                    }

                    AmountHeroPanel(
                        amount = state.display,
                        expression = state.displayExpression,
                        sourceAmount = sync.sourceAmount,
                        targetAmount = sync.targetAmount,
                        isOverriding = isAmountOverridden,
                        activeSide = activeAmountSide,
                        onToggleOverriding = onToggleAmountOverride,
                        onSetFocus = onSetAmountFocus,
                        currency = selectedCurrency.ifBlank { currencies.firstOrNull() ?: "USD" },
                        toCurrency = selectedToCurrency.ifBlank { toCurrencies.firstOrNull() ?: "" },
                        currencies = currencies,
                        toCurrencies = toCurrencies,
                        onCurrencyChange = { onCurrencySelected(it, true) },
                        onToCurrencyChange = { onCurrencySelected(it, false) },
                        accountBalance = accountBalance,
                        toAccountBalance = toAccountBalance,
                        accountCurrency = account?.currencyCode ?: "USD",
                        toAccountCurrency = toAccount?.currencyCode ?: "USD",
                        entryMode = entryMode,
                        exchangeRates = exchangeRates,
                        baseCurrency = baseCurrency
                    )
                }
            )
        }
    }
}

@Composable
private fun EditorialContextStrip(
    template: Template?,
    category: Category?,
    categories: List<Category>,
    account: Account?,
    toAccount: Account? = null,
    accounts: List<Account>,
    accountBalance: BigDecimal,
    toAccountBalance: BigDecimal = BigDecimal.ZERO,
    entryMode: EntryMode,
    onSwapAccounts: () -> Unit,
    onAccountSelected: (String, Boolean) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onToggleEntryMode: () -> Unit,
    onToggleTransfer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val displayCategory = template?.categoryId?.let { _ -> category } ?: category
    val fallbackAccent = AppColors.accent
    val categoryColor = remember(displayCategory?.colorHex) {
        displayCategory?.colorHex
            ?.let { Color(it or (0xFF shl 24)) }
            ?: fallbackAccent
    }
    val accountColor = remember(account?.colorHex) {
        account?.colorHex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
            ?: fallbackAccent
    }
    val toAccountColor = remember(toAccount?.colorHex) {
        toAccount?.colorHex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
            ?: fallbackAccent
    }

    val accountCurrency = account?.currencyCode ?: "USD"
    val toAccountCurrency = toAccount?.currencyCode ?: "USD"

    val categoryTitle = if (template != null) "TEMPLATE" else "CATEGORY"
    val categoryName = template?.name ?: category?.name ?: "Quick Entry"
    val categoryIcon = getIconByName(displayCategory?.iconName)

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        glassVariant = GlassVariant.Item,
        hazeStyle = GlassTokens.itemStyle()
            .copyStyle(bgColor = AppColors.recessedSurface, alpha = 0.4f),
        fallbackColor = AppColors.recessedSurface.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val pivotIcon = @Composable {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = if (entryMode == EntryMode.TRANSFER) "Swap accounts" else "Toggle mode",
                    tint = AppColors.accent,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(AppColors.accent.copy(alpha = 0.1f))
                        .combinedClickable(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (entryMode == EntryMode.TRANSFER) onSwapAccounts() else onToggleEntryMode()
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleTransfer()
                            }
                        )
                        .padding(4.dp)
                )
            }

            val primaryAccountSummary = @Composable { isEnd: Boolean ->
                AccountSummary(
                    name = account?.name ?: "No Account",
                    balance = accountBalance,
                    currency = accountCurrency,
                    color = accountColor,
                    icon = getAccountIconByName(account?.icon),
                    isEnd = isEnd,
                    accounts = accounts,
                    onAccountSelected = { onAccountSelected(it, true) },
                    modifier = Modifier.weight(1f)
                )
            }

            val mainCategorySummary = @Composable { isEnd: Boolean ->
                CategorySummary(
                    title = categoryTitle,
                    name = categoryName,
                    color = categoryColor,
                    icon = categoryIcon,
                    isEnd = isEnd,
                    categories = categories,
                    onCategorySelected = onCategorySelected,
                    modifier = Modifier.weight(1f)
                )
            }

            when (entryMode) {
                EntryMode.TRANSFER -> {
                    val isFromOutside = account?.id.isNullOrEmpty()
                    val isToOutside = toAccount?.id.isNullOrEmpty()

                    // From Account
                    AccountSummary(
                        name = if (isFromOutside) "Outside" else account?.name ?: "From",
                        balance = accountBalance,
                        currency = accountCurrency,
                        color = if (isFromOutside) AppColors.contentSecondary else accountColor,
                        icon = getAccountIconByName(account?.icon ?: "Public"),
                        isEnd = false,
                        accounts = accounts,
                        onAccountSelected = { onAccountSelected(it, true) },
                        modifier = Modifier.weight(1f)
                    )

                    pivotIcon()

                    // To Account
                    AccountSummary(
                        name = if (isToOutside) "Outside" else toAccount?.name ?: "To",
                        balance = toAccountBalance,
                        currency = toAccountCurrency,
                        color = if (isToOutside) AppColors.contentSecondary else toAccountColor,
                        icon = getAccountIconByName(toAccount?.icon ?: "Public"),
                        isEnd = true,
                        accounts = accounts,
                        onAccountSelected = { onAccountSelected(it, false) },
                        modifier = Modifier.weight(1f)
                    )
                }
                EntryMode.INCOME -> {
                    mainCategorySummary(false)
                    pivotIcon()
                    primaryAccountSummary(true)
                }
                EntryMode.EXPENSE -> {
                    primaryAccountSummary(false)
                    pivotIcon()
                    mainCategorySummary(true)
                }
            }
        }
    }
}

@Composable
private fun CategorySummary(
    title: String,
    name: String,
    color: Color,
    icon: ImageVector,
    isEnd: Boolean,
    categories: List<Category> = emptyList(),
    onCategorySelected: (Category) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isEnd) Arrangement.End else Arrangement.Start
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showMenu = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isEnd) {
                    SummaryIcon(icon, color)
                }

                Column(
                    horizontalAlignment = if (isEnd) Alignment.End else Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = AppTextStyles.sectionOverline.copy(fontSize = 7.sp),
                        color = AppColors.contentSecondary.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = name,
                        style = AppTextStyles.managementItemTitle.copy(fontSize = 11.sp),
                        color = AppColors.contentPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isEnd) {
                    SummaryIcon(icon, color)
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(AppColors.popupSurface),
                offset = DpOffset(x = if (isEnd) 32.dp else 0.dp, y = 4.dp)
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = category.name,
                                style = AppTextStyles.caption,
                                color = AppColors.contentPrimary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = getIconByName(category.iconName),
                                contentDescription = null,
                                tint = Color(category.colorHex or (0xFF shl 24)),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            onCategorySelected(category)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryIcon(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .aeonHairlineBorder(CircleShape, accent = color.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun AccountSummary(
    name: String,
    balance: BigDecimal,
    currency: String,
    color: Color,
    icon: ImageVector,
    isEnd: Boolean,
    accounts: List<Account> = emptyList(),
    onAccountSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isEnd) Arrangement.End else Arrangement.Start
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showMenu = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isEnd) {
                    SummaryIcon(icon, color)
                }

                Column(
                    horizontalAlignment = if (isEnd) Alignment.End else Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = name.uppercase(),
                        style = AppTextStyles.sectionOverline.copy(fontSize = 7.sp),
                        color = AppColors.contentSecondary.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (currency.isBlank()) "EXTERNAL" else formatCurrency(balance, currency),
                        style = AppTextStyles.caption.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = LocalAppFont.current.monoFamily
                        ),
                        color = AppColors.contentPrimary,
                        maxLines = 1
                    )
                }

                if (isEnd) {
                    SummaryIcon(icon, color)
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(AppColors.popupSurface),
                offset = DpOffset(x = if (isEnd) 32.dp else 0.dp, y = 4.dp)
            ) {
                val secondaryColor = AppColors.contentSecondary
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Outside of Wallet",
                            style = AppTextStyles.caption,
                            color = AppColors.contentPrimary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = getAccountIconByName("Public"),
                            contentDescription = null,
                            tint = secondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        onAccountSelected("")
                        showMenu = false
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.5.dp,
                    color = AppColors.divider.copy(alpha = 0.3f)
                )

                accounts.filter { it.id.isNotBlank() }.forEach { account ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = account.name,
                                style = AppTextStyles.caption,
                                color = AppColors.contentPrimary
                            )
                        },
                        leadingIcon = {
                            val accColor = remember(account.colorHex) {
                                runCatching { Color(android.graphics.Color.parseColor(account.colorHex)) }.getOrDefault(secondaryColor)
                            }
                            Icon(
                                imageVector = getAccountIconByName(account.icon),
                                contentDescription = null,
                                tint = accColor,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            onAccountSelected(account.id)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountHeroPanel(
    amount: String,
    expression: String,
    sourceAmount: BigDecimal,
    targetAmount: BigDecimal,
    isOverriding: Boolean = false,
    activeSide: AmountEditingSide = AmountEditingSide.SOURCE,
    onToggleOverriding: () -> Unit = {},
    onSetFocus: (AmountEditingSide) -> Unit = {},
    currency: String,
    toCurrency: String = "",
    currencies: List<String>,
    toCurrencies: List<String> = emptyList(),
    onCurrencyChange: (String) -> Unit,
    onToCurrencyChange: (String) -> Unit = {},
    accountBalance: BigDecimal,
    toAccountBalance: BigDecimal = BigDecimal.ZERO,
    accountCurrency: String,
    toAccountCurrency: String = "USD",
    entryMode: EntryMode,
    exchangeRates: ExchangeRateSnapshot,
    baseCurrency: String
) {
    val closingBalance = remember(
        accountBalance,
        sourceAmount,
        entryMode,
        currency,
        accountCurrency,
        exchangeRates,
        baseCurrency
    ) {
        val convertedInput = if (currency == accountCurrency) {
            sourceAmount
        } else {
            CurrencyService.convertAmount(
                amount = sourceAmount,
                fromCurrency = currency,
                toCurrency = accountCurrency,
                exchangeRates = exchangeRates
            )
        }

        if (entryMode == EntryMode.INCOME) accountBalance + convertedInput
        else accountBalance - convertedInput
    }

    val toClosingBalance = remember(
        toAccountBalance,
        targetAmount,
        entryMode,
        toCurrency,
        toAccountCurrency,
        exchangeRates
    ) {
        if (entryMode != EntryMode.TRANSFER) BigDecimal.ZERO
        else {
            val convertedInput = if (toCurrency == toAccountCurrency) {
                targetAmount
            } else {
                CurrencyService.convertAmount(
                    amount = targetAmount,
                    fromCurrency = toCurrency,
                    toCurrency = toAccountCurrency,
                    exchangeRates = exchangeRates
                )
            }
            toAccountBalance + convertedInput
        }
    }

    val marketRate = remember(currency, toCurrency, exchangeRates) {
        if (currency == toCurrency) BigDecimal.ONE
        else TransactionEntryService.getMarketRate(currency, toCurrency, exchangeRates)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.background, RoundedCornerShape(20.dp))
            .aeonHairlineBorder(RoundedCornerShape(20.dp))
    ) {
        // Hero Content
        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (activeSide == AmountEditingSide.TARGET) "DESTINATION AMOUNT" else "SOURCE AMOUNT",
                    style = AppTextStyles.sectionOverline.copy(fontSize = 8.sp),
                    color = AppColors.accent
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(AppColors.divider.copy(alpha = 0.3f))
                )
                if (entryMode == EntryMode.TRANSFER) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (isOverriding) "FIXED" else "AUTO",
                        style = AppTextStyles.sectionOverline.copy(fontSize = 8.sp),
                        color = if (isOverriding) AppColors.accent else AppColors.contentSecondary,
                        modifier = Modifier.clickable { onToggleOverriding() }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (entryMode == EntryMode.TRANSFER) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val isFromOutside = accountCurrency.isBlank()
                    val isToOutside = toAccountCurrency.isBlank()

                    // Source Leg
                    if (!isFromOutside) {
                        TransferLegRow(
                            label = "FROM",
                            amount = sourceAmount.toPlainString(),
                            currency = currency,
                            currencies = currencies,
                            isActive = activeSide == AmountEditingSide.SOURCE,
                            onCurrencyChange = onCurrencyChange,
                            onRowClick = { onSetFocus(AmountEditingSide.SOURCE) }
                        )
                    }

                    // Destination Leg
                    if (!isToOutside) {
                        TransferLegRow(
                            label = "TO",
                            amount = targetAmount.toPlainString(),
                            currency = toCurrency,
                            currencies = toCurrencies,
                            isActive = activeSide == AmountEditingSide.TARGET,
                            onCurrencyChange = onToCurrencyChange,
                            onRowClick = { onSetFocus(AmountEditingSide.TARGET) }
                        )
                    }
                }
            } else {
                // Standard single amount display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CurrencyPill(
                        currency = currency,
                        currencies = currencies,
                        onCurrencyChange = onCurrencyChange
                    )

                    AmountDisplay(amount = amount)
                }
            }

            // Expression line
            if (expression.isNotBlank() && expression != amount && expression != "0") {
                Text(
                    text = expression,
                    style = AppTextStyles.metaLabel.copy(
                        fontSize = 10.sp,
                        fontFamily = LocalAppFont.current.monoFamily
                    ),
                    color = AppColors.contentSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 1.dp, bottom = 8.dp)
                )
            } else {
                Spacer(Modifier.height(4.dp))
            }
        }


        // Footer
        val dividerColor = AppColors.divider.copy(alpha = 0.45f)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
        ) {
            drawLine(
                color = dividerColor,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = size.height,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(15f, 8f),
                    phase = 0f
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Today · Now",
                    style = AppTextStyles.metaLabel.copy(fontSize = 9.sp),
                    color = AppColors.contentSecondary
                )
                if (entryMode == EntryMode.TRANSFER && marketRate != BigDecimal.ONE) {
                    val sync = TransactionEntryService.syncTransferState(
                        sourceAmount = sourceAmount,
                        targetAmount = targetAmount,
                        sourceCurrency = currency,
                        targetCurrency = toCurrency,
                        isFixed = isOverriding,
                        activeSide = activeSide,
                        exchangeRates = exchangeRates
                    )
                    Text(
                        text = "Rate: 1 $currency = ${sync.impliedRate.toPlainString()} $toCurrency",
                        style = AppTextStyles.metaLabel.copy(
                            fontSize = 8.sp,
                            fontFamily = LocalAppFont.current.monoFamily
                        ),
                        color = AppColors.accent.copy(alpha = 0.7f)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (entryMode == EntryMode.TRANSFER) {
                    ClosingBalanceLabel("From: ${formatCurrency(closingBalance, accountCurrency)}")
                    ClosingBalanceLabel("To: ${formatCurrency(toClosingBalance, toAccountCurrency)}")
                } else {
                    ClosingBalanceLabel("Closing: ${formatCurrency(closingBalance, accountCurrency)}")
                }
            }
        }
    }
}

@Composable
private fun TransferLegRow(
    label: String,
    amount: String,
    currency: String,
    currencies: List<String>,
    isActive: Boolean,
    onCurrencyChange: (String) -> Unit,
    onRowClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onRowClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = label,
                style = AppTextStyles.sectionOverline.copy(fontSize = 7.sp),
                color = if (isActive) AppColors.accent else AppColors.contentSecondary.copy(alpha = 0.5f)
            )
            CurrencyPill(
                currency = currency,
                currencies = currencies,
                onCurrencyChange = onCurrencyChange
            )
        }

        AmountDisplay(
            amount = amount,
            style = if (isActive) AppTextStyles.heroAmountLarge.copy(
                fontFamily = LocalAppFont.current.displayFamily,
                fontWeight = FontWeight.Light,
                fontSize = 24.sp,
                lineHeight = 24.sp
            ) else AppTextStyles.heroAmountLarge.copy(
                fontFamily = LocalAppFont.current.displayFamily,
                fontWeight = FontWeight.Light,
                fontSize = 18.sp,
                lineHeight = 18.sp
            ),
            color = if (isActive) AppColors.contentPrimary else AppColors.contentSecondary.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun AmountDisplay(
    amount: String,
    style: TextStyle = AppTextStyles.heroAmountLarge.copy(
        fontFamily = LocalAppFont.current.displayFamily,
        fontWeight = FontWeight.Light,
        fontSize = 24.sp,
        lineHeight = 24.sp
    ),
    color: Color = AppColors.contentPrimary
) {
    val (whole, decimal) = remember(amount) {
        val parts = amount.split(".")
        if (parts.size == 2) {
            parts[0] + "." to parts[1]
        } else {
            amount + "." to "00"
        }
    }

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = whole,
            style = style,
            color = color
        )
        Text(
            text = decimal,
            style = style.copy(fontSize = (style.fontSize.value * 0.83f).sp),
            color = color.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun CurrencyPill(
    currency: String,
    currencies: List<String>,
    onCurrencyChange: (String) -> Unit
) {
    Box {
        if (currencies.size > 1) {
            CurrencySelector(
                selectedCurrency = currency,
                onCurrencySelected = onCurrencyChange,
                supportedCurrencies = currencies
            )
        } else {
            Text(
                text = currency,
                style = AppTextStyles.sectionOverline.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                ),
                color = AppColors.contentSecondary,
                modifier = Modifier
                    .background(
                        AppColors.recessedSurface.copy(alpha = 0.3f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}


@Composable
private fun ClosingBalanceLabel(text: String) {
    Text(
        text = text,
        style = AppTextStyles.metaLabel.copy(
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = LocalAppFont.current.monoFamily
        ),
        color = AppColors.accent.copy(alpha = 0.9f)
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickAddAmountStep() {
    val mockAccount = Account(
        "1",
        "Cash",
        "USD",
        AccountStructureType.CASH,
        "#4CAF50",
        "Payments",
        allowedCurrencies = listOf("USD", "EUR")
    )
    val mockCategory = Category(1, "Food", "Restaurant", -16737025, false, null)

    OssWalletTheme {
        Box(modifier = Modifier.background(AppColors.background)) {
            QuickAddAmountStep(
                amount = BigDecimal("123.45"),
                template = null,
                category = mockCategory,
                categories = listOf(mockCategory),
                account = mockAccount,
                accounts = listOf(mockAccount),
                accountBalance = BigDecimal("1500.00"),
                entryMode = EntryMode.EXPENSE,
                exchangeRates = ExchangeRateSnapshot.EMPTY,
                baseCurrency = "USD",
                selectedCurrency = "USD",
                onCurrencySelected = { _, _ -> },
                onAmountChange = {},
                onDone = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickAddTransferAmountStep() {
    val fromAccount = Account(
        "1",
        "Cash",
        "USD",
        AccountStructureType.CASH,
        "#4CAF50",
        "Payments",
        allowedCurrencies = listOf("USD", "EUR")
    )
    val toAccount = Account(
        "2",
        "Savings",
        "EUR",
        AccountStructureType.BANK,
        "#2196F3",
        "AccountBalance",
        allowedCurrencies = listOf("EUR", "USD")
    )

    OssWalletTheme {
        Box(modifier = Modifier.background(AppColors.background)) {
            QuickAddAmountStep(
                amount = BigDecimal("100.00"),
                template = null,
                category = null,
                categories = emptyList(),
                account = fromAccount,
                toAccount = toAccount,
                accounts = listOf(fromAccount, toAccount),
                accountBalance = BigDecimal("1500.00"),
                toAccountBalance = BigDecimal("500.00"),
                entryMode = EntryMode.TRANSFER,
                exchangeRates = ExchangeRateSnapshot.EMPTY,
                baseCurrency = "USD",
                selectedCurrency = "USD",
                selectedToCurrency = "EUR",
                onCurrencySelected = { _, _ -> },
                onAmountChange = {},
                onDone = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickAddFixedTransferAmountStep() {
    val fromAccount = Account(
        "1",
        "Cash",
        "USD",
        AccountStructureType.CASH,
        "#4CAF50",
        "Payments",
        allowedCurrencies = listOf("USD", "EUR")
    )
    val toAccount = Account(
        "2",
        "Savings",
        "EUR",
        AccountStructureType.BANK,
        "#2196F3",
        "AccountBalance",
        allowedCurrencies = listOf("EUR", "USD")
    )

    OssWalletTheme {
        Box(modifier = Modifier.background(AppColors.background)) {
            QuickAddAmountStep(
                amount = BigDecimal("100.00"),
                amountOverride = BigDecimal("85.50"),
                isAmountOverridden = true,
                template = null,
                category = null,
                categories = emptyList(),
                account = fromAccount,
                toAccount = toAccount,
                accounts = listOf(fromAccount, toAccount),
                accountBalance = BigDecimal("1500.00"),
                toAccountBalance = BigDecimal("500.00"),
                entryMode = EntryMode.TRANSFER,
                exchangeRates = ExchangeRateSnapshot.EMPTY,
                baseCurrency = "USD",
                selectedCurrency = "USD",
                selectedToCurrency = "EUR",
                onCurrencySelected = { _, _ -> },
                onAmountChange = {},
                onToggleAmountOverride = {},
                onDone = {}
            )
        }
    }
}
