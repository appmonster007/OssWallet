package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.computers.LoansCardComputer
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.ui.util.formatCurrency
import com.osscabinet.ossWallet.widget.WidgetDataLoader.CommonData
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LoanWidgetRow(
    val loanId: Long,
    val name: String,
    val lenderOrBorrowerName: String,
    val loanType: String,
    val principalFormatted: String,
    val totalPaidFormatted: String,
    val remainingBalanceFormatted: String,
    val interestAccruedFormatted: String,
    val hasInterest: Boolean,
    val paidFraction: Float,
    val status: String
)

class LoansWidgetFetcher : WidgetDataFetcher {
    override suspend fun updateState(
        context: Context,
        widgetId: Int,
        common: CommonData,
        prefs: MutablePreferences
    ) {
        val config = LoansWidgetPrefsStore(context).loadSafe(widgetId)
        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetDataLoaderEntryPoint::class.java
        )
        val accounts = ep.accountRepository().getAllAccounts(common.profileId).first()
        val loans = ep.loanRepository().getAllLoans(common.profileId).first()

        val effectiveFilter = config.filter.copy(
            timelineRange = config.filter.timelineRange.withExcludeFuture(true)
        )
        val input = CardInput(
            profileId = common.profileId,
            targetCurrency = common.baseCurrency,
            exchangeRates = ExchangeRateSnapshot(anchor = common.baseCurrency, rates = common.exchangeRates),
            filter = effectiveFilter,
            accounts = accounts,
            categories = emptyList(),
            loans = loans
        )
        val cardConfig = LoansCardConfig(
            cardId = "loans_widget_$widgetId",
            filter = effectiveFilter,
            showType = config.showType,
            maxItems = 20
        )

        val renderData = runCatching {
            LoansCardComputer(common.databaseQueryService).compute(input, cardConfig)
        }.getOrNull()

        val items = renderData?.items.orEmpty()
        val active = items.filter { it.loan.status.name == "ACTIVE" }.take(2)
        val settled = items.filter { it.loan.status.name == "SETTLED" }.take(1)

        val rows = (active + settled).map { summary ->
            val loan = summary.loan
            val currency = loan.currency
            val paidFraction = if (loan.principalAmount.signum() != 0) {
                (summary.totalPaid.toFloat() / loan.principalAmount.toFloat()).coerceIn(0f, 1f)
            } else 0f

            LoanWidgetRow(
                loanId = loan.id,
                name = loan.name,
                lenderOrBorrowerName = loan.lenderOrBorrowerName,
                loanType = if (loan.loanType == LoanType.LENT) "Lent" else "Borrowed",
                principalFormatted = formatCurrency(loan.principalAmount, currency),
                totalPaidFormatted = formatCurrency(summary.totalPaid, currency),
                remainingBalanceFormatted = formatCurrency(summary.remainingBalance, currency),
                interestAccruedFormatted = formatCurrency(summary.interestAccrued, currency),
                hasInterest = (loan.interestRate ?: 0f) != 0f,
                paidFraction = paidFraction,
                status = loan.status.name
            )
        }

        prefs[LoansWidgetKeys.IS_LOADED] = "true"
        prefs[LoansWidgetKeys.ROWS_JSON] = Json.encodeToString(rows)
    }
}
