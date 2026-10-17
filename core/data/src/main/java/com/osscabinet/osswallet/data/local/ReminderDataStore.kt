package com.osscabinet.ossWallet.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.osscabinet.ossWallet.domain.model.settings.ReminderConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.reminderStore: DataStore<Preferences> by preferencesDataStore(name = "reminders")

@Singleton
class ReminderDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val LEAD_DAYS = intPreferencesKey("lead_days")
        val HOUR = intPreferencesKey("hour")
        val PLANNED = booleanPreferencesKey("planned_payments")
        val LOANS = booleanPreferencesKey("loans")
        val BUDGETS = booleanPreferencesKey("budget_alerts")
        val SUGGESTIONS = booleanPreferencesKey("suggestions")
        /** Entries are "<epochMillis>:<key>" so old ones can be pruned by age. */
        val SENT = stringSetPreferencesKey("sent")
        const val RETAIN_MS = 90L * 24 * 60 * 60 * 1000
    }

    val config: Flow<ReminderConfig> = context.reminderStore.data.map { p ->
        ReminderConfig(
            enabled = p[ENABLED] ?: true,
            leadDays = p[LEAD_DAYS] ?: ReminderConfig.DEFAULT_LEAD_DAYS,
            hourOfDay = p[HOUR] ?: ReminderConfig.DEFAULT_HOUR,
            plannedPayments = p[PLANNED] ?: true,
            loans = p[LOANS] ?: true,
            budgetAlerts = p[BUDGETS] ?: true,
            suggestions = p[SUGGESTIONS] ?: true
        )
    }

    suspend fun saveConfig(c: ReminderConfig) {
        context.reminderStore.edit {
            it[ENABLED] = c.enabled
            it[LEAD_DAYS] = c.leadDays
            it[HOUR] = c.hourOfDay
            it[PLANNED] = c.plannedPayments
            it[LOANS] = c.loans
            it[BUDGETS] = c.budgetAlerts
            it[SUGGESTIONS] = c.suggestions
        }
    }

    suspend fun sentKeys(): Set<String> =
        (context.reminderStore.data.first()[SENT] ?: emptySet()).map { it.substringAfter(':') }.toSet()

    suspend fun markSent(keys: Collection<String>, now: Long) {
        if (keys.isEmpty()) return
        context.reminderStore.edit { p ->
            val kept = (p[SENT] ?: emptySet()).filter { entry ->
                (entry.substringBefore(':').toLongOrNull() ?: 0L) > now - RETAIN_MS
            }
            p[SENT] = kept.toSet() + keys.map { "$now:$it" }
        }
    }
}
