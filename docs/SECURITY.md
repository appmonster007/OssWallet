# Security Subsystem — OssWallet

OssWallet is built on a "Privacy First" foundation. This document details the cryptographic and structural security measures implemented in the codebase.

## 1. Database Encryption (SQLCipher)

The entire Room database (`osswallet_manager_db`) is encrypted using **SQLCipher** (AES-256).

### 1.1 Passphrase Management
The `SecurePassphraseManager` is responsible for the lifecycle of the database encryption key.
- **Generation**: Uses `java.security.SecureRandom` to generate a 256-bit (32-byte) cryptographically secure random key.
- **Storage**: The key is Base64 encoded and stored in `secure_prefs` (Private SharedPreferences).
- **Injection**: The key is injected into the `SupportOpenHelperFactory` during database initialization in `DatabaseModule`.

### 1.2 Key Code Snippet
```kotlin
fun getOrGeneratePassphrase(): ByteArray {
    val storedPassphrase = sharedPreferences.getString(PASSPHRASE_KEY, null)
    return if (storedPassphrase != null) {
        Base64.decode(storedPassphrase, Base64.NO_WRAP)
    } else {
        val newPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        sharedPreferences.edit().putString(PASSPHRASE_KEY, Base64.encodeToString(newPassphrase, Base64.NO_WRAP)).apply()
        newPassphrase
    }
}
```

---

## 2. Profile Isolation (ARC-19)

OssWallet supports multiple profiles. To prevent data leakage, we implement strict isolation at both the query and cache levels.

### 2.1 DAO Scoping
All persistence operations **must** include a `profileId`. 
```sql
@Query("SELECT * FROM transactions WHERE profileId = :profileId ORDER BY timestamp DESC")
fun getTransactions(profileId: Int): Flow<List<TransactionEntity>>
```

### 2.2 Cache Invalidation
The `OssWalletApp` monitors profile changes and triggers a hard clear of all in-memory caches:
- `QueryCache`: Clears all pre-computed SQL aggregations.
- `CardRenderCache`: Invalidates all rendered card data for the previous profile.

---

## 3. Web Companion Security

The embedded Ktor server implements several layers of security to protect user data while exposed on a local network:
- **QR Authentication**: Access is granted via a dynamic QR code that contains a temporary session token.
- **IP Binding**: The server binds to the specific Wi-Fi interface IP to prevent unintended exposure over cellular or VPN interfaces.
- **Security Headers**:
    - `Content-Security-Policy`: Strictly limits script and style sources.
    - `X-Frame-Options: DENY`: Prevents clickjacking attacks.
    - `X-Content-Type-Options: nosniff`: Prevents MIME-type sniffing.
- **Automatic Timeout**: The server automatically shuts down after 2 hours of inactivity to minimize the attack surface.
