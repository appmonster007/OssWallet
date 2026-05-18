# FEATURE PLAN: Open in Web — Local Web Portal with QR Code Access (v2)

**Status:** Reviewed & refined — ready for implementation
**Revision:** v2.1 — security hardening, asset-loading fix, ViewModel/Service architecture corrected, timing-safe auth, HTTP security headers
**Replaces:** FEATURE_PLAN_OPEN_IN_WEB.md (v1 is superseded and must be deleted)
**Target branch:** feature/open-in-web (branch from main)

---

## Table of Contents

1. Feature Overview
2. Architecture Overview
3. Security Architecture
4. Dependency Changes
5. Permissions and Manifest Changes
6. Domain Layer Changes
7. Data Layer Changes
8. Ktor Server Configuration
9. REST API Endpoints
10. Web UI — Self-Contained HTML
11. AppContainer Wiring
12. ViewModel Layer
13. Compose Screen
14. Foreground Service
15. Navigation Integration
16. R8 / ProGuard Rules
17. AndroidManifest.xml Changes
18. Testing Plan
19. Phase Implementation Order
20. Known Limitations and Future Work

---

## 1. Feature Overview

### Description

"Open in Web" allows a user to start a local HTTP server directly on their Android device. Any device on the same WiFi network can then open a browser, scan a QR code or type the displayed URL, enter a short one-time access code shown on the phone screen, and browse their financial data — accounts, transactions, and net worth summary — in a clean responsive web interface.

All data stays on the local network. There is no cloud relay, no account required, and no data leaves the premises.

### Business Value

- Larger-screen browsing for users who want a spreadsheet-like overview of their finances without needing a desktop app.
- Privacy-first: data never leaves the local WiFi segment.
- Competitive differentiation for a self-hosted, privacy-focused personal finance app.
- Zero ongoing infrastructure cost — no server, no sync service.

### In Scope

- Local HTTP server using Ktor CIO embedded in the Android app.
- Foreground service to keep the server running when the app is backgrounded.
- One-time session token authentication (cryptographically random, not a PIN).
- Rate-limited auth endpoint (max 5 failed attempts, 60-second lockout).
- Session expiry after 30 minutes of inactivity.
- Server binds only to the WiFi interface IP, not 0.0.0.0.
- QR code generation displaying the access URL.
- REST API exposing: accounts with balances, paginated transactions, monthly summary.
- Single-file self-contained HTML/CSS/JS web UI (no npm, no build step).
- Persistence of the "server enabled" preference via DataStore so the server auto-restarts on app open.
- Navigation entry in the existing Settings screen or app drawer.

### Out of Scope (MVP)

- HTTPS / TLS (documented as future work).
- WebSocket real-time data push (polling only in MVP).
- Multi-session support (single active session per server instance).
- mDNS / Bonjour zero-config discovery.
- Write operations from the web UI (read-only access in MVP).
- Data export (CSV/PDF) from the web UI.

---

## 2. Architecture Overview

```
+--------------------------------------------------------------+
|                       ANDROID DEVICE                         |
|                                                              |
|  +----------------------------------------------------------+|
|  |   Jetpack Compose UI  (ui/ layer)                       ||
|  |   OpenInWebScreen                                        ||
|  |    - toggle enabled/disabled                             ||
|  |    - shows QR code bitmap (rendered from WiFi URL)       ||
|  |    - shows 8-char access code for user to type           ||
|  |    - copy-URL button, status indicator, WiFi warning     ||
|  +------------------+---------------------------------------+|
|                     | observes StateFlow<WebServerState>     |
|  +------------------v---------------------------------------+|
|  |   OpenInWebViewModel  (ui/ layer)                       ||
|  |    - depends on WebServerRepository (domain interface)   ||
|  |    - depends on WebServerPreferences (data layer)        ||
|  |    - toggleServer(enabled) persists pref + starts svc    ||
|  +------------------+---------------------------------------+|
|                     | calls domain interface                 |
|  +------------------v---------------------------------------+|
|  |   WebServerRepository  (domain/ interface)              ||
|  |    - start() / stop()                                    ||
|  |    - serverState: StateFlow<WebServerState>              ||
|  |    - getAccessUrl(): String                              ||
|  |    - getAccessCode(): String                             ||
|  +------------------+---------------------------------------+|
|                     | implemented by                         |
|  +------------------v---------------------------------------+|
|  |   WebServerManager  (data/ layer)                       ||
|  |    - owns Ktor ApplicationEngine lifecycle               ||
|  |    - binds to WiFi IP only (via NetworkUtils)            ||
|  |    - delegates auth to AuthSessionManager                ||
|  |    - delegates routes to WebApiRoutes                    ||
|  |    - receives repositories from AppContainer constructor  ||
|  +---+------------------+------------------+---------------+|
|      |                  |                  |                |
|  +---v------+   +-------v-----+   +--------v---------+     |
|  | Account  |   | Transaction |   | AuthSession      |     |
|  | Repo     |   | Repo        |   | Manager          |     |
|  | (domain  |   | (domain     |   | (rate limit,     |     |
|  | interface)|  | interface)  |   | token store,     |     |
|  +----------+   +-------------+   | expiry)          |     |
|                                   +------------------+     |
|                                                            |
|  WebServerService (foreground Service)                     |
|   - gets WebServerManager from OssWalletApp.container      |
|   - posts persistent notification with Stop action         |
|   - START_STICKY                                           |
+--------------------------------------------------------------+
              |  HTTP over WiFi (port 8192)
              |
+--------------------------------------------------------------+
|   DESKTOP / LAPTOP BROWSER (same WiFi segment)              |
|                                                              |
|   1. Scan QR  -> browser opens http://192.168.x.x:8192      |
|   2. Login page -> user types 8-char access code            |
|   3. POST /api/auth -> server returns session token         |
|   4. Browser stores token in sessionStorage                  |
|   5. All /api/* calls send Authorization: Bearer <token>    |
|   6. Hash-routed SPA: #/accounts  #/transactions  #/summary |
+--------------------------------------------------------------+
```

### How it fits the existing layers

The feature introduces artifacts at every layer but never violates layer boundaries:

- `domain/repository/WebServerRepository.kt` — pure Kotlin interface, no Android imports.
- `domain/model/web/WebServerState.kt`, `WebAccessSession.kt` — pure Kotlin sealed classes and data classes.
- `data/web/WebServerManager.kt` — implements `WebServerRepository`, owns the Ktor engine, has Android imports (`Context`, `ConnectivityManager`).
- `data/web/WebServerPreferences.kt` — DataStore wrapper for the "is server enabled" boolean.
- `data/web/NetworkUtils.kt` — WiFi IP detection using `ConnectivityManager` and `NetworkInterface`.
- `data/web/AuthSessionManager.kt` — pure Kotlin in-memory auth; no Android imports required.
- `data/web/WebApiRoutes.kt` — Ktor routing, calls existing domain repository interfaces.
- `data/web/QRCodeGenerator.kt` — Android bitmap generation.
- `ui/screens/openinweb/OpenInWebScreen.kt` + `OpenInWebViewModel.kt` — Compose screen and ViewModel.
- `service/WebServerService.kt` — Android `Service`, gets container from `OssWalletApp`.
- `di/AppContainer.kt` — wires all new components; the only change to existing files beyond navigation.

The API layer accesses financial data exclusively through the existing domain repository interfaces (`TransactionRepository`, `AccountRepository`, `SettingsRepository`). It never touches Room entities, DAOs, or the SQLCipher passphrase.

---

## 3. Security Architecture

This section is the authoritative security specification for the feature. Every decision below must be implemented exactly as described.

### 3.1 Session Token

The access code displayed to the user on screen is **not** the session token. They are two distinct things:

- **Access code**: A human-readable, 8-character alphanumeric string (uppercase letters A-Z and digits 2-9, omitting ambiguous characters 0, O, 1, I). Generated once when the server starts using `SecureRandom`. Displayed on-screen for the user to type into the browser login page. This is the shared secret that proves "you have the phone."
- **Session token**: A 128-bit (16-byte) cryptographically random value from `SecureRandom`, Base64url-encoded (22 characters). Generated server-side when the access code is validated successfully. Returned to the browser in the auth response. Used as a Bearer token for all subsequent API calls. Never displayed to the user.

```kotlin
// AuthSessionManager.kt — token generation
private fun generateAccessCode(): String {
    val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // 32 chars, no ambiguous
    val bytes = ByteArray(8)
    SecureRandom().nextBytes(bytes)
    return bytes.map { alphabet[it.toInt().and(0xFF) % alphabet.length] }.joinToString("")
}

private fun generateSessionToken(): String {
    val bytes = ByteArray(16)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
```

### 3.2 Token Storage

- **Server side**: The session token is held in a `ConcurrentHashMap<String, WebAccessSession>` in `AuthSessionManager`. It is **never written to disk, never written to DataStore, never written to Room**.
- **Browser side**: The session token is stored in `sessionStorage` (not `localStorage`). This means the token is automatically cleared when the browser tab is closed. A new login is required for each browser session.
- **Access code side**: The access code is held in `AuthSessionManager` as a plain `String` field in memory. It is regenerated each time the server starts. It is never written to disk.

### 3.3 Rate Limiting

`AuthSessionManager` tracks failed authentication attempts per remote IP address. Implementation:

The complete thread-safe implementation is in `AuthSessionManager.kt` (Section 7.2). The key structural invariant: `authenticate()` calls `isLocked()` before ever reaching `recordFailedAttempt()`, so the compute lambda in `recordFailedAttempt` does not need a secondary lockout guard — the gate is at the `authenticate()` call site.

Key state shape:
```kotlin
// In AuthSessionManager — internal tracking types
private val failedAttempts = ConcurrentHashMap<String, AttemptRecord>()
private data class AttemptRecord(val count: Int, val lockedUntil: Long)
```

Rules:
- 5th consecutive failed attempt from the same IP triggers a 60-second lockout.
- Successful authentication clears that IP's attempt record.
- Lockout state is in-memory only — resets on server restart (acceptable for MVP; a determined local attacker could restart to clear it, but the access code also regenerates on restart).
- Locked IPs receive `HTTP 429 Too Many Requests` with `Retry-After: <seconds>` header.

### 3.4 Session Expiry

Each `WebAccessSession` tracks `lastActivityAt`. Any authenticated API call refreshes this timestamp. The session expires if no call is made for 30 minutes.

```kotlin
data class WebAccessSession(
    val token: String,
    val createdAt: Long = System.currentTimeMillis(),
    var lastActivityAt: Long = System.currentTimeMillis()
) {
    val expiresAt: Long get() = lastActivityAt + 30 * 60 * 1000L
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    fun refresh() { lastActivityAt = System.currentTimeMillis() }
}
```

`AuthSessionManager.validateToken(token)` returns the session if it exists and `!isExpired()`, calls `session.refresh()`, and returns null (causing 401) otherwise.

A background coroutine in `WebServerManager` sweeps expired sessions every 5 minutes:

```kotlin
// Inside WebServerManager.start()
serverScope.launch {
    while (isActive) {
        delay(5 * 60 * 1000L)
        authSessionManager.purgeExpiredSessions()
    }
}
```

### 3.5 Network Binding

The Ktor server binds to the device's WiFi interface IP address specifically — not `0.0.0.0`. This prevents access via USB tethering, ADB forwarding, or other non-WiFi network interfaces.

```kotlin
// WebServerManager.kt
private fun getWifiIp(): String? = NetworkUtils.getWifiInterfaceIp(context)

// In startServer():
val ip = getWifiIp() ?: throw IllegalStateException("Not connected to WiFi")
engine = embeddedServer(CIO, host = ip, port = SERVER_PORT) {
    webServerModule(authSessionManager, repositories)
}.start(wait = false)
```

If the WiFi IP is unavailable (user disconnects during operation), the existing session continues on its port until the server is stopped or the session expires. The UI should warn users if WiFi disconnects while the server is running.

### 3.6 CORS

CORS is restricted to same-origin only. Since the web UI is served from the same Ktor server that hosts the API, the browser and the API share the same origin (`http://<wifi-ip>:8192`). No cross-origin requests are needed.

```kotlin
install(CORS) {
    // Allow only same-origin requests
    // Do NOT use allowHost("*")
    // The web UI is served from the same origin, so explicit CORS is not required.
    // This block intentionally left restrictive — no allowHost() calls.
}
```

If Ktor's CORS plugin requires at least one `allowHost` entry to function, use:

```kotlin
install(CORS) {
    allowHost("localhost")
    // The actual WiFi IP requests come from same origin; browser does not send CORS preflight
    // for same-origin. This entry is a no-op safety net for development only.
}
```

### 3.7 Data Exposure Policy

The API must not expose:
- Internal Room entity IDs that reveal database schema (use opaque string IDs in responses).
- The SQLCipher passphrase or any key material.
- Any `BuildConfig` field values.
- Full account numbers, sort codes, or any PII beyond what the user sees in the app.
- Stack traces or internal error details in error responses (log them server-side only).

### 3.8 No HTTPS for MVP

Implementing HTTPS on Android requires either:
- A self-signed certificate, which all browsers reject with a hard error (not just a warning on some browsers).
- A certificate signed by a custom CA installed on the client device — unacceptable UX.
- Let's Encrypt — impossible for a local LAN IP address.

The MVP ships HTTP only on the local WiFi segment. This is documented as a known limitation. The threat model is: the attacker must be on the same physical WiFi network. The session token and access code provide authentication. Data in transit is visible to local network sniffing — acceptable for a home/office WiFi scenario.

Future path: Android 14+ supports the `TRANSPORT_VPN` flag; a future version could route the server through a local mTLS tunnel if the ecosystem matures.

### 3.9 R8 Keep Rules

See Section 16 for the full proguard-rules.pro additions.

---

## 4. Dependency Changes

### 4.1 `gradle/libs.versions.toml` — versions section additions

```toml
[versions]
# Add these entries alongside existing ones:
ktor = "3.1.3"
qrcode-kotlin = "4.5.0"
```

### 4.2 `gradle/libs.versions.toml` — libraries section additions

```toml
[libraries]
# Add these entries alongside existing ones:
ktor-server-core                = { group = "io.ktor", name = "ktor-server-core",                version.ref = "ktor" }
ktor-server-cio                 = { group = "io.ktor", name = "ktor-server-cio",                 version.ref = "ktor" }
ktor-server-content-negotiation = { group = "io.ktor", name = "ktor-server-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-server-cors                = { group = "io.ktor", name = "ktor-server-cors",                version.ref = "ktor" }
ktor-server-status-pages        = { group = "io.ktor", name = "ktor-server-status-pages",        version.ref = "ktor" }
ktor-server-compression         = { group = "io.ktor", name = "ktor-server-compression",         version.ref = "ktor" }
ktor-server-call-logging        = { group = "io.ktor", name = "ktor-server-call-logging",        version.ref = "ktor" }
ktor-server-default-headers     = { group = "io.ktor", name = "ktor-server-default-headers",     version.ref = "ktor" }
qrcode-kotlin-android           = { group = "io.github.g0dkar", name = "qrcode-kotlin-android",  version.ref = "qrcode-kotlin" }
```

### 4.3 `app/build.gradle.kts` — implementation lines to add

Add these inside the `dependencies { }` block, grouped with the other `implementation(...)` lines:

```kotlin
// Ktor embedded HTTP server
implementation(libs.ktor.server.core)
implementation(libs.ktor.server.cio)
implementation(libs.ktor.server.content.negotiation)
implementation(libs.ktor.serialization.kotlinx.json)
implementation(libs.ktor.server.cors)
implementation(libs.ktor.server.status.pages)
implementation(libs.ktor.server.compression)
// Call logging is debug-only to avoid log spam in release:
debugImplementation(libs.ktor.server.call.logging)
// HTTP security headers (CSP, X-Content-Type-Options, X-Frame-Options):
implementation(libs.ktor.server.default.headers)
// QR code generation
implementation(libs.qrcode.kotlin.android)
```

Note: `kotlinx-serialization-json` is already in the project (`libs.kotlinx.serialization.json`). Ktor's kotlinx-json serialization plugin shares the same library — no version conflict.

**Note — serialization plugin version mismatch**: `app/build.gradle.kts` line 7 already applies the serialization plugin, but at `version "2.0.21"` while the project Kotlin version is `"2.2.10"`. Update the existing line:
```kotlin
// In app/build.gradle.kts plugins block — change the version on the existing line:
kotlin("plugin.serialization") version "2.2.10"
```
No new plugin entry is needed — only align the version string. Mismatched versions cause subtle IR serialization bugs in release builds.

### 4.4 No new DI framework

No Hilt, no Dagger, no Koin. All wiring is manual through `AppContainer` as with the rest of the app.

---

## 5. Permissions and Manifest Changes

The full diff for `app/src/main/AndroidManifest.xml` is documented in Section 17. Permissions required:

```xml
<!-- Already present — no change needed: -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- New — add these: -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
```

`INTERNET` and `ACCESS_NETWORK_STATE` already exist. `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_DATA_SYNC` are needed for the foreground service. `POST_NOTIFICATIONS` (already present) is needed for the foreground service notification on Android 13+.

The foreground service type is `dataSync`, which is appropriate because the service is serving local data over a network connection. This matches the `android:foregroundServiceType="dataSync"` attribute on the `<service>` declaration.

---

## 6. Domain Layer Changes

All files in this section live under `app/src/main/java/com/osscabinet/ossWallet/domain/`. Zero Android imports allowed.

### 6.1 `domain/model/web/WebServerState.kt`

```kotlin
package com.osscabinet.ossWallet.domain.model.web

sealed class WebServerState {
    object Stopped : WebServerState()
    object Starting : WebServerState()
    data class Running(
        val ip: String,
        val port: Int,
        val accessCode: String,
        val accessUrl: String = "http://$ip:$port"
    ) : WebServerState()
    data class Error(val message: String) : WebServerState()
}
```

### 6.2 Session management — domain boundary note

Session management is encapsulated entirely within `AuthSessionManager` in the `data/` layer. There is **no public domain model for sessions** — the session token is an opaque `String` from all callers' perspectives. `AuthSessionManager` uses an internal `MutableSession` class (data-layer private) to track token, `createdAt`, and `lastActivityAt` without leaking mutability or internal state to the domain or UI layers.

A `WebAccessSession` domain model was considered but removed: it created a false impression that sessions were a domain concept with business rules, when in fact session lifecycle (expiry, refresh, lockout) is purely a data-layer concern for this feature.

If multi-session support is added in a future version, introduce a proper domain session aggregate at that time.

### 6.3 `domain/repository/WebServerRepository.kt`

```kotlin
package com.osscabinet.ossWallet.domain.repository

import com.osscabinet.ossWallet.domain.model.web.WebServerState
import kotlinx.coroutines.flow.StateFlow

interface WebServerRepository {
    val serverState: StateFlow<WebServerState>
    suspend fun start()
    suspend fun stop()
    fun isRunning(): Boolean
    fun getAccessUrl(): String?
    fun getAccessCode(): String?
}
```

---

## 7. Data Layer Changes

All files in this section live under `app/src/main/java/com/osscabinet/ossWallet/data/web/`.

### 7.1 `data/web/NetworkUtils.kt`

```kotlin
package com.osscabinet.ossWallet.data.web

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {

    /**
     * Returns the IPv4 address of the active WiFi interface, or null if not on WiFi.
     * Binds to the WiFi interface specifically — not 0.0.0.0 — to prevent
     * access via USB tethering or ADB port forwarding.
     */
    fun getWifiInterfaceIp(context: Context): String? {
        if (!isConnectedToWifi(context)) return null
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            // WifiManager.connectionInfo.ipAddress is deprecated in API 31 but works.
            // As a fallback, enumerate NetworkInterface.
            val wifiIp = intToIp(wifiManager.connectionInfo.ipAddress)
            if (wifiIp != null && wifiIp != "0.0.0.0") return wifiIp

            // Fallback: enumerate interfaces and match wlan0 / wlan1 family
            NetworkInterface.getNetworkInterfaces()?.asSequence()
                ?.filter { it.name.startsWith("wlan") && it.isUp && !it.isLoopback }
                ?.flatMap { it.inetAddresses.asSequence() }
                ?.filterIsInstance<Inet4Address>()
                ?.map { it.hostAddress }
                ?.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun isConnectedToWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun intToIp(ip: Int): String? {
        if (ip == 0) return null
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}
```

### 7.2 `data/web/AuthSessionManager.kt`

```kotlin
package com.osscabinet.ossWallet.data.web

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the one-time access code and the single active session token.
 * All state is in-memory only — nothing is persisted to disk.
 * Thread-safe via ConcurrentHashMap.
 */
class AuthSessionManager {

    // 8-character human-readable code displayed on the phone screen
    private var currentAccessCode: String = ""

    // Single active session (MVP: one session at a time)
    private var activeSession: MutableSession? = null

    // Rate limiting: tracks failed attempts per remote IP
    private val failedAttempts = ConcurrentHashMap<String, AttemptRecord>()

    private data class AttemptRecord(val count: Int, val lockedUntil: Long)

    private data class MutableSession(
        val token: String,
        val createdAt: Long = System.currentTimeMillis(),
        @Volatile var lastActivityAt: Long = System.currentTimeMillis()
    ) {
        val expiresAt: Long get() = lastActivityAt + SESSION_TIMEOUT_MS
        fun isExpired() = System.currentTimeMillis() > expiresAt
        fun refresh() { lastActivityAt = System.currentTimeMillis() }
    }

    fun initialize() {
        currentAccessCode = generateAccessCode()
        activeSession = null
        failedAttempts.clear()
    }

    fun getAccessCode(): String = currentAccessCode

    /**
     * Validates the supplied access code against the current code.
     * Returns a new session token on success, or null if invalid/rate-limited.
     * Throws [RateLimitedException] if the IP is locked out.
     */
    @Throws(RateLimitedException::class)
    fun authenticate(suppliedCode: String, remoteIp: String): String? {
        if (isLocked(remoteIp)) {
            throw RateLimitedException(getRemainingLockSeconds(remoteIp))
        }
        // Constant-time comparison prevents timing-oracle attacks on the access code
        return if (MessageDigest.isEqual(
            suppliedCode.toByteArray(Charsets.UTF_8),
            currentAccessCode.toByteArray(Charsets.UTF_8)
        )) {
            clearAttempts(remoteIp)
            val token = generateSessionToken()
            activeSession = MutableSession(token)
            token
        } else {
            recordFailedAttempt(remoteIp)
            null
        }
    }

    /**
     * Validates a bearer token. Returns true and refreshes the session if valid.
     * Returns false if missing, expired, or unknown.
     */
    fun validateToken(token: String): Boolean {
        val session = activeSession ?: return false
        // Constant-time comparison prevents timing-oracle attacks on session tokens
        if (!MessageDigest.isEqual(
            session.token.toByteArray(Charsets.UTF_8),
            token.toByteArray(Charsets.UTF_8)
        )) return false
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
        // Unambiguous uppercase alphanumeric: no 0/O/1/I confusion
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val bytes = ByteArray(8)
        SecureRandom().nextBytes(bytes)
        return bytes.map { alphabet[it.toInt().and(0xFF) % alphabet.length] }.joinToString("")
    }

    private fun generateSessionToken(): String {
        val bytes = ByteArray(16) // 128 bits
        SecureRandom().nextBytes(bytes)
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
```

### 7.3 `data/web/QRCodeGenerator.kt`

```kotlin
package com.osscabinet.ossWallet.data.web

import android.graphics.Bitmap
import qrcode.QRCode

object QRCodeGenerator {
    /**
     * Generates a square QR code bitmap for [text].
     * [sizePx] is the output size in pixels (default 512 — scale down in UI as needed).
     */
    fun generate(text: String, sizePx: Int = 512): Bitmap {
        return QRCode.ofSquares()
            .withSize(sizePx)
            .build(text)
            .renderToAndroidBitmap()
    }
}
```

### 7.4 `data/web/WebServerPreferences.kt`

DataStore-backed persistence for the "server enabled" user preference. Uses the existing DataStore dependency already in the project.

```kotlin
package com.osscabinet.ossWallet.data.web

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.webServerDataStore by preferencesDataStore(name = "web_server_prefs")

class WebServerPreferences(private val context: Context) {

    private val KEY_ENABLED = booleanPreferencesKey("server_enabled")

    val isEnabled: Flow<Boolean> = context.webServerDataStore.data
        .map { prefs -> prefs[KEY_ENABLED] ?: false }

    suspend fun setEnabled(enabled: Boolean) {
        context.webServerDataStore.edit { prefs ->
            prefs[KEY_ENABLED] = enabled
        }
    }
}
```

### 7.5 `data/web/WebApiRoutes.kt`

Contains all Ktor route definitions. Receives repositories as constructor parameters so the data layer never violates domain boundaries.

```kotlin
package com.osscabinet.ossWallet.data.web

import android.content.Context
import com.osscabinet.ossWallet.data.web.dto.AccountDto
import com.osscabinet.ossWallet.data.web.dto.ApiResponse
import com.osscabinet.ossWallet.data.web.dto.AuthRequest
import com.osscabinet.ossWallet.data.web.dto.AuthResponse
import com.osscabinet.ossWallet.data.web.dto.MonthlySummaryDto
import com.osscabinet.ossWallet.data.web.dto.PagedResponse
import com.osscabinet.ossWallet.data.web.dto.TransactionDto
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class WebApiRoutes(
    private val context: Context,
    private val authSessionManager: AuthSessionManager,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val settingsRepository: SettingsRepository
) {
    fun Application.installRoutes() {
        routing {
            // Serve index.html for root path
            get("/") {
                call.respondText(
                    loadIndexHtml(),
                    io.ktor.http.ContentType.Text.Html
                )
            }

            // Unauthenticated health check
            get("/api/health") {
                call.respond(ApiResponse(success = true, data = mapOf("status" to "ok")))
            }

            // Authentication endpoint
            post("/api/auth") {
                val remoteIp = call.request.local.remoteAddress
                val request = runCatching { call.receive<AuthRequest>() }.getOrNull()
                    ?: run {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(success = false, error = "Invalid request body")
                        )
                        return@post
                    }

                try {
                    val token = authSessionManager.authenticate(request.accessCode, remoteIp)
                    if (token != null) {
                        call.respond(ApiResponse(success = true, data = AuthResponse(token = token)))
                    } else {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiResponse<Unit>(success = false, error = "Invalid access code")
                        )
                    }
                } catch (e: AuthSessionManager.RateLimitedException) {
                    call.response.headers.append("Retry-After", e.retryAfterSeconds.toString())
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        ApiResponse<Unit>(success = false, error = "Too many attempts. Try again in ${e.retryAfterSeconds}s.")
                    )
                }
            }

            // All /api/* routes below require authentication
            route("/api") {
                // Auth middleware applied per-route via a helper
                get("/accounts") {
                    if (!authenticate(call, authSessionManager)) return@get
                    val profileId = settingsRepository.currentProfileId.first()
                    val accounts = accountRepository.getAccountsForProfile(profileId).first()
                    val dtos = accounts.map { account ->
                        AccountDto(
                            id = account.id,
                            name = account.name,
                            currency = account.currency,
                            balance = account.balance,
                            type = account.type.name
                        )
                    }
                    call.respond(ApiResponse(success = true, data = dtos))
                }

                get("/transactions") {
                    if (!authenticate(call, authSessionManager)) return@get
                    val profileId = settingsRepository.currentProfileId.first()
                    val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull()
                        ?.coerceIn(1, 100) ?: 25
                    val accountId = call.request.queryParameters["accountId"]
                    val offset = page * limit

                    val transactions = transactionRepository
                        .getTransactionsForProfile(profileId, limit, offset, accountId)
                        .first()
                    val total = transactionRepository.countTransactionsForProfile(profileId, accountId)

                    val dtos = transactions.map { tx ->
                        TransactionDto(
                            id = tx.id,
                            title = tx.title,
                            amount = tx.amount,
                            currency = tx.currency,
                            type = tx.type.name,
                            categoryName = tx.categoryName,
                            accountName = tx.accountName,
                            date = tx.date,
                            note = tx.note
                        )
                    }
                    call.respond(
                        ApiResponse(
                            success = true,
                            data = PagedResponse(
                                items = dtos,
                                total = total,
                                page = page,
                                limit = limit,
                                hasMore = offset + limit < total
                            )
                        )
                    )
                }

                get("/summary") {
                    if (!authenticate(call, authSessionManager)) return@get
                    val profileId = settingsRepository.currentProfileId.first()
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val monthStart = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-01"

                    val accounts = accountRepository.getAccountsForProfile(profileId).first()
                    val netWorth = accounts.sumOf { it.balance }
                    val monthlyTx = transactionRepository
                        .getTransactionsForMonth(profileId, now.year, now.monthNumber)
                        .first()
                    val income = monthlyTx.filter { it.type.name == "INCOME" }.sumOf { it.amount }
                    val expenses = monthlyTx.filter { it.type.name == "EXPENSE" }.sumOf { it.amount }

                    call.respond(
                        ApiResponse(
                            success = true,
                            data = MonthlySummaryDto(
                                netWorth = netWorth,
                                monthlyIncome = income,
                                monthlyExpenses = expenses,
                                month = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"
                            )
                        )
                    )
                }
            }
        }
    }

    private suspend fun authenticate(
        call: io.ktor.server.application.ApplicationCall,
        manager: AuthSessionManager
    ): Boolean {
        val authHeader = call.request.headers["Authorization"] ?: run {
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(success = false, error = "Missing Authorization header")
            )
            return false
        }
        val token = authHeader.removePrefix("Bearer ").trim()
        if (!manager.validateToken(token)) {
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(success = false, error = "Session expired or invalid")
            )
            return false
        }
        return true
    }

    private fun loadIndexHtml(): String {
        // Android APK assets are NOT on the classloader's resource path.
        // They must be read via AssetManager. Using classloader here would always
        // return the fallback string at runtime.
        return try {
            context.assets.open("web/index.html").bufferedReader().readText()
        } catch (e: Exception) {
            "<html><body>Web UI not found. Rebuild the app.</body></html>"
        }
    }
}
```

### 7.6 DTO classes — `data/web/dto/`

Create these `@Serializable` data classes in `data/web/dto/`:

```kotlin
// ApiResponse.kt
package com.osscabinet.ossWallet.data.web.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// AuthRequest.kt
@Serializable
data class AuthRequest(val accessCode: String)

// AuthResponse.kt
@Serializable
data class AuthResponse(val token: String)

// AccountDto.kt
@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val currency: String,
    val balance: Double,
    val type: String
)

// TransactionDto.kt
@Serializable
data class TransactionDto(
    val id: Long,
    val title: String,
    val amount: Double,
    val currency: String,
    val type: String,
    val categoryName: String?,
    val accountName: String,
    val date: Long,
    val note: String?
)

// MonthlySummaryDto.kt
@Serializable
data class MonthlySummaryDto(
    val netWorth: Double,
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val month: String
)

// PagedResponse.kt
@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)
```

**Precision note — `Double` vs `BigDecimal`**: `AccountDto.balance` and `TransactionDto.amount` are typed as `Double` because `kotlinx.serialization` serializes `BigDecimal` as a JSON string by default, which would complicate the JavaScript web UI (it would need to parse strings instead of reading JSON numbers). `Double` has ~15 significant digits — sufficient for display, but not for financial arithmetic. The web UI must treat these values as display-only and never perform arithmetic on them. The Android domain layer continues to use `BigDecimal` internally; the precision loss happens only in the DTO mapping step inside `WebApiRoutes`, where `bigDecimalValue.toDouble()` is called before constructing the DTO.

### 7.7 `data/web/WebServerManager.kt`

The core class implementing `WebServerRepository`. This owns the Ktor engine lifecycle.

```kotlin
package com.osscabinet.ossWallet.data.web

import android.content.Context
import com.osscabinet.ossWallet.domain.model.web.WebServerState
import com.osscabinet.ossWallet.domain.repository.AccountRepository
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.TransactionRepository
import com.osscabinet.ossWallet.domain.repository.WebServerRepository
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class WebServerManager(
    private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val settingsRepository: SettingsRepository
) : WebServerRepository {

    private val _serverState = MutableStateFlow<WebServerState>(WebServerState.Stopped)
    override val serverState: StateFlow<WebServerState> = _serverState.asStateFlow()

    private var engine: ApplicationEngine? = null
    private var serverScope: CoroutineScope? = null
    private val authSessionManager = AuthSessionManager()

    override fun isRunning(): Boolean = _serverState.value is WebServerState.Running

    override fun getAccessUrl(): String? =
        (_serverState.value as? WebServerState.Running)?.accessUrl

    override fun getAccessCode(): String? =
        (_serverState.value as? WebServerState.Running)?.accessCode

    override suspend fun start() {
        if (isRunning()) return
        _serverState.value = WebServerState.Starting

        val ip = NetworkUtils.getWifiInterfaceIp(context)
        if (ip == null) {
            _serverState.value = WebServerState.Error("Not connected to WiFi")
            return
        }

        authSessionManager.initialize()
        val accessCode = authSessionManager.getAccessCode()

        val routes = WebApiRoutes(
            context = context,
            authSessionManager = authSessionManager,
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            settingsRepository = settingsRepository
        )

        try {
            val serverJson = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
            engine = embeddedServer(CIO, host = ip, port = SERVER_PORT) {
                install(ContentNegotiation) { json(serverJson) }
                install(Compression) { gzip() }
                install(StatusPages) {
                    exception<Throwable> { call, cause ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("success" to false, "error" to "Internal server error")
                        )
                    }
                }
                // Defense-in-depth security headers for the web UI.
                // 'unsafe-inline' is required because index.html uses inline <style> and <script>.
                // For a future multi-file setup, replace with content hashes.
                install(io.ktor.server.plugins.defaultheaders.DefaultHeaders) {
                    header("Content-Security-Policy",
                        "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'")
                    header("X-Content-Type-Options", "nosniff")
                    header("X-Frame-Options", "DENY")
                    header("Referrer-Policy", "no-referrer")
                }
                if (BuildConfig.DEBUG) {
                    install(io.ktor.server.plugins.calllogging.CallLogging)
                }
                with(routes) { installRoutes() }
            }.start(wait = false)

            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            serverScope = scope

            // Background sweep for expired sessions
            scope.launch {
                while (isActive) {
                    delay(5 * 60 * 1000L)
                    authSessionManager.purgeExpiredSessions()
                }
            }

            _serverState.value = WebServerState.Running(
                ip = ip,
                port = SERVER_PORT,
                accessCode = accessCode
            )
        } catch (e: Exception) {
            _serverState.value = WebServerState.Error(e.message ?: "Failed to start server")
            engine = null
        }
    }

    override suspend fun stop() {
        authSessionManager.invalidateSession()
        engine?.stop(gracePeriodMillis = 500, timeoutMillis = 2000)
        engine = null
        serverScope?.cancel()
        serverScope = null
        _serverState.value = WebServerState.Stopped
    }

    companion object {
        const val SERVER_PORT = 8192
    }
}
```

---

## 8. Ktor Server Configuration

The `Application.module()` is set up inside `WebServerManager.start()` via the `embeddedServer` lambda. Key configuration decisions:

### Plugins installed

| Plugin | Config | Reason |
|---|---|---|
| `ContentNegotiation` | `json(serverJson)` | Serializes `@Serializable` response DTOs to JSON |
| `Compression` | `gzip()` | Reduces transfer size for transaction lists on slow WiFi |
| `StatusPages` | generic `Throwable` handler | Returns JSON error rather than HTML stacktrace — no stack traces exposed |
| `DefaultHeaders` | CSP + X-Content-Type-Options + X-Frame-Options + Referrer-Policy | Defense-in-depth for the embedded web UI |
| `CallLogging` | debug builds only (`BuildConfig.DEBUG` guard) | Avoid log spam in release; conditional install inside `embeddedServer` lambda |

### Auth middleware

Auth is not a Ktor plugin in this implementation — it is a per-route manual check via the `authenticate()` helper function in `WebApiRoutes`. This avoids Ktor Auth plugin complexity and keeps the logic explicit and testable.

Every `/api/*` route except `/api/health` and `POST /api/auth` calls `authenticate(call, authSessionManager)` as its first statement and returns early if it returns `false`.

### Static file serving

The web UI (`index.html`) is served by reading the asset from the APK's `assets/web/` folder via Android's `AssetManager` (`context.assets.open("web/index.html")`). **Do not use classloader `getResourceAsStream` for Android APK assets** — the classloader path does not include the `assets/` directory; only `AssetManager` can read it at runtime. The root `GET /` handler reads and returns the file as `ContentType.Text.Html`. There is no `static {}` Ktor block, avoiding a separate static-file plugin dependency.

### Conditional call logging

`CallLogging` is installed inside the `embeddedServer` lambda in `WebServerManager.start()` behind a `BuildConfig.DEBUG` guard (see Section 7.7). `ktor-server-call-logging` is `debugImplementation` only in `build.gradle.kts` so it is not included in release APKs at all. `BuildConfig` is an Android import, which is permitted in the `data/` layer.

---

## 9. REST API Endpoints

### Full endpoint table

| Method | Path | Auth required | Description |
|---|---|---|---|
| `GET` | `/` | No | Serves `index.html` |
| `GET` | `/api/health` | No | Server liveness check |
| `POST` | `/api/auth` | No | Exchange access code for session token |
| `GET` | `/api/accounts` | Yes | All accounts with balances for current profile |
| `GET` | `/api/transactions` | Yes | Paginated transaction list |
| `GET` | `/api/summary` | Yes | Monthly net worth + income/expense summary |

### Request / Response formats

#### `POST /api/auth`

Request body:
```json
{ "accessCode": "XYZ23ABC" }
```

Response 200:
```json
{
  "success": true,
  "data": { "token": "dGhpcyBpcyBhIHRva2Vu" },
  "timestamp": 1715000000000
}
```

Response 401 (wrong code):
```json
{ "success": false, "error": "Invalid access code", "timestamp": 1715000000000 }
```

Response 429 (rate limited):
```json
{ "success": false, "error": "Too many attempts. Try again in 58s.", "timestamp": 1715000000000 }
```
Header: `Retry-After: 58`

#### `GET /api/accounts`

Header: `Authorization: Bearer <token>`

Response 200:
```json
{
  "success": true,
  "data": [
    { "id": "acc-uuid-1", "name": "Checking", "currency": "USD", "balance": 1250.50, "type": "CHECKING" }
  ],
  "timestamp": 1715000000000
}
```

#### `GET /api/transactions?page=0&limit=25&accountId=acc-uuid-1`

Query parameters:
- `page` — zero-indexed page number (default: 0)
- `limit` — items per page, 1–100 (default: 25)
- `accountId` — optional filter by account

Response 200:
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 42,
        "title": "Grocery Store",
        "amount": 85.30,
        "currency": "USD",
        "type": "EXPENSE",
        "categoryName": "Food",
        "accountName": "Checking",
        "date": 1714900000000,
        "note": null
      }
    ],
    "total": 143,
    "page": 0,
    "limit": 25,
    "hasMore": true
  },
  "timestamp": 1715000000000
}
```

#### `GET /api/summary`

Response 200:
```json
{
  "success": true,
  "data": {
    "netWorth": 42500.00,
    "monthlyIncome": 5000.00,
    "monthlyExpenses": 2340.75,
    "month": "2026-05"
  },
  "timestamp": 1715000000000
}
```

### `ApiResponse<T>` wrapper

Every response body is `ApiResponse<T>` from `data/web/dto/ApiResponse.kt`. The `timestamp` field is always `System.currentTimeMillis()` at the time of response construction. Error responses have `data = null` and a human-readable `error` string (never a stack trace).

---

## 10. Web UI — Self-Contained HTML

File location: `app/src/main/assets/web/index.html`

This is a single HTML file. No build step. No npm. No Node.js. Vanilla HTML5 + CSS Grid/Flexbox + ES2020 JavaScript. Hash-based routing (`#/login`, `#/accounts`, `#/transactions`, `#/summary`).

```html
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>OssWallet — Web View</title>
<style>
  :root {
    --bg: #ffffff;
    --surface: #f5f5f5;
    --border: #e0e0e0;
    --text: #1a1a1a;
    --text-muted: #666666;
    --accent: #1976D2;
    --accent-hover: #1565C0;
    --danger: #c62828;
    --success: #2e7d32;
    --warning: #f57f17;
    --sidebar-w: 220px;
    --header-h: 56px;
  }
  @media (prefers-color-scheme: dark) {
    :root {
      --bg: #121212;
      --surface: #1e1e1e;
      --border: #2c2c2c;
      --text: #e0e0e0;
      --text-muted: #9e9e9e;
      --accent: #42a5f5;
      --accent-hover: #64b5f6;
      --danger: #ef5350;
      --success: #66bb6a;
      --warning: #ffa726;
    }
  }
  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: system-ui, -apple-system, sans-serif; background: var(--bg); color: var(--text); height: 100dvh; overflow: hidden; }

  /* Layout */
  #app { display: flex; flex-direction: column; height: 100%; }
  #header { display: flex; align-items: center; height: var(--header-h); padding: 0 16px; background: var(--surface); border-bottom: 1px solid var(--border); gap: 12px; flex-shrink: 0; }
  #header h1 { font-size: 1.1rem; font-weight: 600; flex: 1; }
  #main { display: flex; flex: 1; overflow: hidden; }
  #sidebar { width: var(--sidebar-w); background: var(--surface); border-right: 1px solid var(--border); display: flex; flex-direction: column; overflow-y: auto; flex-shrink: 0; }
  #content { flex: 1; overflow-y: auto; padding: 24px; }

  /* Sidebar nav */
  .nav-item { display: flex; align-items: center; gap: 10px; padding: 12px 16px; cursor: pointer; border-radius: 0; transition: background 0.15s; color: var(--text); text-decoration: none; font-size: 0.92rem; }
  .nav-item:hover { background: var(--border); }
  .nav-item.active { background: var(--accent); color: #fff; }
  .nav-icon { width: 20px; text-align: center; }

  /* Cards/surfaces */
  .card { background: var(--surface); border: 1px solid var(--border); border-radius: 10px; padding: 20px; margin-bottom: 16px; }
  .card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px; }
  .stat-card { background: var(--surface); border: 1px solid var(--border); border-radius: 10px; padding: 20px; }
  .stat-label { font-size: 0.8rem; color: var(--text-muted); margin-bottom: 4px; }
  .stat-value { font-size: 1.6rem; font-weight: 700; }
  .stat-value.positive { color: var(--success); }
  .stat-value.negative { color: var(--danger); }

  /* Table */
  .table-wrap { overflow-x: auto; }
  table { width: 100%; border-collapse: collapse; }
  th { text-align: left; padding: 10px 12px; font-size: 0.78rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); border-bottom: 1px solid var(--border); }
  td { padding: 12px 12px; border-bottom: 1px solid var(--border); font-size: 0.9rem; }
  tr:last-child td { border-bottom: none; }
  tr:hover td { background: var(--surface); }
  .amount-income { color: var(--success); font-weight: 600; }
  .amount-expense { color: var(--danger); font-weight: 600; }

  /* Buttons */
  .btn { display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px; border-radius: 6px; border: none; cursor: pointer; font-size: 0.9rem; font-weight: 500; transition: background 0.15s; }
  .btn-primary { background: var(--accent); color: #fff; }
  .btn-primary:hover { background: var(--accent-hover); }
  .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
  .btn-ghost { background: transparent; color: var(--text); border: 1px solid var(--border); }
  .btn-ghost:hover { background: var(--border); }

  /* Login page */
  #login-page { display: flex; align-items: center; justify-content: center; min-height: 100dvh; background: var(--bg); padding: 24px; }
  .login-card { background: var(--surface); border: 1px solid var(--border); border-radius: 14px; padding: 40px; max-width: 380px; width: 100%; }
  .login-card h2 { font-size: 1.4rem; margin-bottom: 8px; }
  .login-card p { color: var(--text-muted); font-size: 0.9rem; margin-bottom: 28px; }
  .code-input { width: 100%; padding: 14px; font-size: 1.6rem; letter-spacing: 0.35em; text-align: center; text-transform: uppercase; font-weight: 700; font-family: monospace; border: 2px solid var(--border); border-radius: 8px; background: var(--bg); color: var(--text); outline: none; }
  .code-input:focus { border-color: var(--accent); }
  .error-msg { color: var(--danger); font-size: 0.85rem; margin-top: 10px; min-height: 1.2em; }
  .login-submit { width: 100%; margin-top: 20px; padding: 12px; font-size: 1rem; }

  /* Pagination */
  .pagination { display: flex; align-items: center; gap: 8px; justify-content: flex-end; margin-top: 16px; }
  .page-info { font-size: 0.85rem; color: var(--text-muted); }

  /* Status badge */
  .badge { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 0.75rem; font-weight: 600; }
  .badge-income { background: #e8f5e9; color: var(--success); }
  .badge-expense { background: #ffebee; color: var(--danger); }
  .badge-transfer { background: #e3f2fd; color: var(--accent); }
  @media (prefers-color-scheme: dark) {
    .badge-income { background: #1b5e20; }
    .badge-expense { background: #b71c1c; }
    .badge-transfer { background: #0d47a1; }
  }

  /* Hamburger for mobile */
  #hamburger { display: none; background: none; border: none; cursor: pointer; padding: 4px; color: var(--text); font-size: 1.4rem; }
  @media (max-width: 700px) {
    #hamburger { display: block; }
    #sidebar { position: fixed; top: 0; left: 0; height: 100%; z-index: 200; transform: translateX(-100%); transition: transform 0.25s; border-right: 1px solid var(--border); }
    #sidebar.open { transform: translateX(0); }
    #content { padding: 16px; }
    #overlay { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 199; }
    #overlay.visible { display: block; }
  }

  /* Loading spinner */
  .spinner { width: 32px; height: 32px; border: 3px solid var(--border); border-top-color: var(--accent); border-radius: 50%; animation: spin 0.7s linear infinite; margin: 40px auto; }
  @keyframes spin { to { transform: rotate(360deg); } }
  .loading-wrap { display: flex; justify-content: center; }

  /* Alert */
  .alert { padding: 12px 16px; border-radius: 8px; font-size: 0.88rem; margin-bottom: 16px; }
  .alert-warn { background: #fff3e0; color: var(--warning); border: 1px solid #ffe0b2; }
  @media (prefers-color-scheme: dark) { .alert-warn { background: #3e2000; border-color: #bf360c; } }

  .section-title { font-size: 1.25rem; font-weight: 700; margin-bottom: 20px; }
  .empty-state { text-align: center; color: var(--text-muted); padding: 60px 0; }
</style>
</head>
<body>

<div id="login-page" style="display:none;">
  <div class="login-card">
    <h2>OssWallet Web View</h2>
    <p>Enter the access code shown on your phone to continue.</p>
    <input class="code-input" id="code-input" type="text" maxlength="8" placeholder="XXXXXXXX" autocomplete="off" autocorrect="off" spellcheck="false">
    <div class="error-msg" id="login-error"></div>
    <button class="btn btn-primary login-submit" id="login-btn">Verify</button>
  </div>
</div>

<div id="app" style="display:none;">
  <div id="header">
    <button id="hamburger" aria-label="Menu">&#9776;</button>
    <h1>OssWallet</h1>
    <button class="btn btn-ghost" id="logout-btn" style="font-size:0.85rem; padding:6px 12px;">Log out</button>
  </div>
  <div id="main">
    <div id="overlay"></div>
    <nav id="sidebar">
      <a class="nav-item" href="#/accounts"><span class="nav-icon">&#128178;</span> Accounts</a>
      <a class="nav-item" href="#/transactions"><span class="nav-icon">&#8644;</span> Transactions</a>
      <a class="nav-item" href="#/summary"><span class="nav-icon">&#128200;</span> Summary</a>
    </nav>
    <main id="content">
      <div class="loading-wrap"><div class="spinner"></div></div>
    </main>
  </div>
</div>

<script>
(function() {
  'use strict';

  // --- State ---
  const baseUrl = window.location.origin; // same origin as the server
  let token = sessionStorage.getItem('oss_token') || '';
  let currentPage = 0;
  let currentAccountFilter = '';

  // --- DOM refs ---
  const loginPage = document.getElementById('login-page');
  const appPage   = document.getElementById('app');
  const content   = document.getElementById('content');
  const sidebar   = document.getElementById('sidebar');
  const overlay   = document.getElementById('overlay');

  // --- Boot ---
  if (token) {
    showApp();
    route();
  } else {
    showLogin();
  }

  // --- Login ---
  document.getElementById('code-input').addEventListener('keydown', e => {
    if (e.key === 'Enter') document.getElementById('login-btn').click();
  });
  document.getElementById('login-btn').addEventListener('click', doLogin);

  async function doLogin() {
    const btn   = document.getElementById('login-btn');
    const input = document.getElementById('code-input');
    const errEl = document.getElementById('login-error');
    const code  = input.value.trim().toUpperCase();
    if (code.length < 8) { errEl.textContent = 'Enter the full 8-character code.'; return; }
    btn.disabled = true;
    btn.textContent = 'Verifying…';
    errEl.textContent = '';
    try {
      const res  = await fetch(baseUrl + '/api/auth', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ accessCode: code })
      });
      const json = await res.json();
      if (res.status === 429) {
        errEl.textContent = json.error || 'Too many attempts. Wait 60 seconds.';
      } else if (json.success && json.data?.token) {
        token = json.data.token;
        sessionStorage.setItem('oss_token', token);
        showApp();
        navigate('#/accounts');
      } else {
        errEl.textContent = 'Incorrect code. Try again.';
      }
    } catch {
      errEl.textContent = 'Connection error. Is the server still running?';
    } finally {
      btn.disabled = false;
      btn.textContent = 'Verify';
    }
  }

  // --- Logout ---
  document.getElementById('logout-btn').addEventListener('click', () => {
    token = '';
    sessionStorage.removeItem('oss_token');
    showLogin();
    document.getElementById('code-input').value = '';
    document.getElementById('login-error').textContent = '';
  });

  // --- Mobile sidebar ---
  document.getElementById('hamburger').addEventListener('click', () => {
    sidebar.classList.toggle('open');
    overlay.classList.toggle('visible');
  });
  overlay.addEventListener('click', closeSidebar);
  function closeSidebar() {
    sidebar.classList.remove('open');
    overlay.classList.remove('visible');
  }

  // --- Routing ---
  window.addEventListener('hashchange', route);
  function route() {
    const hash = location.hash || '#/accounts';
    updateActiveNav(hash);
    closeSidebar();
    if (hash.startsWith('#/accounts'))     renderAccounts();
    else if (hash.startsWith('#/transactions')) renderTransactions();
    else if (hash.startsWith('#/summary'))  renderSummary();
    else renderAccounts();
  }

  function navigate(hash) { location.hash = hash; }

  function updateActiveNav(hash) {
    document.querySelectorAll('.nav-item').forEach(el => {
      el.classList.toggle('active', hash.startsWith(el.getAttribute('href')));
    });
  }

  // --- API helper ---
  async function apiFetch(path) {
    const res = await fetch(baseUrl + path, {
      headers: { 'Authorization': 'Bearer ' + token }
    });
    if (res.status === 401) {
      token = '';
      sessionStorage.removeItem('oss_token');
      showLogin();
      throw new Error('Session expired');
    }
    return res.json();
  }

  // --- Render helpers ---
  function showLogin() {
    loginPage.style.display = 'flex';
    appPage.style.display   = 'none';
    setTimeout(() => document.getElementById('code-input').focus(), 100);
  }
  function showApp() {
    loginPage.style.display = 'none';
    appPage.style.display   = 'flex';
  }
  function setContent(html) { content.innerHTML = html; }
  function showSpinner() { setContent('<div class="loading-wrap"><div class="spinner"></div></div>'); }

  function fmt(amount, currency) {
    try {
      return new Intl.NumberFormat(navigator.language, { style: 'currency', currency, minimumFractionDigits: 2 }).format(amount);
    } catch { return amount.toFixed(2) + ' ' + currency; }
  }
  function fmtDate(ts) {
    return new Date(ts).toLocaleDateString(navigator.language, { year: 'numeric', month: 'short', day: 'numeric' });
  }

  // --- Accounts page ---
  async function renderAccounts() {
    showSpinner();
    try {
      const json = await apiFetch('/api/accounts');
      if (!json.success) { setContent('<p class="empty-state">Failed to load accounts.</p>'); return; }
      const accounts = json.data || [];
      if (accounts.length === 0) { setContent('<p class="empty-state">No accounts found.</p>'); return; }
      let html = '<h2 class="section-title">Accounts</h2><div class="card-grid">';
      for (const acc of accounts) {
        const balClass = acc.balance >= 0 ? 'positive' : 'negative';
        html += `<div class="stat-card">
          <div class="stat-label">${esc(acc.type)}</div>
          <div style="font-weight:600; margin-bottom:8px;">${esc(acc.name)}</div>
          <div class="stat-value ${balClass}">${fmt(acc.balance, acc.currency)}</div>
          <div style="margin-top:12px;">
            <a class="btn btn-ghost" style="font-size:0.8rem; padding:4px 10px;" href="#/transactions?accountId=${esc(acc.id)}&accountName=${encodeURIComponent(acc.name)}">View transactions</a>
          </div>
        </div>`;
      }
      html += '</div>';
      setContent(html);
    } catch (e) {
      if (e.message !== 'Session expired') setContent('<p class="empty-state">Error loading accounts.</p>');
    }
  }

  // --- Transactions page ---
  async function renderTransactions() {
    const params  = new URLSearchParams(location.hash.split('?')[1] || '');
    const accountId   = params.get('accountId') || '';
    const accountName = params.get('accountName') ? decodeURIComponent(params.get('accountName')) : '';
    const page    = parseInt(params.get('page') || '0', 10);

    showSpinner();
    try {
      let url = `/api/transactions?page=${page}&limit=25`;
      if (accountId) url += `&accountId=${encodeURIComponent(accountId)}`;
      const json = await apiFetch(url);
      if (!json.success) { setContent('<p class="empty-state">Failed to load transactions.</p>'); return; }
      const data = json.data || { items: [], total: 0, page: 0, limit: 25, hasMore: false };

      let title = accountName ? `Transactions — ${esc(accountName)}` : 'Transactions';
      let html = `<h2 class="section-title">${title}</h2>`;

      if (data.items.length === 0) {
        html += '<p class="empty-state">No transactions found.</p>';
      } else {
        html += `<div class="card"><div class="table-wrap"><table>
          <thead><tr><th>Date</th><th>Title</th><th>Category</th><th>Account</th><th>Type</th><th style="text-align:right">Amount</th></tr></thead><tbody>`;
        for (const tx of data.items) {
          const amtClass = tx.type === 'INCOME' ? 'amount-income' : tx.type === 'EXPENSE' ? 'amount-expense' : '';
          const sign = tx.type === 'EXPENSE' ? '−' : tx.type === 'INCOME' ? '+' : '';
          const badgeCls = tx.type === 'INCOME' ? 'badge-income' : tx.type === 'EXPENSE' ? 'badge-expense' : 'badge-transfer';
          html += `<tr>
            <td style="white-space:nowrap">${fmtDate(tx.date)}</td>
            <td>${esc(tx.title)}${tx.note ? `<br><small style="color:var(--text-muted)">${esc(tx.note)}</small>` : ''}</td>
            <td>${tx.categoryName ? esc(tx.categoryName) : '<span style="color:var(--text-muted)">—</span>'}</td>
            <td>${esc(tx.accountName)}</td>
            <td><span class="badge ${badgeCls}">${esc(tx.type)}</span></td>
            <td style="text-align:right" class="${amtClass}">${sign}${fmt(tx.amount, tx.currency)}</td>
          </tr>`;
        }
        html += '</tbody></table></div>';
        // Pagination
        const totalPages = Math.ceil(data.total / data.limit);
        html += `<div class="pagination">
          <span class="page-info">Page ${page + 1} of ${totalPages} (${data.total} total)</span>
          ${page > 0 ? `<button class="btn btn-ghost" onclick="navPage(${page - 1})">&#8592; Prev</button>` : ''}
          ${data.hasMore ? `<button class="btn btn-primary" onclick="navPage(${page + 1})">Next &#8594;</button>` : ''}
        </div>`;
        html += '</div>';
      }
      setContent(html);
    } catch (e) {
      if (e.message !== 'Session expired') setContent('<p class="empty-state">Error loading transactions.</p>');
    }
  }

  window.navPage = function(page) {
    const params = new URLSearchParams(location.hash.split('?')[1] || '');
    params.set('page', page);
    const base = '#/transactions';
    location.hash = base + '?' + params.toString();
  };

  // --- Summary page ---
  async function renderSummary() {
    showSpinner();
    try {
      const json = await apiFetch('/api/summary');
      if (!json.success) { setContent('<p class="empty-state">Failed to load summary.</p>'); return; }
      const d = json.data;
      const netClass = d.netWorth >= 0 ? 'positive' : 'negative';
      const cashFlow = d.monthlyIncome - d.monthlyExpenses;
      const cashClass = cashFlow >= 0 ? 'positive' : 'negative';
      const html = `
        <h2 class="section-title">Summary — ${esc(d.month)}</h2>
        <div class="card-grid">
          <div class="stat-card"><div class="stat-label">Net Worth</div><div class="stat-value ${netClass}">${fmtRaw(d.netWorth)}</div></div>
          <div class="stat-card"><div class="stat-label">Income this month</div><div class="stat-value positive">+${fmtRaw(d.monthlyIncome)}</div></div>
          <div class="stat-card"><div class="stat-label">Expenses this month</div><div class="stat-value negative">−${fmtRaw(d.monthlyExpenses)}</div></div>
          <div class="stat-card"><div class="stat-label">Cash flow</div><div class="stat-value ${cashClass}">${cashFlow >= 0 ? '+' : '−'}${fmtRaw(Math.abs(cashFlow))}</div></div>
        </div>
        <div class="alert alert-warn">Amounts are shown in each account's native currency. Multi-currency net worth may not reflect real-time exchange rates.</div>
      `;
      setContent(html);
    } catch (e) {
      if (e.message !== 'Session expired') setContent('<p class="empty-state">Error loading summary.</p>');
    }
  }

  function fmtRaw(n) { return n.toLocaleString(navigator.language, { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }

  // XSS prevention
  function esc(str) {
    if (str == null) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

})();
</script>
</body>
</html>
```

Key implementation decisions:
- All dynamic text inserted into the DOM goes through the `esc()` function to prevent XSS.
- Token is stored in `sessionStorage` (not `localStorage`) so it clears on tab close.
- Any 401 response triggers immediate logout and redirect to login.
- The `baseUrl` is `window.location.origin` — same origin, no hardcoded IP needed.
- Dark/light mode via `prefers-color-scheme` CSS media query; no JavaScript toggle needed.
- The `navPage` function is the only globally-exposed function (necessary for inline `onclick` in table rows); everything else is inside an IIFE.

---

## 11. AppContainer Wiring

Add these fields to `app/src/main/java/com/osscabinet/ossWallet/di/AppContainer.kt`.

### New imports to add

```kotlin
import com.osscabinet.ossWallet.data.web.WebServerManager
import com.osscabinet.ossWallet.data.web.WebServerPreferences
import com.osscabinet.ossWallet.domain.repository.WebServerRepository
```

### New fields to add (append after the existing `transactionManager` field)

```kotlin
// Web server — Open in Web feature
val webServerPreferences: WebServerPreferences by lazy {
    WebServerPreferences(context)
}

val webServerManager: WebServerRepository by lazy {
    WebServerManager(
        context = context,
        transactionRepository = transactionRepository,
        accountRepository = accountRepository,
        settingsRepository = settingsRepository
    )
}
```

Construction order is implicit via `lazy`: `transactionRepository`, `accountRepository`, and `settingsRepository` are all already-defined lazy fields, so Kotlin will initialize them before `webServerManager` if they have not been initialized yet. No explicit ordering is needed.

### ViewModel factory addition

The ViewModel factory for `OpenInWebViewModel` does not require changes to `AppContainer` itself — it is created inline at the call site in `OssWalletNavGraph.kt`:

```kotlin
viewModel(
    factory = GenericViewModelFactory {
        OpenInWebViewModel(
            webServerRepository = (context.applicationContext as OssWalletApp).container.webServerManager,
            webServerPreferences = (context.applicationContext as OssWalletApp).container.webServerPreferences,
            settingsRepository = (context.applicationContext as OssWalletApp).container.settingsRepository
        )
    }
)
```

---

## 12. ViewModel Layer

### `ui/screens/openinweb/OpenInWebViewModel.kt`

```kotlin
package com.osscabinet.ossWallet.ui.screens.openinweb

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.lifecycle.viewModelScope
import com.osscabinet.ossWallet.data.web.NetworkUtils
import com.osscabinet.ossWallet.data.web.QRCodeGenerator
import com.osscabinet.ossWallet.data.web.WebServerPreferences
import com.osscabinet.ossWallet.domain.model.web.WebServerState
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.WebServerRepository
import com.osscabinet.ossWallet.service.WebServerService
import com.osscabinet.ossWallet.ui.util.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OpenInWebUiState(
    val serverState: WebServerState = WebServerState.Stopped,
    val isEnabled: Boolean = false,
    val qrBitmap: Bitmap? = null,
    val isWifiConnected: Boolean = true
)

class OpenInWebViewModel(
    private val webServerRepository: WebServerRepository,
    private val webServerPreferences: WebServerPreferences,
    settingsRepository: SettingsRepository,
    private val context: Context
) : BaseViewModel(settingsRepository) {

    private val _uiState = MutableStateFlow(OpenInWebUiState())
    val uiState: StateFlow<OpenInWebUiState> = _uiState.asStateFlow()

    val isEnabled: StateFlow<Boolean> = webServerPreferences.isEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            webServerRepository.serverState.collect { state ->
                val qr = if (state is WebServerState.Running) {
                    runCatching { QRCodeGenerator.generate(state.accessUrl, 512) }.getOrNull()
                } else null
                _uiState.value = _uiState.value.copy(
                    serverState = state,
                    qrBitmap = qr
                )
            }
        }
        refreshWifiStatus()
    }

    fun toggleServer(enabled: Boolean) {
        viewModelScope.launch {
            webServerPreferences.setEnabled(enabled)
            if (enabled) {
                refreshWifiStatus()
                if (!NetworkUtils.isConnectedToWifi(context)) {
                    _uiState.value = _uiState.value.copy(isWifiConnected = false)
                    return@launch
                }
            }
            // The foreground Service owns the server lifecycle — the ViewModel never
            // calls webServerRepository.start()/stop() directly. The Service calls
            // those methods, ensuring the server survives app backgrounding.
            val serviceIntent = Intent(context, WebServerService::class.java).apply {
                action = if (enabled) WebServerService.ACTION_START else WebServerService.ACTION_STOP
            }
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    fun refreshWifiStatus() {
        _uiState.value = _uiState.value.copy(
            isWifiConnected = NetworkUtils.isConnectedToWifi(context)
        )
    }
}
```

### `ui/screens/openinweb/OpenInWebViewModelFactory.kt`

```kotlin
package com.osscabinet.ossWallet.ui.screens.openinweb

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.osscabinet.ossWallet.data.web.WebServerPreferences
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.domain.repository.WebServerRepository

class OpenInWebViewModelFactory(
    private val webServerRepository: WebServerRepository,
    private val webServerPreferences: WebServerPreferences,
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OpenInWebViewModel(
            webServerRepository = webServerRepository,
            webServerPreferences = webServerPreferences,
            settingsRepository = settingsRepository,
            context = context.applicationContext
        ) as T
    }
}
```

---

## 13. Compose Screen

### `ui/screens/openinweb/OpenInWebScreen.kt`

```kotlin
package com.osscabinet.ossWallet.ui.screens.openinweb

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.osscabinet.ossWallet.domain.model.web.WebServerState

@Composable
fun OpenInWebScreen(
    viewModel: OpenInWebViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEnabled by viewModel.isEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // WiFi warning
        if (!uiState.isWifiConnected) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        "Not connected to WiFi. Connect to a WiFi network to enable web access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Toggle card
        Card {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Open in Web", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    val stateLabel = when (val s = uiState.serverState) {
                        is WebServerState.Stopped -> "Disabled"
                        is WebServerState.Starting -> "Starting..."
                        is WebServerState.Running -> "Running on port ${s.port}"
                        is WebServerState.Error -> "Error: ${s.message}"
                    }
                    Text(
                        stateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (uiState.serverState) {
                            is WebServerState.Running -> MaterialTheme.colorScheme.primary
                            is WebServerState.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { viewModel.toggleServer(it) },
                    enabled = uiState.isWifiConnected || isEnabled
                )
            }
        }

        // When running — show QR + access code + URL
        val runningState = uiState.serverState as? WebServerState.Running
        if (runningState != null) {
            // Access code card
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Access Code",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = runningState.accessCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 32.sp,
                        letterSpacing = 6.sp,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Type this code in your browser after opening the URL below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // QR code card
            uiState.qrBitmap?.let { bitmap ->
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Scan QR Code",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR code for ${runningState.accessUrl}",
                            modifier = Modifier
                                .size(240.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }
            }

            // URL card with copy button
            Card {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Access URL",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = runningState.accessUrl,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText("OssWallet URL", runningState.accessUrl)
                                )
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL")
                        }
                    }
                }
            }

            // Instructions card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "How to connect",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "1. Make sure your laptop/desktop is on the same WiFi network as this phone.\n" +
                        "2. Open the URL above in a browser, or scan the QR code.\n" +
                        "3. Enter the access code shown above.\n" +
                        "4. The web view is read-only. The server stops when you turn off the toggle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Starting state
        if (uiState.serverState is WebServerState.Starting) {
            Card {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // Error state
        val errorState = uiState.serverState as? WebServerState.Error
        if (errorState != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Failed to start server",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        errorState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
```

Key design decisions:
- Uses `MaterialTheme.colorScheme.*` throughout — no hardcoded hex values.
- Access code is displayed in `FontFamily.Monospace` at 32sp with letter spacing for readability.
- QR code bitmap is rendered via `Image(bitmap = bitmap.asImageBitmap(), ...)` — no Coil needed for in-memory bitmaps.
- WiFi-not-connected warning uses `MaterialTheme.colorScheme.errorContainer` — consistent with the Aeon design system.
- All state collected with `collectAsStateWithLifecycle()` per project convention.

---

## 14. Foreground Service

### `service/WebServerService.kt`

File location: `app/src/main/java/com/osscabinet/ossWallet/service/WebServerService.kt`

```kotlin
package com.osscabinet.ossWallet.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.osscabinet.ossWallet.MainActivity
import com.osscabinet.ossWallet.OssWalletApp
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.domain.model.web.WebServerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WebServerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Injected manually from AppContainer — no Hilt
    private val webServerManager by lazy {
        (application as OssWalletApp).container.webServerManager
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Start in foreground immediately with a "starting" notification
        startForeground(NOTIFICATION_ID, buildNotification("Starting web server..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startWebServer()
            ACTION_STOP -> stopWebServer()
        }
        return START_STICKY
    }

    private fun startWebServer() {
        serviceScope.launch {
            webServerManager.start()
            // Observe running state to update notification with the access URL
            webServerManager.serverState.collectLatest { state ->
                val notificationText = when (state) {
                    is WebServerState.Running -> "Active at ${state.accessUrl}"
                    is WebServerState.Starting -> "Starting web server..."
                    is WebServerState.Error -> "Error: ${state.message}"
                    is WebServerState.Stopped -> {
                        stopSelf()
                        return@collectLatest
                    }
                }
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIFICATION_ID, buildNotification(notificationText))
            }
        }
    }

    private fun stopWebServer() {
        serviceScope.launch {
            webServerManager.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildNotification(text: String): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainPi = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, WebServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OssWallet Web Access")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // use existing launcher icon
            .setContentIntent(mainPi)
            .addAction(0, "Stop", stopPi)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Web Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shown while web access is active"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.osscabinet.ossWallet.WEB_SERVER_START"
        const val ACTION_STOP  = "com.osscabinet.ossWallet.WEB_SERVER_STOP"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "oss_web_server"
    }
}
```

The ViewModel controls the server exclusively by starting/stopping `WebServerService`. The Service is the sole caller of `webServerRepository.start()` and `webServerRepository.stop()`. The ViewModel observes `webServerRepository.serverState` for UI updates. This separation ensures the server survives app backgrounding without a double-start race condition.

Add to `OssWalletApp` to restore server state on process restart if the user had it enabled:

```kotlin
// In OssWalletApp — add this field alongside other app-level infrastructure:
private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

// In OssWalletApp.onCreate(), after container initialization:
appScope.launch {
    container.webServerPreferences.isEnabled.first().let { wasEnabled ->
        if (wasEnabled) {
            val intent = Intent(this@OssWalletApp, WebServerService::class.java).apply {
                action = WebServerService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
}
```

Required imports for `OssWalletApp`: `import android.content.Intent`, `import android.os.Build`, `import kotlinx.coroutines.CoroutineScope`, `import kotlinx.coroutines.Dispatchers`, `import kotlinx.coroutines.SupervisorJob`, `import kotlinx.coroutines.flow.first`, `import kotlinx.coroutines.launch`.

`appScope` uses `SupervisorJob()` so a failure in one child coroutine does not cancel the scope. The raw `CoroutineScope(Dispatchers.Main)` pattern (without a `Job`) creates an unsupervised scope that cannot be cancelled and will leak if the launch throws.

---

## 15. Navigation Integration

### 15.1 New route in `ui/navigation/Screen.kt`

Add this object to the `sealed class Screen` body:

```kotlin
object OpenInWeb : Screen("open_in_web")
```

### 15.2 Wiring in `OssWalletNavGraph.kt`

Add the import at the top of `OssWalletNavGraph.kt`:

```kotlin
import com.osscabinet.ossWallet.ui.screens.openinweb.OpenInWebScreen
import com.osscabinet.ossWallet.ui.screens.openinweb.OpenInWebViewModelFactory
```

Add the composable destination inside the `NavHost` block, alongside the other route declarations:

```kotlin
composable(Screen.OpenInWeb.route) {
    val context = LocalContext.current
    val vm: OpenInWebViewModel = viewModel(
        factory = OpenInWebViewModelFactory(
            webServerRepository = (context.applicationContext as OssWalletApp).container.webServerManager,
            webServerPreferences = (context.applicationContext as OssWalletApp).container.webServerPreferences,
            settingsRepository = (context.applicationContext as OssWalletApp).container.settingsRepository,
            context = context
        )
    )
    OpenInWebScreen(
        viewModel = vm,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

### 15.3 Entry point in Settings screen

"Open in Web" belongs in the Settings screen as a navigation entry, since it is a device-level configuration option, not a financial data screen. In `SettingsScreen.kt`, add a `ListItem` or equivalent Aeon navigation item that calls:

```kotlin
navController.navigate(Screen.OpenInWeb.route)
```

The exact UI placement depends on the current Settings screen layout, but it should appear in a "Connectivity" or "Advanced" section below the existing settings entries. Use the existing `ListItem`/row pattern already in `SettingsScreen.kt` for consistency.

---

## 16. R8 / ProGuard Rules

Add these rules to `/Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/proguard-rules.pro`:

```proguard
# ============================================================
# Ktor Server (CIO engine + plugins)
# ============================================================
-keep class io.ktor.** { *; }
-keepnames class io.ktor.** { *; }
-dontwarn io.ktor.**

# Ktor relies on kotlinx.coroutines internals via reflection
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Ktor uses SLF4J for logging
-dontwarn org.slf4j.**

# Ktor CIO engine uses Java NIO and networking internals
-dontwarn java.nio.**
-dontwarn sun.misc.**
-dontwarn sun.nio.**

# ============================================================
# kotlinx.serialization (already in project, but be explicit
# for Ktor's ContentNegotiation serialization)
# ============================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all @Serializable classes in the web DTO package
-keep @kotlinx.serialization.Serializable class com.osscabinet.ossWallet.data.web.dto.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================================
# QRCode Kotlin Android
# ============================================================
-keep class qrcode.** { *; }
-dontwarn qrcode.**

# ============================================================
# Restore stack trace line numbers for debugging release crashes
# ============================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
```

---

## 17. AndroidManifest.xml Changes

The complete diff for `app/src/main/AndroidManifest.xml`:

### Permissions to add (alongside existing permissions)

```xml
<!-- New — required for foreground service: -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
```

`INTERNET` and `ACCESS_NETWORK_STATE` are already present. `POST_NOTIFICATIONS` is already present.

### Service declaration to add (inside `<application>`)

```xml
<service
    android:name=".service.WebServerService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="dataSync" />
```

`android:exported="false"` is correct — no external app needs to start this service. The foreground service type `dataSync` matches `FOREGROUND_SERVICE_DATA_SYNC`.

### Network security config

No changes needed. The Ktor server serves over plain HTTP on the local network, which is permitted by default on Android for local IP addresses (not public hosts). The existing network security config (`@xml/data_extraction_rules`, `@xml/backup_rules`) does not restrict outbound or inbound local connections.

If `android:usesCleartextTraffic` is set to `false` somewhere in the project, add this to `res/xml/network_security_config.xml` (create if it doesn't exist, and reference it from `<application android:networkSecurityConfig="@xml/network_security_config">`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Allow cleartext traffic on local WiFi for the embedded HTTP server -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">192.168.0.0</domain>
        <!-- Note: Android network security config does not support CIDR ranges.
             Cleartext is allowed by default for private IP ranges if
             android:usesCleartextTraffic is not set to false app-wide. -->
    </domain-config>
    <base-config cleartextTrafficPermitted="false" />
</network-security-config>
```

Since the app already uses Retrofit over HTTPS for exchange rate APIs, and the existing manifest does not appear to set `usesCleartextTraffic="false"`, this extra step is likely not needed. Verify before shipping.

---

## 18. Testing Plan

### 18.1 Unit Tests

**`AuthSessionManagerTest`** — `test/java/.../data/web/AuthSessionManagerTest.kt`

Tests to write using JUnit4 + no mocks (pure Kotlin, no Android dependencies):
- `authenticate_withCorrectCode_returnsToken`
- `authenticate_withWrongCode_returnsNull`
- `authenticate_afterFiveFailures_throwsRateLimitedException`
- `authenticate_afterLockoutExpiry_succeedsAgain` (use `SystemClock` mock or parameterize with a delay)
- `validateToken_withValidToken_returnsTrue`
- `validateToken_withExpiredSession_returnsFalse`
- `validateToken_withUnknownToken_returnsFalse`
- `purgeExpiredSessions_removesExpiredSessions`
- `invalidateSession_causesValidateToReturnFalse`

```kotlin
class AuthSessionManagerTest {
    private lateinit var manager: AuthSessionManager

    @Before
    fun setup() {
        manager = AuthSessionManager()
        manager.initialize()
    }

    @Test
    fun `authenticate with correct code returns token`() {
        val code = manager.getAccessCode()
        val token = manager.authenticate(code, "192.168.1.100")
        assertNotNull(token)
        assertTrue(token!!.isNotBlank())
    }

    @Test
    fun `authenticate with wrong code returns null`() {
        val token = manager.authenticate("WRONGCOD", "192.168.1.101")
        assertNull(token)
    }

    @Test(expected = AuthSessionManager.RateLimitedException::class)
    fun `authenticate after five failures throws RateLimitedException`() {
        repeat(5) {
            manager.authenticate("WRONGCOD", "192.168.1.102")
        }
    }

    @Test
    fun `validateToken with valid token refreshes and returns true`() {
        val code = manager.getAccessCode()
        val token = manager.authenticate(code, "192.168.1.103")!!
        assertTrue(manager.validateToken(token))
    }

    @Test
    fun `validateToken with unknown token returns false`() {
        assertFalse(manager.validateToken("unknown-token"))
    }
}
```

**`NetworkUtilsTest`** — testing `getWifiInterfaceIp` requires Android context. Skip unit test; cover in manual device test. `isConnectedToWifi` can be tested with a Robolectric test or skipped in favor of manual testing.

**`WebServerManagerTest`** — integration character; see Section 18.3.

### 18.2 What to mock vs what to use real

| Component | Approach |
|---|---|
| `AuthSessionManager` | Real implementation — pure Kotlin, no mocks needed |
| `TransactionRepository` | Fake implementation (`FakeTransactionRepository`) returning canned data |
| `AccountRepository` | Fake implementation |
| `SettingsRepository` | Fake implementation returning fixed `currentProfileId = 1` |
| `NetworkUtils` | Manual device test only (requires WiFi hardware) |
| `QRCodeGenerator` | Smoke test only (call `generate()`, assert bitmap not null) |
| Ktor engine | Start real engine in integration test on loopback (127.0.0.1) |

### 18.3 Integration Test — Server Lifecycle

`androidTest/java/.../WebServerIntegrationTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class WebServerIntegrationTest {
    @Test
    fun server_starts_and_health_endpoint_returns_ok() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = WebServerManager(
            context = context,
            transactionRepository = FakeTransactionRepository(),
            accountRepository = FakeAccountRepository(),
            settingsRepository = FakeSettingsRepository()
        )
        manager.start()
        delay(500) // give the server time to bind
        assertTrue(manager.isRunning())

        val url = manager.getAccessUrl()
        assertNotNull(url)

        val client = OkHttpClient()
        val response = client.newCall(
            Request.Builder().url("$url/api/health").build()
        ).execute()
        assertEquals(200, response.code)
        val body = response.body?.string()
        assertTrue(body?.contains("\"success\":true") == true)

        manager.stop()
        assertFalse(manager.isRunning())
    }
}
```

Note: this test requires a connected device or emulator. It cannot run in a pure unit test environment because Ktor CIO opens a real socket.

### 18.4 Manual Test Checklist (real device, real WiFi)

- [ ] Toggle server on — server status changes to Running
- [ ] WiFi IP shown in UI matches `ip addr` or `ifconfig` output on the phone
- [ ] QR code is scannable with a phone camera and opens the correct URL
- [ ] Laptop browser opens URL and shows login page
- [ ] Typing wrong access code shows error
- [ ] Typing wrong code 5 times shows "too many attempts" message
- [ ] Correct access code grants access; dashboard pages load
- [ ] Accounts page lists all accounts with correct balances
- [ ] Transactions page paginates correctly (next/prev buttons work)
- [ ] Summary page shows correct net worth and month figures
- [ ] Closing the browser tab invalidates session (sessionStorage cleared)
- [ ] Reopening the tab shows the login page again
- [ ] Toggle server off — browser gets 401/connection refused on next request
- [ ] Kill the app — foreground service notification remains; server still accessible
- [ ] Tap Stop in notification — server stops, notification dismissed
- [ ] Re-open app after kill with "enabled" preference true — server auto-restarts
- [ ] Disconnect WiFi while server is running — UI shows WiFi warning; existing requests fail gracefully
- [ ] Release build: confirm no stack traces in API error responses

---

## 19. Phase Implementation Order

Tasks are listed in dependency order. A developer should complete them in this sequence. Hours are estimates for a single developer already familiar with the codebase.

| # | Task | Est. Hours | Acceptance Criteria |
|---|---|---|---|
| 1 | Add Ktor + QRCode dependencies to `libs.versions.toml` and `app/build.gradle.kts`. Sync Gradle. | 0.5h | Project builds without errors after sync. |
| 2 | Create `domain/model/web/WebServerState.kt` and `WebAccessSession.kt` (pure Kotlin models). | 0.5h | Files compile; no Android imports. |
| 3 | Create `domain/repository/WebServerRepository.kt` interface. | 0.5h | Interface compiles; used as the contract for all callers. |
| 4 | Create `data/web/NetworkUtils.kt`. | 1h | `getWifiInterfaceIp()` returns non-null on WiFi, null on mobile data in manual device test. |
| 5 | Create `data/web/AuthSessionManager.kt`. | 2h | All unit tests in Section 18.1 pass. |
| 6 | Create DTO classes in `data/web/dto/`. | 0.5h | All classes annotated `@Serializable`; compile without errors. |
| 7 | Create `data/web/QRCodeGenerator.kt`. | 0.5h | `generate("http://192.168.1.1:8192")` returns non-null `Bitmap` in a unit/smoke test. |
| 8 | Create `data/web/WebServerPreferences.kt`. | 0.5h | Write and read `isEnabled` preference in a simple test. |
| 9 | Create `app/src/main/assets/web/index.html`. | 3h | Open in Chrome desktop; login flow works; all three pages render; dark mode works. |
| 10 | Create `data/web/WebApiRoutes.kt`. | 3h | Routes compile; auth check helper works; DTO serialization correct. |
| 11 | Create `data/web/WebServerManager.kt`. | 3h | Server starts on WiFi IP, health endpoint returns 200, stop cleans up correctly. |
| 12 | Wire `WebServerManager` and `WebServerPreferences` into `AppContainer.kt`. | 0.5h | App builds and launches without crash. |
| 13 | Create `service/WebServerService.kt`. | 2h | Foreground notification appears when server starts; Stop action works; service survives app background. |
| 14 | Add service to `AndroidManifest.xml`; add new permissions. | 0.5h | Manifest compiles; lint passes. |
| 15 | Create `ui/screens/openinweb/OpenInWebViewModel.kt` and `OpenInWebViewModelFactory.kt`. | 2h | ViewModel exposes correct `uiState` when server starts/stops; `toggleServer()` triggers service start. |
| 16 | Create `ui/screens/openinweb/OpenInWebScreen.kt`. | 3h | Screen renders all states (stopped, starting, running, error, WiFi warning) correctly. |
| 17 | Add `Screen.OpenInWeb` to `Screen.kt` and wire route in `OssWalletNavGraph.kt`. | 0.5h | Navigating to `open_in_web` route shows the screen. |
| 18 | Add navigation entry to Settings screen. | 0.5h | Tapping the entry navigates to Open in Web screen. |
| 19 | Add R8 keep rules to `proguard-rules.pro`. | 0.5h | Release build starts server and serves API without R8 stripping Ktor classes. |
| 20 | Run full manual test checklist (Section 18.4) on a real device. | 2h | All checklist items pass. |
| 21 | Write unit tests for `AuthSessionManager` (Section 18.1). | 1.5h | All tests pass with `./gradlew testDebugUnitTest`. |
| 22 | Write integration test (Section 18.3). | 1.5h | Test passes with `./gradlew connectedAndroidTest`. |

**Total estimated effort:** approximately 27 hours for a single developer.

---

## 20. Known Limitations and Future Work

### 20.1 No HTTPS (MVP)

**Problem**: Android does not provide a user-friendly path to HTTPS for local LAN servers. Options:
- Self-signed certificate: All modern browsers show a hard error ("Your connection is not private") that cannot be bypassed without installing the certificate as a trusted CA — which requires device admin rights or manual steps that most users cannot perform.
- Let's Encrypt: Requires a publicly resolvable domain name; LAN IP addresses are not eligible.
- Local CA install: Requires MDM or a setup step that is unacceptable for a consumer app.

**Mitigation in MVP**: The server binds to the WiFi interface IP only, limiting exposure to the local network segment. The session token and access code provide authentication. Traffic is plaintext to local network sniffers — acceptable for a home/private office WiFi where the threat model is low.

**Future path**: Android 14 introduced APIs to detect VPN transport. A future version could route the server via an in-process TLS proxy using Conscrypt. Watch [issue tracker for ktor/ktor#1972](https://github.com/ktorio/ktor/issues/1972) for embedded TLS on Android progress.

### 20.2 Single Session Only

The MVP supports exactly one active browser session at a time. Authenticating from a second browser invalidates the first session's token (because `AuthSessionManager.authenticate()` replaces `activeSession`).

**Future path**: Replace the single `MutableSession?` with a `ConcurrentHashMap<String, MutableSession>` keyed by token, capped at N active sessions (e.g., 3).

### 20.3 Polling Only — No Real-Time Updates

The web UI does not update automatically when data changes in the app. Users must manually refresh the page.

**Future path**: Add a Ktor WebSocket endpoint (`GET /ws`). The server sends a `{"type":"invalidate"}` message when `WidgetInvalidationObserver` fires (the existing observer already reacts to Room `InvalidationTracker`). The browser reconnects and re-fetches on receiving the invalidation signal.

### 20.4 No mDNS / Bonjour Discovery

Users must type the IP address manually if they do not scan the QR code. IP addresses can change between WiFi reconnections.

**Future path**: Use Android's `android.net.nsd.NsdManager` (Network Service Discovery, which uses mDNS under the hood) to advertise the service as `_ossWallet._tcp.local`. Desktop browsers with mDNS support (or companion browser extension) could then discover the service by name. Requires adding `ACCESS_WIFI_STATE` and careful handling of `NsdManager` API level differences.

### 20.5 Read-Only Access

The web UI cannot create, edit, or delete transactions or accounts.

**Future path**: After the read-only MVP is stable and security-reviewed, add write endpoints behind the same Bearer token auth. Writes should be idempotent and validated server-side. Consider a CSRF token for mutation requests even over LAN.

### 20.6 Multi-Currency Net Worth

The `/api/summary` endpoint sums balances across all accounts regardless of currency. For multi-currency users, this sum is mathematically meaningless without a base currency conversion.

**Future path**: Use the existing `CurrencyConversionService` in the `data/` layer to convert all balances to the user's base currency before summing. Pass the result with the base currency code in the response DTO.

### 20.7 Port Conflicts

The server always tries port 8192. If another app on the device is using port 8192, the Ktor `embeddedServer` call will throw a `BindException`. The `WebServerManager.start()` catch block will catch this and transition to `WebServerState.Error`.

**Future path**: Implement automatic port selection — try 8192, 8193, 8194 in sequence, use the first available. Store the chosen port in `WebServerState.Running` and display it in the QR code URL.
