package com.osscabinet.osswallet.domain.service.security

import com.osscabinet.ossWallet.domain.service.security.BackupCrypto
import com.osscabinet.ossWallet.domain.service.security.BackupDecryptionException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random

class BackupCryptoTest {

    private val pass = "correct horse".toCharArray()
    // Low work factor keeps the suite fast; the format reads the count from the header.
    private val iterations = 1_000

    private fun encrypt(data: ByteArray, passphrase: CharArray = pass): ByteArray {
        val sink = ByteArrayOutputStream()
        BackupCrypto.encrypt(sink, passphrase, iterations).use { it.write(data) }
        return sink.toByteArray()
    }

    private fun decrypt(blob: ByteArray, passphrase: CharArray = pass): ByteArray {
        val out = ByteArrayOutputStream()
        BackupCrypto.decrypt(ByteArrayInputStream(blob), out, passphrase)
        return out.toByteArray()
    }

    private fun assertRejected(blob: ByteArray, passphrase: CharArray = pass) {
        try {
            decrypt(blob, passphrase)
            fail("expected BackupDecryptionException")
        } catch (_: BackupDecryptionException) {
        }
    }

    private fun data(n: Int) = ByteArray(n).also { Random(42).nextBytes(it) }

    @Test fun `round trips small data`() {
        val d = data(1234)
        assertArrayEquals(d, decrypt(encrypt(d)))
    }

    @Test fun `round trips empty data`() {
        assertArrayEquals(ByteArray(0), decrypt(encrypt(ByteArray(0))))
    }

    @Test fun `round trips across chunk boundaries`() {
        for (n in listOf(65_535, 65_536, 65_537, 200_000)) {
            val d = data(n)
            assertArrayEquals("size $n", d, decrypt(encrypt(d)))
        }
    }

    @Test fun `wrong passphrase is rejected`() {
        assertRejected(encrypt(data(100)), "wrong".toCharArray())
    }

    @Test fun `flipping any byte is detected`() {
        val blob = encrypt(data(500))
        for (i in listOf(0, 10, 20, 34, 40, blob.size - 1)) {
            val copy = blob.copyOf().also { it[i] = (it[i].toInt() xor 0x01).toByte() }
            assertRejected(copy)
        }
    }

    @Test fun `truncation is detected`() {
        val blob = encrypt(data(150_000))
        assertRejected(blob.copyOf(blob.size - 10))
        // Dropping the whole final chunk must also fail, not yield a "valid" shorter file.
        assertRejected(blob.copyOf(34 + 1 + 4 + 65_536 + 16))
    }

    @Test fun `trailing garbage is rejected`() {
        assertRejected(encrypt(data(100)) + byteArrayOf(1, 2, 3))
    }

    @Test fun `same data encrypts differently each time`() {
        val d = data(100)
        assertFalse(encrypt(d).contentEquals(encrypt(d)))
    }

    @Test fun `ciphertext does not contain the plaintext`() {
        val secret = "TOP-SECRET-LEDGER".toByteArray()
        val blob = encrypt(secret)
        assertFalse(String(blob, Charsets.ISO_8859_1).contains("TOP-SECRET"))
    }

    @Test fun `precomputed key path decrypts with the passphrase`() {
        val salt = BackupCrypto.newSalt()
        val key = BackupCrypto.deriveKey(pass, salt, iterations)
        val sink = ByteArrayOutputStream()
        BackupCrypto.encrypt(sink, key, salt, iterations).use { it.write("vault".toByteArray()) }
        assertArrayEquals("vault".toByteArray(), decrypt(sink.toByteArray()))
    }

    @Test fun `detects encrypted files by magic and rejects plain input`() {
        assertTrue(BackupCrypto.isEncrypted(encrypt(data(10))))
        assertFalse(BackupCrypto.isEncrypted("PK\u0003\u0004zipdata".toByteArray()))
        assertRejected("PK\u0003\u0004 definitely a zip, not an encrypted backup".toByteArray())
    }
}
