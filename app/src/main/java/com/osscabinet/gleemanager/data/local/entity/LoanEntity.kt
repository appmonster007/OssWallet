package com.osscabinet.gleemanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.osscabinet.gleemanager.domain.model.Loan
import com.osscabinet.gleemanager.domain.model.LoanType

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val principal: Double,
    val interestRate: Double,
    val dueDate: Long,
    val repayments: Double,
    val type: String,
    val currency: String
) {
    fun toDomain() = Loan(
        id = id,
        title = title,
        principal = principal,
        interestRate = interestRate,
        dueDate = dueDate,
        repayments = repayments,
        type = LoanType.valueOf(type),
        currency = currency
    )

    companion object {
        fun fromDomain(loan: Loan) = LoanEntity(
            id = loan.id,
            title = loan.title,
            principal = loan.principal,
            interestRate = loan.interestRate,
            dueDate = loan.dueDate,
            repayments = loan.repayments,
            type = loan.type.name,
            currency = loan.currency
        )
    }
}
