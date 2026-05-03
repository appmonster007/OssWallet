package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "loans",
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["status"]),
        Index(value = ["linkedAccountId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedAccountId"],
            onDelete = ForeignKey.SET_NULL
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
    val categoryIds: String = "",
    val tags: String = ""
)
