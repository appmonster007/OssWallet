package com.osscabinet.ossWallet.domain.model

import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.model.util.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

enum class LoanType { LENT, BORROWED }
enum class LoanStatus { ACTIVE, SETTLED }

@Serializable
data class Loan(
    val id: Long = 0,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
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

@Serializable
data class LoanPayment(
    val id: Long = 0,
    val loanId: Long,
    val transactionId: Long? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val date: Long,
    val note: String = ""
)

@Serializable
data class AmortizationEntry(
    val period: Int,
    val date: Long,
    @Serializable(with = BigDecimalSerializer::class)
    val payment: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val principal: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val interest: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal
)

@Serializable
data class LoanSummary(
    val loan: Loan,
    @Serializable(with = BigDecimalSerializer::class)
    val totalPaid: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val remainingBalance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val interestAccrued: BigDecimal
)
