package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import java.math.BigDecimal

enum class LoanType { LENT, BORROWED }
enum class LoanStatus { ACTIVE, SETTLED }

data class Loan(
    val id: Long = 0,
    val name: String,
    val principalAmount: BigDecimal,
    val currency: String,
    val interestRate: Float? = null,
    val startDate: Long,
    val lenderOrBorrowerName: String,
    val loanType: LoanType,
    val linkedAccountId: String,
    val expectedSettlementDate: Long? = null,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val profileId: Int,
    val note: String = "",
    val categoryIds: Set<Long> = emptySet(),
    val tags: Set<String> = emptySet(),
    val trackTypes: Set<TransactionType> = emptySet(),
    val currencies: Set<String> = emptySet(),
    val paymentMethods: Set<PaymentMethod> = emptySet(),
    val statuses: Set<TransactionStatus> = emptySet(),
    val linkedTransactionIds: Set<Long> = emptySet(),
    val timelineRange: TimelineRange = TimelineRange.AllTime(),
    val filterId: Long? = null
)

data class LoanPayment(
    val id: Long = 0,
    val loanId: Long,
    val transactionId: Long? = null,
    val amount: BigDecimal,
    val date: Long,
    val note: String = ""
)

data class AmortizationEntry(
    val period: Int,
    val date: Long,
    val payment: BigDecimal,
    val principal: BigDecimal,
    val interest: BigDecimal,
    val balance: BigDecimal
)

data class LoanSummary(
    val loan: Loan,
    val totalPaid: BigDecimal,
    val remainingBalance: BigDecimal,
    val interestAccrued: BigDecimal
)
