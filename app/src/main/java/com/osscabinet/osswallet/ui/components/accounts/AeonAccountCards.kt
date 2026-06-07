package com.osscabinet.ossWallet.ui.components.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.CashFlowType
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter
import com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.formatAuto
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import com.osscabinet.ossWallet.ui.components.AeonFlowBadge
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.LocalGlassSettings
import com.osscabinet.ossWallet.ui.theme.LocalThemeMode
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.applyAeonTint
import com.osscabinet.ossWallet.ui.util.toColorInt
import java.math.BigDecimal

/**
 * Aeon Account Card components following the Category Tint System.
 */
@Composable
fun AeonAccountCardSmall(
    account: Account,
    balance: BigDecimal,
    balanceInBase: BigDecimal,
    baseCurrency: String,
    currencyBalances: Map<String, BigDecimal> = emptyMap(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accountColor = try {
        Color(account.colorHex.toColorInt())
    } catch (e: Exception) {
        AppColors.primary
    }

    val mode = LocalThemeMode.current
    val (containerColor, contentColor) = accountColor.applyAeonTint(mode)
    val isDark = isSystemInDarkTheme()

    val icon = AppIconMap[account.icon] ?: Icons.Rounded.AccountBalance
    val glassSettings = LocalGlassSettings.current

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        hazeStyle = GlassTokens.createHazeStyle(
            config = glassSettings.card,
            bgColor = containerColor,
            isDark = isDark
        ),
        glassVariant = com.osscabinet.ossWallet.ui.theme.GlassVariant.Card,
        fallbackColor = containerColor,
        borderColor = contentColor.copy(alpha = 0.1f),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 10.dp)
                        .size(48.dp)
                        .alpha(0.1f),
                    tint = contentColor
                )
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        account.name.uppercase(),
                        style = AppTextStyles.overlineLabel.copy(
                            fontSize = 9.sp,
                            letterSpacing = 1.sp
                        ),
                        color = contentColor.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Column {
                        Text(
                            formatAuto(balance, account.currencyCode, shorten = true),
                            style = AppTextStyles.transactionSubtitle.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "≈ ${formatAuto(balanceInBase, baseCurrency, shorten = true)}",
                            style = AppTextStyles.metaLabel,
                            color = contentColor.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AeonAccountCardMedium(
    account: Account,
    balance: BigDecimal,
    balanceInBase: BigDecimal,
    baseCurrency: String,
    currencyBalances: Map<String, BigDecimal> = emptyMap(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accountColor = try {
        Color(account.colorHex.toColorInt())
    } catch (e: Exception) {
        AppColors.primary
    }

    val mode = LocalThemeMode.current
    val (containerColor, contentColor) = accountColor.applyAeonTint(mode)
    val isDark = isSystemInDarkTheme()

    val icon = AppIconMap[account.icon] ?: Icons.Rounded.AccountBalance
    val glassSettings = LocalGlassSettings.current

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        hazeStyle = GlassTokens.createHazeStyle(
            config = glassSettings.card,
            bgColor = containerColor,
            isDark = isDark
        ),
        glassVariant = com.osscabinet.ossWallet.ui.theme.GlassVariant.Card,
        fallbackColor = containerColor,
        borderColor = contentColor.copy(alpha = 0.1f),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 10.dp)
                        .size(80.dp)
                        .alpha(0.1f),
                    tint = contentColor
                )
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = contentColor.copy(alpha = 0.7f)
                        )
                        Text(
                            account.name.uppercase(),
                            style = AppTextStyles.overlineLabel.copy(
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            ),
                            color = contentColor.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val amountText = formatAuto(balance, shorten = true)
                    val symbolText = CurrencyFormatter.getCurrencySymbol(account.currencyCode)
                    val isSymbolLeft = CurrencyFormatter.activeSystem.symbolPosition == CurrencyFormatter.SymbolPosition.LEFT

                    Text(
                        text = buildAnnotatedString {
                            if (isSymbolLeft) {
                                withStyle(SpanStyle(fontWeight = FontWeight.Light, color = contentColor.copy(alpha = 0.6f), fontSize = 16.sp)) {
                                    append(symbolText)
                                }
                                append(" ")
                                append(amountText)
                            } else {
                                append(amountText)
                                append(" ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Light, color = contentColor.copy(alpha = 0.6f), fontSize = 16.sp)) {
                                    append(symbolText)
                                }
                            }
                        },
                        style = AppTextStyles.cardSectionTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        "≈ ${formatAuto(balanceInBase, baseCurrency, shorten = true)}",
                        style = AppTextStyles.tagLabel,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AeonAccountCardLarge(
    account: Account,
    balance: BigDecimal,
    balanceInBase: BigDecimal,
    baseCurrency: String,
    exchangeRates: Map<String, BigDecimal> = emptyMap(),
    currencyBalances: Map<String, BigDecimal> = emptyMap(),
    incomeInAccountCurrency: BigDecimal = BigDecimal.ZERO,
    expenseInAccountCurrency: BigDecimal = BigDecimal.ZERO,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardNumber: String? = null,
    extraLabel: String? = null,
    onClickIncome: (() -> Unit)? = null,
    onClickExpense: (() -> Unit)? = null
) {
    val accountColor = try {
        Color(account.colorHex.toColorInt())
    } catch (e: Exception) {
        AppColors.primary
    }

    val mode = LocalThemeMode.current
    val (containerColor, contentColor) = accountColor.applyAeonTint(mode)
    val isDark = isSystemInDarkTheme()

    val icon = AppIconMap[account.icon] ?: Icons.Rounded.AccountBalance

    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val glassSettings = LocalGlassSettings.current

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .onSizeChanged { cardSize = it },
        shape = RoundedCornerShape(32.dp),
        hazeStyle = GlassTokens.createHazeStyle(
            config = glassSettings.card,
            bgColor = containerColor,
            isDark = isDark
        ),
        glassVariant = com.osscabinet.ossWallet.ui.theme.GlassVariant.Card,
        fallbackColor = containerColor,
        borderColor = contentColor.copy(alpha = 0.1f),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background Icon
                val iconHeight = with(density) { (cardSize.height * 0.75f).toDp() }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(iconHeight)
                        .alpha(0.15f),
                    tint = contentColor
                )

                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = contentColor.copy(alpha = 0.7f)
                            )
                            Text(
                                account.name.uppercase(),
                                style = AppTextStyles.overlineLabel.copy(letterSpacing = 1.2.sp),
                                color = contentColor.copy(alpha = 0.7f)
                            )
                        }


                        // Income/Expense Badges
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AeonFlowBadge(
                                amount = incomeInAccountCurrency,
                                type = CashFlowType.INCOME,
                                currency = account.currencyCode,
                                onClick = onClickIncome
                            )
                            AeonFlowBadge(
                                amount = expenseInAccountCurrency,
                                type = CashFlowType.EXPENSE,
                                currency = account.currencyCode,
                                onClick = onClickExpense
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column {
                        val amountText = formatAuto(balance, shorten = true)
                        val symbolText = CurrencyFormatter.getCurrencySymbol(account.currencyCode)
                        val isSymbolLeft = CurrencyFormatter.activeSystem.symbolPosition == CurrencyFormatter.SymbolPosition.LEFT

                        Text(
                            text = buildAnnotatedString {
                                if (isSymbolLeft) {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Light, color = contentColor.copy(alpha = 0.6f), fontSize = 28.sp)) {
                                        append(symbolText)
                                    }
                                    append(" ")
                                    append(amountText)
                                } else {
                                    append(amountText)
                                    append(" ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.Light, color = contentColor.copy(alpha = 0.6f), fontSize = 28.sp)) {
                                        append(symbolText)
                                    }
                                }
                            },
                            style = AppTextStyles.calculatorDisplay.copy(fontSize = 36.sp),
                            color = contentColor
                        )
                        if (account.currencyCode != baseCurrency) {
                            Text(
                                text = "≈ ${
                                    formatAuto(
                                        balanceInBase,
                                        baseCurrency,
                                        shorten = true
                                    )
                                }",
                                style = AppTextStyles.metaLabel,
                                color = contentColor.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Horizontal scrollable list for subAccounts and currency codes
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = contentColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    account.structureType.displayName.uppercase(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = AppTextStyles.transactionBadge,
                                    color = contentColor
                                )
                            }

                            val sortedCurrencyBalances = remember(currencyBalances) {
                                currencyBalances.toList().sortedBy { it.first }
                            }

                            sortedCurrencyBalances.forEach { (curr, bal) ->
                                Surface(
                                    color = contentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            curr,
                                            style = AppTextStyles.transactionBadge,
                                            color = contentColor.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            formatAuto(bal),
                                            style = AppTextStyles.transactionBadge,
                                            color = contentColor
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Column(horizontalAlignment = Alignment.End) {
                            if (cardNumber != null) {
                                Text(
                                    cardNumber,
                                    style = AppTextStyles.overlineLabel,
                                    color = contentColor.copy(alpha = 0.5f)
                                )
                            }
                            if (extraLabel != null) {
                                Text(
                                    extraLabel,
                                    style = AppTextStyles.overlineLabel,
                                    color = contentColor.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountSelection(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredAccounts = remember(query, accounts) {
        if (query.isBlank()) accounts.sortedBy { it.name }
        else accounts.filter {
            it.name.contains(query, ignoreCase = true) || it.name.fuzzyMatch(
                query
            )
        }
            .sortedBy { it.name }
    }

    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonSelectionScreen(
            title = "Select Account",
            onBack = onDismiss
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                AeonSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search accounts..."
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f, fill = false)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        AeonHybridSelector(
                            items = filteredAccounts,
                            mode = AeonSelectMode.SINGLE,
                            selectedIds = if (selectedAccount != null) setOf(selectedAccount.id) else emptySet(),
                            onSelectionChange = { ids ->
                                ids.firstOrNull()?.let { id ->
                                    filteredAccounts.find { it.id == id }?.let {
                                        onAccountSelected(it)
                                        onDismiss()
                                    }
                                }
                            },
                            idMapper = { it.id },
                            labelMapper = { it.name },
                            detailMapper = { it.structureType.displayName },
                            iconMapper = { AppIconMap[it.icon] ?: Icons.Rounded.AccountBalance },
                            colorMapper = { Color(android.graphics.Color.parseColor(it.colorHex)) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonAccountCardsPreview() {
    val mockAccount = Account(
        id = "1",
        name = "Main Savings",
        currencyCode = "USD",
        structureType = AccountStructureType.SAVINGS,
        colorHex = "#4CAF50",
        icon = "Savings"
    )

    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AeonAccountCardSmall(
                account = mockAccount,
                balance = BigDecimal("1234.56"),
                balanceInBase = BigDecimal("1234.56"),
                baseCurrency = "USD",
                onClick = {}
            )

            AeonAccountCardMedium(
                account = mockAccount,
                balance = BigDecimal("1234.56"),
                balanceInBase = BigDecimal("1234.56"),
                baseCurrency = "USD",
                onClick = {}
            )

            AeonAccountCardLarge(
                account = mockAccount,
                balance = BigDecimal("1234.56"),
                balanceInBase = BigDecimal("1234.56"),
                baseCurrency = "USD",
                onClick = {},
                incomeInAccountCurrency = BigDecimal("500.00"),
                expenseInAccountCurrency = BigDecimal("100.00")
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonAccountCardsDarkPreview() {
    val mockAccount = Account(
        id = "1",
        name = "Main Savings",
        currencyCode = "USD",
        structureType = AccountStructureType.SAVINGS,
        colorHex = "#4CAF50",
        icon = "Savings"
    )

    OssWalletTheme(theme = "Dark") {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AeonAccountCardSmall(
                    account = mockAccount,
                    balance = BigDecimal("1234.56"),
                    balanceInBase = BigDecimal("1234.56"),
                    baseCurrency = "USD",
                    onClick = {}
                )

                AeonAccountCardMedium(
                    account = mockAccount,
                    balance = BigDecimal("1234.56"),
                    balanceInBase = BigDecimal("1234.56"),
                    baseCurrency = "USD",
                    onClick = {}
                )

                AeonAccountCardLarge(
                    account = mockAccount,
                    balance = BigDecimal("1234.56"),
                    balanceInBase = BigDecimal("1234.56"),
                    baseCurrency = "USD",
                    onClick = {},
                    incomeInAccountCurrency = BigDecimal("500.00"),
                    expenseInAccountCurrency = BigDecimal("100.00")
                )
            }
        }
    }
}
