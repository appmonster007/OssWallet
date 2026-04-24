# OssWallet Documentation

This directory contains all project documentation organized by category.

## Directory Structure

```
docs/
├── architecture/          # Architecture plans and designs
├── implementation/        # Completed implementation summaries
├── plans/                # Future implementation plans
├── reference/            # Reference guides and technical documentation
├── guidelines/           # Best practices and coding guidelines
└── maintenance/          # Maintenance tasks and checklists
```

## Architecture

Design documents for system architecture and major components.

- **[Card Architecture Roadmap](architecture/card-architecture-roadmap.md)** - Roadmap for card system improvements, including timeline filtering fixes and architecture cleanup
- **[Filtering Architecture](architecture/filtering-architecture.md)** - Unified filtering system using FilterStack and UnifiedFilter
- **[Filtering Optimization Proposal](architecture/filtering-optimization-proposal.md)** - Proposal to simplify the filter system
- **[Navigation Architecture](architecture/navigation-architecture.md)** - Context navigation architecture using FilterStack

## Implementation

Summaries of completed features and implementations.

- **[Color System Implementation](implementation/color-system-implementation.md)** - Material 3 expressive color revision implementation
- **[Multi-Transaction Linking](implementation/multi-transaction-linking.md)** - Many-to-many transaction linking feature
- **[Navigation Bar Update](implementation/navigation-bar-update.md)** - Update to standard Material 3 NavigationBar
- **[Filtering Implementation](implementation/filtering-implementation.md)** - Phase 1 completion of unified filtering system
- **[Filter Sheet Improvements](implementation/filter-sheet-improvements.md)** - Filter sheet UX improvements (all completed)

## Plans

Future implementation plans for features and improvements.

- **[Dashboard UI Refactor](plans/dashboard-ui-refactor.md)** - Plan to refactor Dashboard navigation and FAB for Material 3
- **[Records Reusability](plans/records-reusability.md)** - Plan to make TransactionListScreen reusable with pre-applied filters
- **[Transaction Status Field](plans/transaction-status-field.md)** - Plan to add status field to transactions
- **[Transaction Swipe Actions](plans/transaction-swipe-actions.md)** - Plan to add swipe gestures for quick transaction actions
- **[Calculator Numpad Migration](plans/calculator-numpad-migration.md)** - Plan to transform numpads into full calculators
- **[Calculator Refactoring](plans/calculator-refactoring.md)** - Plan to recreate robust calculator numpad
- **[Analytics Cards](plans/analytics-cards.md)** - Plan to implement analytics cards (Category Breakdown, Timeline Trajectory, Treemap)
- **[Card Timeline Navigation](plans/card-timeline-navigation.md)** - Plan to update cards to pass timeline range during navigation
- **[Multi-Transaction Linking](plans/multi-transaction-linking.md)** - Original plan for multi-transaction linking feature

## Reference

Technical reference guides and documentation.

- **[Color System Guide](reference/color-system-guide.md)** - Comprehensive guide to Material 3 expressive colors (consolidated)
- **[Material 3 Expressive Color Revision](reference/material3-expressive-color-revision.md)** - Detailed documentation of color system revision
- **[Account Metrics Logic](reference/account-metrics-logic.md)** - Documentation of account metrics calculation from seed data

## Guidelines

Best practices and coding guidelines for development.

- **[MVVM Best Practices](guidelines/mvvm-best-practices.md)** - Ideal architecture and patterns for OssWallet
- **[MVVM Bad Practices](guidelines/mvvm-bad-practices.md)** - Anti-patterns to avoid in Android MVVM projects
- **[Todo Widget Best Practices](guidelines/todo-widget-best-practices.md)** - Best practices for high-performance Todo widgets
- **[Todo Widget Bad Practices](guidelines/todo-widget-bad-practices.md)** - Bad practices to avoid when developing Todo widgets
- **[Widget Best Practices](guidelines/widget-best-practices.md)** - Widget development best practices
- **[Widget Bad Practices](guidelines/widget-bad-practices.md)** - Widget development anti-patterns

## Maintenance

Maintenance tasks, checklists, and pending work.

- **[TODO](maintenance/todo.md)** - Pending tasks and completed phases for card computation refactoring
- **[Pending Optimizations](maintenance/pending-optimizations.md)** - Deferred optimizations for build, assets, and UI/UX
- **[Seeding Removal Checklist](maintenance/seeding-removal-checklist.md)** - Checklist for removing database seeding functionality
- **[Agents](maintenance/agents.md)** - Agent rules and configuration

## Documentation Reorganization (April 2026)

The documentation was reorganized to improve discoverability and reduce duplication:

### Consolidated Files
- **Color System:** 6 files → 1 comprehensive guide + 1 revision history
- **Card Architecture:** 6 files → 1 roadmap document
- **MVVM Guidelines:** Removed duplicates from guidelines/ directory

### New Structure
- Moved completed implementations to `implementation/`
- Moved future plans to `plans/`
- Moved architecture docs to `architecture/`
- Moved reference guides to `reference/`
- Moved best practices to `guidelines/`
- Moved maintenance tasks to `maintenance/`

### Removed Files
- Duplicate MVVM documentation from guidelines/
- Consolidated color system files
- Consolidated card architecture files
- Empty guidelines/ directory

## Quick Links

- **For new developers:** Start with [MVVM Best Practices](guidelines/mvvm-best-practices.md) and [Color System Guide](reference/color-system-guide.md)
- **For architecture decisions:** See [Architecture](architecture/) directory
- **For current work:** Check [TODO](maintenance/todo.md) and [Plans](plans/)
- **For completed work:** See [Implementation](implementation/)
