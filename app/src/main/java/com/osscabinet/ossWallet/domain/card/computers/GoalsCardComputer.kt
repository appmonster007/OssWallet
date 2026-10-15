package com.osscabinet.ossWallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardDataComputer
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.CardType
import com.osscabinet.ossWallet.domain.model.GoalStatus
import com.osscabinet.ossWallet.domain.model.settings.GoalsCardConfig
import com.osscabinet.ossWallet.domain.service.goal.GoalService

class GoalsCardComputer : CardDataComputer<GoalsCardConfig, CardRenderData.Goals> {
    override val supportedType = CardType.GOALS

    override suspend fun compute(input: CardInput, config: GoalsCardConfig): CardRenderData.Goals {
        val now = System.currentTimeMillis()
        val items = input.goals
            .map { GoalService.progress(it, now) }
            .filter { config.showCompleted || it.status != GoalStatus.COMPLETED }
            // Most urgent first: overdue, then behind pace, then by nearest deadline.
            .sortedWith(
                compareBy({ it.status.urgency() }, { it.goal.deadline ?: Long.MAX_VALUE })
            )
            .take(config.maxItems)
        return CardRenderData.Goals(config = config, items = items, currency = input.targetCurrency)
    }

    private fun GoalStatus.urgency() = when (this) {
        GoalStatus.OVERDUE -> 0
        GoalStatus.BEHIND -> 1
        GoalStatus.ON_TRACK -> 2
        GoalStatus.NO_DEADLINE -> 3
        GoalStatus.COMPLETED -> 4
    }
}
