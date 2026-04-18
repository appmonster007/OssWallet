package com.osscabinet.ossWallet.data.seed

import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.BuildConfig
import com.osscabinet.ossWallet.ui.widget.WidgetUpdateHelper
import android.content.Context
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import javax.inject.Inject
import kotlin.random.Random

class DatabaseSeeder @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val context: Context
) {
    suspend fun seed() {
        if (!BuildConfig.DEBUG) return

        // Clear existing data for a fresh start in dev
        accountDao.clearAll()
        transactionDao.clearAll()
        // Categories are mostly seeded by AppDatabase callback, but we can ensure they exist

        val accounts = listOf(
            AccountEntity(
                id = "acc_cash",
                name = "Cash Wallet",
                balance = BigDecimal("1000.00"),
                initialBalance = BigDecimal("1000.00"),
                currencyCode = "USD",
                structureType = "REGULAR",
                colorHex = "#4CAF50",
                icon = "AccountBalanceWallet",
                allowedCurrencies = "USD"
            ),
            AccountEntity(
                id = "acc_bank",
                name = "Main Bank",
                balance = BigDecimal("5000.00"),
                initialBalance = BigDecimal("5000.00"),
                currencyCode = "USD",
                structureType = "REGULAR",
                colorHex = "#2196F3",
                icon = "AccountBalance",
                allowedCurrencies = "USD,INR,EUR"
            ),
            AccountEntity(
                id = "acc_savings",
                name = "Savings",
                balance = BigDecimal("15000.00"),
                initialBalance = BigDecimal("15000.00"),
                currencyCode = "USD",
                structureType = "SAVINGS",
                colorHex = "#9C27B0",
                icon = "Savings",
                allowedCurrencies = "USD,INR"
            ),
            AccountEntity(
                id = "acc_credit",
                name = "Credit Card",
                balance = BigDecimal("-500.00"),
                initialBalance = BigDecimal("0.00"),
                currencyCode = "USD",
                structureType = "CREDIT",
                colorHex = "#F44336",
                icon = "CreditCard",
                allowedCurrencies = "USD"
            ),
            AccountEntity(
                id = "acc_invest",
                name = "Investment",
                balance = BigDecimal("20000.00"),
                initialBalance = BigDecimal("10000.00"),
                currencyCode = "USD",
                structureType = "INVESTMENT",
                colorHex = "#FF9800",
                icon = "TrendingUp",
                allowedCurrencies = "USD"
            )
        )

        accountDao.upsertAccounts(accounts)

        val categories = categoryDao.getAllCategoriesSync()
        if (categories.isEmpty()) return // Should be seeded by DB callback

        val payees = listOf("Starbucks", "Walmart", "Amazon", "Shell", "Netflix", "Landlord", "Employer", "Freelance Client")
        val paymentMethods = listOf("Cash", "Credit Card", "Bank Transfer", "Debit Card")
        val calendar = Calendar.getInstance()

        repeat(20) { // Increased to 100 for better multi-currency visibility
            val account = accounts.random()
            val category = categories.random()
            val isIncome = Random.nextBoolean()
            
            // Handle multi-currency
            val allowedCurrencies = account.allowedCurrencies.split(",").map { it.trim() }
            val selectedCurrency = allowedCurrencies.random()
            
            val amount = BigDecimal(Random.nextDouble(25.0, 100.0)).setScale(2, RoundingMode.HALF_UP)
            
            // Random date within last 3 months
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -Random.nextInt(0, 90))
            
            val transaction = TransactionEntity(
                title = if (isIncome) "Income from ${payees.random()}" else "Purchase at ${payees.random()}",
                amount = amount,
                category = category.name,
                categoryId = category.id,
                timestamp = calendar.timeInMillis,
                currency = selectedCurrency,
                isIncome = isIncome,
                accountId = account.id,
                paymentMethod = paymentMethods.random(),
                payee = payees.random(),
                note = "Randomly generated transaction for testing ($selectedCurrency)",
                profileId = 1
            )
            
            transactionDao.insertTransaction(transaction)
            
            // Update account balance (simplified: assume 1:1 for seeding purposes or only update if same currency)
            // In a real app, we'd convert using exchange rates. 
            // Here, we just update the base balance to ensure the UI shows something.
            val updateAmount = if (isIncome) amount else amount.negate()
            accountDao.updateBalance(account.id, updateAmount)
        }

        // Trigger widget update after seeding
        WidgetUpdateHelper.updateAllWidgets(context)
    }
}
