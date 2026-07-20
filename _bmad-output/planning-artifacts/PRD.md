# OssWallet — Product Requirements Document (Consolidated)
**Status:** DRAFT · **Updated:** 2026-06-25

## 1. Product Vision
To transform OssWallet into a high-performance, architecturally sound, and feature-complete financial management suite across Android and Web.

## 2. Core Strategic Pillars

### P1: Architectural Integrity (Remediation)
Resolve historical technical debt to enable safe scaling and feature isolation.
- **FR1 (Modularization):** Decouple the monolithic `:app` module into `:core:domain`, `:core:data`, and feature-specific modules (`:feature:web`, `:feature:dashboard`).
- **FR2 (MVI Mandate):** Implement strict Unidirectional Data Flow for all screens to ensure deterministic state and prevent logic leakage.
- **FR3 (Serialization):** Standardize all networking and persistence serialization on `kotlinx.serialization`.
- **FR4 (Domain Purity):** Decouple business logic from database-specific query services (e.g., `CardAggregationService`).

### P2: Web UI Feature Parity
Bridge the gap between the Android native experience and the embedded Ktor web interface.
- **FR5 (Planned Payments Page):** Implement a full list view for planned payments with next-due dates and overdue highlighting on the Web UI.
- **FR6 (Transaction Actions):** Add "Mark as Paid" and "Skip Occurrence" actions to the Web UI using existing backend endpoints.
- **FR7 (GPS/Location):** Enable GPS/Location field input and display on the Web transaction forms and list.
- **FR8 (Dashboard Drill-downs):** Add click actions to Web dashboard chart cards for detailed analytics navigation.

### P3: Performance & UX (Stutter-Free Dashboard)
Eliminate UI jank and provide a "premium instrument" feel on the main dashboard.
- **FR9 (Dashboard RCA & Optimization):** Conduct a Root Cause Analysis (RCA) on dashboard scrolling/rendering and implement optimizations (e.g., lazy loading, composition skipping, or improved aggregation caching).
- **FR10 (Fluid Transitions):** Ensure all 3D card flips and glassmorphism effects maintain a consistent 60+ FPS.
- **FR11 (Design Consolidation):** Audit and refactor all existing screens (specifically the Settings screen) to strictly adhere to the "H — Editorial Instrument" design system, removing inconsistent or "over-designed" elements.

## 3. Non-Functional Requirements
- **NFR1 (Security):** Maintain 256-bit AES encryption (SQLCipher) across all data layers.
- **NFR2 (Reliability):** 100% test coverage for all new or refactored Domain Use Cases.
- **NFR3 (Performance):** Dashboard initial load must complete in under 500ms; scrolling must be stutter-free.
- **NFR4 (Concurrency):** All async operations must use injected dispatchers (no hardcoded IO/Main).
