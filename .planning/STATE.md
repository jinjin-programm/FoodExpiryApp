---
gsd_state_version: 1.0
milestone: v2.0
milestone_name: AI Vision Engine Overhaul
status: executing
last_updated: "2026-04-18T03:10:00.000Z"
progress:
  total_phases: 6
  completed_phases: 4
  total_plans: 21
  completed_plans: 20
  percent: 95
---

# Project State: FoodExpiryApp

**Status:** Executing Phase 8 (YOLO Hardening) — 2/3 plans complete

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-08)

**Core value:** Let a new user add their first food item within 30 seconds and reduce the mental burden of food management.
**Current focus:** Phase 8 YOLO Hardening + dual-provider inference (Ollama + LM Studio)

## Current Position

Phase: 08 (yolo-hardening) — 2/3 plans done
Migration: Ollama + LM Studio dual provider — COMPLETE

- **Milestone:** v2.0 AI Vision Engine Overhaul
- **Active Task:** Phase 8 Plan 03 (tests + ViewModel detections)
- **Status:** Ollama migration done, LM Studio provider added, thinking mode disabled, build passing

### v2.0 Progress

```
Phase 4: Foundation       [██████████] 100% ✅
Phase 5: Engine           [██████████] 100% ✅
Phase 6: Detection        [██████████] 100% ✅
Phase 7: Scan UI Overhaul [██████████] 100% ✅
Phase 8: YOLO Hardening   [██████░░░░] 67%  ← 2/3 plans done
Phase 9: Verification     [          ] 0%
```

## Milestone History

### v1.0 MVP (Shipped: 2026-04-08)

- **Phases:** 3/3 complete
- **Plans:** 11/11 complete
- **Requirements:** 14/14 validated
- **UAT:** 14/14 passed
- **Duration:** ~58 days

### v2.0 AI Vision Engine Overhaul (In Progress)

- **Phases:** 4/6 complete (Phases 4-7)
- **Plans:** 18/25 complete
- **Requirements:** 24/27 validated

#### Major Events

| Date | Event | Impact |
|------|-------|--------|
| 2026-04-10 | MNN LLM first successful food identification on device | Phase 5 complete |
| 2026-04-12 | Snapdragon 8 Gen 3 garbage output fix | Cross-device compatibility |
| **2026-04-15** | **Decision: migrate to remote Ollama API** | **Architecture change** |

#### Ollama Migration Rationale

1. **Hallucination**: Qwen3.5-2B-VL via MNN produces unreliable results across devices
2. **Deployment complexity**: ~1.2GB model download, native .so files, JNI bridges, ABI matching
3. **Device compatibility**: Works on some devices, breaks on others
4. **Remote server benefits**: Larger model (qwen3.5:9b), structured JSON Schema output, no local resources needed

## Accumulated Context

### Key Decisions

| Decision | Rationale | Status |
|----------|-----------|--------|
| MNN Chat official component | Avoid custom inference layer, battle-tested | ✅ Done → Now migrating away |
| Qwen3.5-2B-MNN model | 4-bit quantized, lightweight for mobile | ✅ Working → Replaced by remote qwen3.5:9b |
| Qwen3.5-0.8B-MNN evaluated | Too weak for visual food classification | ❌ Rejected |
| YOLO + LLM (skip CLIP) | CLIP overlaps with LLM capability | ✅ Done |
| Dynamic model download from HF | Reduce APK size by ~40MB | ✅ Done → No longer needed |
| Sequential YOLO→LLM processing | Prevent OOM on <6GB devices | ✅ Done |
| Complete llama.cpp removal | libllm.so naming collision | ✅ Done |
| **Remote Ollama API** | Solve hallucination, simplify deployment, use larger model | ✅ Done |
| **Ollama + JSON Schema** | Force structured output to prevent hallucination | ✅ Done |
| **Cloudflare Tunnel** | Public access to home Ollama server | ✅ Done |
| **Ollama think:false** | Disable thinking mode for faster inference | ✅ Done (~16% faster) |
| **LM Studio provider** | OpenAI-compatible alternative, faster inference | ✅ Done |
| **Dual-provider architecture** | User can switch Ollama/LM Studio in settings | ✅ Done |

### Critical Constraints

- Inference requires network connectivity (remote server)
- DetectionPipeline uses FoodVisionClient interface — supports Ollama + LM Studio
- Provider selection persisted in DataStore, switchable at runtime
- MNN code preserved but DI bindings commented out

### References

- Ollama API: https://github.com/ollama/ollama/blob/main/docs/api.md
- LM Studio API: OpenAI-compatible (/v1/chat/completions)
- Models: qwen3.5:9b (Ollama), qwen/qwen3.5-9b (LM Studio)
- Server: User's home PC (AMD RX 9070 XT 16GB + iGPU)
- Network: Cloudflare Tunnel → public domain → localhost:11434 (Ollama) / :1234 (LM Studio)

### Blockers

None — build passing, both providers functional.

### TODO

- [ ] Phase 8 Plan 03: Unit tests + ViewModel detections + device verification
- [ ] Phase 9: Verification & Artifact Cleanup

### Quick Tasks Completed

| # | Description | Date | Directory |
|---|-------------|------|-----------|
| 260418-lmstudio | Add LM Studio dual-provider support + Ollama think:false + debug logging | 2026-04-18 | See git log |
| 260415-ollama | Migrate from local MNN inference to remote Ollama API server | 2026-04-15 | [ollama-remote-migration](./debug/ollama-remote-migration.md) |

## Session Continuity

**Last session:** 2026-04-18T03:10:00.000Z

- **LM Studio Dual Provider + Ollama Improvements**
  - Added LM Studio (OpenAI-compatible) as second inference provider
  - Created `FoodVisionClient` interface — abstracts Ollama/LM Studio behind common API
  - Created `ProviderConfig` — persisted provider selection (Ollama / LM Studio)
  - Created `data/remote/lmstudio/` — full LM Studio client stack (ApiClient, VisionClient, ServerConfig, DTOs)
  - Updated Settings Dialog — toggle between Ollama and LM Studio, separate configs
  - Ollama: disabled thinking mode via `think: false` in DTO
  - Ollama: added detailed debug logging (timing, tokens/sec, raw response)
  - Ollama: fixed JSON parsing to handle markdown code blocks and arrays
  - Updated DetectionPipeline to use FoodVisionClient (no longer hardcoded to Ollama)
  - Updated ChatViewModel for dual-provider support
  - **Build status:** PASSING ✅
  - **Ollama tested:** Strawberry identified correctly in ~12.6s (cold) / ~900ms (warm)
  - **LM Studio server:** Running on port 1234, qwen/qwen3.5-9b loaded, tunnel ready
  - **LM Studio tunnel:** https://installation-freeze-companion-butterfly.trycloudflare.com

**Next action:** Phase 8 Plan 03 — unit tests + ViewModel detections + device verification
