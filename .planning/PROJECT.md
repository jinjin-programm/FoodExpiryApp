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

## Current Milestone: v2.0 AI Vision Engine Overhaul

**Goal:** Transform scanning with YOLO+LLM multi-object detection and switch to MNN inference with dynamic model download.

**Target features:**
- Remove top title bar on all pages, keep only back button
- Horizontal (landscape) photo frame for scanning
- Capture animation → loading screen with AI inference progress
- Barcode scan: manual capture button, remove auto-scan/flash/close
- MNN inference engine replacing GGUF (official MNN Chat component)
- Dynamic model download from HuggingFace (Qwen3.5-2B-MNN)
- YOLO+LLM food detection: new scan tab for multi-object detection
- Batch processing with structured JSON output

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

### Validated (v2.0)
- ✓ SAFE-01: Full project backup before v2.0 — v2.0 Phase 4
- ✓ SAFE-02: Git tag v1.1.0-backup — v2.0 Phase 4
- ✓ MNN-01: MNN v3.5.0 AAR integrated — v2.0 Phase 4
- ✓ MNN-02: llama.cpp completely removed — v2.0 Phase 4

### Active
- MNN-03: Qwen3.5-2B-MNN model inference via MNN engine
- MNN-04: Structured JSON output from LLM
- MNN-05: Model lifecycle manager (mutual exclusion)
- MNN-06: Hilt singleton wrapper for MNN engine
- DL-01 through DL-07: Dynamic model download from HuggingFace
- YOLO-01 through YOLO-08: YOLO+LLM batch detection pipeline
- UI-01 through UI-08: Scan UI overhaul

### Out of Scope
- **Full App Redesign:** Visual identity preserved, UX optimization only.
- **Complex 2-way Planner/Recipe Sync:** Deferred to avoid complexity.
- **CLIP classifier:** YOLO detection + LLM classification is sufficient (no CLIP training).
- **GGUF model support:** Replaced entirely by MNN inference.

## Key Decisions
| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Photo scan as primary input | "Instant scan" confusing; photo scan clearer | ✓ Shipped |
| Unified Food Data Model | Single source of truth across modules | ✓ Shipped |
| Google Sign-In optional | Must not block first-time use | ✓ Shipped |
| OCR confirmation before save | App should not silently save uncertain data | ✓ Shipped |
| WorkManager for notifications | Reliable daily scheduling with boot recovery | ✓ Shipped |
| Horizontal template carousel | Compact template display in Shopping | ✓ Shipped (with ViewPager2 scroll fix) |
| MNN Chat official component | Avoid custom inference layer, use battle-tested engine | — Pending |
| Qwen3.5-2B-MNN model | 4-bit quantized, lightweight enough for mobile | — Pending |
| YOLO + LLM (skip CLIP) | CLIP classifier overlaps with LLM capability | — Pending |
| Dynamic model download from HF | Reduce APK size, model not bundled | — Pending |

## Context
- **Shipped:** v1.0 MVP on 2026-04-08, v1.1.0 UX improvements on 2026-04-08
- **v1.0:** 3 phases, 11 plans, 14/14 requirements validated
- **v1.1.0:** UX optimizations, recipe improvements, planning updates
- **UAT:** 14/14 tests passed across all phases
- **MNN Reference:** https://github.com/alibaba/MNN (v3.5.0, supports Qwen3.5)
- **Model:** taobao-mnn/Qwen3.5-2B-MNN (4-bit quantized)
- **YOLO Reference:** https://github.com/tishuo-wang/YOLO_CLIP_targetDetection (YOLO detection pattern)

## Evolution
This document evolves at phase transitions and milestone boundaries.

---
*Last updated: 2026-04-08 after Phase 4 Foundation completion*
