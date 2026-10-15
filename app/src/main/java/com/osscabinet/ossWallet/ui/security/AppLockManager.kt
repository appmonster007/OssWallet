package com.osscabinet.ossWallet.ui.security

import android.os.SystemClock
import com.osscabinet.ossWallet.domain.model.settings.AppLockConfig
import com.osscabinet.ossWallet.domain.repository.AppLockRepository
import com.osscabinet.ossWallet.domain.service.security.AppLockGate
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LockState { Loading, Locked, Unlocked }

/**
 * Process-wide lock state shared by every entry-point activity.
 *
 * Starts [LockState.Loading] (treated as locked) so nothing renders before the stored
 * config has been read — a cold start can never flash content.
 */
@Singleton
class AppLockManager @Inject constructor(
    private val repository: AppLockRepository
) : AppLockGate {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(LockState.Loading)
    val state: StateFlow<LockState> = _state.asStateFlow()

    /** Null until the first read; consumers treat null as "assume privacy screen on". */
    private val _config = MutableStateFlow<AppLockConfig?>(null)
    val config: StateFlow<AppLockConfig?> = _config.asStateFlow()

    @Volatile private var backgroundedAtMs = 0L
    private var failedAttempts = 0
    private var lockedOutUntilMs = 0L

    init {
        scope.launch {
            repository.config.collect { cfg ->
                _config.value = cfg
                when {
                    !cfg.isActive -> _state.value = LockState.Unlocked
                    _state.value == LockState.Loading -> _state.value = LockState.Locked
                }
            }
        }
    }

    fun onAppBackgrounded() {
        backgroundedAtMs = SystemClock.elapsedRealtime()
    }

    /** Applies the re-lock timeout. Call from every activity's onStart. */
    fun onAppForegrounded() {
        applyTimeout()
        backgroundedAtMs = 0L
    }

    /**
     * Callable from any thread. The timeout is evaluated here, not only on foregrounding, so a
     * phone sitting backgrounded past the timeout counts as locked even though no activity
     * has come back to notice yet.
     */
    override fun isLockedNow(): Boolean {
        applyTimeout()
        return _state.value != LockState.Unlocked
    }

    private fun applyTimeout() {
        val cfg = _config.value
        val since = backgroundedAtMs
        if (cfg == null || !cfg.isActive || since == 0L) return
        if (SystemClock.elapsedRealtime() - since >= cfg.timeoutSeconds * 1000L) {
            _state.value = LockState.Locked
        }
    }

    fun unlock() {
        failedAttempts = 0
        lockedOutUntilMs = 0L
        _state.value = LockState.Unlocked
    }

    /** Verifies [pin]; returns remaining lock-out millis (0 when the attempt was allowed). */
    suspend fun tryPin(pin: CharArray): PinResult {
        val now = SystemClock.elapsedRealtime()
        if (now < lockedOutUntilMs) return PinResult.LockedOut(lockedOutUntilMs - now)
        return if (repository.verifyPin(pin)) {
            unlock()
            PinResult.Success
        } else {
            failedAttempts++
            if (failedAttempts >= MAX_FREE_ATTEMPTS) {
                val steps = failedAttempts - MAX_FREE_ATTEMPTS
                val delayMs = (BASE_LOCKOUT_MS shl steps.coerceAtMost(6))
                lockedOutUntilMs = SystemClock.elapsedRealtime() + delayMs
                PinResult.LockedOut(delayMs)
            } else {
                PinResult.Wrong(MAX_FREE_ATTEMPTS - failedAttempts)
            }
        }
    }

    sealed interface PinResult {
        data object Success : PinResult
        data class Wrong(val attemptsLeft: Int) : PinResult
        data class LockedOut(val remainingMs: Long) : PinResult
    }

    private companion object {
        const val MAX_FREE_ATTEMPTS = 5
        const val BASE_LOCKOUT_MS = 30_000L
    }
}
