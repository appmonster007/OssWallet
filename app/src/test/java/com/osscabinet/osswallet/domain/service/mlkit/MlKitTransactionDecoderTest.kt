package com.osscabinet.ossWallet.domain.service.mlkit

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.EntryMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MlKitTransactionDecoderTest {

    private lateinit var decoder: MlKitTransactionDecoder
    private lateinit var sampleAccounts: List<Account>
    private lateinit var sampleCategories: List<Category>

    @Before
    fun setUp() {
        decoder = MlKitTransactionDecoder()
        sampleAccounts = listOf(
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
        sampleCategories = listOf(
            Category(
                id = 101L,
                name = "Groceries",
                colorHex = 0x123456,
                isDefault = false,
                parentId = null,
                nature = CategoryNature.NEED,
                iconName = "shopping_cart"
            ),
            Category(
                id = 102L,
                name = "Food",
                colorHex = 0x654321,
                isDefault = false,
                parentId = null,
                nature = CategoryNature.WANT,
                iconName = "restaurant"
            )
        )
    }

    @Test
    fun decodeText_parsesDebitExpenseSms() = runTest {
        val smsText = "Debit alert: $45.50 spent at Starbucks on Chase Checking account."
        val result = decoder.decodeText(smsText, sampleAccounts, sampleCategories)

        assertEquals("45.5", result.amount)
        assertEquals(EntryMode.EXPENSE, result.entryMode)
        assertEquals("USD", result.currency)
        assertEquals("acc-chase", result.accountId)
        assertEquals("Starbucks", result.payee)
    }

    @Test
    fun decodeText_parsesCreditIncomeSms() = runTest {
        val smsText = "Credited $1200.00 from Salary deposit into Chase Checking."
        val result = decoder.decodeText(smsText, sampleAccounts, sampleCategories)

        assertEquals("1200.0", result.amount)
        assertEquals(EntryMode.INCOME, result.entryMode)
        assertEquals("USD", result.currency)
    }

    @Test
    fun decodeText_parsesTransferSms() = runTest {
        val smsText = "Transfer alert: $200 moved to Savings account."
        val result = decoder.decodeText(smsText, sampleAccounts, sampleCategories)

        assertEquals("200", result.amount)
        assertEquals(EntryMode.TRANSFER, result.entryMode)
    }

    @Test
    fun decodeText_matchesCategoryWhenMentionedInText() = runTest {
        val smsText = "Paid $25.00 for Groceries at Market Place"
        val result = decoder.decodeText(smsText, sampleAccounts, sampleCategories)

        assertEquals(101L, result.categoryId)
    }

    @Test
    fun decodeText_parsesScapiaFederalCardTransactionDsbApp() = runTest {
        val smsText = "Hi! Your txn of ₹9,936.51 at Dsb App Taastrup Dk on your Scapia Federal Visa credit card was successful. Not you? Go to Scapia support on the app.- Federal Bank"
        val result = decoder.decodeText(smsText, sampleAccounts, sampleCategories)

        assertEquals("9936.51", result.amount)
        assertEquals(EntryMode.EXPENSE, result.entryMode)
        assertEquals("INR", result.currency)
        assertEquals("acc-scapia-4321", result.accountId)
        assertEquals("Dsb App Taastrup Dk", result.payee)
    }

    @Test
    fun decodeText_parsesScapiaFederalCardTransactionKantinen() = runTest {
        val smsText = "Hi! Your txn of ₹242.70 at Kantinen I Mate Aarhus C  on your Scapia Federal Visa credit card was successful. Not you? Go to Scapia support on the app.- Federal Bank"
        val result = decoder.decodeText(smsText, sampleAccounts, sampleCategories)

        assertEquals("242.7", result.amount)
        assertEquals(EntryMode.EXPENSE, result.entryMode)
        assertEquals("INR", result.currency)
        assertEquals("acc-scapia-4321", result.accountId)
        assertEquals("Kantinen I Mate Aarhus C", result.payee)
    }

    @Test
    fun decodeText_parsesEuropeanCurrencyAndFormat() = runTest {
        val smsText = "Bank alert: €1.250,50 debited at Berlin Store using Visa card."
        val result = decoder.decodeText(smsText, sampleAccounts, sampleCategories)

        assertEquals("1250.50", result.amount)
        assertEquals("EUR", result.currency)
        assertEquals(EntryMode.EXPENSE, result.entryMode)
        assertEquals("Berlin Store", result.payee)
    }

    @Test
    fun decodeText_parsesMiddleEasternCurrency() = runTest {
        val smsText = "Purchase alert: AED 350.00 charged at Dubai Mall."
        val result = decoder.decodeText(smsText, sampleAccounts, sampleCategories)

        assertEquals("350.00", result.amount)
        assertEquals("AED", result.currency)
        assertEquals("Dubai Mall", result.payee)
    }

    @Test
    fun decodeText_parsesBritishPoundCurrency() = runTest {
        val smsText = "Paid £89.99 for Groceries at Tesco on credit card."
        val result = decoder.decodeText(smsText, sampleAccounts, sampleCategories)

        assertEquals("89.99", result.amount)
        assertEquals("GBP", result.currency)
        assertEquals("Tesco", result.payee)
        assertEquals(101L, result.categoryId)
    }

    @Test
    fun decodeText_mapsAccountByLast4Digits() = runTest {
        val smsText = "Debit alert: $15.00 spent at Target on card ending 4321."
        val result = decoder.decodeText(smsText, sampleAccounts, sampleCategories)

        assertEquals("acc-scapia-4321", result.accountId)
    }
}
