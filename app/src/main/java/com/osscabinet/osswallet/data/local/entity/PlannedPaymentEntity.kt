package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "planned_payments",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["accountId"]),
        Index(value = ["nextDueDate"]),
        Index(value = ["status"])
    ]
)
data class PlannedPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: BigDecimal,
    val currency: String,
    val transactionType: String,
    val accountId: String,
    val targetAccountId: String? = null,
    val categoryId: Long? = null,
    val paymentMethod: String = "Cash",
    val tags: String = "",
    val note: String = "",
    val payee: String = "",
    val recurrenceType: String,
    val recurrenceInterval: Int = 1,
    val recurrenceDayOfWeek: Int? = null,
    val recurrenceDayOfMonth: Int? = null,
    val startDate: Long,
    val endDate: Long? = null,
    val nextDueDate: Long,
    val lastTriggeredDate: Long? = null,
    val autoConfirm: Boolean = false,
    val status: String = "ACTIVE",
    val profileId: Int = 1
)
