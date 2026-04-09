package com.osscabinet.gleemanager.data.repository

import com.osscabinet.gleemanager.data.local.SettingsDataStore
import com.osscabinet.gleemanager.data.local.dao.ProfileDao
import com.osscabinet.gleemanager.data.local.entity.ProfileEntity
import com.osscabinet.gleemanager.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ProfileRepositoryImpl(
    private val profileDao: ProfileDao,
    private val settingsDataStore: SettingsDataStore
) : ProfileRepository {

    override fun getAllProfiles(): Flow<List<ProfileEntity>> = profileDao.getAllProfiles()

    override suspend fun insertProfile(profile: ProfileEntity) = profileDao.insertProfile(profile)

    override suspend fun getCurrentProfileId(): Flow<Int> = settingsDataStore.currentProfileId

    override suspend fun setCurrentProfileId(id: Int) = settingsDataStore.saveCurrentProfileId(id)

    override suspend fun getProfileById(id: Int): ProfileEntity? = profileDao.getProfileById(id)
}
