package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getAllProfiles(): Flow<List<Profile>>
    suspend fun insertProfile(profile: Profile)
    suspend fun getCurrentProfileId(): Flow<Int>
    suspend fun setCurrentProfileId(id: Int)
    suspend fun getProfileById(id: Int): Profile?
}
