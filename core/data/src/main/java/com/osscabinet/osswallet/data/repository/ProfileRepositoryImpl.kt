package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.dao.ProfileDao
import com.osscabinet.ossWallet.data.mapper.toDomain
import com.osscabinet.ossWallet.data.mapper.toEntity
import com.osscabinet.ossWallet.domain.model.Profile
import com.osscabinet.ossWallet.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val settingsDataStore: SettingsDataStore
) : ProfileRepository {

    override fun getAllProfiles(): Flow<List<Profile>> =
        profileDao.getAllProfiles().map { entities -> entities.map { it.toDomain() } }

    override suspend fun insertProfile(profile: Profile) =
        profileDao.insertProfile(profile.toEntity())

    override suspend fun getCurrentProfileId(): Flow<Int> = settingsDataStore.currentProfileId

    override suspend fun setCurrentProfileId(id: Int) = settingsDataStore.saveCurrentProfileId(id)

    override suspend fun getProfileById(id: Int): Profile? =
        profileDao.getProfileById(id)?.toDomain()
}
