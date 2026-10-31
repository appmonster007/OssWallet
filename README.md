# OssWallet

OssWallet is a privacy-first, encrypted, and highly customizable personal finance manager for Android. All data lives in a local SQLCipher-encrypted database on your device — nothing is uploaded to any server. It's built with Jetpack Compose and Clean Architecture, and ships in two flavors: a `full` build with optional Google Play Services integrations, and a `fdroid` build with no Google dependencies at all, for a fully open, GMS-free install.

## 📥 Download

- **[Latest GitHub Release](https://github.com/appmonster007/OssWallet/releases)** — signed APK builds (`full` and `fdroid` flavors).
- **F-Droid**: submission is in progress — once accepted, OssWallet will be installable and auto-updated directly from the F-Droid client with no Google dependencies.

## 🚀 Getting Started

To get started with development, please refer to the following documentation:

- **[Onboarding Guide](docs/ONBOARDING.md)**: Environment setup, build commands, and testing philosophy.
- **[High-Level Architecture](docs/ARCHITECTURE.md)**: Overview of layers, concurrency, and the card engine.
- **[Technical Stack](docs/TECH_STACK.md)**: Detailed list of libraries (versions) and core tech.
- **[Module Registry](docs/MODULE_REGISTRY.md)**: Detailed map of the project structure.

## 🛡 Security & Advanced Features

- **[Security Subsystem](docs/SECURITY.md)**: Deep dive into SQLCipher encryption and profile isolation.
- **[Database Directive](docs/DATABASE.md)**: Schema rules, migrations, and type converters.
- **[Web Companion Server](docs/COMPANION_SERVER.md)**: Details on the embedded Ktor server and API.
- **[Design System](docs/DESIGN_SYSTEM.md)**: Aeon visual identity, typography, and glassmorphism.

## 🛠 Key Features

- **End-to-End Encryption**: Local SQLite database encrypted with SQLCipher (256-bit AES) — your financial data never leaves your device.
- **Customizable Dashboard**: A card-based "Atelier" lets you add, remove, and reorder summary, tracking, and analytics cards — Net Worth, Cash Flow, Accounts, Budgets, Planned Payments, Loans, Goals, Recent Activities, and a range of charts (category pie/treemap, income vs. expense, cash flow comparison, spending trends, tag UpSet plots).
- **Multi-Profile Support**: Isolate accounts, transactions, and budgets into separate profiles.
- **Budgets, Loans & Goals**: Track spending against limits, money lent or borrowed, and progress toward savings targets.
- **Planned Payments**: Automated tracking and scheduling of recurring payments.
- **Multi-Currency**: Track balances in multiple currencies with live exchange-rate conversion.
- **Advanced Search**: High-performance full-text search (FTS4) on transactions.
- **Home Screen Widgets**: Glance-based widgets for accounts, budgets, and recent activity.
- **Open in Web**: Embedded Ktor server lets you manage your data from a desktop browser on the same network.
- **GMS-Free Option**: The `fdroid` build flavor has zero Google Play Services dependencies.

## ☕ Support the Project

OssWallet is free and open source. If you find it useful, you can support its development via [Buy Me a Coffee](https://buymeacoffee.com/osscabinet) — there's an optional, dismissible prompt in the app, and a direct link under Settings → Support Us.

---
© 2026 OssCabinet. All rights reserved.
