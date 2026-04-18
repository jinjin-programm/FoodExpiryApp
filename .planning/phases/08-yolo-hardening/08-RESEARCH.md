# Phase 8 Research: TFLite YOLOv8 Integration

**Date:** 2026-04-18
**Phase:** 08-yolo-hardening

## Research Summary

Researching how to replace MNN YOLO with TFLite YOLOv8-nano for local object detection in the FoodExpiryApp Android project.

## 1. TFLite on Android

### Dependencies
```groovy
implementation "org.tensorflow:tensorflow-lite:2.14.0"
implementation "org.tensorflow:tensorflow-lite-support:0.4.4"
implementation "org.tensorflow:tensorflow-lite-gpu:2.14.0" // optional GPU delegate
```

### Interpreter Usage
- `Interpreter(modelByteBuffer, options)` — load model from assets
- `interpreter.run(inputBuffer, outputBuffer)` — single inference
- Options: setNumThreads(4), setUseNNAPI(true) for hardware acceleration
- Model loaded as MappedByteBuffer from AssetManager

### Best Practices
- Initialize Interpreter once (heavy operation ~100-500ms)
- Run inference on background thread (Dispatchers.IO)
- Input: ByteBuffer or float[][][][] array (batch=1, height, width, channels=3 RGB)
- Output: float[][][][] or float[][] depending on model

## 2. Exporting YOLOv8-nano to TFLite

### Export Command
```python
from ultralytics import YOLO
model = YOLO("yolov8n.pt")
model.export(format="tflite", imgsz=640, half=False)
# Creates: yolov8n_float32.tflite (~6-12MB)
```

### Export Options
- `imgsz=640` — standard input size
- `half=True` — float16 quantization (smaller, faster, slightly less accurate)
- `int8=True` — INT8 quantization (smallest, fastest, needs calibration data)
- `nms=True` — include NMS in model (simpler post-processing)

### Output Tensor Format (YOLOv8 TFLite without NMS)
- Output shape: `[1, 84, 8400]` for COCO 80 classes
  - 84 = 4 (bbox: x_center, y_center, width, height) + 80 (class scores)
  - 8400 = number of candidate detections
- For custom food model with N classes: `[1, 4+N, 8400]`
- Coordinates are in normalized format relative to input image

### Output Tensor Format (YOLOv8 TFLite with NMS)
- If exported with `nms=True`:
  - Output 1: `[1, num_detections, 4]` — bounding boxes (x1, y1, x2, y2)
  - Output 2: `[1, num_detections]` — class IDs
  - Output 3: `[1, num_detections]` — confidence scores
- **Recommendation:** Use `nms=True` for simpler post-processing. TFLite handles NMS internally.

### Conversion to MnnYoloPostprocessor Format
If using NMS export:
- Boxes already in (x1, y1, x2, y2) format — map directly to `DetectionResult.boundingBox`
- Confidence scores map to `DetectionResult.confidence`
- Class IDs map to `DetectionResult.classId`
- Scale coordinates from model input size (640x640) to original image size

If using non-NMS export:
- Need custom NMS implementation (already exists in `MnnYoloPostprocessor.applyNms`)
- Convert from (x_center, y_center, width, height) to (x1, y1, x2, y2)
- Apply confidence threshold filtering

## 3. TFLite Engine Architecture for This Project

### Interface Alignment
Current `MnnYoloEngine` has:
- `loadModel(): Boolean` — load model from assets
- `detect(bitmap: Bitmap): List<DetectionResult>` — run detection
- `unloadModel()` — release resources
- `isModelLoaded(): Boolean` — check state

Proposed `TFLiteYoloEngine`:
- Same interface pattern
- `loadModel()`: Initialize `Interpreter` from assets ByteBuffer
- `detect(bitmap)`: 
  1. Resize bitmap to 640x640
  2. Convert to ByteBuffer (RGB float32 normalized 0-1)
  3. `interpreter.run(input, output)`
  4. Post-process output tensors → List<DetectionResult>
- `unloadModel()`: Close interpreter, release ByteBuffer

### Model File
- Export YOLOv8n with `nms=True` to simplify post-processing
- Place `yolov8n.tflite` in `app/src/main/assets/`
- Update `MnnYoloConfig.modelAssetPath` or create new config class

### Preprocessing Steps
1. Resize input bitmap to 640x640 (maintain aspect ratio with letterboxing)
2. Convert to float array normalized to [0, 1]
3. Arrange as [1][640][640][3] (NHWC format)

### Postprocessing (with NMS export)
1. Read output tensors: boxes, class_ids, scores
2. Filter by confidence threshold (default 0.5)
3. Scale box coordinates from 640x640 → original image dimensions
4. Map to DetectionResult (boundingBox, confidence, classId, label, category)
5. Cap at maxDetections (8)

## 4. Hilt DI Integration

### Changes to InferenceModule
```kotlin
// Replace MnnYoloEngine binding with TFLiteYoloEngine
@Provides
@Singleton
fun provideYoloEngine(
    @ApplicationContext context: Context,
    config: YoloConfig // new config class
): TFLiteYoloEngine {
    return TFLiteYoloEngine(config, context)
}
```

### DetectionPipeline
- Constructor change: `MnnYoloEngine` → `TFLiteYoloEngine` (or interface)
- Consider extracting a `YoloDetector` interface to allow easy swapping

## 5. VisionScanFragment Toggle

### UI Approach
- Add a `MaterialButtonToggleGroup` or `ChipGroup` (single selection) below or above the camera preview
- Two options: "Single" / "Multi" (or icons)
- Selected mode stored in DataStore via existing SettingsDataStore pattern
- When "Multi" selected: after capture, run through DetectionPipeline instead of direct LLM call
- When "Single" selected: existing behavior (direct LLM via FoodVisionClient)

### Implementation
- Add toggle to `fragment_vision_scan.xml`
- Read preference in `captureAndDetect()` to decide pipeline
- If multi-mode: create/start DetectionPipeline (reuse existing infrastructure)
- Results navigate to same ConfirmationFragment

## 6. Model Selection

### YOLOv8n vs YOLOv8s
- YOLOv8n: ~3.2MB (int8) / ~6.2MB (float32) — fastest, good for mobile
- YOLOv8s: ~11.4MB (float32) — more accurate, still fast enough
- **Recommendation:** Start with YOLOv8n float32 (~6MB). If accuracy insufficient, upgrade to YOLOv8s.

### YOLOv10 (from reference project)
- The reference project uses yolov10b.pt and yolov10s.pt
- YOLOv10 is also supported by ultralytics for TFLite export
- Could use yolov10n.pt for comparable size/speed to yolov8n
- **Recommendation:** Use YOLOv8n first (more widely tested for TFLite on Android). Evaluate YOLOv10n later.

## 7. Risks and Gotchas

1. **TFLite output format varies by export version** — must test with actual exported model
2. **Model accuracy on food items** — YOLOv8n is trained on COCO (80 classes). For food-specific detection, may need a custom-trained model. COCO has some food classes (banana, apple, sandwich, orange, broccoli, carrot, hot dog, pizza, donut, cake).
3. **Preprocessing must match training** — normalization, color channel order (RGB vs BGR)
4. **Memory management** — TFLite Interpreter holds model in memory. Release when not scanning.
5. **Thread safety** — Interpreter is NOT thread-safe. Synchronize access or use single-threaded.
6. **NNAPI compatibility** — some devices have NNAPI bugs. Add fallback to CPU.
7. **Input image size** — letterboxing may affect detection. Test with real fridge photos.

## 8. Recommendations

1. **Export model with NMS included** (`nms=True`) — simpler post-processing
2. **Use float32 initially** — more compatible, debug first then optimize
3. **Start with COCO pretrained YOLOv8n** — food items like apple, banana, etc. already detected
4. **Extract `YoloDetector` interface** — decouple from TFLite implementation details
5. **Test on real device early** — TFLite behavior can differ between emulator and device
6. **Consider custom food model later** — train on food-specific dataset for better detection

---

*Research completed: 2026-04-18*
