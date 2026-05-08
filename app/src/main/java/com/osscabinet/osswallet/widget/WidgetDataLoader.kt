package com.osscabinet.ossWallet.widget

import android.content.Context
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SecurePassphraseManager
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.domain.model.*
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

data class WidgetData(
    val profileId: Int = 0,
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val exchangeRates: Map<String, BigDecimal>,
    val baseCurrency: String,
    val precision: Int,
    val plannedPayments: List<PlannedPayment>,
    val budgets: List<Budget>,
    val loans: List<Loan>,
    val loanPayments: Map<Long, List<LoanPayment>>
)

object WidgetDataLoader {

    suspend fun load(context: Context): WidgetData {
        val settingsDataStore = SettingsDataStore(context)
        val profileId = settingsDataStore.currentProfileId.first()
        val baseCurrency = settingsDataStore.currency.first()
        val precision = settingsDataStore.decimalPrecision.first()

        val db = AppDatabase.getDatabase(
            context,
            SecurePassphraseManager(context).getOrGeneratePassphrase()
        )

        val accounts = db.accountDao().getAllAccountsSync(profileId).map { it.toDomain() }
        val transactions = db.transactionDao().getAllTransactionsSync(profileId).map { it.toDomain() }
        val categories = db.categoryDao().getAllCategoriesSync().map { it.toDomain() }
        val rates = db.currencyDao().getAllRates()
        val exchangeRates = rates.associate { it.currencyCode to BigDecimal.valueOf(it.rate) }

        val plannedPayments = db.plannedPaymentDao()
            .getAllPlannedPaymentsSync(profileId)
            .map { it.toDomain() }

        val budgets = db.budgetDao()
            .getActiveBudgetsSync(profileId)
            .map { it.toDomain() }

        val loans = db.loanDao()
            .getActiveLoansSync(profileId)
            .map { it.toDomain() }

        val loanPayments = loans.associate { loan ->
            loan.id to db.loanPaymentDao().getPaymentsForLoanSync(loan.id).map { it.toDomain() }
        }

        return WidgetData(
            profileId = profileId,
            accounts = accounts,
            transactions = transactions,
            categories = categories,
            exchangeRates = exchangeRates,
            baseCurrency = baseCurrency,
            precision = precision,
            plannedPayments = plannedPayments,
            budgets = budgets,
            loans = loans,
            loanPayments = loanPayments
        )
    }
}
