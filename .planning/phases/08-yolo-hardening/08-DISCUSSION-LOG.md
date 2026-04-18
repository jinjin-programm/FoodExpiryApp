# Phase 8: Remote Detection Hardening - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-18
**Phase:** 08-yolo-hardening
**Areas discussed:** YOLO 偵測方案, UI 切換, Pipeline 重構, 程式碼品質, 測試策略

---

## YOLO 偵測方案

| Option | Description | Selected |
|--------|-------------|----------|
| TFLite YOLOv8-nano | 用 TFLite 跑 YOLO，Android 成熟、不需要自定義 JNI、~6MB 模型 | ✓ |
| 繼續 MNN JNI bridge | 沿用 MNN 格式完成 JNI bridge | |
| ONNX Runtime YOLO | 跨平台但 Android 支援不如 TFLite | |
| 遠端 Ollama YOLO | 在 Ollama 伺服器跑 YOLO，需額外部署 | |
| 遠端 LLM 直接做偵測 | 讓 LLM 回傳 bounding boxes + 名稱 | |
| 先擱置 YOLO | 只做程式碼品質清理 | |

**User's choice:** TFLite YOLOv8-nano
**Notes:** 本地跑 YOLO（只做偵測），遠端跑 LLM（做分類）。用戶確認本地 YOLO 資源佔用不多，可以在手機上跑。參考專案 tishuo-wang/YOLO_CLIP_targetDetection 只用 YOLO detection 部分，不需要 CLIP。

---

## UI 切換按鈕

| Option | Description | Selected |
|--------|-------------|----------|
| 掃描畫面上的切換按鈕 | ToggleButton/Chip，拍照前選擇單圖 or 多物件模式 | ✓ |
| 保持 ViewPager 分頁 | YOLO Scan 和 Photo Scan 分開 | |
| 設定中選擇 | Settings 中預設模式 | |

**User's choice:** 掃描畫面上的切換按鈕
**Notes:** 加在 VisionScanFragment 上。預設「單圖分析」。YOLO Scan tab（YoloScanFragment）保持不變。DataStore 記住選擇。

---

## Pipeline 重構範圍

| Option | Description | Selected |
|--------|-------------|----------|
| 替換 YOLO engine | 移除 MnnYoloEngine 依賴，改用 TFLiteYoloEngine | ✓ |

**User's choice:** 替換 YOLO engine
**Notes:** MnnYoloEngine/MnnYoloNative/mnn_yolo_bridge.cpp 保留不刪除。DI 改為注入 TFLite engine。MnnYoloPostprocessor 純函數繼續使用或根據 TFLite 輸出調整。

---

## 程式碼品質

| Option | Description | Selected |
|--------|-------------|----------|
| 全部納入 | 5 項修復全部做（bitmap 安全、顏色、錯誤回報、取消、bitmap 複製） | ✓ |
| 只做必要的 | 只做與重構相關的 | |
| 全部擱置 | 只做功能 | |

**User's choice:** 全部納入
**Notes:** 舊 CONTEXT 的 D-10~D-14 全部沿用。

---

## Agent's Discretion

- TFLite engine 具體實作（Interpreter、tensor 格式）
- YOLOv8-nano 導出參數
- 切換按鈕 UI 樣式
- 邊界框色值
- DetectionOverlayView 重繪策略
- 測試檔案結構

---

## Deferred Ideas

- YOLO Gallery batch scan (gallery picker UI shell 需接上 pipeline) —— 可能納入本 phase
- MNN 本地 LLM 推論 —— 已擱置
- Shelf Life unit tests —— 非 Phase 8 範圍
