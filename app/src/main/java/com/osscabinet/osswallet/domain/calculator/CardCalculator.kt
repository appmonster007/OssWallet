package com.osscabinet.ossWallet.domain.calculator

import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.analytics.ComputationContext
import com.osscabinet.ossWallet.domain.model.analytics.CardRenderData
import com.osscabinet.ossWallet.domain.model.settings.CardConfig

/**
 * Strategy interface for computing card data.
 * Pure Kotlin, independent of ViewModels or UI.
 */
interface CardCalculator<in C : CardConfig, out R : CardRenderData> {
    fun compute(
        config: C,
        accounts: List<Account>,
        transactions: List<Transaction>,
        categories: List<Category>,
        context: ComputationContext
    ): R
}
