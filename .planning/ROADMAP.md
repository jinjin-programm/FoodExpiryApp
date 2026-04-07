# Proposed Roadmap

**3 phases** | **14 requirements mapped** | All v1 requirements covered ✓

| # | Phase | Goal | Requirements | Success Criteria |
|---|-------|------|--------------|------------------|
| 1 | Main Flow First | Establish clear entry point, photo scan, and visible expiry risk | INVN-01, INVN-02, INVN-03, INVN-04, INVN-05, DATA-01 | 4 |
| 2 | Connect Modules | Connect unified data to recipes and meal planning | RECP-01, RECP-02, PLAN-01 | 3 |
| 3 | Polish & Trust | Implement shopping templates, refine profile, clarify privacy | SHOP-01, SHOP-02, PROF-01, PROF-02, PROF-03 | 4 |

## Phase Details

### Phase 1: Main Flow First
**Goal:** Let a new user successfully add the first food item in under 30 seconds with proper risk visibility.
**Requirements:** INVN-01, INVN-02, INVN-03, INVN-04, INVN-05, DATA-01
**Plans:** 6 plans
- [x] 01-01-PLAN.md — Unified Food Data Model
- [x] 01-02-PLAN.md — Inventory Empty State & Quick Actions
- [x] 01-03-PLAN.md — Expiring Soon Display & Tab Search
- [x] 01-04-PLAN.md — Photo Scan OCR Confirmation Flow
- [x] 01-05-PLAN.md — Fix Inventory Architecture & UI
- [x] 01-06-PLAN.md — Implement OCR Confirmation Bottom Sheet
**Success criteria:**
1. "Get Started" area is visible on empty state on the Home/Inventory screen.
2. User can take a photo, review pre-filled OCR data, and confirm to save the item to the unified data model.
3. "Expiring Soon" section sorts items correctly by risk level (expired, 1, 2-3, 4-7 days).
4. Search field accurately filters only the active tab.

### Phase 2: Connect Modules
**Goal:** Make the app explain itself through connected data (recipes and planning).
**Requirements:** RECP-01, RECP-02, PLAN-01
**Plans:** 2 plans
- [x] 02-01-PLAN.md — Enhance Recipes with Expiry-Aware Scoring & Performance Indicators
- [x] 02-02-PLAN.md — Planner Expiry-Aware Recipe Picker & Slot Assignment
**Success criteria:**
1. Recipes module prominently suggests meals using ingredients expiring within 7 days.
2. Recipe cards display realistic performance indicators ("ingredients rescued").
3. User can assign a chosen recipe to a specific date and meal slot in the Planner.

### Phase 3: Polish & Trust
**Goal:** Finalize the replenishment loop and build user trust through transparency.
**Requirements:** SHOP-01, SHOP-02, PROF-01, PROF-02, PROF-03
**Success criteria:**
1. Shopping module provides at least 3 common templates for quick addition.
2. Shopping items accurately reflect inventory status (items added/remaining).
3. Notification settings are prominently accessible near the top of the Profile.
4. Profile contains privacy copy that explains data usage, AI behavior, and keeps Google Login optional.
