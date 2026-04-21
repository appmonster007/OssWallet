# Database Seeding Module

This package contains all database seeding functionality for development/testing purposes.

## Purpose
Seeds the database with sample data from CSV files during debug builds to facilitate development and testing.

## How to Disable Seeding

### Option 1: Build Config (Recommended)
Set `ENABLE_SEEDING = false` in `app/build.gradle.kts`:
```gradle
buildConfigField("boolean", "ENABLE_SEEDING", "false")
```

### Option 2: Code Toggle
Comment out the seeding block in `MainActivity.kt`:
```kotlin
// if (SeedingConfig.shouldSeed()) {
//     // ... seeding code
// }
```

## How to Remove Seeding Completely

To completely remove seeding from the codebase, follow these steps in order:

### Step 1: Delete the seed package
```bash
rm -rf app/src/main/java/com/osscabinet/ossWallet/data/seed
```

This removes:
- `SeedingConfig.kt`
- `DatabaseSeeder.kt`
- `README.md` (this file)

### Step 2: Remove seeding call from MainActivity
Open `app/src/main/java/com/osscabinet/ossWallet/MainActivity.kt` and delete lines 39-53:

```kotlin
// DELETE THESE LINES:
//        // Seed database if enabled (only in debug builds)
//        // TO DISABLE SEEDING: Set ENABLE_SEEDING = false in app/build.gradle.kts
//        // TO REMOVE SEEDING: Delete the entire block below and the /data/seed package
//        if (SeedingConfig.shouldSeed()) {
//            val seeder = SeedingConfig.createSeeder(
//                database = appContainer.db,
//                settingsDataStore = appContainer.settingsDataStore,
//                context = this
//            )
//            seeder?.let {
//                lifecycleScope.launch {
//                    it.seed()
//                }
//            }
//        }
```

Also remove the import:
```kotlin
import com.osscabinet.ossWallet.data.seed.SeedingConfig
```

### Step 3: Remove build config flag
Open `app/build.gradle.kts` and remove line 30:
```gradle
buildConfigField("boolean", "ENABLE_SEEDING", "true")  // DELETE THIS LINE
```

### Step 4: Remove CSV and JSON assets
```bash
rm app/src/main/assets/accounts.csv
rm app/src/main/assets/transactions.csv
rm app/src/main/assets/account_metrics.json
```

### Step 5: Clean build (recommended)
```bash
./gradlew clean
```

### Verification
After removal, the app should:
- Build successfully without any compilation errors
- Not reference any seeding-related code
- Not attempt to seed the database on launch

## Files in This Package

- `SeedingConfig.kt` - Central configuration and toggle for seeding
- `DatabaseSeeder.kt` - Core seeding logic that reads from separate CSV files

## Assets Files

- `accounts.csv` - Account seed data (id, name, currency, structure type, color, icon, allowed currencies, profile ID)
- `transactions.csv` - Transaction seed data (title, amount, category, timestamp, currency, income/expense, account ID, payment method, payee, note, tags, recurring, profile ID)
- `account_metrics.json` - Pre-calculated account metrics for testing

## Architecture

The seeding logic is intentionally isolated to make it easy to remove:

- **No Hilt dependencies** - Uses manual instantiation
- **No AppContainer coupling** - Removed from dependency container
- **Single entry point** - Only called from MainActivity
- **Feature flag controlled** - Uses BuildConfig for easy toggling
- **Self-contained** - All seeding code lives in this package

## CSV Format

### accounts.csv
```
id,name,currencyCode,structureType,colorHex,icon,allowedCurrencies,profileId
```

Note: Accounts start with zero balance. Initial balances are represented as "Initial Balance" transactions in transactions.csv.

### transactions.csv
```
title,amount,category,categoryId,timestamp,currency,isIncome,accountId,paymentMethod,payee,note,tags,recurring,profileId
```

- Fields with commas (like `allowedCurrencies`) must be quoted
- Transactions reference accounts by `accountId`
- Account balances are updated as transactions are inserted
- Initial balances are represented as income transactions with category "Initial Balance"

## Seeding Behavior

- Only runs in DEBUG builds
- Only runs if `ENABLE_SEEDING = true` in BuildConfig
- Checks `isDatabaseSeeded` flag in SettingsDataStore before seeding
- Marks database as seeded after successful completion
- To re-seed: clear app data or reset the flag in SettingsDataStore
