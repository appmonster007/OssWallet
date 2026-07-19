# Module & File Registry — OssWallet

This registry maps the core logical modules and provides a detailed guide to critical files.

## 1. Project Structure

### 1.1 Data Layer (`com.osscabinet.ossWallet.data`)
| File | Responsibility |
| :--- | :--- |
| `local/AppDatabase.kt` | Manages the encrypted SQLite database and FTS4 migrations. |
| `local/SecurePassphraseManager.kt` | Cryptographically secure key generation and storage. |
| `repository/TransactionRepositoryImpl.kt` | Bridges Room DAOs to domain models. |
| `web/WebServerManager.kt` | Controls the lifecycle of the Ktor embedded server. |

### 1.2 Domain Layer (`com.osscabinet.ossWallet.domain`)
| File | Responsibility |
| :--- | :--- |
| `service/management/TransactionManager.kt` | Centralized business logic for all transaction writes. |
| `service/currency/CurrencyService.kt` | High-precision math engine for currency conversion. |
| `card/CardComputerRegistry.kt` | Strategy factory for dashboard metric widgets. |
| `usecase/TransactionUseCases.kt` | Complex read-operations (e.g., grouped history with balances). |

### 1.3 UI Layer (`com.osscabinet.ossWallet.ui`)
| File | Responsibility |
| :--- | :--- |
| `screens/dashboard/DashboardViewModel.kt` | Orchestrates the reactive pipeline for the main screen. |
| `theme/Theme.kt` | Defines the global Compose theme, glassmorphism, and custom typography. |
| `navigation/OssWalletNavGraph.kt` | Type-safe navigation structure for the entire app. |

---

## 2. Exhaustive Guides

Refer to these documents for deep-dives into specific subsystems:
- **[Architecture Deep-Dive](ARCHITECTURE.md)**: Layers, Concurrency, and Card Engine Checklist.
- **[Security & Privacy](SECURITY.md)**: Passphrase management and profile isolation.
- **[Database Standards](DATABASE.md)**: Schema, Migrations, and TypeConverters.
- **[Design System (Aeon)](DESIGN_SYSTEM.md)**: Visual identity, Color Layering, and Glassmorphism.
- **[Coding Conventions](CONVENTIONS.md)**: Kotlin style, Coroutines, and Error handling.
- **[Onboarding & Testing](ONBOARDING.md)**: Setup, build commands, and testing strategy.
