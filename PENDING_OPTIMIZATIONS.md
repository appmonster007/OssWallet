# Pending Optimizations

This document tracks planned optimizations that were deferred to focus on core feature development and UI refactoring.

## 1. Build & Release Optimizations
- [ ] **R8 / Proguard**: Enable full code shrinking and obfuscation (`isMinifyEnabled = true`).
- [ ] **Resource Shrinking**: Enable `isShrinkResources = true` to remove unused resources.
- [ ] **ABI Splits**: Configure splitting by architecture (arm64-v8a, armeabi-v7a, x86_64) to reduce per-device download size.
- [ ] **Locale Filtering**: Filter resources to only include supported languages.

## 2. Asset & Resource Management
- [ ] **Downloadable Fonts**: Migrate from bundled `.ttf` files to Google Fonts via `androidx.compose.ui:ui-text-google-fonts`.
    - *Status*: Dependency and implementation logic prepared in `Type.kt`, but reverted to avoid dependency synchronization issues during feature work.
    - *Required*: Add font provider certificates to `res/values/font_certs.xml`.
- [ ] **Image Optimization**: Convert remaining PNG/JPG assets to WebP format.
- [ ] **SVG to Vector**: Ensure all icons are using `VectorDrawable` or Compose `ImageVector`.

## 3. UI/UX Consistency
- [ ] **Numpad Logic Consolidation**: The `Numpad` component in `AeonComponents.kt` is now the source of truth for calculator logic. Ensure any future amount-entry screens use this component.
- [ ] **Visibility States**: Standardize the `isNumpadVisible` pattern to hide non-essential UI elements (Drawers, Action Cards) during input to prevent layout jumping and overflow.
