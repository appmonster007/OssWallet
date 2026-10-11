package com.osscabinet.ossWallet.di

import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.BudgetRepository
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import com.osscabinet.ossWallet.domain.repository.LoanRepository
import com.osscabinet.ossWallet.domain.repository.PlannedPaymentRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.service.management.TransactionManager
import com.osscabinet.ossWallet.domain.service.query.DatabaseQueryService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDataLoaderEntryPoint {
    fun settingsDataStore(): SettingsDataStore
    fun db(): AppDatabase
    fun databaseQueryService(): DatabaseQueryService
    fun categoryRepository(): CategoryRepository
    fun accountRepository(): AccountRepository
    // Phase 2 widget rebuild (Budgets/Loans/Planned Payments/Currency Balances/Accounts
    // widgets) — these cards' real XxxCardComputer needs the raw entity list on CardInput
    // (input.budgets / input.loans), same as accountRepository above already does for
    // input.accounts.
    fun budgetRepository(): BudgetRepository
    fun loanRepository(): LoanRepository
    fun plannedPaymentRepository(): PlannedPaymentRepository
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlannedPaymentWorkerEntryPoint {
    fun plannedPaymentRepository(): PlannedPaymentRepository
    fun settingsRepository(): SettingsRepository
    fun categoryRepository(): CategoryRepository
    fun transactionManager(): TransactionManager
}
