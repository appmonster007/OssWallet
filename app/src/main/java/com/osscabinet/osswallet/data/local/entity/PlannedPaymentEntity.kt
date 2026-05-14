package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus

@Entity(
    tableName = "planned_payments",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["templateId"]),
        Index(value = ["status"]),
    ]
)
data class PlannedPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Embedded val schedule: RecurrenceScheduleEntity,
    val autoConfirm: Boolean = false,
    val status: PlannedPaymentStatus = PlannedPaymentStatus.ACTIVE,
    val profileId: Int = 1,
    val templateId: Long
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

data class PlannedPaymentWithTemplate(
    @Embedded val payment: PlannedPaymentEntity,
    @Relation(
        parentColumn = "templateId",
        entityColumn = "id"
    )
    val template: TemplateEntity
)
