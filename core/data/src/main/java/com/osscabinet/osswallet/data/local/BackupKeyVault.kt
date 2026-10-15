package com.osscabinet.ossWallet.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.backupVaultStore: DataStore<Preferences> by preferencesDataStore(name = "backup_vault")

/** A derived backup key plus the KDF parameters that let the same passphrase re-derive it. */
class VaultKey(val key: ByteArray, val salt: ByteArray, val iterations: Int)

/**
 * Lets unattended auto-backups encrypt without prompting for the passphrase. Only the *derived*
 * key is kept, wrapped by a non-exportable Android Keystore key; the passphrase itself is never
 * stored. Backups stay restorable anywhere from the passphrase alone, because the header carries
 * the salt and iteration count.
 */
@Singleton
class BackupKeyVault @Inject constructor(@ApplicationContext private val context: Context) {

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val WRAP_ALIAS = "osswallet_backup_key_wrap"
        val WRAPPED = stringPreferencesKey("wrapped_key")
        val SALT = stringPreferencesKey("salt")
        val ITERATIONS = intPreferencesKey("iterations")
    }

    suspend fun store(key: ByteArray, salt: ByteArray, iterations: Int) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, wrapKey()) }
        val wrapped = cipher.iv + cipher.doFinal(key)
        context.backupVaultStore.edit {
            it[WRAPPED] = Base64.encodeToString(wrapped, Base64.NO_WRAP)
            it[SALT] = Base64.encodeToString(salt, Base64.NO_WRAP)
            it[ITERATIONS] = iterations
        }
    }

    /** Null when nothing is stored or the Keystore key is gone (e.g. device restore). */
    suspend fun load(): VaultKey? = runCatching {
        val p = context.backupVaultStore.data.first()
        val wrapped = Base64.decode(p[WRAPPED] ?: return null, Base64.NO_WRAP)
        val salt = Base64.decode(p[SALT] ?: return null, Base64.NO_WRAP)
        val iterations = p[ITERATIONS] ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, wrapKey(), GCMParameterSpec(128, wrapped, 0, 12))
        }
        VaultKey(cipher.doFinal(wrapped, 12, wrapped.size - 12), salt, iterations)
    }.getOrNull()

    suspend fun clear() {
        context.backupVaultStore.edit { it.clear() }
        runCatching { KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(WRAP_ALIAS) }
    }

    private fun wrapKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(WRAP_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    WRAP_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }
}
