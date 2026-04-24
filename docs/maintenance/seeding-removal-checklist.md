# Database Seeding Removal Checklist

Use this checklist to completely remove database seeding functionality from the codebase.

## Quick Removal (5 steps)

- [ ] **Step 1**: Delete the seed package
  ```bash
  rm -rf app/src/main/java/com/osscabinet/ossWallet/data/seed
  ```

- [ ] **Step 2**: Remove seeding call from MainActivity
  - Open: `app/src/main/java/com/osscabinet/ossWallet/MainActivity.kt`
  - Delete lines 39-53 (the entire seeding block)
  - Delete import: `import com.osscabinet.ossWallet.data.seed.SeedingConfig`

- [ ] **Step 3**: Remove build config flag
  - Open: `app/build.gradle.kts`
  - Delete line 30: `buildConfigField("boolean", "ENABLE_SEEDING", "true")`

- [ ] **Step 4**: Remove CSV and JSON assets
  ```bash
  rm app/src/main/assets/accounts.csv
  rm app/src/main/assets/transactions.csv
  rm app/src/main/assets/account_metrics.json
  ```

- [ ] **Step 5**: Clean and rebuild
  ```bash
  ./gradlew clean
  ./gradlew build
  ```

## Verification

After completing all steps, verify:
- [ ] Project builds successfully without errors
- [ ] No references to `SeedingConfig` in the codebase
- [ ] No references to `DatabaseSeeder` in the codebase
- [ ] No `accounts.csv` file exists
- [ ] No `transactions.csv` file exists
- [ ] No `account_metrics.json` file exists
- [ ] App launches without attempting to seed database

## Alternative: Disable Without Removal

If you want to disable seeding temporarily without removing code:

1. Open `app/build.gradle.kts`
2. Change line 30 to:
   ```gradle
   buildConfigField("boolean", "ENABLE_SEEDING", "false")
   ```

This keeps the code but prevents seeding from running.

## Detailed Documentation

For more detailed information, see: `app/src/main/java/com/osscabinet/ossWallet/data/seed/README.md`
