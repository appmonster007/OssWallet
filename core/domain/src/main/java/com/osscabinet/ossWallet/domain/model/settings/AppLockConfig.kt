package com.osscabinet.ossWallet.domain.model.settings

/**
 * User-facing App Lock + privacy-screen configuration.
 *
 * The PIN itself is never held here — only the salted PBKDF2 hash lives in storage, and it is
 * deliberately not exposed through this model.
 */
data class AppLockConfig(
    val enabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    /** Seconds the app may stay backgrounded before re-locking. 0 = lock immediately. */
    val timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS,
    /** Blocks screenshots and blanks the recents thumbnail (FLAG_SECURE). */
    val privacyScreenEnabled: Boolean = true,
    val hasPin: Boolean = false
) {
    /** App Lock only protects anything when a PIN exists as the mandatory fallback. */
    val isActive: Boolean get() = enabled && hasPin

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 60
        val TIMEOUT_OPTIONS_SECONDS = listOf(0, 60, 300)
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 8
    }
}
