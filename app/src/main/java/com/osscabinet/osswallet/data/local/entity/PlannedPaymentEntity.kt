package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Embedded
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
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
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
    @Embedded val template: TransactionTemplateEntity,
    @Embedded val schedule: RecurrenceScheduleEntity,
    val autoConfirm: Boolean = false,
    val status: String = "ACTIVE",
    val profileId: Int = 1
)

data class TransactionTemplateEntity(
    val title: String,
    val amount: BigDecimal,
    val currency: String,
    val transactionType: String,
    val accountId: String,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    val transferRate: BigDecimal = BigDecimal.ONE,
    val categoryId: Long,
    val paymentMethod: String = "Cash",
    val tags: String = "",
    val note: String = "",
    val payee: String = ""
)

data class RecurrenceScheduleEntity(
    val recurrenceCronExpression: String? = null,
    val startDate: Long,
    val endDate: Long? = null,
    val nextDueDate: Long,
    val lastTriggeredDate: Long? = null
)
