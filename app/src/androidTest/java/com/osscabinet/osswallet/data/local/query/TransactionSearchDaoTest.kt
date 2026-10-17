package com.osscabinet.ossWallet.data.local.query

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/** The SQL behind global search, against a real (in-memory) database. */
@RunWith(AndroidJUnit4::class)
class TransactionSearchDaoTest {
    private lateinit var db: AppDatabase
    private val dao get() = db.transactionDao()

    @Before
    fun setup() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        db.accountDao().upsertAccount(
            AccountEntity(id = "acc1", name = "Checking", currencyCode = "USD", structureType = "STANDARD",
                colorHex = "#fff", icon = "bank", allowedCurrencies = "", profileId = 1)
        )
        db.categoryDao().insertCategory(CategoryEntity(id = 1, name = "Electronics", iconName = "x", colorHex = 0))
        db.categoryDao().insertCategory(CategoryEntity(id = 2, name = "Groceries", iconName = "x", colorHex = 0))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun add(
        name: String, amount: String, ts: Long, payee: String = "", note: String = "", category: Long = 2,
        tags: Set<String> = emptySet(), profile: Int = 1, type: TransactionType = TransactionType.EXPENSE
    ) = dao.insertTransaction(
        TransactionEntity(name = name, amount = BigDecimal(amount), timestamp = ts, currency = "USD", transactionType = type,
            accountId = "acc1", categoryId = category, payee = payee, note = note, tags = tags, profileId = profile)
    )

    @Test
    fun text_matchesNotePayeeAndName_byPrefix_newestFirst() = runBlocking<Unit> {
        add("Gadget", "-1200", 1000, payee = "Dell", note = "work laptop")
        add("Laptop sleeve", "-30", 3000, payee = "Amazon")
        add("Milk", "-2", 2000, payee = "Corner shop")

        val hits = dao.searchText(1, "lap*", "lap", 50)
        assertEquals(listOf("Laptop sleeve", "Gadget"), hits.map { it.name })
        assertEquals(listOf("Gadget"), dao.searchText(1, "dell*", "dell", 50).map { it.name })
    }

    @Test
    fun text_alsoFindsByCategoryName() = runBlocking<Unit> {
        add("Charger", "-20", 1000, category = 1)
        add("Milk", "-2", 2000, category = 2)
        assertEquals(listOf("Charger"), dao.searchText(1, "electr*", "electr", 50).map { it.name })
    }

    @Test
    fun text_isScopedToTheProfile_andRespectsTheLimit() = runBlocking<Unit> {
        add("Laptop A", "-1", 1000)
        add("Laptop B", "-1", 2000)
        add("Laptop C", "-1", 3000, profile = 2)
        assertEquals(2, dao.searchText(1, "laptop*", "laptop", 50).size)
        assertEquals(1, dao.searchText(1, "laptop*", "laptop", 1).size)
    }

    @Test
    fun tag_matchesCaseInsensitively_andPercentIsJustACharacter() = runBlocking<Unit> {
        add("Flights", "-300", 1000, tags = setOf("Holiday"))
        add("Hotel", "-500", 2000, tags = setOf("holiday", "family"))
        add("Milk", "-2", 3000, tags = setOf("food"))

        assertEquals(listOf("Hotel", "Flights"), dao.searchByTag(1, "HOLI", 50).map { it.name })
        assertTrue("a typed % must not act as a wildcard", dao.searchByTag(1, "%", 50).isEmpty())
    }

    @Test
    fun amount_matchesMagnitude_forExpensesAndIncome_withinTheRange() = runBlocking<Unit> {
        add("Rent", "-1200.00", 1000)
        add("Salary", "1200.00", 2000, type = TransactionType.INCOME)
        add("Coffee", "-4.50", 3000)

        assertEquals(setOf("Rent", "Salary"), dao.searchByAmount(1, 1199.995, 1200.005, 50).map { it.name }.toSet())
        assertEquals(listOf("Coffee"), dao.searchByAmount(1, 0.0, 10.0, 50).map { it.name })
        assertEquals(2, dao.searchByAmount(1, 1000.0, Double.MAX_VALUE, 50).size)
    }

    @Test
    fun amount_isScopedToTheProfile() = runBlocking<Unit> {
        add("Mine", "-50", 1000)
        add("Theirs", "-50", 2000, profile = 2)
        assertEquals(listOf("Mine"), dao.searchByAmount(1, 49.0, 51.0, 50).map { it.name })
    }
}
