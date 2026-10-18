package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "credit_card_details",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CreditCardDetailsEntity(
    @PrimaryKey val accountId: String,
    val creditLimit: BigDecimal,
    val statementClosingDay: Int? = null,
    val paymentDueDay: Int? = null,
    val interestRateApr: Float? = null,
    val minimumPaymentAmount: BigDecimal? = null
)
