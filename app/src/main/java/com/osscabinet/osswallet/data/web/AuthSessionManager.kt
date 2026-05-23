package com.osscabinet.ossWallet.data.web

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

class AuthSessionManager {

    private var currentAccessCode: String = ""
    @Volatile
    private var activeSession: MutableSession? = null
    private val failedAttempts = ConcurrentHashMap<String, AttemptRecord>()
    private val secureRandom = SecureRandom()

    private data class AttemptRecord(val count: Int, val lockedUntil: Long)

    private data class MutableSession(
        val token: String,
        val createdAt: Long = System.currentTimeMillis(),
        @Volatile var lastActivityAt: Long = System.currentTimeMillis()
    ) {
        val expiresAt: Long get() = lastActivityAt + SESSION_TIMEOUT_MS
        fun isExpired() = System.currentTimeMillis() > expiresAt
        fun refresh() {
            lastActivityAt = System.currentTimeMillis()
        }
    }

    fun initialize() {
        currentAccessCode = generateAccessCode()
        activeSession = null
        failedAttempts.clear()
    }

    fun getAccessCode(): String = currentAccessCode

    @Throws(RateLimitedException::class)
    fun authenticate(suppliedCode: String, remoteIp: String): String? {
        if (isLocked(remoteIp)) {
            throw RateLimitedException(getRemainingLockSeconds(remoteIp))
        }
        return if (MessageDigest.isEqual(
                suppliedCode.toByteArray(Charsets.UTF_8),
                currentAccessCode.toByteArray(Charsets.UTF_8)
            )
        ) {
            clearAttempts(remoteIp)
            val token = generateSessionToken()
            activeSession = MutableSession(token)
            token
        } else {
            recordFailedAttempt(remoteIp)
            if (isLocked(remoteIp)) {
                throw RateLimitedException(getRemainingLockSeconds(remoteIp))
            }
            null
        }
    }

    fun validateToken(token: String): Boolean {
        val session = activeSession ?: return false
        if (!MessageDigest.isEqual(
                session.token.toByteArray(Charsets.UTF_8),
                token.toByteArray(Charsets.UTF_8)
            )
        ) return false
        if (session.isExpired()) {
            activeSession = null
            return false
        }
        session.refresh()
        return true
    }

    fun purgeExpiredSessions() {
        val session = activeSession ?: return
        if (session.isExpired()) activeSession = null
    }

    fun invalidateSession() {
        activeSession = null
    }

    private fun isLocked(ip: String): Boolean {
        val record = failedAttempts[ip] ?: return false
        return record.lockedUntil > System.currentTimeMillis()
    }

    private fun getRemainingLockSeconds(ip: String): Long {
        val record = failedAttempts[ip] ?: return 0
        return ((record.lockedUntil - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
    }

    private fun recordFailedAttempt(ip: String) {
        val now = System.currentTimeMillis()
        failedAttempts.compute(ip) { _, existing ->
            val count = (existing?.count ?: 0) + 1
            val locked = if (count >= MAX_ATTEMPTS) now + LOCKOUT_DURATION_MS else 0L
            AttemptRecord(count, locked)
        }
    }

    private fun clearAttempts(ip: String) = failedAttempts.remove(ip)

    private fun generateAccessCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val bytes = ByteArray(8)
        secureRandom.nextBytes(bytes)
        return bytes.map { alphabet[it.toInt().and(0xFF) % alphabet.length] }.joinToString("")
    }

    private fun generateSessionToken(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    class RateLimitedException(val retryAfterSeconds: Long) : Exception(
        "Too many failed attempts. Retry after $retryAfterSeconds seconds."
    )

    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 60_000L
        private const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L
    }
}
