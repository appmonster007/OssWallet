package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import java.math.BigDecimal

@Entity(
    tableName = "loans",
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["status"]),
        Index(value = ["linkedAccountId"]),
        Index(value = ["filterId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedAccountId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = FilterEntity::class,
            parentColumns = ["id"],
            childColumns = ["filterId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val principalAmount: BigDecimal,
    val currency: String,
    val interestRate: Float? = null,
    val startDate: Long,
    val lenderOrBorrowerName: String,
    val loanType: String,
    val linkedAccountId: String,
    val expectedSettlementDate: Long? = null,
    val status: String = "ACTIVE",
    val profileId: Int,
    val note: String = "",
    val timelineRange: TimelineRange,
    val filterId: Long
)

data class LoanWithFilter(
    @Embedded val loan: LoanEntity,
    @Relation(
        parentColumn = "filterId",
        entityColumn = "id"
    )
    val filter: FilterEntity
)
