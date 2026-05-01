package com.osscabinet.ossWallet.data.local.dao

import androidx.room.*
import com.osscabinet.ossWallet.data.local.entity.PlannedPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedPaymentDao {

    @Query("SELECT * FROM planned_payments WHERE profileId = :profileId AND status = 'ACTIVE' ORDER BY nextDueDate ASC")
    fun getActivePlannedPayments(profileId: Int): Flow<List<PlannedPaymentEntity>>

    @Query("SELECT * FROM planned_payments WHERE profileId = :profileId ORDER BY nextDueDate ASC")
    fun getAllPlannedPayments(profileId: Int): Flow<List<PlannedPaymentEntity>>

    @Query("SELECT * FROM planned_payments WHERE profileId = :profileId ORDER BY nextDueDate ASC")
    suspend fun getAllPlannedPaymentsSync(profileId: Int): List<PlannedPaymentEntity>

    @Query("SELECT * FROM planned_payments WHERE profileId = :profileId AND status = 'ACTIVE' AND nextDueDate <= :untilDate ORDER BY nextDueDate ASC")
    suspend fun getDuePlannedPayments(profileId: Int, untilDate: Long): List<PlannedPaymentEntity>

    @Query("SELECT * FROM planned_payments WHERE id = :id")
    suspend fun getById(id: Long): PlannedPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlannedPaymentEntity): Long

    @Update
    suspend fun update(entity: PlannedPaymentEntity)

    @Query("DELETE FROM planned_payments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
