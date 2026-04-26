package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.osscabinet.ossWallet.domain.model.Loan
import com.osscabinet.ossWallet.domain.model.LoanType

import java.math.BigDecimal

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val principal: BigDecimal,
    val interestRate: BigDecimal,
    val dueDate: Long,
    val repayments: BigDecimal,
    val type: String,
    val currency: String,
    val profileId: Int = 1
)
