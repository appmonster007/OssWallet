package com.osscabinet.ossWallet.domain.service.discovery

import com.osscabinet.ossWallet.domain.model.PlannedPayment
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.Transaction
import java.time.ZoneId

/**
 * What to suggest to the user: the discovered repeats, minus the ones already scheduled, already
 * dismissed, or too weak to bother with. Pure, so it can be tested without a database.
 */
object RecurringSuggestionService {

    fun suggest(
        transactions: List<Transaction>,
        plannedPayments: List<PlannedPayment>,
        templates: List<Template>,
        dismissedKeys: Set<String>,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<DiscoveredPattern> {
        // A completed plan ended on purpose; paused and active ones still count as "already planned".
        val plans = plannedPayments.filter { it.status != PlannedPaymentStatus.COMPLETED }
        return PatternDiscoveryService.discoverPatterns(transactions, now, zone)
            .asSequence()
            .filter { it.key !in dismissedKeys }
            .filter { it.recurrenceRule != null }
            .filter { pattern -> plans.none { matches(pattern, it.name, it.payee, it.categoryId, it.entryMode) } }
            .map { pattern ->
                pattern.copy(
                    existsAsTemplate = templates.any { matches(pattern, it.name, it.payee, it.categoryId, it.entryMode) }
                )
            }
            .toList()
    }

    /**
     * The same thing if it goes the same way, for the same category, and the payee (or name) lines up.
     * The amount is deliberately ignored: a rent increase doesn't make it a different payment.
     */
    private fun matches(
        pattern: DiscoveredPattern,
        name: String,
        payee: String?,
        categoryId: Long,
        entryMode: com.osscabinet.ossWallet.domain.model.EntryMode
    ): Boolean {
        if (entryMode != pattern.entryMode || categoryId != pattern.categoryId) return false
        val wanted = setOf(PatternDiscoveryService.normalize(pattern.payee), PatternDiscoveryService.normalize(pattern.name))
            .filter { it.isNotEmpty() }
        if (wanted.isEmpty()) return false
        val candidate = setOf(PatternDiscoveryService.normalize(payee.orEmpty()), PatternDiscoveryService.normalize(name))
            .filter { it.isNotEmpty() }
        return wanted.any { it in candidate }
    }
}
