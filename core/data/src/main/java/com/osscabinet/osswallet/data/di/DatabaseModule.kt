package com.osscabinet.ossWallet.data.di

import android.content.Context
import com.osscabinet.ossWallet.data.local.AppDatabase
import com.osscabinet.ossWallet.data.local.SecurePassphraseManager
import com.osscabinet.ossWallet.data.local.dao.AccountDao
import com.osscabinet.ossWallet.data.local.dao.BudgetDao
import com.osscabinet.ossWallet.data.local.dao.CardCacheDao
import com.osscabinet.ossWallet.data.local.dao.AttachmentDao
import com.osscabinet.ossWallet.data.local.dao.GoalDao
import com.osscabinet.ossWallet.data.local.dao.CategoryDao
import com.osscabinet.ossWallet.data.local.dao.CurrencyDao
import com.osscabinet.ossWallet.data.local.dao.FilterDao
import com.osscabinet.ossWallet.data.local.dao.LoanDao
import com.osscabinet.ossWallet.data.local.dao.LoanPaymentDao
import com.osscabinet.ossWallet.data.local.dao.PlannedPaymentDao
import com.osscabinet.ossWallet.data.local.dao.PlannedPaymentOccurrenceDao
import com.osscabinet.ossWallet.data.local.dao.ProfileDao
import com.osscabinet.ossWallet.data.local.dao.TagDao
import com.osscabinet.ossWallet.data.local.dao.TemplateDao
import com.osscabinet.ossWallet.data.local.dao.TransactionDao
import com.osscabinet.ossWallet.data.local.dao.TransactionLinkDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSecurePassphraseManager(@ApplicationContext context: Context): SecurePassphraseManager {
        return SecurePassphraseManager(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        securePassphraseManager: SecurePassphraseManager
    ): AppDatabase {
        return AppDatabase.getDatabase(context, securePassphraseManager.getOrGeneratePassphrase())
    }

    @Provides
    @Singleton
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    @Singleton
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    @Singleton
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    @Singleton
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()

    @Provides
    @Singleton
    fun provideTemplateDao(db: AppDatabase): TemplateDao = db.templateDao()

    @Provides
    @Singleton
    fun provideTransactionLinkDao(db: AppDatabase): TransactionLinkDao = db.transactionLinkDao()

    @Provides
    @Singleton
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides
    @Singleton
    fun provideLoanDao(db: AppDatabase): LoanDao = db.loanDao()

    @Provides
    @Singleton
    fun provideLoanPaymentDao(db: AppDatabase): LoanPaymentDao = db.loanPaymentDao()

    @Provides
    @Singleton
    fun providePlannedPaymentDao(db: AppDatabase): PlannedPaymentDao = db.plannedPaymentDao()

    @Provides
    @Singleton
    fun providePlannedPaymentOccurrenceDao(db: AppDatabase): PlannedPaymentOccurrenceDao =
        db.plannedPaymentOccurrenceDao()

    @Provides
    @Singleton
    fun provideFilterDao(db: AppDatabase): FilterDao = db.filterDao()

    @Provides
    @Singleton
    fun provideCurrencyDao(db: AppDatabase): CurrencyDao = db.currencyDao()

    @Provides
    @Singleton
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    @Singleton
    fun provideCardCacheDao(db: AppDatabase): CardCacheDao = db.cardCacheDao()

    @Provides
    @Singleton
    fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()

    @Provides
    @Singleton
    fun provideAttachmentDao(db: AppDatabase): AttachmentDao = db.attachmentDao()
}
