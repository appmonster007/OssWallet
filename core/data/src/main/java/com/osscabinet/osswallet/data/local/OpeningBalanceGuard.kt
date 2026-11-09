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

    // Dropped and re-created on every open so a corrected rule replaces one an older build installed.
    //
    // The insert rule ignores the row being written (`id IS NOT NEW.id`, null-safe for a new row).
    // Room's @Upsert inserts first and only falls back to an update when that insert fails with a
    // UNIQUE/PRIMARY KEY error. Counting the row against itself would abort the insert with this
    // trigger's own message instead, so every update of an opening balance (moving it when an
    // earlier row is added or deleted) would throw and roll back the save that caused it.
    private val statements = listOf(
        // Earlier develop builds enforced this with an index; leave nothing behind for Room to trip on.
        "DROP INDEX IF EXISTS index_transactions_opening_balance_unique",
        "DROP TRIGGER IF EXISTS opening_balance_unique_insert",
        "DROP TRIGGER IF EXISTS opening_balance_unique_update",
        """
        CREATE TRIGGER opening_balance_unique_insert BEFORE INSERT ON transactions
        WHEN NEW.categoryId = $OPENING AND EXISTS (
            SELECT 1 FROM transactions WHERE categoryId = $OPENING AND profileId = NEW.profileId
              AND accountId IS NEW.accountId AND currency = NEW.currency AND id IS NOT NEW.id
        )
        BEGIN SELECT RAISE(ABORT, 'duplicate opening balance'); END
        """.trimIndent(),
        """
        CREATE TRIGGER opening_balance_unique_update
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
