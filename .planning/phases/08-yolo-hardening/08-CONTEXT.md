# Phase 8: Remote Detection Hardening - Context

**Gathered:** 2026-04-18
**Status:** Ready for planning

<domain>
## Phase Boundary

讓 YOLO 掃描流程基於「本地 TFLite YOLO + 遠端 Ollama/LM Studio LLM」架構正常運作。包含：
1. TFLite YOLOv8-nano 替換 MNN YOLO（本地物件偵測 + 裁切）
2. AI Vision Scan 畫面加切換按鈕（單圖分析 vs 多物件 YOLO 分析）
3. DetectionPipeline 重構（移除 MNN 依賴，改用 TFLite）
4. 程式碼品質修復（bitmap 安全、顏色抽取、錯誤回報、取消傳播）
5. 單元測試 + 實機驗證

MNN 本地推論（LLM 部分）已擱置，LLM 分類改用遠端 Ollama/LM Studio。YOLO 偵測保留本地執行但改用 TFLite。

</domain>

<decisions>
## Implementation Decisions

### 架構變更：MNN → TFLite YOLO + 遠端 LLM
- **D-01:** YOLO 偵測改用 **TFLite YOLOv8-nano**。MNN JNI bridge 方案已擱置。TFLite 在 Android 上成熟穩定，不需要自定義 JNI，模型約 ~6MB。
- **D-02:** YOLOv8-nano 模型從 ultralytics 導出為 TFLite 格式（float16），放入 APK assets。不需要動態下載。
- **D-03:** LLM 分類維持現有遠端架構（Ollama / LM Studio 雙供應商，FoodVisionClient 介面）。YOLO 只負責「物件偵測 + bounding box + 裁切」，不負責分類。
- **D-04:** 參考專案：https://github.com/tishuo-wang/YOLO_CLIP_targetDetection —— 只用 YOLO 的物件偵測能力，不需要 CLIP 分類（交給遠端 LLM）。
- **D-05:** Pipeline 流程不變：拍照 → YOLO 偵測（本地 TFLite）→ 裁切多張圖 → 逐張送遠端 LLM → 結果確認 → 存入資料庫。差別只在 YOLO 引擎從 MNN 換成 TFLite。

### UI 變更：掃描模式切換
- **D-06:** 在 AI Vision Scan（VisionScanFragment）畫面上加一個切換按鈕（ToggleButton 或 Chip），讓使用者在拍照前選擇模式：
  - **單圖分析**：現有行為，整張照片送 LLM 分析
  - **多物件分析**：先用本地 YOLO 偵測物件 + bounding box → 裁切 → 逐張送 LLM
- **D-07:** 預設模式為「單圖分析」。使用者選擇的模式用 DataStore 記住。
- **D-08:** YOLO Scan tab（YoloScanFragment）保持不變，它固定做 YOLO + LLM 多物件流程。切換按鈕只加在 VisionScanFragment。

### DetectionPipeline 重構
- **D-09:** 移除 `DetectionPipeline` 對 `MnnYoloEngine` 的依賴。新增 `TFLiteYoloEngine`（或類似名稱）作為 YOLO 引擎。
- **D-10:** `TFLiteYoloEngine` 實作相同介面/模式：`loadModel()` → `detect(bitmap)` → `unloadModel()`。內部使用 TFLite Interpreter。
- **D-11:** 現有 `MnnYoloEngine`、`MnnYoloNative`、`mnn_yolo_bridge.cpp` 可以保留（不刪除），但 DI 改為注入 TFLite 引擎。
- **D-12:** `MnnYoloPostprocessor` 的純函數（parseDetections, applyNms, cropDetection, mapLabelToCategory）繼續使用或根據 TFLite 輸出格式調整。研究員需確認 TFLite YOLOv8 的輸出 tensor 格式。

### 程式碼品質修復（沿用舊 D-10~D-14）
- **D-13:** 修復 `DetectionPipeline` 中 bitmap 雙重回收問題 —— `detection.cropBitmap?.recycle()` 可能回收仍在被 DetectionResult 引用的 bitmap。回收後將 reference 設為 null。
- **D-14:** 在 `YoloScanFragment.captureAndDetect()` 加入防禦性 bitmap 複製 —— `latestBitmap` 在推論期間可能被 camera frame callback 覆蓋/回收。
- **D-15:** 修復 `ConfirmationViewModel.saveAll()` 靜默吞錯誤（empty catch）—— 改為透過 `_saveResult` StateFlow 回報錯誤。
- **D-16:** 抽取 `DetectionResultAdapter` 中 6 個硬編碼 `Color.parseColor()` 到 `colors.xml`。
- **D-17:** 驗證 `cancelDetection()` 正確傳播取消到 pipeline coroutine。

### 測試策略
- **D-18:** 三階段測試：
  1. 手動實機測試（連接 Android 裝置，跑 YOLO 偵測，確認 Logcat 輸出）
  2. Agent 修復編譯/連結錯誤
  3. 單元測試：TFLite YOLO 後處理的純函數
- **D-19:** 在兩台 Android 裝置上測試，確認無硬體特定問題。
- **D-20:** 單元測試範圍：後處理純函數（parseDetections, applyNms, cropDetection, mapLabelToCategory）—— 不 mock 引擎，不依賴 native。

### Agent's Discretion
- TFLite YOLO engine 的具體實作（Interpreter 初始化、輸入 tensor 預處理、輸出 tensor 解析）
- YOLOv8-nano 模型的具體導出參數（input size、量化方式、confidence threshold）
- 切換按鈕的具體 UI 樣式（Chip vs ToggleButton vs SegmentedButton）
- 邊界框顏色的具體色值
- DetectionOverlayView 重繪策略
- Bitmap 生命週期管理方式
- 測試檔案結構和命名

### Folded Todos
- Phase 8 Plan 03: Unit tests + ViewModel detections + device verification —— 納入本 phase
- YOLO Gallery: Wire gallery picker for YOLO batch scan —— 目前只有 UI shell，需要接上 TFLite pipeline

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### TFLite YOLO Integration
- `app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloEngine.kt` — 目前 YOLO engine（需了解介面模式，TFLite engine 應遵循相同模式）
- `app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloPostprocessor.kt` — 後處理純函數（parseDetections, applyNms, cropDetection, mapLabelToCategory）
- `app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloConfig.kt` — YOLO 配置（inputSize, iouThreshold, maxDetections）
- https://github.com/tishuo-wang/YOLO_CLIP_targetDetection — 參考專案（只用 YOLO detection 部分）

### 遠端 LLM 架構
- `app/src/main/java/com/example/foodexpiryapp/domain/client/FoodVisionClient.kt` — LLM 介面（analyzeFood, testConnection）
- `app/src/main/java/com/example/foodexpiryapp/data/remote/ollama/OllamaVisionClient.kt` — Ollama 實作
- `app/src/main/java/com/example/foodexpiryapp/data/remote/lmstudio/LmStudioVisionClient.kt` — LM Studio 實作
- `app/src/main/java/com/example/foodexpiryapp/data/remote/ProviderConfig.kt` — 供應商切換邏輯

### Pipeline & ViewModel
- `app/src/main/java/com/example/foodexpiryapp/inference/pipeline/DetectionPipeline.kt` — 核心管線（需重構移除 MnnYoloEngine 依賴）
- `app/src/main/java/com/example/foodexpiryapp/presentation/viewmodel/YoloScanViewModel.kt` — YOLO 掃描 ViewModel
- `app/src/main/java/com/example/foodexpiryapp/presentation/viewmodel/ConfirmationViewModel.kt` — 確認 ViewModel（saveAll 錯誤處理需修復）

### UI
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/yolo/YoloScanFragment.kt` — YOLO 掃描 Fragment
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/yolo/DetectionOverlayView.kt` — 邊界框 overlay
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/vision/VisionScanFragment.kt` — AI Vision Scan（需加切換按鈕）
- `app/src/main/java/com/example/foodexpiryapp/presentation/adapter/DetectionResultAdapter.kt` — 硬編碼顏色需抽取
- `app/src/main/res/layout/fragment_vision_scan.xml` — VisionScan 佈局
- `app/src/main/res/values/colors.xml` — 顏色資源

### DI & Architecture
- `app/src/main/java/com/example/foodexpiryapp/di/InferenceModule.kt` — Hilt DI 模組
- `.planning/codebase/ARCHITECTURE.md` — Clean Architecture 分層
- `.planning/codebase/STRUCTURE.md` — 目錄結構
- `.planning/codebase/CONVENTIONS.md` — 程式碼慣例

### Requirements
- `.planning/REQUIREMENTS.md` §YOLO+LLM Food Detection (YOLO-01, YOLO-03) — Batch pipeline, bounding boxes

### Prior Phase Context
- `.planning/phases/05-engine/05-CONTEXT.md` — Phase 5 決策
- `.planning/phases/06-detection/06-CONTEXT.md` — Phase 6 決策（DetectionPipeline、batch flow、ConfirmationFragment）
- `.planning/phases/07-scan-ui/07-CONTEXT.md` — Phase 7 決策（full-bleed camera、DetectionOverlayView、flash animation）

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `DetectionPipeline`: 完整的管線架構（YOLO detect → crop → LLM classify），只需替換 YOLO engine
- `FoodVisionClient` (Ollama + LM Studio): 遠端 LLM 分類已完全可用
- `DetectionOverlayView`: 邊界框渲染已完成，setDetections() 已在 YoloScanFragment 中接線
- `YoloScanViewModel`: UI 狀態管理、pipeline 控制、結果持久化都已完成
- `YoloScanFragment`: 相機、overlay、gallery picker、進度狀態都已完成
- `MnnYoloPostprocessor`: 後處理純函數可繼續使用（或根據 TFLite 輸出調整）
- `ConfirmationFragment` + `ConfirmationViewModel`: 確認流程已完成

### Established Patterns
- Clean Architecture: domain/repository/ 介面, data/repository/ 實作
- HiltViewModel + StateFlow 響應式 UI 狀態
- FragmentResultListener 跨 Fragment 通訊
- ProviderConfig 雙供應商切換模式
- DataStore 使用者偏好持久化
- ViewBinding in all fragments

### Integration Points
- `DetectionPipeline` constructor: 需要替換 `MnnYoloEngine` → `TFLiteYoloEngine`
- `InferenceModule.kt`: Hilt DI 需要提供新的 TFLite engine
- `VisionScanFragment`: 需要加切換按鈕 UI + 根據模式啟動不同 pipeline
- `DetectionPipeline.kt:136`: bitmap 雙重回收需修復
- `ConfirmationViewModel.kt:104`: empty catch 需修復
- `DetectionResultAdapter`: 6 個硬編碼顏色需抽取

</code_context>

<specifics>
## Specific Ideas

- 本地 TFLite YOLOv8-nano 做物件偵測（~6MB 模型），遠端 Ollama/LM Studio 做食物分類
- 在 VisionScanFragment 加 ToggleButton/Chip 切換「單圖分析」vs「多物件 YOLO 分析」
- 參考專案 https://github.com/tishuo-wang/YOLO_CLIP_targetDetection 的 YOLO 偵測方式
- 只需要 YOLO 的物件偵測（bounding box + 裁切），不需要 CLIP 分類
- 流程：拍照 → 本地 YOLO 偵測 bounding boxes → 裁切 → 逐張送遠端 LLM → 確認 → 存入資料庫
- MNN 相關程式碼（MnnYoloEngine, MnnYoloNative, mnn_yolo_bridge.cpp）保留不刪除，但不再使用

</specifics>

<deferred>
## Deferred Ideas

- YOLO Gallery batch scan: YOLO scan tab 的 gallery picker 目前只是 UI shell，需要接上 TFLite pipeline —— 可納入本 phase 或後續
- MNN 本地 LLM 推論: 已擱置，未來可能回來
- DefaultAttributeEngine 和 ShelfLifeEstimator 的棄用/移除 —— 需要單元測試先完成
- Shelf Life EditDialog 接線 —— 需要 Shelf Life unit tests 先完成

### Reviewed Todos (not folded)
- Shelf Life: Unit tests for LookupShelfLifeUseCase cascade —— 屬於 Shelf Life 功能，非 Phase 8 範圍

</deferred>

---

*Phase: 08-yolo-hardening*
*Context gathered: 2026-04-18*
