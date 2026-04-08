# Feature Landscape: MNN-Powered AI Scanning

**Domain:** Android Food Expiry App - AI Vision Engine Overhaul  
**Researched:** 2026-04-08  
**Confidence:** HIGH (based on official MNN docs, HuggingFace model specs, and existing codebase analysis)

---

## Executive Summary

This research covers the feature landscape for adding MNN LLM inference, YOLO multi-object detection, dynamic model download, and scan UI overhaul to an existing Android food expiry app. The existing codebase already has functional implementations of GGUF-based LLM scanning, YOLO detection with TensorFlow Lite, and CameraX-based scanning flows. The v2.0 milestone focuses on replacing the inference engine with MNN for better performance and adding multi-object detection workflows.

---

## Table Stakes Features

Features users expect in an AI-powered food scanning app. Missing = product feels incomplete.

### 1. Multi-Object Food Detection
**Why Expected:** Modern food tracking apps should recognize multiple items in a single photo.  
**Complexity:** HIGH  
**Status:** Partially implemented (single-object YOLO detection exists)  
**Implementation Notes:**
- Current `YoloDetector.kt` already returns `List<DetectionResult>` (line 318-346)
- Need to add batch processing pipeline: `YOLO detect → crop regions → send to LLM`
- Detection confidence threshold: 0.35 (35%) - existing in code
- Display threshold: 0.40 (40%) - existing in `YoloScanFragment.kt` line 234

**User Experience:**
1. User points camera at multiple food items
2. Bounding boxes appear around all detected items (existing `DetectionOverlayView`)
3. User taps "Use Detection" to select items
4. Batch processing shows progress: "Analyzing 3 items..."
5. Results appear as structured list with food name + expiry date

---

### 2. Capture Animation + Loading Screen
**Why Expected:** Instant feedback on capture, clear AI processing state.  
**Complexity:** MEDIUM  
**Status:** Partially implemented  
**Implementation Notes:**
- Existing `LlmScanFragment` has status indicator and progress bar
- Existing `VisionScanFragment` has progress ticker with elapsed time (lines 377-397)
- Need horizontal (landscape) frame overlay for camera preview

**User Experience:**
1. Tap capture button
2. Shutter animation + flash effect (visual feedback)
3. Horizontal frame overlay freezes on captured frame
4. Loading screen appears: "AI Analysis in Progress..."
5. Progress indicator shows: "Detecting objects..." → "Analyzing 3 items..." → "Finalizing results"
6. Results screen displays structured JSON output

---

### 3. Manual Barcode Capture (Replacing Auto-Scan)
**Why Expected:** User control over when to scan, avoid accidental scans.  
**Complexity:** LOW  
**Status:** Auto-scan currently implemented in `ScanFragment.kt`  
**Implementation Notes:**
- Current implementation: Auto-detects barcode and auto-pops back (lines 143-172)
- Required change: Add manual capture button, remove auto-scan
- Keep barcode highlighting for visual feedback
- Remove flash/close buttons as per requirements

**User Experience:**
1. Point camera at barcode
2. See highlighting overlay when barcode detected
3. Tap "Capture" button to confirm scan
4. Barcode value returned to inventory screen

---

### 4. Dynamic Model Download from HuggingFace
**Why Expected:** Reduce APK size (models can be 1-2GB), download only when needed.  
**Complexity:** HIGH  
**Status:** Not implemented (models bundled in assets)  
**Implementation Notes:**
- Target model: `taobao-mnn/Qwen3.5-2B-MNN` (4-bit quantized, ~1-2GB)
- Download location: `context.filesDir/llm/`
- Use `huggingface_hub` Python SDK pattern or direct HTTP download
- Show download progress: percentage, speed, remaining time
- Handle network errors, resume capability

**User Experience:**
1. First scan attempt → "Model not downloaded"
2. Prompt: "Download AI model (1.2GB)? This will enable advanced food recognition."
3. Download progress screen: "Downloading 45% (540MB/1.2GB) - ~3 minutes remaining"
4. Auto-proceed to scanning once complete
5. Subsequent scans: instant startup (model cached locally)

---

## Differentiators

Features that set product apart. Not expected, but valued.

### 1. YOLO + LLM Batch Processing Pipeline
**Value Proposition:** Detect multiple foods, get structured expiry data for each item in one scan.  
**Complexity:** HIGH  
**Status:** Concept only (no existing implementation)  
**Implementation Notes:**
- Pipeline: `Camera Frame → YOLO Detection → Crop Multiple Regions → Batch LLM Inference → Structured JSON Output`
- Existing YOLO output: `[1, 300, 6]` array with `[x1, y1, x2, y2, confidence, class_id]`
- Need to implement:
  1. Crop detected bounding boxes from original bitmap
  2. Resize crops for LLM input (existing `resizeForVision` function in VisionScanFragment line 413)
  3. Batch process with progress tracking
  4. Aggregate results into structured JSON: `{ "items": [{ "name": "Apple", "expiry": "2026-04-15", "confidence": 0.92 }] }`

**User Experience:**
1. Point camera at fridge contents
2. YOLO detects: apple, milk, cheese
3. LLM analyzes each crop: "Apple - expires in 7 days", "Milk - expires in 3 days"
4. Results screen shows all items with suggested expiry dates
5. User can edit/confirm each item before saving to inventory

---

### 2. MNN Inference Engine (Replacing GGUF)
**Value Proposition:** 8.6x faster prefill, 2.3x faster decode than llama.cpp (per MNN docs).  
**Complexity:** HIGH  
**Status:** GGUF implementation exists in `LlamaBridge` (JNI-based)  
**Implementation Notes:**
- Official MNN Chat App reference: `/alibaba/MNN/tree/master/apps/Android/MnnLlmChat`
- Build flags: `-DMNN_BUILD_LLM=true -DMNN_LOW_MEMORY=true -DMNN_CPU_WEIGHT_DEQUANT_GEMM=true`
- Model files needed:
  - `llm.mnn` (model graph)
  - `llm.mnn.weight` (quantized weights)
  - `tokenizer.mtok` (tokenizer)
  - `config.json` (runtime config)
- Existing JNI build in `app/src/main/jni/CMakeLists.txt`

**User Experience:**
1. Faster cold start: Model loads in ~2-3 seconds (vs ~10s with GGUF)
2. Faster inference: ~1-2 tokens/second on mid-range devices (vs ~0.3-0.5 tok/s with GGUF)
3. Lower memory: Can run on devices with 4GB RAM
4. Better multi-turn: KV cache reuse for faster follow-up queries

---

### 3. Structured JSON Output from LLM
**Value Proposition:** Machine-readable results for seamless inventory integration.  
**Complexity:** MEDIUM  
**Status:** Partially implemented (regex parsing in VisionScanFragment lines 437-467)  
**Implementation Notes:**
- Prompt engineering: Instruct LLM to output JSON format
- Example prompt: `Analyze this food image. Reply with JSON: {"food": "name", "expiry": "YYYY-MM-DD", "confidence": 0.0-1.0}`
- Existing parse functions can be extended
- Fallback: Text parsing if JSON invalid

**User Experience:**
1. Scan result auto-populates inventory fields
2. Food name, category, suggested expiry date pre-filled
3. User confirms or edits before saving
4. No manual data entry needed

---

### 4. Horizontal (Landscape) Scan Frame
**Value Proposition:** Better framing for multiple food items, matches fridge layout.  
**Complexity:** LOW  
**Status:** Not implemented (current frame is portrait)  
**Implementation Notes:**
- Modify camera preview aspect ratio
- Add horizontal bounding box overlay in XML layout
- Crop captured image to frame bounds (existing crop function in VisionScanFragment line 232)

**User Experience:**
1. Scan screen shows horizontal guide frame
2. User arranges food items horizontally within frame
3. Better captures multiple items side-by-side
4. Matches natural fridge shelf arrangement

---

## Anti-Features

Features to explicitly NOT build.

### 1. CLIP Classifier Integration
**Why Avoid:** LLM already provides classification capability. Over-engineering.  
**What to Do Instead:** Use YOLO for detection + LLM for classification. Skip intermediate CLIP step.

### 2. GGUF Model Support (Legacy)
**Why Avoid:** Replaced by MNN for better performance. Maintaining two inference engines is wasteful.  
**What to Do Instead:** Complete migration to MNN, remove llama.cpp/GGUF code.

### 3. Auto-Scan Barcode
**Why Avoid:** User testing showed accidental scans frustrating. Users want control.  
**What to Do Instead:** Manual capture button with visual feedback.

### 4. Cloud-Based LLM Processing
**Why Avoid:** Privacy concerns, requires internet, adds latency/cost.  
**What to Do Instead:** Keep all inference on-device with MNN.

---

## Feature Dependencies

```
Dynamic Model Download → MNN LLM Inference → Structured JSON Output
                                         ↓
YOLO Multi-Object Detection → Batch Processing Pipeline → Multiple Inventory Items
                                         ↓
Horizontal Scan Frame → Better Multi-Item Capture → Improved Detection Accuracy
```

**Critical Path:**
1. Dynamic Model Download (foundation for MNN)
2. MNN Integration (enables fast inference)
3. Batch Processing Pipeline (combines YOLO + LLM)

---

## MVP Recommendation

**Phase 1: Core Infrastructure**
1. ✅ MNN LLM inference integration (replace GGUF)
2. ✅ Dynamic model download from HuggingFace
3. ✅ Structured JSON output parsing

**Phase 2: Enhanced Detection**
4. ✅ YOLO multi-object detection (already returns list)
5. ✅ Batch processing pipeline (crop → LLM → aggregate)
6. ✅ Horizontal scan frame UI

**Phase 3: UX Polish**
7. ✅ Capture animation + loading screen
8. ✅ Manual barcode capture redesign
9. ✅ Remove title bar from scan screens

**Defer:**
- Advanced model management (multi-model switching)
- Custom model training UI
- Real-time streaming LLM output

---

## Mobile UX Patterns

### Camera Permission Flow
```
App Launch → Check Permission → [Granted] → Open Camera
                            → [Denied] → Show Rationale → Request Permission
                                                      → [Denied Forever] → Settings Prompt
```

### Model Download Flow
```
First Scan → Check Model Exists → [No] → Prompt Download → Show Progress → Auto-Start Scan
                                 → [Yes] → Start Scan
```

### Batch Processing Flow
```
Capture Frame → YOLO Detect → [No Objects] → Show "No food detected" message
                            → [1 Object] → Single LLM analysis → Show result
                            → [2+ Objects] → Batch LLM analysis → Show progress → Show results list
```

---

## Performance Benchmarks (From MNN Docs)

**CPU Inference Comparison (Qwen-7B on Android):**
| Engine | Prefill Speedup | Decode Speedup |
|--------|-----------------|----------------|
| MNN-LLM | 8.6x vs llama.cpp | 2.3x vs llama.cpp |
| MNN-LLM | 20.5x vs fastllm | 8.9x vs fastllm |

**Expected Device Requirements:**
- Minimum: 4GB RAM, ARMv8 CPU
- Recommended: 6GB+ RAM, ARMv8.2 with FP16 support
- Model size: Qwen3.5-2B-MNN ~1-2GB (4-bit quantized)

---

## Integration with Existing Codebase

### Files to Modify
| File | Changes Needed |
|------|----------------|
| `LlmScanFragment.kt` | Replace `LlamaBridge` with MNN wrapper, add batch processing |
| `YoloScanFragment.kt` | Add batch crop + LLM pipeline, modify UI for multiple items |
| `ScanFragment.kt` | Add manual capture button, remove auto-scan logic |
| `YoloDetector.kt` | Keep existing, already returns list of detections |
| `build.gradle.kts` | Add MNN dependencies, remove llama.cpp deps |
| `nav_graph.xml` | Update scan destinations |

### Files to Create
| File | Purpose |
|------|---------|
| `MnnLlmWrapper.kt` | JNI wrapper for MNN inference engine |
| `BatchProcessor.kt` | Pipeline for YOLO → crop → LLM batch processing |
| `ModelDownloadService.kt` | Background service for HuggingFace downloads |
| `ScanResultAdapter.kt` | RecyclerView adapter for multiple scan results |

### Native Code
| File | Purpose |
|------|---------|
| `jni/CMakeLists.txt` | Add MNN library compilation |
| `jni/mnn_wrapper.cpp` | JNI bridge to MNN LLM runtime |

---

## Sources

1. **MNN GitHub Repository** - https://github.com/alibaba/MNN  
   Confidence: HIGH (official documentation, active development, 14.8k stars)

2. **MNN Chat Android App** - https://github.com/alibaba/MNN/blob/master/apps/Android/MnnLlmChat/README.md  
   Confidence: HIGH (reference implementation for Android LLM integration)

3. **MNN-LLM Documentation** - https://mnn-docs.readthedocs.io/en/latest/transformers/llm.html  
   Confidence: HIGH (official docs with configuration examples)

4. **Qwen3.5-2B-MNN Model** - https://huggingface.co/taobao-mnn/Qwen3.5-2B-MNN  
   Confidence: HIGH (official quantized model from Taobao team)

5. **Existing Codebase Analysis** - FoodExpiryApp source code  
   Confidence: HIGH (direct code inspection of YoloScanFragment, LlmScanFragment, VisionScanFragment)

6. **YOLO TFLite Export Pattern** - YOLO_FOOD_SCANNER_DOCS.md  
   Confidence: HIGH (project documentation for existing YOLO implementation)

---

## Open Questions

1. **Model Size vs Device Storage**: Should we prompt users to download over WiFi only? (1.2GB model)
2. **Batch Processing Limit**: Max items to process in one scan? (Performance consideration)
3. **Fallback Strategy**: If LLM inference fails, should we show YOLO labels only?
4. **Model Updates**: How to handle model version updates? (Background download + swap)

---

## Confidence Assessment

| Area | Confidence | Reason |
|------|------------|--------|
| MNN Integration | HIGH | Official Android app reference, detailed docs |
| YOLO Detection | HIGH | Already implemented and tested in codebase |
| Dynamic Download | MEDIUM | Standard HTTP pattern, but error handling complexity |
| Batch Processing | MEDIUM | Clear pipeline, but performance tuning needed |
| UI Overhaul | HIGH | Standard Android UI patterns, existing layouts |

---

*Last Updated: 2026-04-08*  
*Maintained by: Research Agent*