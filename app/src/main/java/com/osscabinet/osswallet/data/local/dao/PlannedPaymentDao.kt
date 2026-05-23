package com.osscabinet.ossWallet.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentEntity
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentWithTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedPaymentDao {

    @Transaction
    @Query("SELECT * FROM planned_payments WHERE profileId = :profileId AND status = 'ACTIVE'")
    fun getActivePlannedPayments(profileId: Int): Flow<List<PlannedPaymentWithTemplate>>

    @Transaction
    @Query("SELECT * FROM planned_payments WHERE profileId = :profileId")
    fun getAllPlannedPayments(profileId: Int): Flow<List<PlannedPaymentWithTemplate>>

    @Transaction
    @Query("SELECT * FROM planned_payments WHERE profileId = :profileId")
    suspend fun getAllPlannedPaymentsSync(profileId: Int): List<PlannedPaymentWithTemplate>

    @Transaction
    @Query("SELECT * FROM planned_payments WHERE profileId = :profileId AND status = 'ACTIVE'")
    suspend fun getDuePlannedPayments(profileId: Int): List<PlannedPaymentWithTemplate>

    @Transaction
    @Query("SELECT * FROM planned_payments")
    suspend fun getAllPlannedPaymentsSync(): List<PlannedPaymentWithTemplate>

    @Query("DELETE FROM planned_payments")
    suspend fun clearAll()

    @Transaction
    @Query("SELECT * FROM planned_payments WHERE id = :id")
    suspend fun getById(id: Long): PlannedPaymentWithTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlannedPaymentEntity): Long

    @Update
    suspend fun update(entity: PlannedPaymentEntity)

    @Query("DELETE FROM planned_payments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM planned_payments WHERE templateId = :templateId")
    suspend fun getCountByTemplateId(templateId: Long): Int
}
