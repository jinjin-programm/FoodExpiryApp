# Phase 8: Remote Detection Hardening - Context

**Gathered:** 2026-04-18
**Status:** Ready for planning (v2 — updated after device testing)

<domain>
## Phase Boundary

讓 YOLO 掃描流程端到端正常運作。本地 ONNX Runtime 跑 YOLOE-26n-seg-pf 偵測物件 → 裁切 → 遠端 Ollama/LM Studio LLM 分類。包含：
1. ONNX Runtime + YOLOE-26n-seg-pf 替換 TFLite YOLO（本地物件偵測 + 裁切）
2. VisionScanFragment Single/Multi 切換按鈕（已實作，需配合新引擎調整）
3. TFLiteYoloEngine 改寫為 OnnxEoEngine（支援 YOLOE 輸出格式）
4. 程式碼品質修復（已完成 ✓）
5. Debug overlay 版本：實機測試時顯示邊界框，確認流程正確後移除
6. 單元測試 + 實機驗證

**架構演進：** MNN YOLO → TFLite YOLOv8n → **ONNX Runtime YOLOE-26n-seg-pf**
- TFLite YOLOv8n 已測試但因 tensor 格式假設錯誤，實機「no food items detected」
- YOLOE PF 無法匯出 TFLite（LRPC reshape bug），但 ONNX 匯出成功
- YOLOE-26n-seg-pf：4,585 類通用物件（無提示模式），16MB ONNX

</domain>

<decisions>
## Implementation Decisions

### 模型選擇：YOLOE-26n-seg-pf + ONNX Runtime
- **D-01:** YOLO 偵測改用 **YOLOE-26n-seg-pf ONNX**（16MB, 4,585 類，無提示模式）。TFLite 方案已擱置。
- **D-02:** 使用 **ONNX Runtime Mobile** 在 Android 上跑 ONNX 模型。新增 `onnxruntime-android` 依賴。
- **D-03:** 模型檔案：`yoloe-26n-seg-pf.onnx` 放入 `app/src/main/assets/`（16MB）。
- **D-04:** YOLOE 輸出格式：兩個 tensor —— `(1, 300, 38)`（detections: 300 個候選框 × 38 = 4 bbox + 32 mask + 1 score + 1 class）和 `(1, 32, 160, 160)`（masks）。我們只需要第一個 tensor 的 bbox + score 部分。
- **D-05:** YOLO 只做「通用物件偵測 + bounding box + 裁切」，不判斷是不是食物。食物分類交給遠端 LLM（Ollama / LM Studio）。
- **D-06:** Pipeline 流程：拍照 → YOLOE 偵測所有物件（本地 ONNX）→ 裁切 → 逐張送遠端 LLM → 結果確認 → 存入資料庫。

### ONNX Engine 實作
- **D-07:** 新建 `OnnxYoloEngine`（或重寫 `TFLiteYoloEngine`）實作 `YoloDetector` 介面。內部使用 ONNX Runtime `OrtSession`。
- **D-08:** `detect()` 後處理：
  - 讀取第一個輸出 tensor `(1, 300, 38)`
  - 每行前 4 個值是 bounding box（格式待確認：xyxy 或 xywh）
  - 第 33 個值是 confidence score
  - 過濾 confidence < 0.5
  - 取 top-8 detections
  - 縮放到原圖尺寸
  - 返回 `List<DetectionResult>`
- **D-09:** 現有 `YoloDetector` 介面不變（loadModel/detect/unloadModel/isModelLoaded）。
- **D-10:** `MnnYoloPostprocessor` 可繼續使用的純函數：`applyNms`, `cropDetection`, `mapLabelToCategory`。`parseDetections` 需根據 YOLOE 輸出格式調整。

### UI（已實作，不需修改）
- **D-11:** VisionScanFragment 已有 Single/Multi 切換按鈕（長方形方塊，選中橙色底）。✓ 已 commit。
- **D-12:** 預設模式為「單圖分析」。✓
- **D-13:** YOLO Scan tab（YoloScanFragment）保持不變。✓
- **D-14:** Multi-mode 流程：全自動（偵測 → 分類 → ConfirmationFragment）。✓

### Debug Overlay（開發者版本）
- **D-15:** 建立 **Debug 模式**：Multi-mode 偵測完成後，在原圖上繪製邊界框 + label + confidence，讓開發者在手機上清楚看到偵測結果。
- **D-16:** Debug overlay 使用 `DetectionOverlayView`（已有）。在 YOLO 偵測結果繪製完畢後停留 3 秒，然後再送 LLM 分類。
- **D-17:** Debug 模式通過 BuildConfig.DEBUG 或 DataStore 開關控制。正式版移除或隱藏。
- **D-18:** 開發者在 Logcat 也能看到偵測結果（TAG: OnnxYoloEngine，每個偵測的 label, confidence, bbox）。

### 程式碼品質修復（已完成 ✓）
- **D-19:** DetectionPipeline 改用 YoloDetector 介面 ✓
- **D-20:** Bitmap 雙重回收修復 ✓
- **D-21:** ConfirmationViewModel 錯誤回報 ✓
- **D-22:** 硬編碼顏色抽取到 colors.xml ✓
- **D-23:** 防禦性 bitmap 複製 ✓
- **D-24:** 取消傳播驗證 ✓

### 測試策略
- **D-25:** 實機測試流程：
  1. Build + install APK
  2. Single-mode 測試（確認不影響現有功能）
  3. Multi-mode 測試（拍照冰箱，確認 YOLOE 偵測到物件）
  4. Debug overlay 確認邊界框位置正確
  5. 確認 LLM 分類正常
- **D-26:** 單元測試：後處理純函數（applyNms, cropDetection）

### Agent's Discretion
- ONNX Runtime 具體初始化方式（OrtSession, OrtEnvironment）
- YOLOE 輸出 tensor 的精確解析（bbox 格式可能需要根據實際輸出微調）
- Debug overlay 的具體渲染邏輯（顏色、字體大小、停留時間）
- ONNX model 在 assets 中的載入方式
- 舊的 TFLite 相關程式碼（TFLiteYoloEngine, yolo11n_float32.tflite）的處理方式
- 測試檔案結構和命名

### Folded Todos
- Phase 8 Plan 03: Unit tests + device verification —— 納入本 phase
- YOLO Gallery: Wire gallery picker for YOLO batch scan —— 目前只有 UI shell

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### ONNX Runtime + YOLOE
- `app/src/main/java/com/example/foodexpiryapp/inference/tflite/YoloDetector.kt` — 介面（OnnxYoloEngine 應實作此介面）
- `app/src/main/java/com/example/foodexpiryapp/inference/tflite/TFLiteYoloEngine.kt` — 目前引擎（需改寫為 ONNX）
- `app/src/main/assets/yoloe-26n-seg-pf.onnx` — YOLOE 模型（需從專案根目錄複製）
- https://docs.ultralytics.com/models/yoloe/ — YOLOE 文檔（輸出格式、prompt-free 模式）
- https://onnxruntime.ai/docs/get-started/with-android.html — ONNX Runtime Android 整合

### 遠端 LLM 架構
- `app/src/main/java/com/example/foodexpiryapp/domain/client/FoodVisionClient.kt` — LLM 介面
- `app/src/main/java/com/example/foodexpiryapp/data/remote/ProviderConfig.kt` — 供應商切換邏輯

### Pipeline & ViewModel
- `app/src/main/java/com/example/foodexpiryapp/inference/pipeline/DetectionPipeline.kt` — 核心管線（已改用 YoloDetector）
- `app/src/main/java/com/example/foodexpiryapp/presentation/viewmodel/YoloScanViewModel.kt` — YOLO 掃描 ViewModel
- `app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloPostprocessor.kt` — 後處理純函數（applyNms, cropDetection）

### UI（已實作）
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/vision/VisionScanFragment.kt` — AI Vision Scan + Single/Multi toggle
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/yolo/DetectionOverlayView.kt` — 邊界框 overlay（debug 用）
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/yolo/YoloScanFragment.kt` — YOLO 掃描 Fragment

### DI & Architecture
- `app/src/main/java/com/example/foodexpiryapp/di/InferenceModule.kt` — Hilt DI 模組
- `.planning/codebase/ARCHITECTURE.md` — Clean Architecture 分層

### Requirements
- `.planning/REQUIREMENTS.md` §YOLO+LLM Food Detection (YOLO-01, YOLO-03)

### Prior Phase Context
- `.planning/phases/06-detection/06-CONTEXT.md` — DetectionPipeline、batch flow
- `.planning/phases/07-scan-ui/07-CONTEXT.md` — DetectionOverlayView、flash animation

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `YoloDetector` 介面：loadModel/detect/unloadModel/isModelLoaded — 不變，OnnxYoloEngine 直接實作
- `DetectionPipeline`：已改用 YoloDetector 介面，只需換引擎實作
- `FoodVisionClient` (Ollama + LM Studio)：遠端 LLM 分類已完全可用
- `DetectionOverlayView`：邊界框渲染已完成，可用於 debug overlay
- `VisionScanFragment`：Single/Multi toggle 已實作
- `MnnYoloPostprocessor.applyNms`, `cropDetection`：後處理純函數可繼續使用

### Established Patterns
- Clean Architecture: domain/repository/ 介面, data/repository/ 實作
- HiltViewModel + StateFlow 響應式 UI 狀態
- YoloDetector 介面 + Hilt DI 注入
- ModelLifecycleManager 防止 OOM（只同時載入一個重型模型）

### Integration Points
- `InferenceModule.kt`: 需要改為注入 OnnxYoloEngine
- `DetectionPipeline.kt`: 已用 YoloDetector 介面，不需改動
- `VisionScanFragment.kt`: Multi-mode 已接上 DetectionPipeline
- `assets/`: 需要放入 `yoloe-26n-seg-pf.onnx`（目前在工作目錄根目錄）

### 已知問題（實測結果）
- TFLite YOLOv8n 的 `detect()` 假設 3 個 NMS tensor 輸出，但實際模型輸出 `[1, 84, 8400]` → 全部判為 no detection
- YOLOE PF 無法匯出 TFLite（LRPC reshape bug in onnx2tf）→ 改用 ONNX Runtime
- Multi-mode toggle 的 UI 回饋感已改善（長方形方塊 + 橙色選中狀態）

</code_context>

<specifics>
## Specific Ideas

- 本地 ONNX Runtime 跑 YOLOE-26n-seg-pf（16MB, 4,585 類通用物件），遠端 LLM 做食物分類
- Debug overlay 版本：偵測後在原圖上顯示邊界框 + label + confidence，停留 3 秒，讓開發者確認偵測結果正確
- 確認流程正常後移除 debug overlay（或改為 hidden 開關）
- YOLOE 是無提示模型（prompt-free），不需要指定偵測什麼類別，自動偵測所有物件
- 只需要 bounding box + 裁切，不需要 YOLOE 的實例分割 mask

</specifics>

<deferred>
## Deferred Ideas

- YOLOE TFLite 版本：等 onnx2tf 修復 LRPC reshape bug 後，可考慮改回 TFLite（更小的 APK、更快的啟動）
- YOLO Gallery batch scan: gallery picker 目前只是 UI shell，需要接上 ONNX pipeline
- MNN 本地 LLM 推論: 已擱置
- DefaultAttributeEngine 和 ShelfLifeEstimator 的棄用/移除
- Shelf Life EditDialog 接線

### Reviewed Todos (not folded)
- Shelf Life: Unit tests for LookupShelfLifeUseCase cascade —— 屬於 Shelf Life 功能，非 Phase 8 範圍

</deferred>

---

*Phase: 08-yolo-hardening*
*Context gathered: 2026-04-18*
