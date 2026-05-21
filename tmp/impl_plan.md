# Implementation Plan - Onboarding Setup Screen

This plan outlines the addition of an onboarding setup screen that appears when the user opens the app for the first time. The onboarding will guide the user through basic configurations like profile name, primary currency, and theme selection.

## User Review Required

> [!NOTE]
> The onboarding will use a multi-step pager interface. Users can skip or complete it to reach the main dashboard.
> Completion status will be stored in `SettingsDataStore`.

## Proposed Changes

### Data & Repository

#### [SettingsDataStore.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/data/local/SettingsDataStore.kt)
- Add `IS_ONBOARDING_COMPLETED_KEY` to track first-run status.
- Add `isOnboardingCompleted` Flow and `saveOnboardingCompleted` function.

#### [SettingsRepository.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/domain/repository/SettingsRepository.kt)
- Add `isOnboardingCompleted: Flow<Boolean>` to the interface.
- Add `suspend fun saveOnboardingCompleted(completed: Boolean)` to the interface.

#### [SettingsRepositoryImpl.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/data/repository/SettingsRepositoryImpl.kt)
- Implement the new properties and functions.

---

### Navigation

#### [Screen.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/navigation/Screen.kt)
- Add `Onboarding` object to the `Screen` sealed class.

#### [OssWalletNavGraph.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/navigation/OssWalletNavGraph.kt)
- Add the `Onboarding` route to the `NavHost`.
- Use a `LaunchedEffect` or custom logic to redirect to Onboarding if `isOnboardingCompleted` is false.

---

### UI - Onboarding Screen

#### [NEW] [OnboardingViewModel.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/screens/onboarding/OnboardingViewModel.kt)
- Manage state for onboarding steps (Welcome, Profile, Currency, Appearance).
- Handle saving settings and marking onboarding as completed.

#### [NEW] [OnboardingScreen.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/screens/onboarding/OnboardingScreen.kt)
- Implement the multi-step UI using `HorizontalPager`.
- Use `AeonTheme` and `AppColors` for consistency.

---

### Main ViewModel

#### [MainViewModel.kt](file:///Users/prathitaswar/Desktop/Dev/AndroidStudioProjects/OssWallet/app/src/main/java/com/osscabinet/ossWallet/ui/screens/main/MainViewModel.kt)
- Expose `isOnboardingCompleted` from `SettingsRepository`.

## Verification Plan

### Automated Tests
- N/A (UI-heavy task, manual verification preferred)

### Manual Verification
1. **First Run**: Clear app data and launch the app. Verify it navigates to the Onboarding screen.
2. **Onboarding Steps**:
    - Verify Welcome screen is displayed.
    - Verify Profile name can be edited.
    - Verify Currency selection works.
    - Verify Theme selection updates the UI immediately or upon completion.
3. **Completion**: Click "Get Started" and verify it navigates to the Dashboard.
4. **Subsequent Launches**: Close and reopen the app. Verify it goes straight to the Dashboard.
5. **Back Behavior**: Verify that clicking back on the first step of onboarding doesn't close the app (or does, depending on requirements), and back navigation works between steps.
