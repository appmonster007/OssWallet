package com.osscabinet.ossWallet.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.osscabinet.ossWallet.di.WidgetDataLoaderEntryPoint
import com.osscabinet.ossWallet.domain.card.CardInput
import com.osscabinet.ossWallet.domain.card.computers.BudgetsCardComputer
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.settings.BudgetsCardConfig
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import com.osscabinet.ossWallet.domain.repository.ReminderRepository
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.management.BudgetService
import com.osscabinet.ossWallet.domain.service.reminder.BudgetAlertInput
import com.osscabinet.ossWallet.domain.model.settings.ReminderConfig
import com.osscabinet.ossWallet.domain.service.reminder.ReminderEvent
import com.osscabinet.ossWallet.domain.service.reminder.ReminderPlanner
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepository: ReminderRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val config = reminderRepository.config.first()
        if (!config.enabled) return Result.success()
        // Nothing would be shown, so leave events unsent: they fire once permission is granted.
        if (!ReminderNotifier.canPost(appContext)) return Result.success()

        val now = System.currentTimeMillis()
        val events = planReminders(appContext, config, now, reminderRepository.sentKeys())
        events.forEach { ReminderNotifier.post(appContext, it) }
        reminderRepository.markSent(events.map { it.key }, now)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "reminder_worker"

        /** Runs every few hours; ReminderPlanner gates on time of day and de-duplicates by key. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(3, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}

/** What is due according to [config] at [now], minus [sentKeys]; shared by the periodic and snooze workers. */
internal suspend fun planReminders(
    context: Context,
    config: ReminderConfig,
    now: Long,
    sentKeys: Set<String>
): List<ReminderEvent> {
    val ep = EntryPointAccessors.fromApplication(context, WidgetDataLoaderEntryPoint::class.java)
    val profileId = ep.settingsDataStore().currentProfileId.first()
    val plannedPayments = if (config.plannedPayments) {
        ep.plannedPaymentRepository().getAllSync(profileId)
    } else emptyList()
    val loans = if (config.loans) ep.loanRepository().getActiveLoansSync(profileId) else emptyList()
    val budgetAlerts = if (config.budgetAlerts) loadBudgetAlerts(ep, profileId, now) else emptyList()
    return ReminderPlanner.plan(
        now = now,
        config = config,
        plannedPayments = plannedPayments,
        loans = loans,
        budgetAlerts = budgetAlerts,
        sentKeys = sentKeys
    )
}

private suspend fun loadBudgetAlerts(
    ep: WidgetDataLoaderEntryPoint,
    profileId: Int,
    now: Long
): List<BudgetAlertInput> {
    val budgets = ep.budgetRepository().getActiveBudgetsSync(profileId).filter { !it.isArchived }
    if (budgets.isEmpty()) return emptyList()
    val baseCurrency = ep.settingsDataStore().currency.first()
    val rates = ep.db().currencyDao().getAllRates().associate { it.currencyCode to it.rate.toBigDecimal() }
    val filter = TransactionFilter(timelineRange = TimelineRange.AllTime(excludeFuture = true))
    val input = CardInput(
        profileId = profileId,
        targetCurrency = baseCurrency,
        exchangeRates = ExchangeRateSnapshot(anchor = baseCurrency, rates = rates),
        filter = filter,
        accounts = ep.accountRepository().getAllAccounts(profileId).first(),
        categories = emptyList(),
        budgets = budgets
    )
    val cardConfig = BudgetsCardConfig(
        cardId = "reminder_budget_alerts",
        filter = filter,
        showArchived = false,
        maxItems = Int.MAX_VALUE
    )
    val items = runCatching { BudgetsCardComputer(ep.databaseQueryService()).compute(input, cardConfig) }
        .getOrNull()?.items.orEmpty()
    return items.map { p ->
        BudgetAlertInput(
            budgetId = p.budget.id,
            name = p.budget.name,
            percentUsed = (p.percentage * 100).toInt(),
            level = p.alertLevel,
            periodStart = BudgetService.periodIdentity(p.budget, now)
        )
    }
}

