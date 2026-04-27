package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "loan_metadata")
data class LoanEntity(
    @PrimaryKey val accountId: String, // Links to AccountEntity.id
    val principal: BigDecimal,
    val interestRate: BigDecimal,
    val maturityDate: Long,
    val loanType: String, // "CREDIT" or "DEBIT"
    val profileId: Int = 1
)
