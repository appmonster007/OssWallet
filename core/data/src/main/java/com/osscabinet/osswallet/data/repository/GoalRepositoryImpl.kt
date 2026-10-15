package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.dao.GoalDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Goal
import com.osscabinet.ossWallet.domain.repository.GoalRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalRepository {
    override fun getAllGoals(profileId: Int): Flow<List<Goal>> =
        goalDao.getAllGoals(profileId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAllGoalsSync(profileId: Int): List<Goal> =
        goalDao.getAllGoalsSync(profileId).map { it.toDomain() }

    override suspend fun getGoalById(id: Long, profileId: Int): Goal? =
        goalDao.getById(id, profileId)?.toDomain()

    override suspend fun insertGoal(goal: Goal): Long = goalDao.insert(goal.toEntity())
    override suspend fun updateGoal(goal: Goal) = goalDao.update(goal.toEntity())
    override suspend fun deleteGoal(id: Long, profileId: Int) = goalDao.deleteById(id, profileId)
}
