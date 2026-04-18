---
status: in_progress
trigger: "MNN local vision model has hallucination issues and is difficult to deploy on mobile"
created: 2026-04-15T12:00:00Z
updated: 2026-04-15T14:30:00Z
---

## Current Focus
hypothesis: Replace MNN local inference with remote Ollama API to solve hallucination issues and simplify deployment
next_action: Fix DI dependency chain — DetectionPipeline still depends on MnnLlmEngine

## Background

### Problem Statement
1. **Hallucination Issues**: Qwen3.5-2B-VL via MNN produces unreliable results ("4444...", wrong foods, chat template tokens)
2. **Deployment Complexity**: Local model requires ~1.2GB download, native .so files, JNI bridges, ABI matching
3. **Device Compatibility**: Works on some devices (S10+), breaks on others (MTK Dimensity, Snapdragon 8 Gen 3)

### Solution
Move vision analysis to a remote Ollama server running `qwen3.5:9b` (or other vision-capable model), accessed via HTTP API.

## Architecture Change

### Before (MNN Local)
```
VisionScanFragment → IdentifyFoodUseCase → LlmInferenceRepository → MnnLlmEngine (JNI) → Native MNN
```

### After (Ollama Remote)
```
VisionScanFragment → IdentifyFoodUseCase → LlmInferenceRepository → OllamaVisionClient → HTTP → Remote Ollama Server
```

## Implementation Progress

### Completed
| Component | File | Status |
|-----------|------|--------|
| Ollama DTO | `data/remote/ollama/dto/OllamaDto.kt` | ✅ Created |
| Ollama API Client | `data/remote/ollama/OllamaApiClient.kt` | ✅ Created |
| Ollama Vision Client | `data/remote/ollama/OllamaVisionClient.kt` | ✅ Created |
| Server Config | `data/remote/ollama/OllamaServerConfig.kt` | ✅ Created |
| Repository Impl | `data/repository/LlmInferenceRepositoryImpl.kt` | ✅ Modified |
| DI Module | `di/InferenceModule.kt` | ✅ Modified |
| DataStore Module | `di/DataStoreModule.kt` | ✅ Modified |
| VisionScanFragment | `presentation/ui/vision/VisionScanFragment.kt` | ✅ Modified |
| ChatViewModel | `presentation/ui/chat/ChatViewModel.kt` | ✅ Modified |
| ChatFragment | `presentation/ui/chat/ChatFragment.kt` | ✅ Modified |
| Settings Dialog Layout | `res/layout/dialog_ollama_settings.xml` | ✅ Created |
| Settings Dialog | `presentation/ui/vision/OllamaSettingsDialog.kt` | ✅ Created |

### Blocked
| Component | Issue | Resolution |
|-----------|-------|------------|
| Build | `DetectionPipeline` depends on `MnnLlmEngine` for YOLO+LLM batch processing | Need to update DetectionPipeline to use OllamaVisionClient |
| DI | `YoloDetectionRepositoryImpl` cannot be constructed | DetectionPipeline dependency chain broken |

## Key Design Decisions

1. **Dynamic URL**: OkHttp client with runtime-configurable baseUrl (via DataStore)
2. **JSON Schema Output**: Ollama `format` parameter enforces structured output → solves hallucination
3. **Image Compression**: Bitmap resized to 512px max, JPEG 85% quality, Base64 encoded
4. **Timeouts**: 30s connect, 120s read (public network latency + model inference)
5. **MNN Code Preserved**: Commented out in DI, files retained for potential future use

## Server Setup Requirements

User needs to:
1. Install Ollama on their computer
2. Pull vision model: `ollama pull qwen3.5:9b`
3. Expose via Cloudflare Tunnel (or use LAN IP)
4. Configure URL in app settings

## Remaining Work

1. **Fix DetectionPipeline** — Update to use OllamaVisionClient instead of MnnLlmEngine
2. **Test Build** — Verify compilation succeeds
3. **End-to-End Test** — Verify food identification works via remote API
4. **Update MNN-related docs** — Mark as deprecated/migrated

## Files Changed

### New Files
- `app/src/main/java/com/example/foodexpiryapp/data/remote/ollama/dto/OllamaDto.kt`
- `app/src/main/java/com/example/foodexpiryapp/data/remote/ollama/OllamaApiService.kt`
- `app/src/main/java/com/example/foodexpiryapp/data/remote/ollama/OllamaApiClient.kt`
- `app/src/main/java/com/example/foodexpiryapp/data/remote/ollama/OllamaVisionClient.kt`
- `app/src/main/java/com/example/foodexpiryapp/data/remote/ollama/OllamaServerConfig.kt`
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/vision/OllamaSettingsDialog.kt`
- `app/src/main/res/layout/dialog_ollama_settings.xml`

### Modified Files
- `app/src/main/java/com/example/foodexpiryapp/data/repository/LlmInferenceRepositoryImpl.kt`
- `app/src/main/java/com/example/foodexpiryapp/di/InferenceModule.kt`
- `app/src/main/java/com/example/foodexpiryapp/di/DataStoreModule.kt`
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/vision/VisionScanFragment.kt`
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/chat/ChatViewModel.kt`
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/chat/ChatFragment.kt`

### Removed
- `app/app/` — Duplicate module causing compilation conflicts

## Related Debug Sessions
- `.planning/debug/qwen-vision-hallucination-4444.md` — Previous MNN hallucination investigation
