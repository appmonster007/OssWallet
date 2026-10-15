package com.osscabinet.ossWallet.domain.service.security

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** The passphrase was wrong, or the backup was altered or truncated. Deliberately not distinguished. */
class BackupDecryptionException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Passphrase-based authenticated encryption for backup files (AES-256-GCM, PBKDF2-HMAC-SHA256).
 *
 * The file is a sequence of independently authenticated 64 KiB chunks, so large backups stream in
 * bounded memory, yet any tampering, reordering or truncation is detected: every chunk's tag covers
 * the header and a last-chunk flag, and the nonce embeds the chunk counter.
 *
 * Layout: `"OSSBK"` | version(1) | iterations(4) | salt(16) | noncePrefix(8), then per chunk
 * `flag(1) | ciphertextLength(4) | ciphertext+tag`. Pure JVM so it is unit-testable.
 */
object BackupCrypto {
    const val DEFAULT_ITERATIONS = 310_000
    const val SALT_BYTES = 16

    private val MAGIC = byteArrayOf('O'.code.toByte(), 'S'.code.toByte(), 'S'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
    private const val VERSION: Byte = 1
    private const val NONCE_PREFIX_BYTES = 8
    private const val HEADER_BYTES = 5 + 1 + 4 + SALT_BYTES + NONCE_PREFIX_BYTES
    private const val CHUNK = 64 * 1024
    private const val TAG_BYTES = 16
    private const val FLAG_MORE: Byte = 0
    private const val FLAG_LAST: Byte = 1
    /** Refuses absurd work factors from a hostile file. */
    private const val MAX_ITERATIONS = 5_000_000

    private val random = SecureRandom()

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also(random::nextBytes)

    fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int = DEFAULT_ITERATIONS): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** True when [prefix] (the first bytes of a file) carries the encrypted-backup magic. */
    fun isEncrypted(prefix: ByteArray): Boolean =
        prefix.size >= MAGIC.size && MAGIC.indices.all { prefix[it] == MAGIC[it] }

    /** Number of leading bytes [isEncrypted] needs. */
    val MAGIC_LENGTH: Int get() = MAGIC.size

    fun encrypt(out: OutputStream, passphrase: CharArray, iterations: Int = DEFAULT_ITERATIONS): OutputStream {
        val salt = newSalt()
        return encrypt(out, deriveKey(passphrase, salt, iterations), salt, iterations)
    }

    /**
     * Encrypts with an already-derived [key] (whose [salt]/[iterations] go in the header so the same
     * passphrase can re-derive it later). Closing the returned stream finishes the file and closes [out].
     */
    fun encrypt(out: OutputStream, key: ByteArray, salt: ByteArray, iterations: Int): OutputStream {
        require(salt.size == SALT_BYTES && key.size == 32)
        val noncePrefix = ByteArray(NONCE_PREFIX_BYTES).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC).put(VERSION).putInt(iterations).put(salt).put(noncePrefix).array()
        out.write(header)
        return ChunkWriter(out, SecretKeySpec(key, "AES"), header, noncePrefix)
    }

    /** Decrypts all of [input] to [output]; throws [BackupDecryptionException] unless fully authentic. */
    fun decrypt(input: InputStream, output: OutputStream, passphrase: CharArray) {
        val header = ByteArray(HEADER_BYTES)
        try {
            readFully(input, header)
        } catch (e: EOFException) {
            throw BackupDecryptionException("Not an encrypted backup", e)
        }
        if (!isEncrypted(header)) throw BackupDecryptionException("Not an encrypted backup")
        val buf = ByteBuffer.wrap(header).position(5)
        if (buf.get() != VERSION) throw BackupDecryptionException("Unsupported backup version")
        val iterations = buf.getInt()
        if (iterations !in 1..MAX_ITERATIONS) throw BackupDecryptionException("Corrupt backup header")
        val salt = ByteArray(SALT_BYTES).also { buf.get(it) }
        val noncePrefix = ByteArray(NONCE_PREFIX_BYTES).also { buf.get(it) }
        val key = SecretKeySpec(deriveKey(passphrase, salt, iterations), "AES")

        var counter = 0
        while (true) {
            val flag = input.read()
            if (flag < 0) throw BackupDecryptionException("Backup is truncated")
            val lenBytes = ByteArray(4)
            try {
                readFully(input, lenBytes)
            } catch (e: EOFException) {
                throw BackupDecryptionException("Backup is truncated", e)
            }
            val len = ByteBuffer.wrap(lenBytes).getInt()
            if (len !in TAG_BYTES..(CHUNK + TAG_BYTES)) throw BackupDecryptionException("Corrupt backup")
            val ct = ByteArray(len)
            try {
                readFully(input, ct)
            } catch (e: EOFException) {
                throw BackupDecryptionException("Backup is truncated", e)
            }
            val plain = try {
                chunkCipher(Cipher.DECRYPT_MODE, key, noncePrefix, counter, header, flag.toByte()).doFinal(ct)
            } catch (e: AEADBadTagException) {
                throw BackupDecryptionException("Wrong passphrase, or the backup is damaged", e)
            } catch (e: GeneralSecurityException) {
                throw BackupDecryptionException("Wrong passphrase, or the backup is damaged", e)
            }
            output.write(plain)
            if (flag.toByte() == FLAG_LAST) {
                if (input.read() != -1) throw BackupDecryptionException("Unexpected data after end of backup")
                return
            }
            if (flag.toByte() != FLAG_MORE) throw BackupDecryptionException("Corrupt backup")
            counter++
        }
    }

    private fun chunkCipher(mode: Int, key: SecretKeySpec, prefix: ByteArray, counter: Int, header: ByteArray, flag: Byte): Cipher {
        val nonce = ByteBuffer.allocate(12).put(prefix).putInt(counter).array()
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, key, GCMParameterSpec(TAG_BYTES * 8, nonce))
            updateAAD(header)
            updateAAD(byteArrayOf(flag))
        }
    }

    private fun readFully(input: InputStream, dst: ByteArray) {
        var off = 0
        while (off < dst.size) {
            val n = input.read(dst, off, dst.size - off)
            if (n < 0) throw EOFException()
            off += n
        }
    }

    private class ChunkWriter(
        private val out: OutputStream,
        private val key: SecretKeySpec,
        private val header: ByteArray,
        private val noncePrefix: ByteArray
    ) : OutputStream() {
        private val buffer = ByteArrayOutputStream(CHUNK)
        private var counter = 0
        private var closed = false

        override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

        override fun write(b: ByteArray, off: Int, len: Int) {
            check(!closed) { "Stream closed" }
            var pos = off
            var left = len
            while (left > 0) {
                // A full buffer is only emitted once more data proves it is not the last chunk.
                if (buffer.size() == CHUNK) emit(FLAG_MORE)
                val n = minOf(left, CHUNK - buffer.size())
                buffer.write(b, pos, n)
                pos += n
                left -= n
            }
        }

        private fun emit(flag: Byte) {
            val ct = chunkCipher(Cipher.ENCRYPT_MODE, key, noncePrefix, counter, header, flag)
                .doFinal(buffer.toByteArray())
            out.write(flag.toInt())
            out.write(ByteBuffer.allocate(4).putInt(ct.size).array())
            out.write(ct)
            buffer.reset()
            counter++
        }

        override fun flush() = out.flush()

        override fun close() {
            if (closed) return
            closed = true
            try {
                emit(FLAG_LAST)
                out.flush()
            } finally {
                out.close()
            }
        }
    }
}
