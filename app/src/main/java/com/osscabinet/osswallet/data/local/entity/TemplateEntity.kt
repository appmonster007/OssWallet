package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "templates",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long,
    val amount: BigDecimal,
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
