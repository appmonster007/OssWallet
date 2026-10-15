package com.osscabinet.osswallet.domain.card.computers

import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.computers.GoalsCardComputer
import com.osscabinet.ossWallet.domain.model.Goal
import com.osscabinet.ossWallet.domain.model.settings.GoalsCardConfig
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class GoalsCardComputerTest {

    private val now = System.currentTimeMillis()
    private val day = 24L * 60 * 60 * 1000

    private fun goal(id: Long, saved: String, deadline: Long? = null, createdAt: Long = now - 30 * day) = Goal(
        id = id, name = "G$id", targetAmount = BigDecimal("1000"), savedAmount = BigDecimal(saved),
        currency = "USD", deadline = deadline, profileId = 1, createdAt = createdAt
    )

    private fun compute(goals: List<Goal>, config: GoalsCardConfig = GoalsCardConfig(cardId = "c")) =
        runBlocking {
            GoalsCardComputer().compute(
                CardInput(
                    accounts = emptyList(), categories = emptyList(),
                    exchangeRates = ExchangeRateSnapshot(anchor = "USD", rates = emptyMap()),
                    targetCurrency = "USD", goals = goals
                ),
                config
            )
        }

    @Test fun `completed goals are hidden unless asked for`() {
        val goals = listOf(goal(1, "1000"), goal(2, "100"))
        assertEquals(listOf(2L), compute(goals).items.map { it.goal.id })
        assertEquals(2, compute(goals, GoalsCardConfig(cardId = "c", showCompleted = true)).items.size)
    }

    @Test fun `overdue first then behind then nearest deadline then open-ended`() {
        val goals = listOf(
            goal(1, "100"),                                   // no deadline
            goal(2, "800", deadline = now + 10 * day),         // on track
            goal(3, "100", deadline = now + 10 * day),         // behind
            goal(4, "100", deadline = now - 3 * day)           // overdue
        )
        assertEquals(listOf(4L, 3L, 2L), compute(goals).items.map { it.goal.id })
    }

    @Test fun `result is capped at maxItems`() {
        val goals = (1L..6L).map { goal(it, "10") }
        assertEquals(2, compute(goals, GoalsCardConfig(cardId = "c", maxItems = 2)).items.size)
    }
}
