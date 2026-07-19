package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.math.BigDecimal

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["isArchived"]),
        Index(value = ["filterId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = FilterEntity::class,
            parentColumns = ["id"],
            childColumns = ["filterId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currency: String,
    val limitAmount: BigDecimal,
    val alertThreshold: Float = 0.8f,
    val rollover: Boolean = false,
    val isArchived: Boolean = false,
    val profileId: Int,
    val note: String = "",
    val filterId: Long
)

data class BudgetWithFilter(
    @Embedded val budget: BudgetEntity,
    @Relation(
        parentColumn = "filterId",
        entityColumn = "id"
    )
    val filter: FilterEntity
)
