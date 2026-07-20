---
name: project-web-ui-gaps
description: "Feature parity gaps between the Android app and the Ktor web UI — what's missing, partial, or done. Full tracker at docs/web-ui-feature-parity.md"
metadata: 
  node_type: memory
  type: project
  originSessionId: 557eeb87-8d98-4223-b314-ae423f251fbb
---

Full tracker is at `docs/web-ui-feature-parity.md` in the repo. Do not duplicate entries here — read that file for the current state.

**Why:** Web UI is a companion browser interface served by the embedded Ktor server. It should eventually reach feature parity with the Android app for core workflows.

**How to apply:** When adding any web UI feature, check the tracker first. When completing a feature, update the ✅/🔲/⚠️ status in the tracker doc.

## Critical gaps (as of 2026-05-24)
- Transaction detail view — missing entirely
- Templates — missing entirely (list, create, apply to transaction form)
- Payment method field — missing from create/edit form (filter only)
- Warranty fields — missing from transaction form
- Planned payments full page — card only; no mark-as-paid / skip
- Split transactions — missing entirely

## High priority gaps
- Account / Budget / Loan detail pages — list only, no drill-down
- Linked transactions — missing
- Settings writability — all settings read-only in web
- Account initial balance on create — missing
- Transaction clone — missing
- GPS / location fields — missing

## What IS complete in web UI
- All 14 dashboard cards rendered with full data
- Per-card filter sheet (all dimensions, server-persisted via PUT /dashboard/cards/{cardId})
- Card add / remove / reorder
- Transaction list + create + edit + delete
- Accounts CRUD + reorder
- Budgets CRUD
- Loans CRUD + repayment log/delete
- Planned payments CRUD (no mark-as-paid)
- Categories CRUD (no icon picker, no reorder)
- Tags CRUD (no merge)
- Settings read-only view

[[project_roadmap]]
