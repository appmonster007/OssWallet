package com.osscabinet.ossWallet.domain.service.security

/** Read-only view of App Lock for layers that must refuse work while the app is locked. */
interface AppLockGate {
    /** True when the app is locked, including a timeout that elapsed while backgrounded. */
    fun isLockedNow(): Boolean
}
