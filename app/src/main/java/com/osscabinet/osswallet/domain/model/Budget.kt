package com.osscabinet.ossWallet.domain.model

data class Budget(
    val category: String,
    val spent: Double,
    val limit: Double,
    val currency: String = "USD"
) {
    val progress: Float
        get() = if (limit > 0) (spent / limit).toFloat() else 0f
    
    val remaining: Double
        get() = (limit - spent).coerceAtLeast(0.0)
    
    val isOverBudget: Boolean
        get() = spent > limit
}
