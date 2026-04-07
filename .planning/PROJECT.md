# FoodExpiryApp UX Optimization

## Core Value
A food rescue assistant that helps users scan, record, track, and use ingredients before they go bad, without redesigning the app's visual style. The goal is to let a new user add their first food item within 30 seconds and reduce the mental burden of food management.

## Constraints & Context
- **Preserve Visual Identity:** Keep the existing visual language (card layouts, rounded corners, soft shadows). Use a healthy, warm, friendly default style.
- **Color Semantics:** Red (high risk), Orange (attention needed), Green (safe), Blue (guidance), Gray (secondary).
- **Brownfield project:** Existing architecture and codebase mapping has already been completed and committed to `.planning/codebase`.

## Key Decisions
| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Photo scan as primary | "Instant scan" terminology is confusing; photo scan is easier to understand | Pending |
| Unified Food Data Model | Users should not enter the same food twice across modules; single source of truth | Pending |
| Google Sign-In optional | Should not block first-time use, cloud sync is a secondary decision | Pending |
| Ask to verify AI data | The app should not silently save uncertain OCR results without user confirmation | Pending |

## Requirements
*(See REQUIREMENTS.md)*

## Evolution
This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-08 after initialization*