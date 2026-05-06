package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.LoanType
import com.osscabinet.ossWallet.domain.model.settings.LoansCardConfig
import com.osscabinet.ossWallet.domain.service.management.LoanService

class LoansCardComputer : CardDataComputer<LoansCardConfig, CardRenderData.Loans> {
    override val supportedType = CardType.LOANS

    override fun compute(input: CardInput, config: LoansCardConfig): CardRenderData.Loans {
        val now = System.currentTimeMillis()
        val filtered = when (config.showType) {
            "LENT" -> input.loans.filter { it.loanType == LoanType.LENT }
            "BORROWED" -> input.loans.filter { it.loanType == LoanType.BORROWED }
            else -> input.loans
        }
        val items = filtered.take(config.maxItems).map { loan ->
            val payments = input.loanPayments[loan.id] ?: emptyList()
            LoanService.buildSummary(loan, payments, now, input.precision)
        }
        return CardRenderData.Loans(config = config, items = items, precision = input.precision)
    }
}
