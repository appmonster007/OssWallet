# Web Companion Server (Ktor) — OssWallet

OssWallet features an embedded HTTP server that allows users to manage their wallet via a desktop browser. This is powered by **Ktor** and runs directly on the Android device.

## 1. Server Architecture

- **Engine**: `Ktor CIO` (Coroutine-based IO).
- **Port**: `8192` (Default).
- **Service Model**: Managed by `WebServerManager`, which runs as a Singleton.

### 1.1 Lifecycle
The server is **not** started automatically at boot. It must be manually enabled by the user from the "Open in Web" screen. It includes an idle timeout of **2 hours**, after which it automatically stops to save battery and reduce security exposure.

---

## 2. Authentication Flow

1. **Initialization**: When the server starts, `AuthSessionManager` generates a unique `accessCode`.
2. **QR Login**: The mobile app displays a QR code containing `http://<IP>:8192/auth?code=<accessCode>`.
3. **Session Issuance**: Upon visiting the URL with the correct code, the server issues a session token stored in the browser's local storage.
4. **Validation**: Every subsequent REST API call must include this token in the headers.

---

## 3. API Endpoints

The server provides a subset of the mobile app's functionality via REST:

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/summary` | Returns net worth, accounts, and recent activity. |
| `GET` | `/api/v1/transactions` | Paginated transaction list for the active profile. |
| `POST` | `/api/v1/transactions` | Create a new transaction from the web UI. |
| `GET` | `/api/v1/categories` | List all categories and icons. |
| `GET` | `/api/v1/cards` | Fetches computed dashboard cards. |

---

## 4. Technical Implementation Details

### 4.1 Content Negotiation
We use `Kotlinx Serialization` for all JSON payloads. The configuration ignores unknown keys to allow for future API evolution without breaking existing web clients.

### 4.2 Compression
`Gzip` compression is enabled for all responses over 1KB, significantly improving performance when loading large transaction histories over local Wi-Fi.

### 4.3 Activity Tracking
A custom Ktor plugin (`ActivityTracker`) monitors every incoming request to reset the idle timeout timer, ensuring the server stays alive while the user is actively using the web UI.
