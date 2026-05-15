package com.osscabinet.ossWallet.data.seed

import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.dao.*
import com.osscabinet.ossWallet.data.local.entity.*
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.BuildConfig
import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.first

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false

    for (char in line) {
        when (char) {
            '"' -> {
                inQuotes = !inQuotes
            }
            ',' -> {
                if (inQuotes) {
                    current.append(char)
                } else {
                    result.add(current.toString())
                    current = StringBuilder()
                }
            }
            else -> {
                current.append(char)
            }
        }
    }
    result.add(current.toString())
    return result
}

class DatabaseSeeder @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val filterDao: FilterDao,
    private val profileDao: ProfileDao,
    private val budgetDao: BudgetDao,
    private val loanDao: LoanDao,
    private val templateDao: TemplateDao,
    private val plannedPaymentDao: PlannedPaymentDao,
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) {
    suspend fun seed() {
        if (!BuildConfig.DEBUG) return

        // Check if already seeded
        val isSeeded = settingsDataStore.isDatabaseSeeded.first()
        if (isSeeded) {
            android.util.Log.d("DatabaseSeeder", "Database already seeded, skipping")
            return
        }

        android.util.Log.d("DatabaseSeeder", "Starting database seeding...")

        // Clear existing data for a fresh start in dev
        accountDao.clearAll()
        transactionDao.clearAll()
        categoryDao.clearAll()
        filterDao.clearAll()
        profileDao.clearAll()
        budgetDao.clearAll()
        loanDao.clearAll()
        templateDao.clearAll()
        plannedPaymentDao.clearAll()
        android.util.Log.d("DatabaseSeeder", "Cleared existing data")

        // Insert default profile
        profileDao.insertProfile(ProfileEntity(id = 1, name = "Default Profile", baseCurrency = "USD", type = "PERSONAL"))
        android.util.Log.d("DatabaseSeeder", "Inserted default profile")

        // Re-insert mandatory categories
        categoryDao.insertCategory(CategoryEntity(id = 1000, name = "Others", iconName = "Menu", colorHex = -7829368, isDefault = true, parentId = null))
        categoryDao.insertCategory(CategoryEntity(id = 1001, name = "Missing", iconName = "MoreHoriz", colorHex = -7829368, isDefault = true, parentId = 1000))
        categoryDao.insertCategory(CategoryEntity(id = 1002, name = "Unknown", iconName = "QuestionMark", colorHex = -7829368, isDefault = true, parentId = 1000))
        categoryDao.insertCategory(CategoryEntity(id = 1003, name = "Transfer", iconName = "SwapHoriz", colorHex = -7829368, isDefault = true, parentId = 1000))
        android.util.Log.d("DatabaseSeeder", "Re-inserted mandatory categories")

        // Load seed data from separate CSV files
        val accounts = loadAccountsFromCsv()
        android.util.Log.d("DatabaseSeeder", "Loaded ${accounts.size} accounts from CSV")

        val categories = loadCategoriesFromCsv()
        android.util.Log.d("DatabaseSeeder", "Loaded ${categories.size} categories from CSV")

        val transactions = loadTransactionsFromCsv()
        android.util.Log.d("DatabaseSeeder", "Loaded ${transactions.size} transactions from CSV")

        // Validate that we have data to seed
        if (accounts.isEmpty()) {
            android.util.Log.e("DatabaseSeeder", "No accounts found in CSV, aborting seeding")
            return
        }

        if (categories.isEmpty()) {
            android.util.Log.e("DatabaseSeeder", "No categories found in CSV, aborting seeding")
            return
        }

        // Insert categories (parents first, then children)
        val categoryNameToId = mutableMapOf<String, Long>()
        val categoryNameToColor = mutableMapOf<String, Int>()
        
        // First pass: insert parent categories (where parentName is null or empty)
        val parentCategories = categories.filter { it.parentName.isNullOrEmpty() }
        parentCategories.forEach { category ->
            categoryDao.insertCategory(category.toCategoryEntity())
            categoryNameToColor[category.name] = category.colorHex.removePrefix("#").toLongOrNull(16)?.toInt() ?: -16777216
            android.util.Log.d("DatabaseSeeder", "Inserted parent category: ${category.name}")
        }
        
        // Query all categories to get their IDs
        val allCategories = categoryDao.getAllCategoriesSync()
        allCategories.forEach { category ->
            categoryNameToId[category.name] = category.id
        }
        
        // Second pass: insert child categories with correct parentId and inherited color
        val childCategories = categories.filter { !it.parentName.isNullOrEmpty() }
        childCategories.forEach { category ->
            val parentId = categoryNameToId[category.parentName]
            if (parentId != null) {
                // Inherit parent color if child color is empty
                val colorHex = if (category.colorHex.isBlank()) {
                    categoryNameToColor[category.parentName] ?: -16777216
                } else {
                    category.colorHex.removePrefix("#").toLongOrNull(16)?.toInt() ?: -16777216
                }
                
                val categoryEntity = CategoryEntity(
                    name = category.name,
                    iconName = category.iconName,
                    colorHex = colorHex,
                    isDefault = category.isDefault,
                    parentId = parentId
                )
                categoryDao.insertCategory(categoryEntity)
                android.util.Log.d("DatabaseSeeder", "Inserted child category: ${category.name} with parent ${category.parentName}")
            } else {
                android.util.Log.w("DatabaseSeeder", "Parent category '${category.parentName}' not found for child category '${category.name}'")
            }
        }
        
        // Query all categories again to get the final IDs including children
        val finalCategories = categoryDao.getAllCategoriesSync()
        finalCategories.forEach { category ->
            categoryNameToId[category.name] = category.id
        }
        
        android.util.Log.d("DatabaseSeeder", "Inserted ${categoryNameToId.size} categories total")

        // Insert accounts
        accountDao.upsertAccounts(accounts)
        android.util.Log.d("DatabaseSeeder", "Inserted ${accounts.size} accounts")

        // Insert transactions with correct category IDs
        var successfulTransactions = 0
        var failedTransactions = 0

        transactions.forEach { transaction ->
            try {
                val categoryId = categoryNameToId[transaction.category]
                if (categoryId == null) {
                    android.util.Log.w("DatabaseSeeder", "Category '${transaction.category}' not found for transaction")
                    failedTransactions++
                    return@forEach
                }

                val transactionWithCategoryId = transaction.copy(categoryId = categoryId)
                transactionDao.insertTransaction(transactionWithCategoryId.toTransactionEntity())

                successfulTransactions++
            } catch (e: Exception) {
                android.util.Log.e("DatabaseSeeder", "Failed to insert transaction: ${e.message}")
                failedTransactions++
            }
        }

        android.util.Log.d("DatabaseSeeder", "Inserted $successfulTransactions transactions successfully, $failedTransactions failed")

        // Seed preset filters
        seedPresetFilters(categoryNameToId)
        android.util.Log.d("DatabaseSeeder", "Seeded preset filters")

        // Seed Templates
        val templateIds = seedTemplates(categoryNameToId)
        android.util.Log.d("DatabaseSeeder", "Seeded templates")

        // Seed Planned Payments
        seedPlannedPayments(templateIds)
        android.util.Log.d("DatabaseSeeder", "Seeded planned payments")

        // Seed Budgets
        seedBudgets(categoryNameToId)
        android.util.Log.d("DatabaseSeeder", "Seeded budgets")

        // Seed Loans
        seedLoans()
        android.util.Log.d("DatabaseSeeder", "Seeded loans")

        // Seed Scenario Transactions
        seedScenarioTransactions(categoryNameToId)
        android.util.Log.d("DatabaseSeeder", "Seeded scenario transactions")

        // Mark as seeded
        settingsDataStore.saveDatabaseSeeded(seeded = true)
        android.util.Log.d("DatabaseSeeder", "Database seeding completed successfully")
    }

    private suspend fun seedPresetFilters(categoryNameToId: Map<String, Long>) {
        val presetFilters = listOf(
            FilterEntity(
                name = "Food & Dining Expense",
                categoryIds = setOf(categoryNameToId["Food & Dining"] ?: 1L),
                transactionTypes = setOf(TransactionType.EXPENSE)
            ),
            FilterEntity(
                name = "Shopping Expense",
                categoryIds = setOf(categoryNameToId["Shopping"] ?: 3L),
                transactionTypes = setOf(TransactionType.EXPENSE)
            ),
            FilterEntity(
                name = "Salary Income",
                categoryIds = setOf(categoryNameToId["Income"] ?: 8L),
                transactionTypes = setOf(TransactionType.INCOME)
            ),
            FilterEntity(
                name = "Transportation Expense",
                categoryIds = setOf(categoryNameToId["Transportation"] ?: 2L),
                transactionTypes = setOf(TransactionType.EXPENSE)
            ),
            FilterEntity(
                name = "Entertainment Expense",
                categoryIds = setOf(categoryNameToId["Entertainment"] ?: 5L),
                transactionTypes = setOf(TransactionType.EXPENSE)
            )
        )

        presetFilters.forEach { filter ->
            try {
                filterDao.insertFilter(filter)
                android.util.Log.d("DatabaseSeeder", "Inserted preset filter: ${filter.name}")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseSeeder", "Failed to insert preset filter '${filter.name}': ${e.message}")
            }
        }
    }

    private suspend fun seedTemplates(categoryNameToId: Map<String, Long>): Map<String, Long> {
        val templates = listOf(
            TemplateEntity(
                name = "Monthly Rent",
                categoryId = categoryNameToId["Rent"] ?: 4L,
                amount = BigDecimal("1850.00"),
                transactionType = TransactionType.EXPENSE,
                currency = "USD",
                paymentMethod = PaymentMethod.BANK_TRANSFER,
                payee = "Property Management",
                note = "Monthly rent payment",
                tags = setOf("Housing", "Essential")
            ),
            TemplateEntity(
                name = "Quick Coffee",
                categoryId = categoryNameToId["Coffee & Snacks"] ?: 1L,
                amount = BigDecimal("6.50"),
                transactionType = TransactionType.EXPENSE,
                currency = "USD",
                paymentMethod = PaymentMethod.CASH,
                payee = "Starbucks",
                note = "Latte",
                tags = setOf("Social")
            ),
            TemplateEntity(
                name = "Salary Deposit",
                categoryId = categoryNameToId["Salary"] ?: 8L,
                amount = BigDecimal("5250.00"),
                transactionType = TransactionType.INCOME,
                currency = "USD",
                paymentMethod = PaymentMethod.BANK_TRANSFER,
                payee = "TechCorp Inc",
                note = "Monthly salary",
                tags = setOf("Work")
            ),
            TemplateEntity(
                name = "Gojek Bali",
                categoryId = categoryNameToId["Rideshare"] ?: 2L,
                amount = BigDecimal("35000.00"),
                transactionType = TransactionType.EXPENSE,
                currency = "IDR",
                paymentMethod = PaymentMethod.DIGITAL_WALLET,
                payee = "Gojek",
                note = "Ride in Bali",
                tags = setOf("Travel")
            )
        )

        val templateIds = mutableMapOf<String, Long>()
        templates.forEach { template ->
            val id = templateDao.insertTemplate(template)
            templateIds[template.name] = id
        }
        return templateIds
    }

    private suspend fun seedPlannedPayments(templateIds: Map<String, Long>) {
        val now = System.currentTimeMillis()
        val plannedPayments = listOf(
            PlannedPaymentEntity(
                templateId = templateIds["Monthly Rent"] ?: 0L,
                schedule = RecurrenceScheduleEntity(
                    recurrenceType = "MONTHLY",
                    dayOfMonth = 1,
                    startDate = now,
                    nextDueDate = now + 86400000L * 30
                ),
                autoConfirm = true
            ),
            PlannedPaymentEntity(
                templateId = templateIds["Salary Deposit"] ?: 0L,
                schedule = RecurrenceScheduleEntity(
                    recurrenceType = "MONTHLY",
                    dayOfMonth = 1,
                    startDate = now,
                    nextDueDate = now + 86400000L * 30
                ),
                autoConfirm = false
            ),
            PlannedPaymentEntity(
                templateId = templateIds["Quick Coffee"] ?: 0L,
                schedule = RecurrenceScheduleEntity(
                    recurrenceType = "WEEKLY",
                    dayOfWeek = 2, // Monday
                    startDate = now,
                    nextDueDate = now + 86400000L * 7
                ),
                autoConfirm = false
            )
        )

        plannedPayments.forEach { planned ->
            if (planned.templateId != 0L) {
                plannedPaymentDao.insert(planned)
            }
        }
    }

    private suspend fun seedBudgets(categoryNameToId: Map<String, Long>) {
        // Food Budget
        val foodFilter = FilterEntity(
            name = "Food Budget Filter",
            categoryIds = setOf(
                categoryNameToId["Food & Dining"] ?: 0L,
                categoryNameToId["Groceries"] ?: 0L,
                categoryNameToId["Coffee & Snacks"] ?: 0L
            ).filter { it != 0L }.toSet(),
            transactionTypes = setOf(TransactionType.EXPENSE)
        )
        val foodFilterId = filterDao.insertFilter(foodFilter)
        
        budgetDao.insert(
            BudgetEntity(
                name = "Monthly Food",
                currency = "USD",
                limitAmount = BigDecimal("800.00"),
                filterId = foodFilterId,
                profileId = 1
            )
        )

        // Travel Budget JPY
        val travelFilter = FilterEntity(
            name = "Japan Trip Filter",
            currencies = setOf("JPY"),
            categoryIds = setOf(categoryNameToId["Travel"] ?: 0L).filter { it != 0L }.toSet()
        )
        val travelFilterId = filterDao.insertFilter(travelFilter)

        budgetDao.insert(
            BudgetEntity(
                name = "Japan Trip",
                currency = "JPY",
                limitAmount = BigDecimal("200000.00"),
                filterId = travelFilterId,
                profileId = 1
            )
        )
    }

    private suspend fun seedLoans() {
        // Car Loan
        val carLoanFilter = FilterEntity(
            name = "Car Loan Filter",
            tags = setOf("CarLoan")
        )
        val carLoanFilterId = filterDao.insertFilter(carLoanFilter)

        loanDao.insert(
            LoanEntity(
                name = "Car Loan",
                principalAmount = BigDecimal("25000.00"),
                currency = "USD",
                interestRate = 3.5f,
                startDate = System.currentTimeMillis() - 86400000L * 100,
                lenderOrBorrowerName = "Chase Auto",
                loanType = "BORROWED",
                linkedAccountId = "acc_bank",
                profileId = 1,
                filterId = carLoanFilterId
            )
        )

        // Friend Loan
        val friendLoanFilter = FilterEntity(
            name = "Friend Loan Filter",
            queryPayee = "John Doe"
        )
        val friendLoanFilterId = filterDao.insertFilter(friendLoanFilter)

        loanDao.insert(
            LoanEntity(
                name = "Lent to John",
                principalAmount = BigDecimal("500.00"),
                currency = "USD",
                startDate = System.currentTimeMillis() - 86400000L * 10,
                lenderOrBorrowerName = "John Doe",
                loanType = "LENT",
                linkedAccountId = "acc_cash",
                profileId = 1,
                filterId = friendLoanFilterId
            )
        )
    }

    private suspend fun seedScenarioTransactions(categoryNameToId: Map<String, Long>) {
        val now = System.currentTimeMillis()
        val extraTransactions = listOf(
            // Car Loan payment
            TransactionEntity(
                name = "Car Loan Payment",
                amount = BigDecimal("450.00"),
                timestamp = now - 86400000L * 5,
                currency = "USD",
                transactionType = TransactionType.EXPENSE,
                accountId = "acc_bank",
                categoryId = categoryNameToId["Others"] ?: 1000L,
                paymentMethod = PaymentMethod.BANK_TRANSFER,
                payee = "Chase Auto",
                note = "Monthly installment",
                tags = setOf("CarLoan"),
                profileId = 1
            ),
            // Japan Trip expenses
            TransactionEntity(
                name = "Sushi Dinner Tokyo",
                amount = BigDecimal("8500.00"),
                timestamp = now - 86400000L * 2,
                currency = "JPY",
                transactionType = TransactionType.EXPENSE,
                accountId = "acc_jpy",
                categoryId = categoryNameToId["Travel"] ?: 0L,
                paymentMethod = PaymentMethod.CASH,
                payee = "Kyubey Sushi",
                note = "Amazing sushi",
                tags = setOf("Travel", "Food"),
                profileId = 1
            ),
            // Friend loan repayment (partial)
            TransactionEntity(
                name = "John Repayment",
                amount = BigDecimal("100.00"),
                timestamp = now - 86400000L * 1,
                currency = "USD",
                transactionType = TransactionType.INCOME,
                accountId = "acc_cash",
                categoryId = categoryNameToId["Others"] ?: 1000L,
                paymentMethod = PaymentMethod.CASH,
                payee = "John Doe",
                note = "Partial repayment of loan",
                tags = emptySet(),
                profileId = 1
            ),
            // EUR Transaction
            TransactionEntity(
                name = "Berlin Hotel",
                amount = BigDecimal("120.00"),
                timestamp = now - 86400000L * 10,
                currency = "EUR",
                transactionType = TransactionType.EXPENSE,
                accountId = "acc_bank",
                categoryId = categoryNameToId["Travel"] ?: 0L,
                paymentMethod = PaymentMethod.CREDIT_CARD,
                payee = "Hilton Berlin",
                note = "Business trip",
                tags = setOf("Travel"),
                profileId = 1
            ),
            // INR Transaction
            TransactionEntity(
                name = "Bangalore Dinner",
                amount = BigDecimal("2500.00"),
                timestamp = now - 86400000L * 3,
                currency = "INR",
                transactionType = TransactionType.EXPENSE,
                accountId = "acc_bank",
                categoryId = categoryNameToId["Food & Dining"] ?: 0L,
                paymentMethod = PaymentMethod.MOBILE_PAYMENT,
                payee = "Toit",
                note = "Dinner in Bangalore",
                tags = setOf("Social"),
                profileId = 1
            )
        )

        extraTransactions.forEach { 
            transactionDao.insertTransaction(it)
        }
    }

    private fun loadAccountsFromCsv(): List<AccountEntity> {
        val accounts = mutableListOf<AccountEntity>()
        try {
            val inputStream = context.assets.open("accounts.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Skip header
            reader.readLine()

            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        try {
                            accounts.add(AccountSeedRow.fromCsv(line).toAccountEntity())
                        } catch (e: Exception) {
                            android.util.Log.e("DatabaseSeeder", "Failed to parse account line: $line, error: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to load accounts.csv: ${e.message}")
        }
        return accounts
    }

    private fun loadTransactionsFromCsv(): List<TransactionSeedRow> {
        val transactions = mutableListOf<TransactionSeedRow>()
        try {
            val inputStream = context.assets.open("transactions.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Skip header
            reader.readLine()

            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        try {
                            transactions.add(TransactionSeedRow.fromCsv(line))
                        } catch (e: Exception) {
                            android.util.Log.e("DatabaseSeeder", "Failed to parse transaction line: $line, error: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to load transactions.csv: ${e.message}")
        }
        return transactions
    }

    private data class AccountSeedRow(
        val id: String,
        val name: String,
        val currencyCode: String,
        val structureType: String,
        val colorHex: String,
        val icon: String,
        val allowedCurrencies: String,
        val profileId: Int,
        val isArchived: Boolean = false,
        val displayOrder: Int = 0
    ) {
        fun toAccountEntity(): AccountEntity {
            return AccountEntity(
                id = id,
                name = name,
                currencyCode = currencyCode,
                structureType = structureType,
                colorHex = colorHex,
                icon = icon,
                allowedCurrencies = allowedCurrencies.removeSurrounding("\""),
                profileId = profileId,
                isArchived = isArchived,
                displayOrder = displayOrder
            )
        }

        companion object {
            fun fromCsv(line: String): AccountSeedRow {
                val parts = parseCsvLine(line)
                return AccountSeedRow(
                    id = parts.getOrNull(0) ?: "",
                    name = parts.getOrNull(1) ?: "",
                    currencyCode = parts.getOrNull(2) ?: "USD",
                    structureType = parts.getOrNull(3) ?: "REGULAR",
                    colorHex = parts.getOrNull(4) ?: "#000000",
                    icon = parts.getOrNull(5) ?: "",
                    allowedCurrencies = parts.getOrNull(6) ?: "USD",
                    profileId = parts.getOrNull(7)?.toIntOrNull() ?: 1,
                    isArchived = parts.getOrNull(8)?.toBoolean() ?: false,
                    displayOrder = parts.getOrNull(9)?.toIntOrNull() ?: 0
                )
            }
        }
    }

    private fun loadCategoriesFromCsv(): List<CategorySeedRow> {
        val categories = mutableListOf<CategorySeedRow>()
        try {
            val inputStream = context.assets.open("categories.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Skip header
            reader.readLine()

            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        try {
                            categories.add(CategorySeedRow.fromCsv(line))
                        } catch (e: Exception) {
                            android.util.Log.e("DatabaseSeeder", "Failed to parse category line: $line, error: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Failed to load categories.csv: ${e.message}")
        }
        return categories
    }

    private data class CategorySeedRow(
        val name: String,
        val iconName: String,
        val colorHex: String,
        val isDefault: Boolean,
        val parentName: String?
    ) {
        fun toCategoryEntity(): CategoryEntity {
            return CategoryEntity(
                name = name,
                iconName = iconName,
                colorHex = colorHex.removePrefix("#").toLongOrNull(16)?.toInt() ?: -16777216,
                isDefault = isDefault,
                parentId = null // Will be set when inserting child categories
            )
        }

        companion object {
            fun fromCsv(line: String): CategorySeedRow {
                val parts = parseCsvLine(line)
                return CategorySeedRow(
                    name = parts.getOrNull(0) ?: "",
                    iconName = parts.getOrNull(1) ?: "Category",
                    colorHex = parts.getOrNull(2) ?: "#000000",
                    isDefault = parts.getOrNull(3)?.toBoolean() ?: false,
                    parentName = parts.getOrNull(4)?.takeIf { it.isNotBlank() }
                )
            }
        }
    }

    private data class TransactionSeedRow(
        val amount: BigDecimal,
        val category: String,
        val categoryId: Long?,
        val timestamp: Long?,
        val currency: String,
        val transactionType: TransactionType,
        val accountId: String,
        val paymentMethod: PaymentMethod,
        val payee: String,
        val note: String,
        val tags: String,
        val sourcePlannedPaymentId: Long? = null,
        val profileId: Int,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val warrantyExpiration: Long? = null,
        val status: TransactionStatus = TransactionStatus.CLEARED
    ) {
        fun toTransactionEntity(): TransactionEntity {
            return TransactionEntity(
                name = category,
                amount = amount,
                timestamp = timestamp ?: System.currentTimeMillis(),
                currency = currency,
                transactionType = transactionType,
                accountId = accountId,
                categoryId = categoryId ?: 1002L,
                paymentMethod = paymentMethod,
                payee = payee,
                note = note,
                tags = tags.split(",").filter { it.isNotBlank() }.toSet(),
                sourcePlannedPaymentId = sourcePlannedPaymentId,
                profileId = profileId,
                latitude = latitude,
                longitude = longitude,
                warrantyExpiration = warrantyExpiration,
                status = status
            )
        }

        companion object {
            fun fromCsv(line: String): TransactionSeedRow {
                val parts = parseCsvLine(line)
                return TransactionSeedRow(
                    amount = parts.getOrNull(1)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    category = parts.getOrNull(2) ?: "",
                    categoryId = parts.getOrNull(3)?.toLongOrNull(),
                    timestamp = parts.getOrNull(4)?.toLongOrNull(),
                    currency = parts.getOrNull(5) ?: "USD",
                    transactionType = TransactionType.fromValue(parts.getOrNull(6) ?: "expense"),
                    accountId = parts.getOrNull(7) ?: "",
                    paymentMethod = PaymentMethod.fromString(parts.getOrNull(8) ?: "Cash"),
                    payee = parts.getOrNull(9) ?: "",
                    note = parts.getOrNull(10) ?: "",
                    tags = parts.getOrNull(11) ?: "",
                    sourcePlannedPaymentId = parts.getOrNull(12)?.toLongOrNull(),
                    profileId = parts.getOrNull(13)?.toIntOrNull() ?: 1,
                    latitude = parts.getOrNull(14)?.toDoubleOrNull(),
                    longitude = parts.getOrNull(15)?.toDoubleOrNull(),
                    warrantyExpiration = parts.getOrNull(16)?.toLongOrNull(),
                    status = TransactionStatus.fromString(parts.getOrNull(17) ?: "cleared")
                )
            }
        }
    }
}
