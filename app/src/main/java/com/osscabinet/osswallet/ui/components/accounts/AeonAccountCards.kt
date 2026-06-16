package com.osscabinet.ossWallet.ui.components.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.NativeWithConvertedSubtext
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.aeonDashedTopRule
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.toColorInt
import java.math.BigDecimal

/**
 * Aeon Account Card components following the Category Tint System.
 */
@Composable
private fun accountDotColor(account: Account): Color = try {
    Color(account.colorHex.toColorInt())
} catch (e: Exception) {
    AppColors.accent
}

@Composable
private fun AccountColorDot(color: Color, size: Dp = 8.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

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
    val dotColor = accountDotColor(account)
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.cardSurfaceProminent)
            .aeonHairlineBorder(shape)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccountColorDot(dotColor, 7.dp)
                Text(
                    account.name.uppercase(),
                    style = AppTextStyles.sectionOverline,
                    color = AppColors.contentSecondary.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            NativeWithConvertedSubtext(
                amount = balance,
                nativeCurrency = account.currencyCode,
                targetCurrency = baseCurrency,
                convertedAmount = balanceInBase,
                primaryStyle = AppTextStyles.transactionAmount.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 17.sp
                ),
                primaryColor = AppColors.contentPrimary,
                subtextStyle = AppTextStyles.metaLabel,
                subtextColor = AppColors.contentSecondary.copy(alpha = 0.7f),
                horizontalAlignment = Alignment.Start,
            )
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
    val dotColor = accountDotColor(account)
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.cardSurfaceProminent)
            .aeonHairlineBorder(shape)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AccountColorDot(dotColor, 7.dp)
                Column {
                    Text(
                        account.name,
                        style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Medium),
                        color = AppColors.contentPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        account.structureType.displayName.uppercase(),
                        style = AppTextStyles.metaLabel,
                        color = AppColors.contentSecondary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            NativeWithConvertedSubtext(
                amount = balance,
                nativeCurrency = account.currencyCode,
                targetCurrency = baseCurrency,
                convertedAmount = balanceInBase,
                primaryStyle = AppTextStyles.transactionAmount.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp
                ),
                primaryColor = AppColors.contentPrimary,
                subtextStyle = AppTextStyles.metaLabel,
                subtextColor = AppColors.contentSecondary.copy(alpha = 0.7f),
                horizontalAlignment = Alignment.Start,
            )
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
    val dotColor = accountDotColor(account)
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.cardSurfaceProminent)
            .aeonHairlineBorder(shape)
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountColorDot(dotColor, 8.dp)
                    Column {
                        Text(
                            account.name,
                            style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Medium),
                            color = AppColors.contentPrimary
                        )
                        Text(
                            account.structureType.displayName.uppercase(),
                            style = AppTextStyles.metaLabel,
                            color = AppColors.contentSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (cardNumber != null) {
                        Text(
                            cardNumber,
                            style = AppTextStyles.metaLabel,
                            color = AppColors.contentSecondary.copy(alpha = 0.6f)
                        )
                    }
                    if (extraLabel != null) {
                        Text(
                            extraLabel,
                            style = AppTextStyles.metaLabel,
                            color = AppColors.contentSecondary.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            NativeWithConvertedSubtext(
                amount = balance,
                nativeCurrency = account.currencyCode,
                targetCurrency = baseCurrency,
                convertedAmount = balanceInBase,
                modifier = Modifier.fillMaxWidth(),
                primaryStyle = AppTextStyles.transactionAmount.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 32.sp
                ),
                primaryColor = AppColors.contentPrimary,
                subtextStyle = AppTextStyles.metaLabel,
                subtextColor = AppColors.contentSecondary.copy(alpha = 0.7f),
                horizontalAlignment = Alignment.Start,
            )

            if (incomeInAccountCurrency.signum() != 0 || expenseInAccountCurrency.signum() != 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aeonDashedTopRule()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    AccountFlowReadout(
                        label = "IN",
                        amount = incomeInAccountCurrency,
                        currency = account.currencyCode,
                        color = AppColors.positive,
                        onClick = onClickIncome,
                        modifier = Modifier.weight(1f)
                    )
                    AccountFlowReadout(
                        label = "OUT",
                        amount = expenseInAccountCurrency,
                        currency = account.currencyCode,
                        color = AppColors.negative,
                        onClick = onClickExpense,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val sortedCurrencyBalances = remember(currencyBalances) {
                currencyBalances.toList().sortedBy { it.first }
            }
            if (sortedCurrencyBalances.size > 1) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sortedCurrencyBalances.forEach { (curr, bal) ->
                        val chipShape = RoundedCornerShape(8.dp)
                        Row(
                            modifier = Modifier
                                .clip(chipShape)
                                .aeonHairlineBorder(chipShape)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                curr,
                                style = AppTextStyles.metaLabel,
                                color = AppColors.contentSecondary.copy(alpha = 0.7f)
                            )
                            Text(
                                formatCurrency(bal),
                                style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.Medium),
                                color = AppColors.contentPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountFlowReadout(
    label: String,
    amount: BigDecimal,
    currency: String,
    color: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            label,
            style = AppTextStyles.sectionOverline,
            color = AppColors.contentSecondary.copy(alpha = 0.7f)
        )
        Text(
            formatCurrency(amount, currency),
            style = AppTextStyles.transactionAmount.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

private fun glassPreviewAccounts() = Triple(
    Account(id = "1", name = "Main Savings", currencyCode = "USD", structureType = AccountStructureType.SAVINGS, colorHex = "#4CAF50", icon = "Savings"),
    Account(id = "2", name = "Checking", currencyCode = "EUR", structureType = AccountStructureType.BANK, colorHex = "#2196F3", icon = "AccountBalance"),
    Account(id = "3", name = "Portfolio", currencyCode = "USD", structureType = AccountStructureType.INVESTMENT, colorHex = "#9C27B0", icon = "ShowChart")
)

@Preview(showBackground = true, name = "Glass – Light")
@Composable
fun AeonAccountCardsGlassLightPreview() {
    val (savings, checking, portfolio) = glassPreviewAccounts()
    OssWalletTheme(theme = "Light") {
        // hazeEffect is a GPU operation that Compose Preview can't execute — it renders as white.
        // Disabling glass shows the fallback tinted colors instead, which is useful for layout review.
        CompositionLocalProvider(LocalGlassmorphismEnabled provides false) {
            AeonHazeProvider { _ ->
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AeonAccountCardSmall(
                            account = savings,
                            balance = BigDecimal("12345.67"),
                            balanceInBase = BigDecimal("12345.67"),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        AeonAccountCardSmall(
                            account = checking,
                            balance = BigDecimal("3421.50"),
                            balanceInBase = BigDecimal("3754.20"),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AeonAccountCardMedium(
                        account = portfolio,
                        balance = BigDecimal("48320.00"),
                        balanceInBase = BigDecimal("48320.00"),
                        baseCurrency = "USD",
                        onClick = {}
                    )
                    AeonAccountCardLarge(
                        account = savings,
                        balance = BigDecimal("12345.67"),
                        balanceInBase = BigDecimal("12345.67"),
                        baseCurrency = "USD",
                        onClick = {},
                        incomeInAccountCurrency = BigDecimal("5200.00"),
                        expenseInAccountCurrency = BigDecimal("1840.50"),
                        cardNumber = "•••• 4921"
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Glass – Dark")
@Composable
fun AeonAccountCardsGlassDarkPreview() {
    val (savings, checking, portfolio) = glassPreviewAccounts()
    OssWalletTheme(theme = "Dark") {
        CompositionLocalProvider(LocalGlassmorphismEnabled provides false) {
            AeonHazeProvider { _ ->
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AeonAccountCardSmall(
                            account = savings,
                            balance = BigDecimal("12345.67"),
                            balanceInBase = BigDecimal("12345.67"),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        AeonAccountCardSmall(
                            account = checking,
                            balance = BigDecimal("3421.50"),
                            balanceInBase = BigDecimal("3754.20"),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AeonAccountCardMedium(
                        account = portfolio,
                        balance = BigDecimal("48320.00"),
                        balanceInBase = BigDecimal("48320.00"),
                        baseCurrency = "USD",
                        onClick = {}
                    )
                    AeonAccountCardLarge(
                        account = savings,
                        balance = BigDecimal("12345.67"),
                        balanceInBase = BigDecimal("12345.67"),
                        baseCurrency = "USD",
                        onClick = {},
                        incomeInAccountCurrency = BigDecimal("5200.00"),
                        expenseInAccountCurrency = BigDecimal("1840.50"),
                        cardNumber = "•••• 4921"
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Glass – AMOLED")
@Composable
fun AeonAccountCardsGlassAmoledPreview() {
    val (savings, checking, portfolio) = glassPreviewAccounts()
    OssWalletTheme(theme = "Amoled") {
        CompositionLocalProvider(LocalGlassmorphismEnabled provides false) {
            AeonHazeProvider { _ ->
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AeonAccountCardSmall(
                            account = savings,
                            balance = BigDecimal("12345.67"),
                            balanceInBase = BigDecimal("12345.67"),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        AeonAccountCardSmall(
                            account = checking,
                            balance = BigDecimal("3421.50"),
                            balanceInBase = BigDecimal("3754.20"),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AeonAccountCardMedium(
                        account = portfolio,
                        balance = BigDecimal("48320.00"),
                        balanceInBase = BigDecimal("48320.00"),
                        baseCurrency = "USD",
                        onClick = {}
                    )
                    AeonAccountCardLarge(
                        account = savings,
                        balance = BigDecimal("12345.67"),
                        balanceInBase = BigDecimal("12345.67"),
                        baseCurrency = "USD",
                        onClick = {},
                        incomeInAccountCurrency = BigDecimal("5200.00"),
                        expenseInAccountCurrency = BigDecimal("1840.50"),
                        cardNumber = "•••• 4921"
                    )
                }
            }
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
