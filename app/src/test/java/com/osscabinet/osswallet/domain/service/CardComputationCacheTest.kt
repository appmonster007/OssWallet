package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.model.settings.NetWorthCardConfig
import org.junit.Assert.assertSame
import org.junit.Assert.assertNotSame
import org.junit.Test
import java.math.BigDecimal

class CardComputationCacheTest {

    private val context = ComputationContext("USD", emptyMap(), 2)
    private val accounts = listOf(
        Account("1", "Test", BigDecimal.TEN, "USD", "Cash", "#FFFFFF", "icon", emptyList(), false, 0)
    )
    private val transactions = emptyList<Transaction>()
    private val categories = emptyList<Category>()

    @Test
    fun `computeCard returns cached result for same input`() {
        val config = NetWorthCardConfig(cardId = "test_card")
        
        val firstResult = CardComputationEngine.computeCard(config, accounts, transactions, categories, context)
        val secondResult = CardComputationEngine.computeCard(config, accounts, transactions, categories, context)
        
        assertSame("Results should be the same instance (cached)", firstResult, secondResult)
    }

    @Test
    fun `computeCard returns new result when data changes`() {
        val config = NetWorthCardConfig(cardId = "test_card")
        
        val firstResult = CardComputationEngine.computeCard(config, accounts, transactions, categories, context)
        
        val modifiedAccounts = accounts + Account("2", "New", BigDecimal.ONE, "USD", "Cash", "#FFFFFF", "icon", emptyList(), false, 1)
        val secondResult = CardComputationEngine.computeCard(config, modifiedAccounts, transactions, categories, context)
        
        assertNotSame("Results should be different instances", firstResult, secondResult)
    }
}
