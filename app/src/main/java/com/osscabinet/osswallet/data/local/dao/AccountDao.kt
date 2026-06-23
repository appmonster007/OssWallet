package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE profileId = :profileId ORDER BY displayOrder ASC")
    fun getAllAccounts(profileId: Int): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id AND profileId = :profileId")
    suspend fun getAccountById(id: String, profileId: Int): AccountEntity?

    @Upsert
    suspend fun upsertAccounts(accounts: List<AccountEntity>)

    @Upsert
    suspend fun upsertAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE profileId = :profileId ORDER BY displayOrder ASC")
    suspend fun getAllAccountsSync(profileId: Int): List<AccountEntity>

    /**
     * Cross-profile bulk read. ONLY for backup/export flows that intentionally need
     * every profile's data. Profile-scoped UI must use the [getAllAccountsSync] overload
     * with a profileId.
     */
    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsSync(): List<AccountEntity>

    @Query("DELETE FROM accounts")
    suspend fun clearAll()
}
