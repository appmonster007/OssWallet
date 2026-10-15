package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.AppLockDataStore
import com.osscabinet.ossWallet.domain.model.settings.AppLockConfig
import com.osscabinet.ossWallet.domain.repository.AppLockRepository
import com.osscabinet.ossWallet.domain.service.security.PinHasher
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Singleton
class AppLockRepositoryImpl @Inject constructor(
    private val store: AppLockDataStore
) : AppLockRepository {

    override val config: Flow<AppLockConfig> = store.config

    override suspend fun enable(pin: CharArray, biometricEnabled: Boolean) {
        val (hash, salt) = hashNew(pin)
        store.enable(hash, salt, biometricEnabled)
    }

    override suspend fun disable() = store.disable()

    override suspend fun changePin(pin: CharArray) {
        val (hash, salt) = hashNew(pin)
        store.savePin(hash, salt)
    }

    override suspend fun verifyPin(pin: CharArray): Boolean {
        val (hash, salt) = store.credentials() ?: return false
        return withContext(Dispatchers.Default) { PinHasher.verify(pin, salt, hash) }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) = store.setBiometric(enabled)
    override suspend fun setTimeoutSeconds(seconds: Int) = store.setTimeoutSeconds(seconds)
    override suspend fun setPrivacyScreenEnabled(enabled: Boolean) = store.setPrivacyScreen(enabled)

    private suspend fun hashNew(pin: CharArray): Pair<String, String> =
        withContext(Dispatchers.Default) {
            val salt = PinHasher.newSalt()
            PinHasher.hash(pin, salt) to salt
        }
}
