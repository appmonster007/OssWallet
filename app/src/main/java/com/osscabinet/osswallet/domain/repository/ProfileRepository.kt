package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getAllProfiles(): Flow<List<ProfileEntity>>
    suspend fun insertProfile(profile: ProfileEntity)
    suspend fun getCurrentProfileId(): Flow<Int>
    suspend fun setCurrentProfileId(id: Int)
    suspend fun getProfileById(id: Int): ProfileEntity?
}
