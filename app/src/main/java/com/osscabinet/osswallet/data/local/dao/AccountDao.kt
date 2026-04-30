package com.osscabinet.ossWallet.data.local.dao

import androidx.room.*
import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE profileId = :profileId ORDER BY displayOrder ASC")
    fun getAllAccounts(profileId: Int): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: String): AccountEntity?

    @Upsert
    suspend fun upsertAccounts(accounts: List<AccountEntity>)

    @Upsert
    suspend fun upsertAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE profileId = :profileId ORDER BY displayOrder ASC")
    suspend fun getAllAccountsSync(profileId: Int): List<AccountEntity>

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsSync(): List<AccountEntity>

    @Query("DELETE FROM accounts")
    suspend fun clearAll()
}
