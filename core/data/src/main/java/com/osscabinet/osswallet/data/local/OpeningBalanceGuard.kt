package com.osscabinet.ossWallet.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import com.osscabinet.ossWallet.domain.model.Category

/**
 * An account has at most one opening balance per currency.
 *
 * Room cannot declare a partial unique index, and an index it does not know about makes its schema
 * validation fail after a migration. Triggers are not validated, so the rule is enforced with two of
 * them (the same trick as the FTS sync triggers). They are installed on every open, which also covers
 * databases that were created before the rule existed.
 */
object OpeningBalanceGuard {
    private val OPENING = Category.SYSTEM_INITIAL_BALANCE

    private val statements = listOf(
        // Earlier develop builds enforced this with an index; leave nothing behind for Room to trip on.
        "DROP INDEX IF EXISTS index_transactions_opening_balance_unique",
        """
        CREATE TRIGGER IF NOT EXISTS opening_balance_unique_insert BEFORE INSERT ON transactions
        WHEN NEW.categoryId = $OPENING AND EXISTS (
            SELECT 1 FROM transactions WHERE categoryId = $OPENING AND profileId = NEW.profileId
              AND accountId IS NEW.accountId AND currency = NEW.currency
        )
        BEGIN SELECT RAISE(ABORT, 'duplicate opening balance'); END
        """.trimIndent(),
        """
        CREATE TRIGGER IF NOT EXISTS opening_balance_unique_update
        BEFORE UPDATE OF categoryId, profileId, accountId, currency ON transactions
        WHEN NEW.categoryId = $OPENING AND EXISTS (
            SELECT 1 FROM transactions WHERE categoryId = $OPENING AND profileId = NEW.profileId
              AND accountId IS NEW.accountId AND currency = NEW.currency AND id <> NEW.id
        )
        BEGIN SELECT RAISE(ABORT, 'duplicate opening balance'); END
        """.trimIndent()
    )

    fun install(db: SupportSQLiteDatabase) = statements.forEach { db.execSQL(it) }
}
