package com.osscabinet.ossWallet.data.local

import android.content.Context
import java.security.SecureRandom

/**
 * Manages secure storage and retrieval of database passphrase.
 * Generates a secure passphrase at runtime and stores it in SharedPreferences.
 * The actual database is encrypted by SQLCipher, providing the primary security layer.
 */
class SecurePassphraseManager(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PASSPHRASE_KEY = "db_passphrase"
        private const val PASSPHRASE_LENGTH = 32
    }

    /**
     * Retrieves the database passphrase. Generates and stores a new one if it doesn't exist.
     */
    fun getOrGeneratePassphrase(): ByteArray {
        val storedPassphrase = sharedPreferences.getString(PASSPHRASE_KEY, null)
        
        return if (storedPassphrase != null) {
            // Decode stored passphrase
            android.util.Base64.decode(storedPassphrase, android.util.Base64.NO_WRAP)
        } else {
            // Generate new secure passphrase
            val newPassphrase = generateSecurePassphrase()
            // Store it
            sharedPreferences.edit()
                .putString(PASSPHRASE_KEY, android.util.Base64.encodeToString(newPassphrase, android.util.Base64.NO_WRAP))
                .apply()
            newPassphrase
        }
    }

    /**
     * Generates a cryptographically secure random passphrase.
     */
    private fun generateSecurePassphrase(): ByteArray {
        val passphrase = ByteArray(PASSPHRASE_LENGTH)
        SecureRandom().nextBytes(passphrase)
        return passphrase
    }
}
