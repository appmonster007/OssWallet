package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getAllGoals(profileId: Int): Flow<List<Goal>>
    suspend fun getAllGoalsSync(profileId: Int): List<Goal>
    suspend fun getGoalById(id: Long, profileId: Int): Goal?
    suspend fun insertGoal(goal: Goal): Long
    suspend fun updateGoal(goal: Goal)
    suspend fun deleteGoal(id: Long, profileId: Int)
}
