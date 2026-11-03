package com.osscabinet.ossWallet.domain.service.mlkit

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.entityextraction.DateTimeEntity
import com.google.mlkit.nl.entityextraction.Entity
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

    // ML Kit only: these call the extractor directly and never reach the decoder's regex fallback.

    @Test
    fun mlKit_moneyEntity_usdWholeDollars() = assertMoney("Paid \$25.00 for Groceries", 25, 0)

    @Test
    fun mlKit_moneyEntity_usdCents() = assertMoney("Debit alert: \$45.50 spent at Starbucks.", 45, 50)

    @Test
    fun mlKit_moneyEntity_usdThousands() = assertMoney("Credited \$1200.00 from Salary deposit.", 1200, 0)

    @Test
    fun mlKit_moneyEntity_inrGroupedThousands() =
        assertMoney("Hi! Your txn of ₹9,936.51 at Dsb App Taastrup Dk.", 9936, 51)

    @Test
    fun mlKit_moneyEntity_gbp() = assertMoney("Paid £89.99 for Groceries at Tesco.", 89, 99)

    @Test
    fun mlKit_moneyEntity_aedCode() = assertMoney("Purchase alert: AED 350.00 charged at Dubai Mall.", 350, 0)

    @Test
    fun mlKit_moneyEntity_europeanFormat() = assertMoney("Bank alert: €1.250,50 debited at Berlin Store.", 1250, 50)

    @Test
    fun mlKit_returnsNoMoneyEntity_whenTextHasNoAmount() {
        val money = annotate("Your balance is unchanged and the card is active.").filterIsInstance<MoneyEntity>()
        assertTrue("ML Kit found a MoneyEntity in text with no amount: $money", money.isEmpty())
    }

    @Test
    fun mlKit_dateTimeEntity_longFormDate() = assertDate("Debit alert: \$45.50 spent on March 12, 2026.", LocalDate.of(2026, 3, 12))

    @Test
    fun mlKit_dateTimeEntity_isoDate() = assertDate("Payment received on 2026-03-12 at Starbucks.", LocalDate.of(2026, 3, 12))

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

    private fun assertMoney(text: String, integerPart: Int, fractionalPart: Int) {
        val money = annotate(text).filterIsInstance<MoneyEntity>()
        assertTrue("ML Kit found no MoneyEntity in: $text", money.isNotEmpty())
        assertEquals(integerPart, money.first().integerPart)
        assertEquals(fractionalPart, money.first().fractionalPart)
    }

    private fun assertDate(text: String, expected: LocalDate) {
        val dateTime = annotate(text).filterIsInstance<DateTimeEntity>().firstOrNull()
        assertNotNull("ML Kit found no DateTimeEntity in: $text", dateTime)
        assertEquals(expected, localDateOf(dateTime!!.timestampMillis))
    }

    /** Runs the ML Kit extractor directly, bypassing [MlKitTransactionDecoder] and its regex fallback. */
    private fun annotate(text: String): List<Entity> {
        val extractor = EntityExtraction.getClient(
            EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
        )
        try {
            Tasks.await(extractor.downloadModelIfNeeded())
            return Tasks.await(extractor.annotate(EntityExtractionParams.Builder(text).build()))
                .flatMap { it.entities }
        } finally {
            extractor.close()
        }
    }

    private fun localDateOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}
