package com.osscabinet.ossWallet.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Receipt images at rest. The SQLCipher database does not cover files, so each image is sealed
 * with AES-256-GCM under a non-exportable Android Keystore key (`iv(12) | ciphertext+tag`).
 * Names are random, so the file system reveals nothing about the transaction.
 */
@Singleton
class AttachmentStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val dir: File get() = File(context.filesDir, DIR_NAME).apply { mkdirs() }

    /** Seals [bytes] and returns the opaque file name it was stored under. */
    fun write(bytes: ByteArray): String {
        val name = UUID.randomUUID().toString() + ".bin"
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val sealed = cipher.iv + cipher.doFinal(bytes)
        val tmp = File(dir, "$name.tmp")
        tmp.writeBytes(sealed)
        check(tmp.renameTo(File(dir, name))) { "Could not store receipt" }
        return name
    }

    /** Null when the file is missing or can't be opened (e.g. the Keystore key is gone). */
    fun read(name: String): ByteArray? = runCatching {
        val sealed = File(dir, safe(name)).readBytes()
        val cipher = Cipher.getInstance(TRANSFORM).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, sealed, 0, IV_BYTES))
        }
        cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES)
    }.getOrNull()

    fun delete(name: String) {
        File(dir, safe(name)).delete()
    }

    fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    fun listNames(): List<String> = dir.listFiles()?.map { it.name }.orEmpty()

    /** Names come from our own DB, but never let one escape the directory. */
    private fun safe(name: String): String = File(name).name

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    private companion object {
        const val DIR_NAME = "attachments"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "osswallet_attachment_key"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
