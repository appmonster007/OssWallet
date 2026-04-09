package com.osscabinet.gleemanager.domain.model

data class Transaction(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val timestamp: Long,
    val currency: String,
    val isIncome: Boolean,
    val accountId: String,
    val categoryId: Long? = null,
    val tags: List<String> = emptyList()
)
