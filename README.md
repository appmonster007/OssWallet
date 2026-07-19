# OssWallet

OssWallet is a privacy-first, encrypted, and highly customizable personal finance manager for Android.

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

- **End-to-End Encryption**: SQLite database encrypted with SQLCipher (256-bit AES).
- **Open in Web**: Embedded Ktor server allows you to manage your data from a desktop browser.
- **Advanced Search**: High-performance full-text search (FTS4) on transactions.
- **Multi-Profile Support**: Isolate your data into separate profiles.
- **Planned Payments**: Automated tracking and scheduling of recurring payments.

---
© 2026 OssCabinet. All rights reserved.
