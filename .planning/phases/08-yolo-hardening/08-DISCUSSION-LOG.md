# Phase 8: Remote Detection Hardening - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-18
**Phase:** 08-yolo-hardening
**Areas discussed:** Tensor 後處理重寫, 偵測覆蓋範圍, Multi-mode UX 流程, 模型選擇, Debug overlay

---

## 模型選擇（架構變更）

| Option | Description | Selected |
|--------|-------------|----------|
| TFLite YOLOv8-nano (10MB) | 成熟穩定，80 類 COCO | （已嘗試，實機失敗） |
| YOLOE-26n-seg-pf + TFLite | 4,585 類，無提示 | TFLite 匯出失敗（LRPC bug） |
| YOLOE-11s-seg-pf + TFLite | 4,621 類，無提示 | TFLite 匯出失敗（同上） |
| **YOLOE-26n-seg-pf + ONNX Runtime** | **16MB ONNX, 4,585 類** | **✓** |
| YOLOE-11s-seg-pf + ONNX | 46MB ONNX, 4,621 類 | |
| 遠端 YOLO 服務 | 不需本地模型，依賴網路 | |

**User's choice:** YOLOE-26n-seg-pf + ONNX Runtime
**Notes:** 用戶偏好 YOLOE 系列的無提示模式。TFLite 匯出失敗後改用 ONNX Runtime。選擇 26n（16MB）而非 11s（46MB）以控制 APK 大小。

---

## Tensor 後處理

| Option | Description | Selected |
|--------|-------------|----------|
| 完整後處理在 Engine 裡 | YOLOE 輸出解析 + NMS + 過濾全在 detect() 中 | ✓ |
| 抽取 Postprocessor 類 | 分離純函數方便測試 | |

**User's choice:** 後處理在 Engine 裡（Agent's Discretion 細節）
**Notes:** YOLOE 輸出 `(1, 300, 38)` + `(1, 32, 160, 160)`。只需讀第一個 tensor 的 bbox + score。已有 MnnYoloPostprocessor 純函數可複用。

---

## Multi-mode UX 流程

| Option | Description | Selected |
|--------|-------------|----------|
| 現有全自動流程 | 偵測 → 分類 → ConfirmationFragment | ✓ |
| 先顯示偵測結果再分類 | 多一步確認 | |

**User's choice:** 現有流程 + Debug overlay
**Notes:** 用戶要求一個「開發者版本」在偵測後顯示邊界框，確認流程正確後移除。正式版用現有全自動流程。

---

## Debug Overlay

| Option | Description | Selected |
|--------|-------------|----------|
| BuildConfig.DEBUG 控制 | Debug build 自動顯示 | ✓ |
| DataStore 開關 | 用戶可在設定中切換 | |
| 永久保留 | 正式版也保留 | |

**User's choice:** 開發者版本，確認後移除
**Notes:** 用 DetectionOverlayView 在原圖上繪製邊界框 + label + confidence，停留 3 秒讓開發者確認。

---

## Agent's Discretion

- ONNX Runtime 初始化方式（OrtSession, OrtEnvironment）
- YOLOE 輸出 tensor 精確解析
- Debug overlay 渲染邏輯
- 舊 TFLite 程式碼處理方式

## Deferred Ideas

- YOLOE TFLite 版本：等 onnx2tf 修復後可考慮改回
- YOLO Gallery batch scan：目前只有 UI shell
