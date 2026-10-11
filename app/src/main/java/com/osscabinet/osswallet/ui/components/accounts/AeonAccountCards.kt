package com.osscabinet.ossWallet.ui.components.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.analytics.AccountSummaryItem
import com.osscabinet.ossWallet.domain.service.financial.FinancialSummary
import com.osscabinet.ossWallet.domain.util.fuzzyMatch
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonHazeProvider
import com.osscabinet.ossWallet.ui.components.AeonHybridSelector
import com.osscabinet.ossWallet.ui.components.AeonSearchField
import com.osscabinet.ossWallet.ui.components.AeonSelectMode
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import com.osscabinet.ossWallet.ui.components.AppIconMap
import com.osscabinet.ossWallet.ui.components.NativeWithConvertedSubtext
import com.osscabinet.ossWallet.ui.components.aeonDashedTopRule
import com.osscabinet.ossWallet.ui.components.aeonHairlineBorder
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.util.CurrencyDisplaySpec
import com.osscabinet.ossWallet.ui.util.CurrencyReprUi
import com.osscabinet.ossWallet.ui.util.formatCurrency
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
    summary: AccountSummaryItem,
    baseCurrency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val account = summary.account
    val dotColor = accountDotColor(account)
    val shape = RoundedCornerShape(12.dp)
    val icon = AppIconMap[account.icon] ?: Icons.Rounded.AccountBalance

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.cardSurfaceProminent)
            .aeonHairlineBorder(shape)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(28.dp)
                .graphicsLayer { alpha = 0.12f },
            tint = dotColor
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AccountColorDot(dotColor, 5.dp)
                Text(
                    account.name.uppercase(),
                    style = AppTextStyles.sectionOverline.copy(fontSize = 8.sp),
                    color = AppColors.contentSecondary.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            NativeWithConvertedSubtext(
                amount = summary.balance,
                nativeCurrency = account.currencyCode,
                targetCurrency = baseCurrency,
                convertedAmount = summary.balanceInBase,
                primaryStyle = AppTextStyles.transactionAmount.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 9.sp
                ),
                primaryColor = AppColors.contentPrimary,
                subtextStyle = AppTextStyles.metaLabel.copy(fontSize = 7.sp),
                subtextColor = AppColors.contentSecondary.copy(alpha = 0.7f),
                horizontalAlignment = Alignment.Start,
            )
        }
    }
}

@Composable
fun AeonAccountCardMedium(
    summary: AccountSummaryItem,
    baseCurrency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val account = summary.account
    val dotColor = accountDotColor(account)
    val shape = RoundedCornerShape(16.dp)
    val icon = AppIconMap[account.icon] ?: Icons.Rounded.AccountBalance

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.cardSurfaceProminent)
            .aeonHairlineBorder(shape)
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp)
                .graphicsLayer { alpha = 0.12f },
            tint = dotColor
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AccountColorDot(dotColor, 6.dp)
                Column {
                    Text(
                        account.name,
                        style = AppTextStyles.listItemTitle.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = AppColors.contentPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        account.structureType.displayName.uppercase(),
                        style = AppTextStyles.metaLabel.copy(fontSize = 8.sp),
                        color = AppColors.contentSecondary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            NativeWithConvertedSubtext(
                amount = summary.balance,
                nativeCurrency = account.currencyCode,
                targetCurrency = baseCurrency,
                convertedAmount = summary.balanceInBase,
                primaryStyle = AppTextStyles.transactionAmount.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp
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
    modifier: Modifier = Modifier,
    summary: AccountSummaryItem,
    baseCurrency: String,
    perspective: com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective = com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective.Accounting,
    splitTransfers: Boolean = false,
    onClick: () -> Unit,
    previousSummary: AccountSummaryItem? = null,
    cardNumber: String? = null,
    extraLabel: String? = null,
    onClickIncome: (() -> Unit)? = null,
    onClickExpense: (() -> Unit)? = null,
    onClickTransfer: (() -> Unit)? = null,
    onClickInflow: (() -> Unit)? = null,
    onClickOutflow: (() -> Unit)? = null,
    onClickNet: (() -> Unit)? = null,
    onClickNetFlow: (() -> Unit)? = null
) {
    val account = summary.account
    val dotColor = accountDotColor(account)
    val shape = RoundedCornerShape(20.dp)
    val icon = AppIconMap[account.icon] ?: Icons.Rounded.AccountBalance

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.cardSurfaceProminent)
            .aeonHairlineBorder(shape)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(56.dp)
                .graphicsLayer { alpha = 0.12f },
            tint = dotColor
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            style = AppTextStyles.listItemTitle.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            ),
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
                amount = summary.balance,
                nativeCurrency = account.currencyCode,
                targetCurrency = baseCurrency,
                convertedAmount = summary.balanceInBase,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (perspective is com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective.Accounting) {
                            onClickNet?.invoke()
                        } else {
                            onClickNetFlow?.invoke()
                        }
                    },
                primaryStyle = AppTextStyles.transactionAmount.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 24.sp
                ),
                primaryColor = AppColors.contentPrimary,
                subtextStyle = AppTextStyles.metaLabel,
                subtextColor = AppColors.contentSecondary.copy(alpha = 0.7f),
                horizontalAlignment = Alignment.Start,
            )

            val flow = summary.currentPeriodMetrics
            val prevFlow = previousSummary?.currentPeriodMetrics ?: summary.previousPeriodMetrics

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .aeonDashedTopRule()
                    .padding(top = 8.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (perspective) {
                    is com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective.Accounting -> {
                        AccountFlowReadout(
                            label = "INC",
                            amount = flow.income,
                            currency = account.currencyCode,
                            color = AppColors.positive,
                            onClick = onClickIncome,
                            previousAmount = prevFlow?.income,
                            modifier = Modifier.weight(1f)
                        )
                        AccountFlowSeparator()
                        AccountFlowReadout(
                            label = "EXP",
                            amount = flow.expense,
                            currency = account.currencyCode,
                            color = AppColors.negative,
                            onClick = onClickExpense,
                            previousAmount = prevFlow?.expense,
                            modifier = Modifier.weight(1f)
                        )
                        AccountFlowSeparator()
                        if (splitTransfers) {
                            AccountFlowReadout(
                                label = "TRF IN",
                                amount = flow.transferIn,
                                currency = account.currencyCode,
                                color = AppColors.accent,
                                onClick = onClickTransfer,
                                previousAmount = prevFlow?.transferIn,
                                modifier = Modifier.weight(1f),
                                forceShowPlus = true
                            )
                            AccountFlowSeparator()
                            AccountFlowReadout(
                                label = "TRF OUT",
                                amount = flow.transferOut,
                                currency = account.currencyCode,
                                color = AppColors.accent,
                                onClick = onClickTransfer,
                                previousAmount = prevFlow?.transferOut,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            AccountFlowReadout(
                                label = "TRF",
                                amount = flow.transfer,
                                currency = account.currencyCode,
                                color = AppColors.accent,
                                onClick = onClickTransfer,
                                previousAmount = prevFlow?.transfer,
                                modifier = Modifier.weight(1f),
                                forceShowPlus = true
                            )
                        }
                    }

                    is com.osscabinet.ossWallet.domain.model.settings.AccountingPerspective.CashFlow -> {
                        AccountFlowReadout(
                            label = "IN",
                            amount = flow.inflow,
                            currency = account.currencyCode,
                            color = AppColors.positive,
                            onClick = onClickInflow,
                            previousAmount = prevFlow?.inflow,
                            modifier = Modifier.weight(1f)
                        )
                        AccountFlowSeparator()
                        AccountFlowReadout(
                            label = "OUT",
                            amount = flow.outflow,
                            currency = account.currencyCode,
                            color = AppColors.negative,
                            onClick = onClickOutflow,
                            previousAmount = prevFlow?.outflow,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            val currencyBalances = summary.currencyBalances
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
                                formatCurrency(bal, curr),
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
private fun AccountFlowSeparator() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(AppColors.outlineVariant)
    )
}

@Composable
private fun AccountFlowReadout(
    label: String,
    amount: BigDecimal,
    currency: String,
    color: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    previousAmount: BigDecimal? = null,
    forceShowPlus: Boolean = false
) {
    Column(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                label,
                style = AppTextStyles.sectionOverline.copy(fontSize = 10.sp),
                color = AppColors.contentSecondary.copy(alpha = 0.5f),
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (previousAmount != null) {
                val change = amount.subtract(previousAmount)
                if (change.signum() != 0 && previousAmount.signum() != 0) {
                    val percentage = change.multiply(BigDecimal("100"))
                        .divide(previousAmount.abs(), 1, java.math.RoundingMode.HALF_UP)
                    val up = change.signum() > 0

                    val displayPercentage = if (percentage.abs() > BigDecimal("999")) {
                        "${if (percentage.signum() > 0) ">999" else "<-999"}%"
                    } else {
                        "${if (up) "+" else ""}$percentage%"
                    }

                    Text(
                        text = "${if (up) "▲" else "▼"} $displayPercentage",
                        style = AppTextStyles.metaLabel.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        ),
                        color = color,
                        maxLines = 1
                    )
                }
            }
        }
        Text(
            text = CurrencyReprUi.formatCurrencyAnnotated(
                amount = amount,
                currencyCode = currency,
                spec = CurrencyDisplaySpec(
                    useSemanticColor = false,
                    forceShowPlus = forceShowPlus,
                    shortenThreshold = 10
                )
            ),
            style = AppTextStyles.transactionAmount.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                letterSpacing = (-0.3).sp
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
    val mockSummary = AccountSummaryItem(
        account = mockAccount,
        currencyBalances = mapOf("USD" to BigDecimal("1234.56")),
        balance = BigDecimal("1234.56"),
        balanceInBase = BigDecimal("1234.56"),
        currentPeriodMetrics = FinancialSummary(
            income = BigDecimal("500.00"),
            expense = BigDecimal("100.00"),
            transferIn = BigDecimal.ZERO,
            transferOut = BigDecimal.ZERO
        ),
        previousPeriodMetrics = FinancialSummary(
            income = BigDecimal("400.00"),
            expense = BigDecimal("150.00"),
            transferIn = BigDecimal.ZERO,
            transferOut = BigDecimal.ZERO
        )
    )

    OssWalletTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AeonAccountCardSmall(
                summary = mockSummary,
                baseCurrency = "USD",
                onClick = {}
            )

            AeonAccountCardMedium(
                summary = mockSummary,
                baseCurrency = "USD",
                onClick = {}
            )

            AeonAccountCardLarge(
                summary = mockSummary,
                baseCurrency = "USD",
                onClick = {}
            )
        }
    }
}

private fun glassPreviewAccounts() = Triple(
    Account(
        id = "1",
        name = "Main Savings",
        currencyCode = "USD",
        structureType = AccountStructureType.SAVINGS,
        colorHex = "#4CAF50",
        icon = "Savings"
    ),
    Account(
        id = "2",
        name = "Checking",
        currencyCode = "EUR",
        structureType = AccountStructureType.BANK,
        colorHex = "#2196F3",
        icon = "AccountBalance"
    ),
    Account(
        id = "3",
        name = "Portfolio",
        currencyCode = "USD",
        structureType = AccountStructureType.INVESTMENT,
        colorHex = "#9C27B0",
        icon = "ShowChart"
    )
)

private fun mockSummary(account: Account) = AccountSummaryItem(
    account = account,
    currencyBalances = mapOf(account.currencyCode to BigDecimal("12345.67")),
    balance = BigDecimal("12345.67"),
    balanceInBase = BigDecimal("12345.67"),
    currentPeriodMetrics = FinancialSummary(
        income = BigDecimal("5200.00"),
        expense = BigDecimal("1840.50"),
        transferIn = BigDecimal.ZERO,
        transferOut = BigDecimal.ZERO
    )
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
                            summary = mockSummary(savings),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        AeonAccountCardSmall(
                            summary = mockSummary(checking).copy(balanceInBase = BigDecimal("3754.20")),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        AeonAccountCardSmall(
                            summary = mockSummary(portfolio),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AeonAccountCardMedium(
                            summary = mockSummary(portfolio),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        AeonAccountCardMedium(
                            summary = mockSummary(checking).copy(balanceInBase = BigDecimal("3754.20")),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AeonAccountCardLarge(
                        summary = mockSummary(savings),
                        baseCurrency = "USD",
                        onClick = {},
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
                            summary = mockSummary(savings),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        AeonAccountCardSmall(
                            summary = mockSummary(checking).copy(balanceInBase = BigDecimal("3754.20")),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        AeonAccountCardSmall(
                            summary = mockSummary(portfolio),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AeonAccountCardMedium(
                            summary = mockSummary(portfolio),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        AeonAccountCardMedium(
                            summary = mockSummary(checking).copy(balanceInBase = BigDecimal("3754.20")),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AeonAccountCardLarge(
                        summary = mockSummary(savings),
                        baseCurrency = "USD",
                        onClick = {},
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
                            summary = mockSummary(savings),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        AeonAccountCardSmall(
                            summary = mockSummary(checking).copy(balanceInBase = BigDecimal("3754.20")),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        AeonAccountCardSmall(
                            summary = mockSummary(portfolio),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AeonAccountCardMedium(
                            summary = mockSummary(portfolio),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        AeonAccountCardMedium(
                            summary = mockSummary(checking).copy(balanceInBase = BigDecimal("3754.20")),
                            baseCurrency = "USD",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AeonAccountCardLarge(
                        summary = mockSummary(savings),
                        baseCurrency = "USD",
                        onClick = {},
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
    val mockSummary = AccountSummaryItem(
        account = mockAccount,
        currencyBalances = mapOf("USD" to BigDecimal("1234.56")),
        balance = BigDecimal("1234.56"),
        balanceInBase = BigDecimal("1234.56"),
        currentPeriodMetrics = FinancialSummary(
            income = BigDecimal("500.00"),
            expense = BigDecimal("100.00"),
            transferIn = BigDecimal.ZERO,
            transferOut = BigDecimal.ZERO
        )
    )

    OssWalletTheme(theme = "Dark") {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AeonAccountCardSmall(
                    summary = mockSummary,
                    baseCurrency = "USD",
                    onClick = {}
                )

                AeonAccountCardMedium(
                    summary = mockSummary,
                    baseCurrency = "USD",
                    onClick = {}
                )

                AeonAccountCardLarge(
                    summary = mockSummary,
                    baseCurrency = "USD",
                    onClick = {}
                )
            }
        }
    }
}
