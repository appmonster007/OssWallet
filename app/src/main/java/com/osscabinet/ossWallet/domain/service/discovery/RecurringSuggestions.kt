package com.osscabinet.ossWallet.domain.service.discovery

import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.SuggestionStateRepository
import com.osscabinet.ossWallet.domain.repository.TemplateRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Recurring-payment suggestions for the current profile, kept current as transactions, plans and
 * dismissals change. One place for the Smart Suggestions screen, the dashboard nudge and the weekly scan.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Singleton
class RecurringSuggestions @Inject constructor(
    private val transactions: TransactionRepository,
    private val plannedPayments: PlannedPaymentRepository,
    private val templates: TemplateRepository,
    private val state: SuggestionStateRepository,
    private val settings: SettingsRepository
) {
    fun observe(): Flow<List<DiscoveredPattern>> = settings.currentProfileId.flatMapLatest { profileId ->
        combine(
            transactions.getAllTransactions(profileId),
            plannedPayments.getAllPlannedPayments(profileId),
            templates.getAllTemplates(profileId),
            state.dismissedKeys(profileId)
        ) { txs, plans, tpls, dismissed ->
            RecurringSuggestionService.suggest(txs, plans, tpls, dismissed)
        }
            // Saving a transaction can touch these tables in quick succession; scan once they settle.
            .debounce(300)
            .flowOn(Dispatchers.Default)
    }

    /** Suggestions the user hasn't looked at yet: what the dashboard nudges about. */
    fun observeUnseen(): Flow<List<DiscoveredPattern>> = settings.currentProfileId.flatMapLatest { profileId ->
        combine(observe(), state.seenKeys(profileId)) { suggestions, seen -> suggestions.filter { it.key !in seen } }
    }

    fun observeDismissedCount(): Flow<Int> =
        settings.currentProfileId.flatMapLatest { state.dismissedKeys(it) }.map { it.size }

    /** A one-off scan for background work. */
    suspend fun current(): List<DiscoveredPattern> {
        val profileId = settings.currentProfileId.first()
        return RecurringSuggestionService.suggest(
            transactions = transactions.getAllTransactionsSync(profileId),
            plannedPayments = plannedPayments.getAllSync(profileId),
            templates = templates.getAllTemplatesSync(profileId),
            dismissedKeys = state.dismissedKeys(profileId).first()
        )
    }

    /** What a background scan should consider telling the user about: not dismissed, not yet looked at. */
    suspend fun currentUnseen(): List<DiscoveredPattern> {
        val profileId = settings.currentProfileId.first()
        val seen = state.seenKeys(profileId).first()
        return current().filter { it.key !in seen }
    }

    suspend fun dismiss(pattern: DiscoveredPattern) = state.dismiss(settings.currentProfileId.first(), pattern.key)

    suspend fun restoreDismissed() = state.restoreDismissed(settings.currentProfileId.first())

    suspend fun markSeen(patterns: Collection<DiscoveredPattern>) =
        state.markSeen(settings.currentProfileId.first(), patterns.map { it.key })
}
