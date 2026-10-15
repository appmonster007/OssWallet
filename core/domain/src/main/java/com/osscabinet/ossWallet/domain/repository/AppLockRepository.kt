package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.settings.AppLockConfig
import kotlinx.coroutines.flow.Flow

interface AppLockRepository {
    val config: Flow<AppLockConfig>

    /** Stores a new PIN (hashed) and turns App Lock on. */
    suspend fun enable(pin: CharArray, biometricEnabled: Boolean)

    /** Turns App Lock off and wipes the stored PIN hash. */
    suspend fun disable()

    suspend fun changePin(pin: CharArray)
    suspend fun verifyPin(pin: CharArray): Boolean
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setTimeoutSeconds(seconds: Int)
    suspend fun setPrivacyScreenEnabled(enabled: Boolean)
}
