package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.SettingsDataStore
import com.osscabinet.ossWallet.data.local.dao.ProfileDao
import com.osscabinet.ossWallet.data.local.entity.ProfileEntity
import com.osscabinet.ossWallet.domain.repository.ProfileRepository
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
