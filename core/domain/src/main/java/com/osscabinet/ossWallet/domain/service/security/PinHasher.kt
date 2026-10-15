package com.osscabinet.ossWallet.domain.service.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Salted, deliberately slow PIN hashing (PBKDF2-HMAC-SHA256). Pure JVM so it is unit-testable. */
object PinHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 150_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    fun newSalt(random: SecureRandom = SecureRandom()): String =
        Base64.getEncoder().encodeToString(ByteArray(SALT_BYTES).also(random::nextBytes))

    fun hash(pin: CharArray, salt: String): String {
        val spec = PBEKeySpec(pin, Base64.getDecoder().decode(salt), ITERATIONS, KEY_BITS)
        try {
            val bytes = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
            return Base64.getEncoder().encodeToString(bytes)
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(pin: CharArray, salt: String, expectedHash: String): Boolean =
        MessageDigest.isEqual(
            hash(pin, salt).toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
}
