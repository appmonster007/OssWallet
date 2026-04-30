package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import org.junit.Assert.assertSame
import org.junit.Assert.assertNotSame
import org.junit.Test
import java.math.BigDecimal

class CardComputationCacheTest {

    private val exchangeRates = mapOf("USD" to BigDecimal.ONE)
    private val accounts = listOf(
        Account("1", "Test", "USD", "Cash", "#FFFFFF", "icon")
    )
    private val transactions = emptyList<Transaction>()
    private val categories = emptyList<Category>()
    private val registry = CardComputerRegistry.DEFAULT

    private fun createInput(accs: List<Account> = accounts) = CardInput(
        transactions = transactions,
        accounts = accs,
        categories = categories,
        exchangeRates = exchangeRates,
        baseCurrency = "USD",
        precision = 2
    )

    @Test
    fun `compute returns cached result for same input`() {
        val config = NetWorthCardConfig(cardId = "test_card")
        val input = createInput()
        
        val firstResult = registry.compute(CardType.NET_WORTH, input, config)
        val secondResult = registry.compute(CardType.NET_WORTH, input, config)
        
        assertSame("Results should be the same instance (cached)", firstResult, secondResult)
    }

    @Test
    fun `compute returns new result when data changes`() {
        val config = NetWorthCardConfig(cardId = "test_card")
        
        val firstResult = registry.compute(CardType.NET_WORTH, createInput(), config)
        
        val modifiedAccounts = accounts + Account("2", "New", "USD", "Cash", "#FFFFFF", "icon")
        val secondResult = registry.compute(CardType.NET_WORTH, createInput(modifiedAccounts), config)
        
        assertNotSame("Results should be different instances", firstResult, secondResult)
    }
}
