package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import java.math.BigDecimal

@Entity(
    tableName = "planned_payments",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["templateId"]),
        Index(value = ["nextDueDate"]),
        Index(value = ["status"]),
        Index(value = ["categoryId"])
    ]
)
data class PlannedPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long,
    val amount: BigDecimal,
    val transactionType: Int, // 0: Expense, 1: Income, 2: Transfer
    val accountId: String? = null,
    val targetAccountId: String? = null,
    val targetCurrency: String? = null,
    val transferRate: BigDecimal = BigDecimal.ONE,
    val note: String? = null,
    val payee: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val warrantyExpiration: Long? = null,
    val transactionStatus: TransactionStatus = TransactionStatus.CLEARED,
    val paymentMethod: PaymentMethod? = null,
    val currency: String,
    val tags: String = "", // Comma-separated tags
    @Embedded val schedule: RecurrenceScheduleEntity,
    val autoConfirm: Boolean = false,
    val status: PlannedPaymentStatus = PlannedPaymentStatus.ACTIVE,
    val profileId: Int = 1,
    val templateId: Long? = null // Optional link to a template
)

data class RecurrenceScheduleEntity(
    val recurrenceType: String,
    val interval: Int = 1,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val recurrenceCronExpression: String? = null,
    val startDate: Long,
    val endDate: Long? = null,
    val nextDueDate: Long,
    val lastTriggeredDate: Long? = null
)

/**
 * Legacy support for relations if needed, but now fields are flat in PlannedPaymentEntity
 */
data class PlannedPaymentWithTemplate(
    @Embedded val payment: PlannedPaymentEntity,
    @Relation(
        parentColumn = "templateId",
        entityColumn = "id"
    )
    val template: TemplateEntity?
)
