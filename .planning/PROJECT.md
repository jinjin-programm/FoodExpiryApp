# FoodExpiryApp

## What This Is
A food rescue assistant Android app that helps users scan, record, track, and use ingredients before they go bad. Uses photo scanning (OCR + barcode), expiry-aware recipe suggestions, meal planning, and shopping list management.

## Core Value
Let a new user add their first food item within 30 seconds and reduce the mental burden of food management.

## Constraints & Context
- **Preserve Visual Identity:** Keep the existing visual language (card layouts, rounded corners, soft shadows). Warm, healthy, friendly default style.
- **Color Semantics:** Red (high risk), Orange (attention needed), Green (safe), Blue (guidance), Gray (secondary).
- **Tech Stack:** Kotlin, Android (Material Design 3), Room, Hilt, WorkManager, Google ML Kit, CameraX
- **Architecture:** MVVM with Clean Architecture layers (domain/data/presentation)

## Requirements

### Validated (v1.0)
- ✓ INVN-01: "Get Started" entry point on inventory screen — v1.0
- ✓ INVN-02: Photo Scan with OCR confirmation flow — v1.0
- ✓ INVN-03: Expiring Soon section sorted by risk level — v1.0
- ✓ INVN-04: Tab-aware inventory search — v1.0
- ✓ INVN-05: Quick actions dropdown — v1.0
- ✓ DATA-01: Unified Food Data Model — v1.0
- ✓ RECP-01: Expiry-weighted recipe recommendations — v1.0
- ✓ RECP-02: Recipe performance indicators (items rescued) — v1.0
- ✓ PLAN-01: Recipe assignment to date/meal slots — v1.0
- ✓ SHOP-01: Shopping list templates (3 built-in) — v1.0
- ✓ SHOP-02: Inventory status on shopping items — v1.0
- ✓ PROF-01: Notification Settings at top of Profile — v1.0
- ✓ PROF-02: Privacy & Data usage explanations — v1.0
- ✓ PROF-03: Google Sign-In optional — v1.0

### Active
*(None — define next milestone for new requirements)*

### Out of Scope
- **Full App Redesign:** Visual identity preserved, UX optimization only.
- **Complex 2-way Planner/Recipe Sync:** Deferred to avoid complexity.
- **Flash Mode in Scanning:** Removed if not adding value.

## Key Decisions
| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Photo scan as primary input | "Instant scan" confusing; photo scan clearer | ✓ Shipped |
| Unified Food Data Model | Single source of truth across modules | ✓ Shipped |
| Google Sign-In optional | Must not block first-time use | ✓ Shipped |
| OCR confirmation before save | App should not silently save uncertain data | ✓ Shipped |
| WorkManager for notifications | Reliable daily scheduling with boot recovery | ✓ Shipped |
| Horizontal template carousel | Compact template display in Shopping | ✓ Shipped (with ViewPager2 scroll fix) |

## Context
- **Shipped:** v1.0 MVP on 2026-04-08
- **3 phases, 11 plans, 14/14 requirements validated**
- **UAT:** 14/14 tests passed across all phases
- **Timeline:** ~58 days (Feb 10 → Apr 8, 2026)

## Evolution
This document evolves at phase transitions and milestone boundaries.

---
*Last updated: 2026-04-08 after v1.0 milestone*
