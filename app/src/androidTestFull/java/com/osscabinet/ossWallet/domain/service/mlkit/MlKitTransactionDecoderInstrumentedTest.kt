package com.osscabinet.ossWallet.domain.service.mlkit

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.entityextraction.DateTimeEntity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractionParams
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import com.google.mlkit.nl.entityextraction.MoneyEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.EntryMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Runs the real Google ML Kit entity extractor on a device/emulator. Unlike the JVM unit test,
 * this proves the `full` flavor's ML Kit path actually loads and produces entities.
 */
@RunWith(AndroidJUnit4::class)
class MlKitTransactionDecoderInstrumentedTest {

    private lateinit var decoder: MlKitTransactionDecoder
    private lateinit var accounts: List<Account>
    private lateinit var categories: List<Category>

    @Before
    fun setUp() {
        decoder = MlKitTransactionDecoder()
        accounts = listOf(
            Account(
                id = "acc-chase",
                name = "Chase Checking",
                currencyCode = "USD",
                structureType = AccountStructureType.BANK,
                colorHex = "#00FF00",
                icon = "wallet"
            ),
            Account(
                id = "acc-scapia-4321",
                name = "Scapia Federal Visa",
                currencyCode = "INR",
                structureType = AccountStructureType.CREDIT,
                colorHex = "#FF0000",
                icon = "credit_card"
            )
        )
        categories = listOf(
            Category(
                id = 101L,
                name = "Groceries",
                colorHex = 0x123456,
                isDefault = false,
                parentId = null,
                nature = CategoryNature.NEED,
                iconName = "shopping_cart"
            )
        )
    }

    @Test
    fun mlKit_extractsMoneyEntity_fromRealModel() = runBlocking {
        val extractor = EntityExtraction.getClient(
            EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
        )
        try {
            Tasks.await(extractor.downloadModelIfNeeded())
            val annotations = Tasks.await(
                extractor.annotate(EntityExtractionParams.Builder("Paid \$25.00 for Groceries").build())
            )
            val money = annotations.flatMap { it.entities }.filterIsInstance<MoneyEntity>()
            assertTrue("ML Kit returned no MoneyEntity", money.isNotEmpty())
            assertEquals(25, money.first().integerPart)
        } finally {
            extractor.close()
        }
    }

    @Test
    fun decodeText_parsesDebitExpense_withMlKit() = runBlocking {
        val result = decoder.decodeText(
            "Debit alert: \$45.50 spent at Starbucks on Chase Checking account.",
            accounts,
            categories
        )

        assertNull("ML Kit failed; decoder fell back to regex", decoder.lastMlKitError)
        assertTrue("ML Kit found no entities; decoder used regex only", decoder.lastMlKitEntityCount > 0)
        assertEquals(0, BigDecimal("45.5").compareTo(BigDecimal(result.amount)))
        assertEquals(EntryMode.EXPENSE, result.entryMode)
        assertEquals("USD", result.currency)
        assertEquals("acc-chase", result.accountId)
        assertEquals("Starbucks", result.payee)
    }

    @Test
    fun decodeText_parsesScapiaCardTransaction_withMlKit() = runBlocking {
        val result = decoder.decodeText(
            "Hi! Your txn of ₹9,936.51 at Dsb App Taastrup Dk on your Scapia Federal Visa credit card was successful. Not you? Go to Scapia support on the app.- Federal Bank",
            accounts,
            categories
        )

        assertNull("ML Kit failed; decoder fell back to regex", decoder.lastMlKitError)
        assertTrue("ML Kit found no entities; decoder used regex only", decoder.lastMlKitEntityCount > 0)
        assertEquals(0, BigDecimal("9936.51").compareTo(BigDecimal(result.amount)))
        assertEquals(EntryMode.EXPENSE, result.entryMode)
        assertEquals("INR", result.currency)
        assertEquals("acc-scapia-4321", result.accountId)
    }

    @Test
    fun mlKit_extractsDateTimeEntity_fromRealModel() = runBlocking {
        val extractor = EntityExtraction.getClient(
            EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
        )
        try {
            Tasks.await(extractor.downloadModelIfNeeded())
            val annotations = Tasks.await(
                extractor.annotate(
                    EntityExtractionParams.Builder("Debit alert: \$45.50 spent at Starbucks on March 12, 2026.").build()
                )
            )
            val dateTime = annotations.flatMap { it.entities }.filterIsInstance<DateTimeEntity>().firstOrNull()
            assertNotNull("ML Kit returned no DateTimeEntity", dateTime)
            assertEquals(LocalDate.of(2026, 3, 12), localDateOf(dateTime!!.timestampMillis))
        } finally {
            extractor.close()
        }
    }

    @Test
    fun decodeText_usesMlKitDateTime_forTransactionDate() = runBlocking {
        // Today is not March 12 2026, so this only passes if ML Kit's date is used, not the fallback clock.
        val result = decoder.decodeText(
            "Debit alert: \$45.50 spent at Starbucks on March 12, 2026.",
            accounts,
            categories
        )

        assertNull("ML Kit failed; decoder fell back to regex", decoder.lastMlKitError)
        assertTrue("ML Kit found no entities; decoder used regex only", decoder.lastMlKitEntityCount > 0)
        assertEquals(LocalDate.of(2026, 3, 12), localDateOf(requireNotNull(result.timestamp)))
    }

    private fun localDateOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}
