---
status: draft
---
# Phase 1: Main Flow First - UI-SPEC

## 1. Context & Design System
- **Phase:** 1 - Main Flow First
- **Platform:** Android (XML/View system)
- **Design System:** Custom Material-like (detected via `colors.xml`)
- **Theme:** Nature/Earth tones (Primary: #4D644F)

## 2. Layout & Spacing
- **Base Scale:** 8-point system (4dp, 8dp, 16dp, 24dp, 32dp, 48dp, 64dp).
- **Exceptions:** 12dp allowed for tight nested padding inside food cards.
- **Card Layout:** Standard Material cards with 8dp corner radius and 2dp elevation.

## 3. Typography
- **Headings (Titles):** 22sp (e.g., "Expiring Soon", "Get Started"), Medium weight (600).
- **Subheadings/Item Names:** 16sp, Medium weight (600).
- **Body/Metadata:** 14sp, Regular weight (400) - used for expiry dates, quantities.
- **Micro-copy (Labels):** 12sp, Regular weight (400) - used for risk badges.
- **Line Height:** 1.5 for body, 1.2 for headings (standard Android text appearances).

## 4. Color Strategy
- **Dominant (60%):** `surface` (#FBF9F5) and `surface_container` variants for backgrounds and cards.
- **Secondary (30%):** `primary_container` (#8DA68E) and `surface_variant` (#E4E2DE) for standard inactive elements, tabs, search bars.
- **Accent (10%):** `primary` (#4D644F) reserved ONLY for primary CTAs ("Get Started", "Scan Photo", FAB), active tab indicators, and confirmed states.
- **Semantic/Risk (Expiring Soon colors):**
  - **Expired:** `error` (#BA1A1A) text on `error_container` (#FFDAD6) badge.
  - **1 Day (High Risk):** Deep Orange / Amber text on light orange background.
  - **2-3 Days (Medium Risk):** Warning Yellow / Gold.
  - **4-7 Days (Low Risk):** Primary Green / Safe variant.

## 5. Copywriting & Content
- **Primary CTA Label:** "Scan Item" or "Add Item"
- **Empty State Copy:** "Your inventory is empty. Start by scanning a food item to keep track of your groceries."
- **Empty Search Copy:** "No items found matching your search."
- **Error State Copy:** "We couldn't read the image clearly. Please try scanning again or enter manually."
- **Destructive Actions:** 
  - "Delete Item"
  - **Confirmation Approach:** A standard dialog with "Cancel" and "Delete" (in error color) is required before deletion.

## 6. Component Specifics (Phase 1 Focus)
- **"Get Started" Block:** Centered on empty inventory. Prominent illustration/icon, title text, body copy, and a large `primary` button ("Scan Item").
- **"Photo Scan" OCR Flow:** 
  - Loading State: "Analyzing image..." with a determinate/indeterminate circular progress.
  - Confirmation Modal: Bottom sheet or dialog showing pre-filled OCR data (Name, Date, Category) with editable fields and "Save to Inventory" CTA.
- **"Expiring Soon" Section:** Horizontal scrolling or top-pinned list. Cards must prominently display the Risk Badge (Semantic Color) and Days Remaining.
- **Tab-specific Search:** Search bar (`surface_variant` background, 16dp height) placed below tabs. Placeholder: "Search in [Tab Name]...".
- **Quick Actions Dropdown:** Speed dial FAB or top-right menu.
  - Options: "Photo Scan" (Icon: Camera), "Manual Entry" (Icon: Edit), "View Expiring" (Icon: Warning), "View Analysis" (Icon: Bar Chart).

## 7. Registries & Third-Party
- **Tool:** Android XML / Material Components
- **Third-Party Libraries:** None new required. Existing AndroidX components.
