package com.osscabinet.ossWallet.domain.service

import com.osscabinet.ossWallet.domain.calculator.*
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.model.analytics.CardRenderData
import com.osscabinet.ossWallet.domain.model.settings.*

/**
 * Registry-based engine that orchestrates card computations.
 * Maps CardConfig types to their respective CardCalculator strategies.
 */
object CardComputationEngine {
    private val calculators = mapOf<Class<out CardConfig>, CardCalculator<*, *>>(
        NetWorthCardConfig::class.java to NetWorthCalculator(),
        CashFlowCardConfig::class.java to CashFlowCalculator(),
        CurrencyBalancesCardConfig::class.java to CurrencyBalancesCalculator(),
        AccountsCardConfig::class.java to AccountsCalculator(),
        CategoryBreakdownCardConfig::class.java to CategoryBreakdownCalculator(),
        CategoryExpenseTreemapCardConfig::class.java to CategoryTreemapCalculator()
    )

    // Simple in-memory cache
    private val cache = mutableMapOf<String, CardRenderData>()

    /**
     * Computes render-ready data for a given card configuration.
     * 
     * @param config The configuration for the card to compute.
     * @param accounts Current list of accounts.
     * @param transactions Current list of transactions.
     * @param categories Current list of categories.
     * @param context Computation context (base currency, rates, etc.)
     * @return CardRenderData ready for UI consumption.
     */
    @Suppress("UNCHECKED_CAST")
    fun <C : CardConfig> computeCard(
        config: C,
        accounts: List<Account>,
        transactions: List<Transaction>,
        categories: List<Category>,
        context: ComputationContext
    ): CardRenderData {
        val cacheKey = generateCacheKey(config, accounts, transactions, categories, context)
        cache[cacheKey]?.let { return it }

        val calculator = calculators[config.javaClass] as? CardCalculator<C, CardRenderData>
            ?: throw IllegalArgumentException("No calculator registered for ${config.javaClass.simpleName}")
        
        val result = calculator.compute(config, accounts, transactions, categories, context)
        cache[cacheKey] = result
        return result
    }

    private fun generateCacheKey(
        config: CardConfig,
        accounts: List<Account>,
        transactions: List<Transaction>,
        categories: List<Category>,
        context: ComputationContext
    ): String {
        // Simple hash-based key. In a real app, we might want a more robust versioning system.
        return "${config.hashCode()}_${accounts.hashCode()}_${transactions.hashCode()}_${categories.hashCode()}_${context.hashCode()}"
    }

    /**
     * Clears the entire cache. Useful when global state changes significantly.
     */
    fun clearCache() {
        cache.clear()
    }
}
