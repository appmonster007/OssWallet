package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus

@Entity(
    tableName = "planned_payment_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = PlannedPaymentEntity::class,
            parentColumns = ["id"],
            childColumns = ["plannedPaymentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["plannedPaymentId", "scheduledDate"]),
        Index(value = ["status"])
    ]
)
data class PlannedPaymentOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plannedPaymentId: Long,
    val scheduledDate: Long,
    val generatedTransactionId: Long? = null,
    val status: OccurrenceStatus = OccurrenceStatus.PENDING
)
