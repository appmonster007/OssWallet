package com.osscabinet.gleemanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val amount: Double,
    val transactionType: Int, // 0: Expense, 1: Income, 2: Transfer
    val icon: String? = null,
    val colorHex: String? = null,
    val accountId: String? = null,
    val note: String? = null,
    val payee: String? = null,
    val location: String? = null,
    val paymentMethod: String? = null,
    val currency: String? = null,
    val tags: String = "", // Comma-separated tags
    val isStarred: Boolean = false,
    val displayOrder: Int = 0
)
