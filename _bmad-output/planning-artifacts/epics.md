---
stepsCompleted:
  - Step 1: Validate Prerequisites and Extract Requirements
  - Step 2: Design Epic List
  - Step 2 (Addendum): Bug-fix Epics 6 & 7 + Epic 3 stories 3.4-3.5 added from user-reported bugs
  - Step 3: Generate Epics and Stories
  - Step 4: Final Validation
inputDocuments:
  - _bmad-output/planning-artifacts/PRD.md
  - _bmad-output/planning-artifacts/architecture/architecture-OssWallet-20260625/ARCHITECTURE-SPINE.md
---

# OssWallet - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for OssWallet, decomposing the requirements from the PRD, UX Design if it exists, and Architecture requirements into implementable stories.

## Requirements Inventory

### Functional Requirements

FR1: Decouple the monolithic :app module into :core:domain, :core:data, and feature-specific modules (:feature:web, :feature:dashboard).
FR2: Implement strict Unidirectional Data Flow for all screens to ensure deterministic state and prevent logic leakage.
FR3: Standardize all networking and persistence serialization on kotlinx.serialization.
FR4: Decouple business logic from database-specific query services (e.g., CardAggregationService).
FR5: Implement a full list view for planned payments with next-due dates and overdue highlighting on the Web UI.
FR6: Add "Mark as Paid" and "Skip Occurrence" actions to the Web UI using existing backend endpoints.
FR7: Enable GPS/Location field input and display on the Web transaction forms and list.
FR8: Add click actions to Web dashboard chart cards for detailed analytics navigation.
FR9: Conduct a Root Cause Analysis (RCA) on dashboard scrolling/rendering and implement optimizations (e.g., lazy loading, composition skipping, or improved aggregation caching).
FR10: Ensure all 3D card flips and glassmorphism effects maintain a consistent 60+ FPS.

### NonFunctional Requirements

NFR1: Maintain 256-bit AES encryption (SQLCipher) across all data layers.
NFR2: 100% test coverage for all new or refactored Domain Use Cases.
NFR3: Dashboard initial load must complete in under 500ms; scrolling must be stutter-free.
NFR4: All async operations must use injected dispatchers (no hardcoded IO/Main).

### Additional Requirements

- AD-1: Domain Pureness - Services/use cases must operate exclusively on Pure Domain Models.
- AD-4: Centralized Cache Lifecycle - Cache invalidation managed by a scoped DI CacheCoordinator.
- AD-5: Standardized Search Strategy - Room-native FTS4 support only.
- AD-7: Result-Based Error Propagation - All layer boundaries must return a DomainResult<T>.
- AD-9: Dependency-Injected Concurrency - Hardcoded Dispatchers are banned.

### UX Design Requirements

UX-DR1: Implement "H — Editorial Instrument" design system across new feature modules.
UX-DR2: Consolidate "H — Editorial Instrument" design across all existing screens and layers. Currently implemented inconsistently, resulting in "over-designed" or messy layouts (e.g., Settings screen). Focus on simplifying and applying high-fidelity editorial standards consistently.
UX-DR3: Ensure 3D flip animation in AeonExpressiveCard remains performant during MVI state updates.
UX-DR4: Context-level glassmorphism (Haze) policy implementation.

### FR Coverage Map

FR1: Epic 1 - Modularization of monolith
FR2: Epic 2 - Mandatory UDF/MVI implementation
FR3: Epic 1 - Standardize on kotlinx.serialization
FR4: Epic 1 - Domain purity & query abstraction
FR5: Epic 4 - Web Planned Payments full page
FR6: Epic 4 - Web Mark as Paid/Skip actions
FR7: Epic 4 - Web GPS/Location field & display
FR8: Epic 4 - Web dashboard drill-downs
FR9: Epic 2 - Dashboard performance RCA & fix
FR10: Epic 2 - Fluid UI transitions (60+ FPS)
FR11: Epic 3 - "H" Design system consolidation
BUG-FR1: Epic 6 - Fix RFC 4180-compliant CSV quoting in import parser
BUG-FR2: Epic 6 - Add per-account currency selector to import account-mapping step
BUG-FR3: Epic 7 - Suppress duplicate numpad launch on Planned Payment entry screen
BUG-FR4: Epic 7 - Fix crash when saving Planned Payment from Smart Suggestion screen
BUG-FR5: Epic 7 - Fix prefill of PP entry and Template entry screens from Smart Suggestion; remove implicit background save
BUG-FR6: Epic 7 - Quick Add sheet opens template selection first (if templates exist), with toggle to category
BUG-FR7: Epic 3 - RCA and fix for editorial input fields rendering incorrectly in non-glassmorphism mode
BUG-FR8: Epic 6 - Fix double-negative amount display; audit all screens for further instances
BUG-NFR1: Epic 6 - Import errors surface as user-visible messages; no unhandled exceptions
BUG-NFR2: Epic 7 - No crash escapes from Planned Payment or Smart Suggestion flows

## Epic List

### Epic 1: Architectural Foundation & Domain Purity
Establish the structural "Spine" by decoupling the monolith and purifying the domain logic. This is the essential "enabler" epic for all future scaling.
**FRs covered:** FR1, FR3, FR4, NFR4, AD-1, AD-9.

### Epic 2: Universal UI State & Performance (MVI & Dashboard RCA)
Standardize state management with MVI and fix the "jank" on the main dashboard through a dedicated RCA and refactor.
**FRs covered:** FR2, FR9, FR10, NFR3, AD-3, UX-DR3.

### Epic 3: Design System Consolidation ("H" Identity)
Audit and refactor all existing screens to strictly adhere to the H-Editorial Instrument standards, focusing on simplification and Settings screen cleanup. Includes fixes for input field rendering in non-glass mode and removal of glassmorphism from the Quick Add sheet.
**FRs covered:** FR11, UX-DR1, UX-DR2, UX-DR4, AD-10, BUG-FR7.

### Epic 4: Web UI - Operational Parity
Bridge the functionality gap for life-management features (Planned Payments, Location) on the Ktor-served Web UI.
**FRs covered:** FR5, FR6, FR7, FR8.

### Epic 5: Search & Infrastructure Hardening
Standardize search on Room-native FTS4 and implement the centralized Cache Coordinator and Result wrappers for a crash-free experience.
**FRs covered:** AD-4, AD-5, AD-7, NFR1, NFR2.

### Epic 6: Import Reliability, Account Configuration & Display Formatting
Users can reliably import an OssWallet ZIP backup — quoted CSV fields with commas parse correctly, each account's currency is set explicitly during import, and amount values display with the correct sign throughout the app (no double-negatives).
**FRs covered:** BUG-FR1, BUG-FR2, BUG-FR8, BUG-NFR1.

### Epic 7: Planned Payment, Smart Suggestion & Quick Add Flow Stability
Users can open a Planned Payment entry without the numpad firing twice, save from Smart Suggestion without crashing, have all suggestion data correctly pre-filled when navigating to PP or Template entry, and use Quick Add with a template-first selection step when templates exist.
**FRs covered:** BUG-FR3, BUG-FR4, BUG-FR5, BUG-FR6, BUG-NFR2.

## Epic 1: Architectural Foundation & Domain Purity

Establish the structural "Spine" by decoupling the monolith and purifying the domain logic. This is the essential "enabler" epic for all future scaling.

### Story 1.1: Core Infrastructure Baseline (Serialization & Concurrency)

As a developer,
I want a standardized serialization and concurrency framework,
So that I can write thread-safe, modular code without framework-specific leakage.

**Acceptance Criteria:**

**Given** a project using Moshi and hardcoded `Dispatchers.IO`
**When** I implement a global `kotlinx.serialization` configuration and an injected `DispatcherProvider`
**Then** all `BigDecimal` math uses a consistent Kotlinx serializer
**And** `Retrofit` is configured with the `kotlinx-serialization-converter`
**And** all Repositories receive their dispatchers via Hilt constructor injection
**And** Moshi dependencies are completely removed from the project.

### Story 1.2: Establish Pure Domain Module (:core:domain)

As a developer,
I want a pure Kotlin domain module,
So that my business logic is completely isolated from Android and Database frameworks.

**Acceptance Criteria:**

**Given** a monolithic `:app` module
**When** I create a `:core:domain` module and move core models (`Transaction`, `Account`, `Category`) into it
**Then** these models are stripped of all Room (`@Entity`, `@ColumnInfo`) and Moshi annotations
**And** `:core:domain` has zero dependencies on `androidx.room`, `android.*`, or `com.google.dagger:hilt-android`
**And** the project compiles with `:app` depending on `:core:domain`.

### Story 1.3: Data Layer Isolation & Feature Stubs (:core:data & :feature:dashboard)

As a developer,
I want isolated Data and Feature modules,
So that I can enforce strict boundary rules and prevent "spaghetti" dependencies.

**Acceptance Criteria:**

**Given** the `:core:domain` module exists
**When** I create `:core:data` for Room/Network implementations and `:feature:dashboard` as a UI stub
**Then** `:core:data` implements the Repository interfaces defined in `:core:domain`
**And** `:feature:dashboard` depends ONLY on `:core:domain` and `:core:ui` (design system)
**And** `Hilt` entry points are established to provide implementation instances across module boundaries.

### Story 1.4: Purify Aggregation Logic (Domain Query Abstraction)

As a developer,
I want business aggregation logic to be decoupled from the database,
So that I can test financial rules without a running SQLite instance.

**Acceptance Criteria:**

**Given** `CardAggregationService` which currently injects `DatabaseQueryService` (impl)
**When** I create a `DomainQueryEngine` interface in `:core:domain`
**Then** `CardAggregationService` is refactored into a pure service that only knows about Domain models and the Query interface
**And** all SQL-specific query generation (the "grammar") is moved to a concrete implementation in `:core:data`
**And** the service can be verified with 100% code coverage using unit tests and mocks.

## Epic 2: Universal UI State & Performance (MVI & Dashboard RCA)

Standardize state management with MVI and fix the "jank" on the main dashboard through a dedicated RCA and refactor.

### Story 2.1: Dashboard Performance Root Cause Analysis (RCA)

As a developer,
I want to identify the technical sources of UI stutter on the dashboard,
So that I can implement targeted optimizations that ensure a 60+ FPS experience.

**Acceptance Criteria:**

**Given** a dashboard that exhibits "jank" during scrolling and 3D card flips
**When** I perform a deep-trace audit using the Android Studio Profiler and Layout Inspector
**Then** I must identify specific "re-composition loops" or expensive calculation blocks causing frame drops
**And** I must produce a remediation plan identifying specific components that require `@Stable`/`@Immutable` markers or `remember` caching
**And** the root cause of "flip-animation stutter" must be explicitly isolated (e.g., state-update collisions).

### Story 2.2: Mandatory MVI Pattern for Dashboard

As a developer,
I want the Dashboard to follow a strict Unidirectional Data Flow (MVI),
So that UI state is deterministic, testable, and optimized for Compose skip-conventions.

**Acceptance Criteria:**

**Given** a Dashboard currently using fragmented reactive flows
**When** I refactor the Dashboard ViewModel to expose a single immutable `DashboardUiState` and a single `onIntent(DashboardIntent)` entry point
**Then** all local state mutations in the UI are removed
**And** the ViewModel uses a `Reducer` pattern to handle intents
**And** all data classes used in the `UiState` are marked with `@Immutable` to maximize Compose re-composition skipping.

### Story 2.3: Dashboard Rendering Optimization (Lazy Loading & Caching)

As a developer,
I want to implement high-performance rendering for the Dashboard cards,
So that the UI remains fluid even with complex financial aggregations.

**Acceptance Criteria:**

**Given** the performance remediation plan from Story 2.1 and the MVI structure from 2.2
**When** I implement lazy loading for off-screen cards and optimize the card-render cache
**Then** the dashboard initial load must complete in under 500ms
**And** scrolling must maintain a consistent 60+ FPS on mid-range devices
**And** 3D card flips must occur without visible frame drops during the state transition.

### Story 2.4: Per-Card Parallel Computation & Independent Loading States

As a developer,
I want each dashboard card to compute its data in parallel and maintain its own loading/error state,
So that the dashboard remains interactive even if one card is slow or fails.

**Acceptance Criteria:**

**Given** a dashboard that currently computes all cards sequentially or blocks the UI
**When** I refactor the computation engine to launch per-card coroutines
**Then** each card transitions from 'loading' to 'success' or 'error' independently
**And** a failure in one card's aggregation logic does not prevent other cards from rendering.

### Story 2.5: Universal Currency Integrity & Consistent Formatting

As a user,
I want all charts and cards to display amounts with the correct currency symbol and formatting,
so that I have full confidence in the multi-currency accounting data I see on my dashboard and analytics.

**Acceptance Criteria:**

**Given** a multi-currency profile
**When** I view dashboard cards or analytics charts (Treemap, Pie, Bar, Line)
**Then** all amounts are displayed with their correct native or converted currency symbols (₹, $, €, etc.)
**And** chart Y-axis ticks and hover-tooltips correctly reflect the active currency
**And** multi-currency distribution badges in Account Hero cards show symbols for each currency breakdown.

## Epic 3: Design System Consolidation ("H" Identity)

Audit and refactor all existing screens to strictly adhere to the H-Editorial Instrument standards, focusing on simplification and Settings screen cleanup.

### Story 3.1: Design Token Audit & Theme Refactor

As a developer,
I want a centralized and strictly enforced design token system,
So that I can ensure visual consistency across all feature modules without code duplication.

**Acceptance Criteria:**

**Given** a codebase with some hardcoded colors and inconsistent typography usage
**When** I audit the `ui.theme` package and refactor `OssWalletTheme` to be the single source of truth for the "H" identity
**Then** all hardcoded hex codes and `sp` values are replaced with tokens from `AeonTheme` and `AppTextStyles`
**And** the theme supports dynamic accent switching while preserving the "H" neutral palettes
**And** `CompositionLocalProvider` is used to inject all design dimensions and scales globally.

### Story 3.2: Settings Screen Simplification & Refactor

As a user,
I want a clean, legible, and simple Settings interface,
So that I can manage my preferences without being overwhelmed by "over-designed" clutter.

**Acceptance Criteria:**

**Given** the current "messy" and inconsistent Settings screen
**When** I refactor the screen to use a high-fidelity "Editorial" list pattern
**Then** all non-essential decorative elements are removed
**And** the screen strictly adheres to the "H" Light/Dark/AMOLED palettes
**And** it uses standard `AeonTopAppBar` and modular section headers
**And** all interactive controls (switches, sliders) are unified into a consistent design language.

### Story 3.3: Global "H" Design Consistency Pass

As a user,
I want a visually cohesive experience across all screens (Accounts, Records, Analytics),
So that the app feels like a single, unified "premium instrument."

**Acceptance Criteria:**

**Given** the Accounts, Records, and Analytics screens which may have minor design deviations
**When** I perform a global design sweep applying the "H" standards
**Then** all screens use the same modular `AeonTopAppBar` pill layouts
**And** all data containers are unified as `AeonExpressiveCard` instances
**And** the **Component-Level Glass Policy** (AD-10) is applied (e.g., opaque cards, glass popups)
**And** the navigation transitions between these screens are fluid and consistent.

### Story 3.4: Editorial Input Field Solid-Mode RCA & Fix

As a user,
I want input fields to render with a clean, solid container style when glassmorphism is disabled,
So that the app looks polished regardless of which surface mode is active.

**Acceptance Criteria:**

**Given** glassmorphism is disabled in app settings
**When** I open any screen containing editorial input fields (e.g., NOTE, PAYEE, Amount)
**Then** each input field renders as a solid, filled container with correct border radius and background
**And** no malformed outline, double-border, or transparent artifact is visible
**And** the root cause (divergent conditional styling between glass and non-glass render paths) is documented and fixed at the component level so all future input fields inherit the correct behaviour.

### Story 3.5: Fix AtelierPanel Background — Blend Accounts & Category/Template Areas with Sheet Surface

As a user,
I want the accounts area and the category/template area inside the Quick Add sheet to visually blend with the rest of the sheet,
So that those regions do not appear as a distinct inset panel with a noticeably different background.

**RCA:** `AtelierPanel` (`ui/screens/dashboard/quickadd/steps/QuickAddAtelier.kt`) wraps both areas with a `GlassSurface` that composites `AppColors.background` at 55% alpha on top of the sheet's own glass surface, producing a visually distinct "box". In non-glass mode, `fallbackColor = AppColors.background` conflicts with the sheet's `AppColors.cardSurface`, making the panel stand out as a solid inset block.

**Acceptance Criteria:**

**Given** the Quick Add sheet is open (glass mode enabled)
**When** I view the accounts step or the template/category step
**Then** the `AtelierPanel` area has no inner `GlassSurface` of its own — it uses a lightweight container with `AppColors.recessedSurface` as the background (the same tone-on-tone used by chips, tiles, and toggles in the sheet)
**And** the panel background is visually indistinguishable from the surrounding sheet glass surface — it does not appear as a separate floating panel
**And** the hairline border and rounded corners of `AtelierPanel` are preserved

**Given** the Quick Add sheet is open (glass mode disabled)
**When** I view the accounts step or the template/category step
**Then** the `AtelierPanel` background also uses `AppColors.recessedSurface` and blends with the sheet's `AppColors.cardSurface` — no mismatched solid "box" is visible
**And** no other visual regressions are introduced (overline, grid, search field, footer remain unchanged).

## Epic 4: Web UI - Operational Parity

Bridge the functionality gap for life-management features (Planned Payments, Location) on the Ktor-served Web UI.

### Story 4.1: Web UI - Planned Payments Management Page

As a user,
I want to manage my recurring payments via the web interface,
So that I can keep my records up to date from my computer without using my phone.

**Acceptance Criteria:**

**Given** the current Web UI where planned payments are only visible as a dashboard summary
**When** I navigate to the standalone "Planned Payments" page
**Then** I can see a full list of recurring payments sorted by next-due date
**And** overdue payments are clearly highlighted in red
**And** I can click "Mark as Paid" or "Skip" to trigger the corresponding `POST` endpoints on the backend
**And** the UI reflects the updated payment state immediately without a full page refresh.

### Story 4.2: Web UI - GPS & Location Management

As a user,
I want to input and view location data for my transactions on the web,
So that I can maintain detailed geographic records across both platforms.

**Acceptance Criteria:**

**Given** a transaction created or edited via the Web UI
**When** I open the transaction form
**Then** I can see and edit the "Location Name" and "GPS Coordinates" fields
**And** these values are successfully persisted to the backend `TransactionDto`
**And** the location data is displayed in the Web transaction list alongside the payee and amount
**And** parity is maintained with data captured on the Android native app.

### Story 4.3: Web UI - Interactive Dashboard & Analytics Drill-downs

As a user,
I want to interact with the charts on my web dashboard,
So that I can quickly investigate my spending trends.

**Acceptance Criteria:**

**Given** the dashboard chart cards (Pie, Treemap, Cash Flow) on the Web UI
**When** I click on a chart segment or data point
**Then** the UI navigates to the "Records" page with the appropriate filters (Category, Account, or Timeline) automatically applied
**And** all chart components support hover-tooltips for detailed value readouts
**And** the interaction speed is optimized to prevent UI lag during navigation transitions.

### Story 4.4: Web UI - "H — Editorial Instrument" Redesign & Consistency

As a user,
I want the Web UI to share the same premium editorial aesthetic as the Android app,
So that I have a consistent and high-fidelity experience across both platforms.

**Acceptance Criteria:**

**Given** the current Web UI which uses a basic or inconsistent design
**When** I apply the "H — Editorial Instrument" design tokens to the Web CSS and layouts
**Then** the Web UI uses the high-fidelity warm-black/ivory/champagne (dark) and ivory/ink/bronze (light) palettes
**And** it incorporates the same typography standards (Fraunces for headings/amounts, Inter for body, JetBrains Mono for micro-readouts) via web fonts
**And** the navigation header is refactored into a responsive, modular "pill-based" layout mirroring the `AeonTopAppBar`
**And** all dashboard cards and tables adopt the "Opaque Hairline" panel aesthetic for a clean, editorial look.

## Epic 5: Search & Infrastructure Hardening

Standardize search on Room-native FTS4 and implement the centralized Cache Coordinator and Result wrappers for a crash-free experience.

### Story 5.1: Implement Room-Native FTS4 Search

As a developer,
I want to use Room's native FTS4 support for transaction indexing,
So that the search logic is reliable, maintainable, and free from manual trigger errors.

**Acceptance Criteria:**

**Given** a database using manual FTS triggers and FTS4
**When** I refactor the entities to use `@Fts4` and `@FtsExternalContent` annotations
**Then** all `manual_fts_content_sync` triggers are removed from the migration scripts
**And** search remains performant across Payee, Note, and Category names
**And** a verified migration path from v27 to v28 is implemented and tested with existing data.

### Story 5.2: Centralized Cache Lifecycle (CacheCoordinator)

As a developer,
I want cache invalidation to be handled by a dedicated service,
So that the Application class is decoupled from low-level caching rules.

**Acceptance Criteria:**

**Given** the current `OssWalletApp.kt` which manually clears caches on profile switch
**When** I implement a Hilt-injected `CacheCoordinator`
**Then** `QueryCache` and `CardRenderCache` are managed exclusively by this coordinator
**And** invalidation is triggered via an internal event bus or repository signal
**And** `OssWalletApp` is stripped of all caching logic.

### Story 5.3: Domain-Wide Result Wrapper & Boundary Protection

As a developer,
I want all data-layer operations to return a protected Result wrapper,
So that I can ensure a crash-free UI that handles errors gracefully.

**Acceptance Criteria:**

**Given** a project where repositories may bubble up exceptions
**When** I implement a `DomainResult<T>` functional wrapper across all layers
**Then** every Repository and Use Case method returns either a `Success` or `Failure` object
**And** ViewModels explicitly map these results to the `UiState`
**And** the UI displays appropriate "Empty" or "Error" states without crashing.

## Epic 6: Import Reliability, Account Configuration & Display Formatting

Users can reliably import an OssWallet ZIP backup — quoted CSV fields with commas parse correctly, each account's currency is set explicitly during import, and amount values display with the correct sign throughout the app.

### Story 6.1: Fix CSV Parser to Handle Quoted Fields Containing Commas

As a user,
I want to import an OssWallet ZIP backup that contains fields with commas in them,
So that the import does not fail with a parse error on my real-world data.

**Acceptance Criteria:**

**Given** an OssWallet ZIP export where one or more CSV fields contain a comma (e.g., a payee name like "Coffee, Tea & More")
**When** I trigger the import flow
**Then** the CSV parser treats any field enclosed in double quotes as a single value, regardless of internal commas (RFC 4180 compliance)
**And** the import completes without throwing a parse exception
**And** all imported values (payee, note, category, amount) match the original exported data exactly
**And** if a parse error does occur on a row, a user-visible error message identifies the problematic row rather than crashing silently.

### Story 6.2: Per-Account Currency Selector in Import Account-Mapping Step

As a user,
I want to set each imported account's currency individually during the import flow,
So that my accounts are not all silently assigned the app's global base currency.

**Acceptance Criteria:**

**Given** the account-mapping step of the ZIP import flow
**When** I review the list of accounts to be imported
**Then** each account row displays a currency selector (dropdown or chip) pre-populated with the currency stored in the export file
**And** I can override the currency for any individual account before confirming the import
**And** accounts that are not explicitly changed retain the currency from the export file, not the app's global base currency
**And** the selected per-account currencies are correctly persisted when the import completes.

### Story 6.3: Fix Double-Negative Amount Display & Global Audit

As a user,
I want amount values to display with a single correct sign (e.g., `–220.00 INR`, not `--220.00 INR`),
So that I can read my financial data without confusing formatting artefacts.

**Acceptance Criteria:**

**Given** a negative transaction or template amount anywhere in the app
**When** it is rendered in a list, detail screen, or entry field
**Then** the amount shows exactly one negative sign (or the app's configured debit indicator), never two
**And** the root cause (double sign-prepending in the amount formatter or display layer) is fixed at the shared formatting utility so all call sites benefit
**And** a search across all screens that display amounts (Link Template, Transaction List, Dashboard cards, Analytics) confirms no remaining double-negative instances
**And** positive amounts and zero amounts are unaffected.

## Epic 7: Planned Payment, Smart Suggestion & Quick Add Flow Stability

Users can open a Planned Payment entry without the numpad firing twice, save from Smart Suggestion without crashing, have all suggestion data correctly pre-filled when navigating to PP or Template entry, and use Quick Add with a template-first selection step when templates exist.

### Story 7.1: Suppress Duplicate Numpad Launch on Planned Payment Entry Screen

As a user,
I want the calculator numpad to open exactly once when I enter the Planned Payment entry screen,
So that I am not forced to dismiss a duplicate keyboard before I can start entering data.

**Acceptance Criteria:**

**Given** I navigate to the Planned Payment entry screen
**When** the screen opens
**Then** the calculator numpad appears exactly once
**And** no duplicate `LaunchedEffect`, `SideEffect`, or focus-request triggers a second numpad open
**And** dismissing the numpad and re-focusing the amount field opens it exactly once again
**And** the fix does not regress numpad behaviour on the standard Transaction entry screen.

### Story 7.2: Fix Smart Suggestion Crash When Saving as Planned Payment

As a user,
I want to save a Smart Suggestion as a Planned Payment without the app crashing,
So that I can convert AI-detected patterns into recurring payments reliably.

**Acceptance Criteria:**

**Given** I am on the Smart Suggestion screen with a suggestion displayed
**When** I tap "Save as Planned Payment"
**Then** the app does not throw an unhandled exception (NPE, IllegalStateException, or similar)
**And** all required fields on the `PlannedPayment` domain model are populated before the save call is made
**And** null-safety is enforced for every optional field (amount, category, recurrence) before they are passed to `PlannedPaymentService`
**And** on success, the user is navigated to a confirmation state or back to the suggestion list.

### Story 7.3: Fix Prefill When Navigating from Smart Suggestion to PP or Template Entry

As a user,
I want the Planned Payment entry screen and Template entry screen to be pre-filled with suggestion data when I navigate to them from Smart Suggestion,
So that I do not have to re-enter data I already reviewed on the suggestion screen.

**Acceptance Criteria:**

**Given** I am on the Smart Suggestion screen and tap "Save as Planned Payment" or "Save as Template"
**When** the destination entry screen opens
**Then** all available suggestion fields (payee, amount, category, recurrence frequency, note) are pre-filled in the corresponding input fields
**And** the navigation arguments carrying the suggestion data are correctly serialised and deserialised across the nav graph
**And** the implicit background save that previously occurred during the crash path (Story 7.2) is removed — no template or planned payment is created without the user completing the entry screen
**And** the user can edit any pre-filled field before confirming the save.

### Story 7.4: Quick Add Sheet — Template-First Selection Step

As a user,
I want the Quick Add sheet to show my saved templates first when I have any,
So that I can log a recurring transaction in one tap instead of re-selecting category and payee every time.

**Acceptance Criteria:**

**Given** I open the Quick Add bottom sheet and I have at least one saved transaction template
**When** the sheet renders
**Then** it opens on a "Select Template" step that lists all available templates (name, category, amount)
**And** I can search or scroll through templates
**And** selecting a template pre-fills the Quick Add form and proceeds to the confirmation/amount step
**And** a visible toggle or "Skip" action lets me bypass template selection and go directly to category selection
**Given** I open the Quick Add sheet and I have no saved templates
**When** the sheet renders
**Then** it opens directly on the category selection step (current behaviour), with no template step shown.
